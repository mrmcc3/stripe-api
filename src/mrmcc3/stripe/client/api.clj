(ns mrmcc3.stripe.client.api
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [mrmcc3.stripe.client.http.api :as http]
    [mrmcc3.stripe.client.util.params :as params]
    [mrmcc3.stripe.client.util.response :as resp]
    [mrmcc3.stripe.client.util.form-encoder :as form])
  (:import (java.util UUID)))

(defn load-http-client [ns]
  (try
    (require ns)
    ((ns-resolve ns 'client))
    (catch Exception _ false)))

(defn default-http-client []
  (or (load-http-client 'mrmcc3.stripe.client.http.jetty)
      (load-http-client 'mrmcc3.stripe.client.http.java11)
      (load-http-client 'mrmcc3.stripe.client.http.url-connection)))

(defn load-spec []
  (let [path "mrmcc3/stripe/api/spec.edn"]
    (try
      (-> path io/resource slurp edn/read-string)
      (catch Exception ex
        (-> (format "Failed to load %s from classpath" path)
            (ex-info {:ex ex})
            throw)))))

;; public api

(defn client
  "Returns a stripe api client map. With the following keys

  :http-client - implements http/Client, used to make api requests
  :spec - a stripe api spec
  :api-key - a stripe api key
  :timeout - timeout in milliseconds for requests

  A map can be provided to bypass the following default behaviour
  :http-client - will try to load a suitable default implementation
  :spec - will try to load from the default classpath location
  :api-key - will use the STRIPE_API_KEY environment variable
  :timeout - 10 seconds"
  [{:keys [http-client spec api-key timeout]}]
  {:http-client (or http-client (default-http-client))
   :spec        (or spec (load-spec))
   :api-key     (or api-key (System/getenv "STRIPE_API_KEY"))
   :timeout     (or timeout 10000)})

(defn info
  "Returns a map with the following keys
  :version - the stripe api version in use
  :sha - the git commit sha that was used to generate the api spec"
  [client]
  (select-keys (:spec client) [:version :sha]))

(defn ops
  "A sorted list of all available operation (op) keywords"
  [client]
  (-> client :spec :ops keys sort))

(defn doc-str [client op param]
  (let [{:keys [method path params] :as op-map}
        (-> client :spec :ops op)]
    (when path
      (str "--------------------------------------\n"
           (format "%s -> %s %s" op method path) "\n\n"
           (:desc op-map "No docs.") "\n\n"

           (if-let [{:keys [req? desc] :as pm} (get params param)]
             (str "--------------------------------------\n"
                  :params " -> " param
                  (when req? " [REQUIRED]") "\n\n"
                  desc (when desc "\n\n")
                  (params/shape-str pm))
             (if param
               (str "--------------------------------------\n"
                    :params " -> " param "\n\n"
                    "Not Found!\n")
               (str "--------------------------------------\n"
                    :params "\n\n"
                    (params/shape-str
                      {:type :map :keys params})
                    (when-let [req (params/required params)]
                      (str "\nREQUIRED\n\n" (pr-str req) "\n")))))))))

(defn doc
  "Print the documentation for a given operation (op) keyword.
  An optional third argument (keyword) can be used to include
  additional info about a specific parameter."
  [client op & [param]]
  (println (doc-str client op param)))

(defn invoke
  "Invoke a client operation. `client` is a stripe api client map.
  `request` is a map containing :op the api operation to invoke.

  Other optional keys in the request map include:
  :params - a map of additional parameters. see doc for the given :op
  :api-key - stripe api key. overrides the client api-key
  :timeout - request timeout in milliseconds. overrides the client timeout
  :stripe-account - the account id to make request as connected account
  :idempotency-key - provide a key for idempotent requests"
  [client request]
  (let [{:keys [http-client spec api-key op params timeout
                stripe-account idempotency-key]}
        (merge client request)
        {:keys [path method] :as op-map}
        (get-in spec [:ops op])
        {:keys [in-path in-query in-body]}
        (params/group-params (:params op-map))
        path-params  (select-keys params in-path)
        query-params (select-keys params in-query)
        body-params  (select-keys params in-body)
        url          (params/gen-url
                       (:url spec) (subs path 1)
                       path-params query-params)
        i-key        (or idempotency-key
                         (and (= method "POST")
                              (str (UUID/randomUUID))))
        headers      (cond->
                       {"Accept"         "application/json"
                        "Accept-Charset" "UTF-8"
                        "Content-Type"   "application/x-www-form-urlencoded"
                        "Authorization"  (str "Bearer " api-key)
                        "Stripe-Version" (:version spec)}
                       stripe-account
                       (assoc "Stripe-Account" stripe-account)
                       i-key
                       (assoc "Idempotency-Key" i-key))
        request-map  {:url     url
                      :method  method
                      :headers headers
                      :body    (form/encode body-params)
                      :timeout timeout}]
    (-> (http/send! http-client request-map)
        resp/decode)))
