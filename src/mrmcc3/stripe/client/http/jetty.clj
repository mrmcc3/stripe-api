(ns mrmcc3.stripe.client.http.jetty
  (:require
    [clojure.data.json :as json]
    [mrmcc3.stripe.client.util.form-encoder :as form]
    [mrmcc3.stripe.client.http.api :as http])
  (:import
    (org.eclipse.jetty.client HttpClient HttpRequest HttpContentResponse)
    (org.eclipse.jetty.util.ssl SslContextFactory)
    (org.eclipse.jetty.client.util StringContentProvider)
    (java.util.concurrent TimeUnit)))

(defn client []
  (doto (HttpClient. (SslContextFactory.)) (.start)))

(defn with-headers [req headers]
  (reduce-kv #(.header %1 (name %2) (str %3)) req headers))

(defn with-params [req params]
  (reduce-kv #(.param %1 (name %2) (str %3)) req params))

(defn with-content [req form-data]
  (cond-> req
    form-data
    (.content
      (StringContentProvider. (form/encode form-data))
      "application/x-www-form-urlencoded")))

(defn request
  [client {:keys [url query-params method headers form-data timeout]}]
  (-> (.newRequest client url)
      (.method method)
      (with-params query-params)
      (with-headers headers)
      (with-content form-data)
      (.timeout (or timeout 10000) TimeUnit/MILLISECONDS)))

(extend-type HttpClient
  http/Client
  (send! [client request-map]
    (-> (request client request-map)
        (.send)
        (.getContentAsString)
        (json/read-str))))

