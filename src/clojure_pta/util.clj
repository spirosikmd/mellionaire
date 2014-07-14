(ns clojure-pta.util
  (:require [noir.io :as io]
            [markdown.core :as md]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [noir.session :as session]
            [noir.response :as response]
            [taoensso.timbre :as timbre]))

(defn md->html [filename]
  "reads a markdown file from public/md and returns an HTML string"
  (->>
    (io/slurp-resource filename)
    (md/md-to-html-string)))

(defn parse-response [response]
  "gets the body of the response and converts it to json object"
  (json/read-str (get response :body)))

(defn error-response [message]
  "return an error json response with a message"
  (response/json {:error true, :message message}))

(defn get-pta-base-url []
  "returns the base pta url with a resource"
  "https://ticketapi.sandbox.paylogic.com/")

(defn get-resource-uri [resource uid]
  "create the resource uri given the resource and the uid"
  (str (get-pta-base-url) resource "/" uid))

(defn get-resource-uri-with-revision [resource uid revision]
  "get the resource uri with a revision"
  (str (get-pta-base-url) resource "/" uid "/revisions/" revision))

(defn get-resource-uid [uri]
  "get the resource uid given a resource uri"
  (nth (string/split uri #"/") 4))

(defn get-resource-revision [uri]
  "get the resource revision given a resource uri"
  (nth (string/split uri #"/") 6))

(defn set-user [email]
  "set the user in session using the email"
  (session/put! :user email)
  (session/get :user))

(defn get-user []
  "get the session user"
  (session/get :user))

(defn remove-user []
  "remove the user from session"
  (session/remove! :user)
  (session/get :user))

(defn clear-session []
  "clear the session"
  (session/clear!))

(defn get-currency-symbol [currency]
  "get the currency symbol given a currency code"
  (get {"EUR" "€"} currency))
