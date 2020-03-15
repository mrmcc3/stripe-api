(ns mrmcc3.stripe.client.util.path
  (:require
    [clojure.string :as str]
    [mrmcc3.stripe.client.util.form-encoder :as form]))

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

(defn url [base-url path path-params query-params]
  (let [qs (form/encode query-params)]
    (str base-url
         (replace-in-path (subs path 1) path-params)
         (when (seq qs) (str "?" qs)))))
