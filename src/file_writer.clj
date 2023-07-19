(ns file-writer
  (:import [java.util UUID])
  (:require [clojure.edn :as edn :refer [read-string]]
            [util :refer [string->uuid string->filesystem-path-ready]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [babashka.fs :as fs]
            [clojure.string :as str]))

(def whole-dataset (read-string (slurp "temp/db.edn")))

(def now (java.util.Date.))

(def last-fetch-at-millis (edn/read-string (str/trim (slurp "last-fetch.txt"))))

(defn file-writer! []
  (doseq [{:keys [distrito concelho imt-href] :as school} whole-dataset]
    (let [href-id (string->uuid imt-href)
          file-id (subs (str href-id) 0 8)
          filepath (str "parsed-data/" (string->filesystem-path-ready distrito) "/" (string->filesystem-path-ready concelho) "/" file-id ".edn")
          school (assoc school
                        :href-id href-id)]
      (io/make-parents filepath)
      (spit filepath (with-out-str (pprint school))))))

(defn all-parsed-files []
  (fs/glob "parsed-data" "**/*.edn"))

(defn deleted-files []
  (filter #(< (-> % fs/last-modified-time fs/file-time->millis) (.getTime now)) (all-parsed-files)))

(defn add-archived-last-seen-at-key []
  (doseq [deleted-file (deleted-files)]
    (let [filepath (str deleted-file)
          data (read-string (slurp filepath))
          data (assoc data :archived-last-seen-at (str (fs/file-time->instant (fs/millis->file-time last-fetch-at-millis))))]
      (spit filepath (with-out-str (pprint data))))))

(defn -main []

  (file-writer!)

  ;;remove duplicates

  (add-archived-last-seen-at-key)

  (spit "last-fetch.txt" (.getTime now)))


(defn get-file-id [unix-path-object]
  (subs (fs/file-name unix-path-object) 0 8))

(defn files-with-duplicates []
  (filter #(-> % last count (> 1)) (group-by #(-> % get-file-id)  (all-parsed-files))))

(count (files-with-duplicates))
(->> (files-with-duplicates)
     (map (fn [[file-id files-a-and-b]]
            (map #(read-string (slurp (str %))) files-a-and-b))))
