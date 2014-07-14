(ns clojure-pta.routes.profile
  (:use compojure.core)
  (:require
    [noir.response :as response]
    [clojure-pta.views.layout :as layout]
    [clojure-pta.util :as util]
    [clojure-pta.private :as private]
    [clojure-pta.models.db :as db]
    [clj-http.client :as client]
    [taoensso.timbre :as timbre]))

(defn get-baskets-by-profile [profile_uri]
  "retrieve a basket given a profile uri"
  (try
    (client/get (str (util/get-pta-base-url) "baskets")
      {:basic-auth [(private/get-app-id) (private/get-app-secret)]
       :content-type :json
       :accept :json
       :query-params
         {:profile__eq profile_uri}})
  (catch Exception e (timbre/info e) nil)))

(defn profile-page
  [& {:keys [message]
    :or {message ""}}]
  "render the profile page"
    (layout/render "profile.html"
      {:user (db/get-user (util/get-user))
       :message message}))

(defn get-profile [uri]
  "get the profile given a profile uri"
  (client/get (str (util/get-pta-base-url) "profiles")
    {:basic-auth [(private/get-app-id) (private/get-app-secret)]
     :content-type :json
     :accept :json
     :query-params
       {:uri__eq uri}}))

(defn create-profile [email first_name last_name date_of_birth country gender]
  "create a profile"
  (try
    (client/post (str (util/get-pta-base-url) "profiles")
      {:basic-auth [(private/get-app-id) (private/get-app-secret)]
       :content-type :json
       :accept :json
       :form-params
         {:email email
          :first_name first_name
          :last_name last_name
          :date_of_birth date_of_birth
          :country country
          :gender gender}})
  (catch Exception e (timbre/info e) nil)))

(defn update-profile [email first_name last_name date_of_birth country gender]
  "update a profile"
  (try
    (let [uid (get (db/get-user-by-email email) :uid)]
      (let [url (str (util/get-pta-base-url) "profiles/" uid)]
        (client/put url
          {:basic-auth [(private/get-app-id) (private/get-app-secret)]
           :content-type :json
           :accept :json
           :form-params
             {:email email
              :first_name first_name
              :last_name last_name
              :date_of_birth date_of_birth
              :country country
              :gender gender}})))
  (catch Exception e (timbre/info e) nil)))

(defn login [email password user]
  (cond

    (empty? email)
    (util/error-response "Someone forgot to provide an email!")

    (empty? password)
    (util/error-response "Ops! Your secret please :)")

    :else
    (do
      (cond

        (not= email (get user :email))
        (util/error-response "Mmm, this email does not exist. Let's try again!")

        (not= password (get user :pass))
        (util/error-response "Did you forget your password?")

        :else
        (do
          (util/set-user (get user :id)))))))

(defn register [email first_name last_name pass date_of_birth country gender]
  (cond

    (empty? email)
    (profile-page :message "Someone forgot to provide an email!")

    ((complement nil?) (db/get-user-by-email email))
    (profile-page :message "Let me check... Ah, we already have this email :(")

    (empty? pass)
    (profile-page :message "Ops! Your secret please :)")

    (empty? first_name)
    (profile-page :message "And... What is your name?")

    (empty? last_name)
    (profile-page :message "Ohhh and your last name. Please!")

    (empty? date_of_birth)
    (profile-page :message "Born on...?")

    :else
    (do
      (let [profile (create-profile email first_name last_name date_of_birth country gender)]
        (cond

          (nil? profile)
          (profile-page :message "We are sorry.. Registration was not successful :(")

          :else
          (do
            (let [uid (util/get-resource-uid (get (util/parse-response profile) "uri"))]
              (let [revision (util/get-resource-revision (get (util/parse-response profile) "uri"))]
                (db/create-user email first_name last_name pass date_of_birth country gender uid revision)
                (login email pass (db/get-user-by-email email))
                (response/redirect "/profile")))))))))

(defn update [email first_name last_name pass date_of_birth country gender]
  (cond

    (empty? email)
    (profile-page :message "Someone forgot to provide an email!")

    (nil? (db/get-user-by-email email))
    (profile-page :message "You can't update your email :(")

    (empty? pass)
    (profile-page :message "Ops! Your secret please :)")

    (empty? first_name)
    (profile-page :message "And... What is your name?")

    (empty? last_name)
    (profile-page :message "Ohhh and your last name. Please!")

    (empty? date_of_birth)
    (profile-page :message "Born on...?")

    :else
    (do
      (let [user (db/get-user-by-email email)]
        (let [profile (update-profile email first_name last_name date_of_birth country gender)]
          (cond

            (nil? profile)
            (profile-page :message "We are sorry.. Updating was not successful :(")

            :else
            (let [uid (util/get-resource-uid (get (util/parse-response profile) "uri"))]
              (let [revision (util/get-resource-revision (get (util/parse-response profile) "uri"))]
                (db/update-user (get user :id) first_name last_name email pass date_of_birth country gender uid revision)
                (response/redirect "/profile")))))))))

(defroutes profile-routes
  (GET "/profile" [] (profile-page))
  (GET "/profile/logout" []
    (util/clear-session))
  (POST "/profile/login"
    [email password]
    (login email password (db/get-user-by-email email)))
  (GET "/profile/register" []
    (profile-page))
  (POST "/profile/register"
    [email first_name last_name password date_of_birth country gender]
    (register email first_name last_name password date_of_birth country gender))
  (POST "/profile/update"
    [email first_name last_name password date_of_birth country gender]
    (update email first_name last_name password date_of_birth country gender)))
