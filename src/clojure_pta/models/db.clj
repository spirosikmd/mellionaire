(ns clojure-pta.models.db
  (:use korma.core
        [korma.db :only (defdb)])
  (:require [clojure-pta.models.schema :as schema]))

(defdb db schema/db-spec)

(defentity users)

(defn create-user
  [email first_name last_name pass date_of_birth country gender uid revision]
  (insert users
          (values {:first_name first_name
                   :last_name last_name
                   :email email
                   :date_of_birth date_of_birth
                   :country country
                   :gender gender
                   :pass pass
                   :uid uid
                   :revision revision})))

(defn update-user
  [id first_name last_name email pass date_of_birth country gender uid revision]
  (update users
  (set-fields {:first_name first_name
               :last_name last_name
               :email email
               :date_of_birth date_of_birth
               :country country
               :gender gender
               :pass pass
               :uid uid
               :revision revision})
  (where {:id id})))

(defn get-user [id]
  (first (select users
                 (where {:id id})
                 (limit 1))))

(defn get-user-by-email [email]
  (first (select users
                 (where {:email email})
                 (limit 1))))
