(ns congr.core
  (:require [etaoin.api :as api]
            [etaoin.api2 :refer [with-firefox-headless]]
            [clojure.data.json :as json]
            [clojure.string :as s])
  (:gen-class))

(defn get-album-title [ff album-entry]
  (->> {:class "album-title"}
       (api/child ff album-entry)
       (api/get-element-text-el ff)))

(defn get-album-image [ff base-url album-entry]
  (as-> album-entry $
    (api/child ff $ {:tag "img"})
    (api/get-element-attr-el ff $ :src)
    (s/replace $ #"\?.*" "")
    (str base-url $)))

(defn login [ff user pass]
  (api/fill-multi ff {:username user :password pass})
  (api/submit ff {:id "password"}))

(defn get-directory [base-url user pass]
  (with-firefox-headless [ff]
    (api/go ff (str base-url "/members/login/"))
    (login ff user pass)
    (Thread/sleep 5000) ;; TODO: improve
    (api/go ff (str base-url "/members/directory"))
    (->> {:class "album"}
         (api/query-all ff)
         (mapv (juxt (partial get-album-title ff)
                     (partial get-album-image ff base-url))))))

(defn -main [& args]
  (doseq [entry (apply get-directory args)]
    (println (json/write-str entry))))
