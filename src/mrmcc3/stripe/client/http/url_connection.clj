(ns mrmcc3.stripe.client.http.url-connection
  (:require [mrmcc3.stripe.client.http.api :as http])
  (:import (java.net URL HttpURLConnection)))

(defn set-headers [conn headers]
  (doseq [[k v] headers]
    (.setRequestProperty conn k v)))

(defn set-content [^HttpURLConnection conn body]
  (when body
    (.setDoOutput conn true)
    (with-open [out (.getOutputStream conn)]
      (.write out (.getBytes body "UTF-8")))))

(defrecord URLClient []
  http/Client
  (send! [_ {:keys [url method headers body timeout]}]
    (let [conn ^HttpURLConnection (.openConnection (URL. url))]
      (try
        (doto conn
          (.setConnectTimeout timeout)
          (.setReadTimeout timeout)
          (.setUseCaches false)
          (set-headers headers)
          (.setRequestMethod method)
          (set-content body))
        (if (< 199 (.getResponseCode conn) 300)
          (slurp (.getInputStream conn))
          (slurp (.getErrorStream conn)))
        (finally (.disconnect conn))))))

(defn client []
  (URLClient.))
