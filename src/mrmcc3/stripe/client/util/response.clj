(ns mrmcc3.stripe.client.util.response
  (:require [clojure.data.json :as json]))

(defn remove-nil [_ v]
  (if (nil? v) remove-nil v))

(defn decode [s]
  (json/read-str s :key-fn keyword :value-fn remove-nil))
