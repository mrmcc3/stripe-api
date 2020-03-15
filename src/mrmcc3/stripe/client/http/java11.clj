(ns mrmcc3.stripe.client.http.java11
  (:require
    [clojure.data.json :as json]
    [mrmcc3.stripe.client.util.form-encoder :as form]
    [mrmcc3.stripe.client.http.api :as http])
  (:import
    (java.net URI)
    (java.net.http
      HttpClient HttpRequest HttpRequest$Builder HttpResponse
      HttpRequest$BodyPublishers HttpResponse$BodyHandlers)
    (java.time Duration)))

(defn client []
  (-> (HttpClient/newBuilder)
      (.connectTimeout (Duration/ofSeconds 10))
      (.build)))

(defn with-headers [req headers]
  (reduce-kv #(.header %1 (name %2) (str %3)) req headers))

(defn string-body [s]
  (HttpRequest$BodyPublishers/ofString s))

(defn with-content [req method body]
  (if body
    (-> (.method req method (string-body (form/encode body)))
        (.header "Content-Type" "application/x-www-form-urlencoded"))
    (.method req method (HttpRequest$BodyPublishers/noBody))))

(defn request [{:keys [url method headers body timeout]}]
  (-> (URI/create url)
      (HttpRequest/newBuilder)
      (with-headers headers)
      (with-content method body)
      (.timeout (Duration/ofMillis timeout))
      (.build)))

(extend-type HttpClient
  http/Client
  (send! [client request-map]
    (-> (.send
          client
          (request request-map)
          (HttpResponse$BodyHandlers/ofString))
        (.body)
        (json/read-str))))

