(ns mrmcc3.stripe.client.http
  (:require
    [clojure.data.json :as json]
    [mrmcc3.stripe.client.form :as form])
  (:import
    (org.eclipse.jetty.client HttpClient HttpRequest HttpContentResponse)
    (org.eclipse.jetty.util.ssl SslContextFactory)
    (org.eclipse.jetty.client.util StringContentProvider)
    (java.util.concurrent TimeUnit)))

(defn with-headers [req headers]
  (reduce-kv #(.header %1 (name %2) (str %3)) req headers))

(defn with-params [req params]
  (reduce-kv #(.param %1 (name %2) (str %3)) req params))

(defn request
  [client {:keys [url query-params method headers form-data timeout]}]
  (cond-> (.newRequest client url)
    method (.method method)
    query-params (with-params query-params)
    headers (with-headers headers)
    form-data (.content
                (StringContentProvider. (form/encode form-data))
                "application/x-www-form-urlencoded")
    timeout (.timeout timeout TimeUnit/MILLISECONDS)))

(defn response-content [resp]
  (let [content (.getContentAsString resp)]
    (case (.getMediaType resp)
      "application/json" (json/read-str content)
      content)))

(defn send! [client request-map]
  (.send (request client request-map)))

(defn client []
  (doto (HttpClient. (SslContextFactory.)) (.start)))
