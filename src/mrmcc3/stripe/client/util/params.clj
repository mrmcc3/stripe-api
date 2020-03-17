(ns mrmcc3.stripe.client.util.params
  (:require
    [clojure.pprint :as pprint]
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

;; TODO make sure no {var} left in path
(defn gen-url [base-url path path-params query-params]
  (let [qs (form/encode query-params)]
    (str base-url
         (replace-in-path (subs path 1) path-params)
         (when (seq qs) (str "?" qs)))))

(defn group-param [m p {:keys [in]}]
  (let [k (case in :path :in-path :query :in-query :in-body)]
    (update m k conj p)))

(defn group-params [params]
  (reduce-kv group-param {} params))

(defn shape [{:keys [type keys of opts]}]
  (case type
    :map (reduce-kv #(assoc %1 %2 (shape %3)) {} keys)
    :seq [:seq-of (shape of)]
    :enum (set opts)
    :one-of (into [:one-of] (map shape) opts)
    (symbol type)))

(defn shape-str [params]
  (with-out-str (pprint/pprint (shape params))))
