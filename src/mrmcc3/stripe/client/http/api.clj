(ns mrmcc3.stripe.client.http.api)

(defprotocol Client
  (send! [o request-map]))
