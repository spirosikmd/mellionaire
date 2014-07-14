(ns clojure-pta.routes.order
  (:use compojure.core)
  (:require
    [taoensso.timbre :as timbre]
    [clj-http.client :as client]
    [clojure-pta.views.layout :as layout]
    [clojure-pta.util :as util]
    [clojure-pta.private :as private]
    [clojure-pta.models.db :as db]
    [noir.util.route :as route]))

(defn create-order [basket_uri]
  "create an order given a basket uri"
  (try
    (client/post (str (util/get-pta-base-url) "orders")
      {:basic-auth [(private/get-app-id) (private/get-app-secret)]
       :content-type :json
       :accept :json
       :form-params
         {:basket basket_uri}})
  (catch Exception e (timbre/info e) nil)))

(defn get-orders-by-profile [profile_uri]
  "filter orders given a profile uri, which will return a list of orders for a specific user"
  (try
    (client/get (str (util/get-pta-base-url) "orders")
      {:basic-auth [(private/get-app-id) (private/get-app-secret)]
       :content-type :json
       :accept :json
       :query-params {"profile__eq" profile_uri}})
  (catch Exception e (timbre/info e) nil)))

(defn get-orders []
  "get a list of all completed orders"
  (let [user (db/get-user (util/get-user))]
    (cond

      ((complement nil?) user)
      (let [profile_uri (util/get-resource-uri-with-revision "profiles" (get user :uid) (get user :revision))]
        (util/parse-response (get-orders-by-profile profile_uri))))))

(defn orders-page
  [& {:keys [message]
    :or {message ""}}]
  "render the orders page"
    (layout/render "orders.html"
      {:user (db/get-user (util/get-user))
       :message message
       :orders (get-orders)}))

(defroutes order-routes
  (GET "/orders" [] (route/restricted (orders-page)))
  (POST "/order/create"
    [basket_uri]
    (create-order basket_uri)))

