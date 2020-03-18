(ns mrmcc3.stripe.client.util.params
  (:require
    [clojure.pprint :as pprint]
    [clojure.string :as str]
    [mrmcc3.stripe.client.util.form-encoder :as form]))

;; url generation - path params & query params

(defn wrap-key [m k v]
  (assoc m (str "{" (name k) "}") (str v)))

(defn replace-in-path [path params]
  (let [smap (reduce-kv wrap-key {} params)]
    (->> (str/split path #"/")
         (replace smap)
         (map form/encode-url)
         (str/join "/"))))

(defn gen-url [base path-tpl path-params query-params]
  (let [qs   (form/encode query-params)
        path (replace-in-path path-tpl path-params)]
    (str base path (and qs "?") qs)))

;; group params from api spec by target (path, query or body)

(defn group-param [m p {:keys [in]}]
  (let [k (case in :path :in-path :query :in-query :in-body)]
    (update m k conj p)))

(defn group-params [params]
  (reduce-kv group-param {} params))

;; shape params from api spec for doc purposes

(defn shape [{:keys [type keys of opts]}]
  (case type
    :map (reduce-kv #(assoc %1 %2 (shape %3)) {} keys)
    :seq [:seq-of (shape of)]
    :enum (set opts)
    :one-of (into [:one-of] (map shape) opts)
    (symbol type)))

(defn shape-str [params]
  (with-out-str (pprint/pprint (shape params))))
