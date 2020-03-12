(ns mrmcc3.stripe.client.api
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [mrmcc3.stripe.client.http :as http]))

;; utils for path parameter replacement

(defn params->smap [params]
  (reduce-kv
    #(assoc %1
       (str "{" (name %2) "}")
       (str %3))
    {}
    params))

(defn replace-in-path [path params]
  (->> (str/split path #"/")
       (replace (params->smap params))
       (str/join "/")))

(defn gen-url [base path params]
  (str base (replace-in-path (subs path 1) params)))

;; try loading the stripe api spec from the classpath

(defn load-spec []
  (let [path "mrmcc3/stripe/api/spec.edn"]
    (try
      (-> path io/resource slurp edn/read-string)
      (catch Exception ex
        (-> (format "Failed to load %s from classpath" path)
            (ex-info {:ex ex})
            throw)))))

;; public api

(defn client [{:keys [api-key]}]
  {:http-client (http/client)
   :spec        (load-spec)
   :api-key     api-key})

(defn info [client]
  (select-keys (:spec client) [:version :sha]))

(defn ops [client]
  (-> client :spec :ops keys sort))

(defn doc [client op]
  (-> client :spec :ops op))

(defn invoke [client request]
  (let [{:keys [http-client spec api-key
                op query-params path-params
                timeout form-data]}
        (merge client request)

        {:keys [path method]}
        (get-in spec [:ops op])

        http-request
        {:url          (gen-url (:url spec) path path-params)
         :method       method
         :headers      {"Authorization"  (str "Bearer " api-key)
                        "Stripe-Version" (:version spec)}
         :query-params query-params
         :timeout      timeout
         :form-data    form-data}]
    (-> http-client
        (http/send! http-request)
        http/response-content)))

