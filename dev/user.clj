(ns user
  (:require
    [mrmcc3.stripe.client.api :as stripe]))

(comment

  (def client
    (stripe/client {:api-key (System/getenv "STRIPE_API_KEY")}))

  (stripe/info client)

  (stripe/ops client)

  (stripe/doc client :GetCustomers)

  (stripe/invoke client {:op           :GetCustomers
                         :query-params {:limit 1}})

  @(def cus (get-in *1 ["data" 0 "id"]))

  (stripe/doc client :PostCustomersCustomer)

  (stripe/invoke client {:op          :PostCustomersCustomer
                         :path-params {:customer cus}
                         :form-data   {:metadata {:order_id "6732"}}})

  (get *1 "metadata")

  )
