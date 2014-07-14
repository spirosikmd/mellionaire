(ns clojure-pta.routes.home
  (:use compojure.core)
  (:require [clojure-pta.views.layout :as layout]
            [clojure-pta.util :as util]
            [clojure-pta.private :as private]
            [clj-http.client :as client]
            [clojure-pta.models.db :as db]))

(defn get-merchants []
  "get a list of merchants"
  (client/get (str (util/get-pta-base-url) "merchants")
    {:basic-auth [(private/get-app-id) (private/get-app-secret)]
     :content-type :json
     :accept :json}))

(defn home-page []
  "render the home page"
  (layout/render
    "home.html"
      {:content (util/md->html "/md/docs.md")
       :user (db/get-user (util/get-user))
       :merchants (util/parse-response (get-merchants))}))

(defroutes home-routes
  (GET "/" [] (home-page)))
