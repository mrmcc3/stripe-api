(ns user
  (:require
    [mrmcc3.stripe.client.api :as stripe]))

(comment

  (def client (stripe/client {}))

  (stripe/info client)

  (stripe/ops client)

  (stripe/doc client :GetCustomers)

  (stripe/invoke client {:op     :GetCustomers
                         :params {:limit   1
                                  :created {:gt 0}}})

  @(def cus (get-in *1 ["data" 0 "id"]))

  (stripe/doc client :PostCustomersCustomer)

  (stripe/invoke client {:op     :PostCustomersCustomer
                         :params {:customer cus
                                  :metadata {:order_id "6732"}}})

  (get *1 "metadata")

  )
