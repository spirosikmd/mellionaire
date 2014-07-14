(ns clojure-pta.routes.basket
  (:use compojure.core)
  (:require [taoensso.timbre :as timbre]
            [clj-http.client :as client]
            [clojure-pta.util :as util]
            [clojure-pta.private :as private]
            [clojure-pta.routes.profile :as profile]
            [clojure-pta.models.db :as db]))

(defn create-basket []
  "get the profile to relate to basket and then create a basket"
  (let [user (db/get-user (util/get-user))]
    (let [uri (util/get-resource-uri-with-revision "profiles" (get user :uid) (get user :revision))]
      (let [profile (first (util/parse-response (profile/get-profile uri)))]
        (try
          (client/post (str (util/get-pta-base-url) "baskets")
            {:basic-auth [(private/get-app-id) (private/get-app-secret)]
             :content-type :json
             :accept :json
             :form-params
               {:profile (get profile "uri")}})
        (catch Exception e (timbre/info e) nil))))))

(defn create-ticket [basket_uri product_uri]
  "create a ticket given a basket uri and a product uri"
  (try
    (client/post (str (util/get-pta-base-url) "tickets")
      {:basic-auth [(private/get-app-id) (private/get-app-secret)]
       :content-type :json
       :accept :json
       :form-params
         {:basket basket_uri
          :product product_uri}})
  (catch Exception e (timbre/info e) nil)))

(defn get-basket [basket_uri]
  "retrieve a basket given a basket uri"
  (try
    (let [basket_uid (util/get-resource-uid basket_uri)]
      (client/get (str (util/get-pta-base-url) "baskets/" basket_uid)
        {:basic-auth [(private/get-app-id) (private/get-app-secret)]
         :content-type :json
         :accept :json}))
  (catch Exception e (timbre/info e) nil)))

(defn confirm-basket [data]
  "first create basket, then add the products to basket and then confirm it"
  (let [basket_uri ((util/parse-response (create-basket)) "uri")]
    (doseq [[uid product] (data :products)]
      (let [product_uri (util/get-resource-uri "products" (get product :uid))]
        (create-ticket basket_uri product_uri)))
    (try
      (let [basket_uid (util/get-resource-uid basket_uri)]
        (client/post (str (util/get-pta-base-url) "baskets/" basket_uid "/confirm")
          {:basic-auth [(private/get-app-id) (private/get-app-secret)]
           :content-type :json
           :accept :json}))
    (catch Exception e (timbre/info e) nil))))

(defroutes basket-routes
  (POST "/basket/confirm"
    [data]
    (confirm-basket data)))
