(ns mrmcc3.stripe.client.api
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [mrmcc3.stripe.client.http.api :as http]
    [mrmcc3.stripe.client.util.path :as path]))

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

(defn client [{:keys [http-client api-key]}]
  {:spec        (load-spec)
   :api-key     api-key
   :http-client (or http-client (default-http-client))})

(defn info [client]
  (select-keys (:spec client) [:version :sha]))

(defn ops [client]
  (-> client :spec :ops keys sort))

(defn doc [client op]
  (-> client :spec :ops op))

(defn invoke [client request]
  (let [{:keys [http-client spec api-key op params timeout]}
        (merge client request)
        {:keys [path method] :as op}
        (get-in spec [:ops op])
        path-params  (select-keys params (:path-params op))
        query-params (select-keys params (:query-params op))
        body-params  (select-keys params (:body-params op))]
    (http/send!
      http-client
      {:url     (path/url (:url spec) path path-params query-params)
       :method  (str/upper-case method)
       :body    body-params
       :timeout timeout
       :headers {"Authorization"  (str "Bearer " api-key)
                 "Stripe-Version" (:version spec)}})))
