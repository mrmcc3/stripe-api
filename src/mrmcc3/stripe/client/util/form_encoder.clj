(ns mrmcc3.stripe.client.util.form-encoder
  (:require
    [clojure.string :as str])
  (:import
    (java.net URLEncoder)
    (java.util Date)))

(defn encode-url [^String s]
  (URLEncoder/encode s "UTF-8"))

(defn encode-key [k]
  (encode-url (if (instance? Long k) (str k) (name k))))

(defn key-by-path
  ([data] (key-by-path [] data))
  ([acc data]
   (cond
     ;; recurse into vectors + maps and collect keys
     (associative? data)
     (reduce-kv
       (fn [acc k v]
         (let [add-key #(update % 0 conj (encode-key k))]
           (into acc (map add-key) (key-by-path v))))
       acc
       data)

     ;; lists, sets, seqs -> vectors
     (coll? data)
     (key-by-path acc (into [] data))

     ;; dates -> timestamps
     (instance? Date data)
     [[nil (format "%d" (quot (.getTime data) 1000))]]

     ;; encode everything else
     :else
     [[nil (-> data str encode-url)]])))

(defn wrap-key [k]
  (str "[" k "]"))

(defn encode-pair [[[k & ks] value]]
  (when k
    (str (apply str k (map wrap-key ks)) "=" value)))

(defn encode [data]
  (->> (key-by-path data)
       (map encode-pair)
       (str/join "&")))
