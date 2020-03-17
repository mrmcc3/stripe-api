(ns mrmcc3.stripe.client.api
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [clojure.string :as str]
    [mrmcc3.stripe.client.http.api :as http]
    [mrmcc3.stripe.client.util.params :as params]
    [mrmcc3.stripe.client.util.response :as resp]))

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

(defn doc
  "Print the documentation for a given operation (op) keyword. WIP"
  [client op & args]
  (let [{:keys [method path desc params]}
        (-> client :spec :ops op)]
    (if desc
      (println
        (str/join
          "\n"
          (cond->
            ["--------------------------------------"
             (format "%s -> %s %s" op method path) ""
             desc ""]
            params
            (into ["--------------------------------------"
                   :params ""
                   (params/shape-str {:type :map :keys params})]))))
      (println "No docs for" op))))

(defn invoke
  "Invoke a client operation. `client` is a stripe api client map.
  `request` is a map containing :op the api operation to invoke.

  Other optional keys in the request map include:
  :params - a map of additional parameters. see doc for the given :op
  :api-key - stripe api key. overrides the client api-key
  :timeout - request timeout in milliseconds. overrides the client timeout"
  [client request]
  (let [{:keys [http-client spec api-key op params timeout]}
        (merge client request)
        {:keys [path method] :as op-map}
        (get-in spec [:ops op])
        {:keys [in-path in-query in-body]}
        (params/group-params (:params op-map))
        path-params  (select-keys params in-path)
        query-params (select-keys params in-query)
        body-params  (select-keys params in-body)
        request-map  {:url     (params/gen-url
                                 (:url spec) path
                                 path-params query-params)
                      :method  method
                      :body    body-params
                      :timeout timeout
                      :headers {"Authorization"  (str "Bearer " api-key)
                                "Stripe-Version" (:version spec)}}]
    (-> (http/send! http-client request-map)
        resp/decode)))
