(ns mrmcc3.stripe.client.http.jetty
  (:require [mrmcc3.stripe.client.http.api :as http])
  (:import
    (org.eclipse.jetty.client HttpClient HttpRequest HttpContentResponse)
    (org.eclipse.jetty.client.util StringContentProvider)
    (org.eclipse.jetty.util.ssl SslContextFactory)
    (java.util.concurrent TimeUnit)))

(defn client []
  (doto (HttpClient. (SslContextFactory.)) (.start)))

(defn with-headers [req headers]
  (reduce-kv #(.header %1 (name %2) (str %3)) req headers))

(defn with-content [req s]
  (cond-> req s (.content (StringContentProvider. s))))

(defn request
  [client {:keys [url method headers body timeout]}]
  (-> (.newRequest client url)
      (.method method)
      (with-headers headers)
      (with-content body)
      (.timeout timeout TimeUnit/MILLISECONDS)))

(extend-type HttpClient
  http/Client
  (send! [client request-map]
    (-> (request client request-map)
        (.send)
        (.getContentAsString))))

