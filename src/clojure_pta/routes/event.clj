(ns clojure-pta.routes.event
  (:use compojure.core)
  (:use selmer.filters)
  (:require [clojure-pta.views.layout :as layout]
            [clojure-pta.util :as util]
            [clojure-pta.private :as private]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [clojure-pta.models.db :as db]
            [clj-http.client :as client]
            [taoensso.timbre :as timbre]))

(defn get-currency-symbol [currency]
  "get the currency symbol given a currency code"
  (get {"EUR" "€"} currency))

(add-filter! :get #(get %1 %2))

(add-filter! :price
  #(string/join [
    (get-currency-symbol (get %1 "currency"))
    (get %1 "amount")
    " "
    (get %1 "currency")]))

(defn get-events []
  "get a list of events"
  (client/get (str (util/get-pta-base-url) "events")
    {:basic-auth [(private/get-app-id) (private/get-app-secret)]
     :content-type :json
     :accept :json}))

(defn filter-events [eventUid]
  "filter events given an event uid"
  (client/get (str (util/get-pta-base-url) "events")
    {:basic-auth [(private/get-app-id) (private/get-app-secret)]
     :content-type :json
     :accept :json
     :query-params {"uri__eq" (util/get-resource-uri "events" eventUid)}}))

(defn get-products [eventUid]
  "get products given an event uid"
  (client/get (str (util/get-pta-base-url) "products")
    {:basic-auth [(private/get-app-id) (private/get-app-secret)]
     :content-type :json
     :accept :json
     :query-params {"event__eq" (util/get-resource-uri "events" eventUid)}}))

(defn filter-locations [locationUri]
  "filter locations give a location uri"
  (client/get (str (util/get-pta-base-url) "locations")
    {:basic-auth [(private/get-app-id) (private/get-app-secret)]
     :content-type :json
     :accept :json
     :query-params {"uri__eq" locationUri}}))

(defn events-page []
  "render the event list page"
  (layout/render
    "events.html"
    {:events (util/parse-response (get-events))
     :user (db/get-user (util/get-user))}))

(defn event-page [eventUid]
  "render the event page"
  (let [event (first (util/parse-response (filter-events eventUid)))]
    (let [locationUri (get event "location")]
      (layout/render
        "event.html"
          {:event event
           :products (util/parse-response (get-products eventUid))
           :location (first (util/parse-response (filter-locations locationUri)))
           :user (db/get-user (util/get-user))}))))

(defroutes event-routes
  (GET "/events" [] (events-page))
  (GET "/events/:eventUid" [eventUid] (event-page eventUid)))
