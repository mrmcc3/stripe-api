(ns mrmcc3.stripe.client.http.java11
  (:require [mrmcc3.stripe.client.http.api :as http])
  (:import
    (java.net URI)
    (java.net.http
      HttpClient HttpRequest HttpRequest$Builder
      HttpRequest$BodyPublishers HttpResponse$BodyHandlers)
    (java.time Duration)))

(defn client []
  (HttpClient/newHttpClient))

(defn with-headers [req headers]
  (reduce-kv #(.header %1 (name %2) (str %3)) req headers))

(defn with-content [req method body]
  (if body
    (.method req method (HttpRequest$BodyPublishers/ofString body))
    (.method req method (HttpRequest$BodyPublishers/noBody))))

(defn request [{:keys [url method headers body timeout]}]
  (-> (URI/create url)
      (HttpRequest/newBuilder)
      (with-headers headers)
      (with-content method body)
      (.timeout (Duration/ofMillis timeout))
      (.build)))

(def of-string (HttpResponse$BodyHandlers/ofString))

(extend-type HttpClient
  http/Client
  (send! [client request-map]
    (.body (.send client (request request-map) of-string))))

