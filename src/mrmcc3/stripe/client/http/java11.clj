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

(defn create-uri [url query-params]
  (let [qs  (form/encode query-params)
        uri (if (empty? qs) url (str url "?" qs))]
    (URI/create uri)))

(defn string-body [s]
  (HttpRequest$BodyPublishers/ofString s))

(defn with-content [req method form-data]
  (cond
    form-data
    (-> (.method req method (string-body (form/encode form-data)))
        (.header "Content-Type" "application/x-www-form-urlencoded"))
    :else
    (.method req method (HttpRequest$BodyPublishers/noBody))))

(defn request [{:keys [url query-params method headers form-data timeout]}]
  (-> (create-uri url query-params)
      (HttpRequest/newBuilder)
      (with-headers headers)
      (with-content method form-data)
      (.timeout (Duration/ofMillis (or timeout 10000)))
      (.build)))

(extend-type HttpClient
  http/Client
  (send! [client request-map]
    (-> client
        (.send
          (request request-map)
          (HttpResponse$BodyHandlers/ofString))
        (.body)
        (json/read-str))))

