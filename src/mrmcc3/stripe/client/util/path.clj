(ns mrmcc3.stripe.client.util.path
  (:require
    [clojure.string :as str]))

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

