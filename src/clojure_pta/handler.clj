(ns clojure-pta.handler
  (:require [compojure.core :refer [defroutes]]
            [clojure-pta.routes.home :refer [home-routes]]
            [clojure-pta.routes.profile :refer [profile-routes]]
            [clojure-pta.routes.event :refer [event-routes]]
            [clojure-pta.routes.basket :refer [basket-routes]]
            [clojure-pta.routes.order :refer [order-routes]]
            [clojure-pta.middleware :as middleware]
            [clojure-pta.models.schema :as schema]
            [noir.util.middleware :refer [app-handler]]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.rotor :as rotor]
            [selmer.parser :as parser]
            [environ.core :refer [env]]))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (timbre/set-config!
    [:appenders :rotor]
    {:min-level :info
     :enabled? true
     :async? false ; should be always false for rotor
     :max-message-per-msecs nil
     :fn rotor/appender-fn})

  (timbre/set-config!
    [:shared-appender-config :rotor]
    {:path "clojure_pta.log" :max-size (* 512 1024) :backlog 10})

  (if (env :dev) (parser/cache-off!))

  ;;initialize the database if needed
  (if-not (schema/initialized?) (schema/create-tables))

  (timbre/info "clojure-pta started successfully"))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "clojure-pta is shutting down..."))

(def app (app-handler
           ;; add your application routes here
           [home-routes profile-routes event-routes basket-routes order-routes app-routes]
           ;; add custom middleware here
           :middleware [middleware/template-error-page
                        middleware/log-request]
           ;; add access rules here
           :access-rules []
           ;; serialize/deserialize the following data formats
           ;; available formats:
           ;; :json :json-kw :yaml :yaml-kw :edn :yaml-in-html
           :formats [:json-kw :edn]))
