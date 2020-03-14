(ns mrmcc3.stripe.client.webhook
  (:require
    [clojure.data.json :as json]
    [clojure.string :as str])
  (:import
    (javax.crypto Mac)
    (javax.crypto.spec SecretKeySpec)
    (java.security MessageDigest)))

(defn get-bytes [s]
  (.getBytes s "UTF-8"))

(defn s= [a b]
  (MessageDigest/isEqual (get-bytes a) (get-bytes b)))

(defn byte->hex [b]
  (-> (bit-and b 0xff) (+ 0x100) (Integer/toString 16) (subs 1)))

(defn hmac-sha256 [key msg]
  (let [hasher    (Mac/getInstance "HmacSHA256")]
    (.init hasher (SecretKeySpec. (get-bytes key) "HmacSHA256"))
    (->> (.doFinal hasher (get-bytes msg))
         (map byte->hex)
         (str/join))))

(defn parse-timestamp [t sig-header]
  (try
    (Long/parseLong t)
    (catch Exception _
      (-> "Unable to extract timestamp from header"
          (ex-info {:sig-header sig-header})
          (throw)))))

(defn add-pair [m pair]
  (apply assoc m (str/split pair #"=")))

(defn parse-header [sig-header]
  (let [{:strs [t v1]} (reduce add-pair {} (str/split sig-header #","))]
    (when-not v1
      (-> "No signatures found with expected scheme"
          (ex-info {:sig-header sig-header})
          (throw)))
    {:timestamp  (parse-timestamp t sig-header)
     :signatures (set (str/split v1 #","))}))

(defn validate
  ([payload sig-header secret]
   (validate payload sig-header secret 300))
  ([payload sig-header secret tolerance]
   (let [event              (json/read-str payload)
         {:keys [timestamp signatures]} (parse-header sig-header)
         signed-payload     (format "%d.%s" timestamp payload)
         expected-signature (hmac-sha256 secret signed-payload)
         time-now           (quot (System/currentTimeMillis) 1000)]
     (when-not (some (partial s= expected-signature) signatures)
       (-> "No signatures found matching the expected signature for payload"
           (ex-info {:sig-header sig-header})
           (throw)))
     (when-not (< 0 (- time-now timestamp) tolerance)
       (-> "Timestamp outside the tolerance zone"
           (ex-info {:sig-header sig-header})
           (throw)))
     event)))

