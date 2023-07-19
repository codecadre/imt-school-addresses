(ns file-writer
  (:import [java.util UUID])
  (:require [clojure.edn :as edn :refer [read-string]]
            [util :refer [string->uuid string->filesystem-path-ready latest-file sort-files-by-latest-first]]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [babashka.fs :as fs]
            [clojure.string :as str]
            [babashka.tasks :as tasks]))

(defn latest-snapshot []
  (latest-file (fs/glob "snapshots" "*.edn")))

(def whole-dataset (read-string (slurp (str (latest-snapshot)))))

(def now (java.util.Date.))

(def last-fetch-at-millis (edn/read-string (str/trim (slurp "last-fetch.txt"))))

(defn file-writer! []
  (doseq [{:keys [distrito concelho imt-href name] :as school} whole-dataset]
    (let [href-id (string->uuid imt-href)
          file-id (subs (str href-id) 0 8)
          filepath (format "parsed-data/%s/%s/%s-%s.edn"
                           (string->filesystem-path-ready distrito)
                           (string->filesystem-path-ready concelho)
                           file-id
                           (string->filesystem-path-ready name))
          school (assoc school
                        :href-id href-id)]
      (io/make-parents filepath)
      (spit filepath (with-out-str (pprint school))))))

(defn all-parsed-files []
  (fs/glob "parsed-data" "**/*.edn"))

(defn deleted-files []
  (filter #(< (-> % fs/last-modified-time fs/file-time->millis) (.getTime now)) (all-parsed-files)))

(defn add-archived-last-seen-at-key []
  (let [files (deleted-files)]
    (doseq [deleted-file files]
      (let [filepath (str deleted-file)
            data (read-string (slurp filepath))
            data (assoc data :archived-last-seen-at (str (fs/file-time->instant (fs/millis->file-time last-fetch-at-millis))))]
        (spit filepath (with-out-str (pprint data)))))
    (println "Archived: " (count files))))

(defn get-file-id [unix-path-object]
  (first (str/split (str (fs/file-name unix-path-object)) #"\.")))


(defn files-with-duplicates []
  (filter #(-> % last count (> 1))
          (group-by #(-> % get-file-id) (all-parsed-files))))

(defn delete-old-duplicate []
  (doseq [[file-id files-a-and-b] (files-with-duplicates)]
    (let [old-file (last (sort-files-by-latest-first files-a-and-b))]
      (fs/delete old-file))))

(defn stats []
  (let [everything-that-changed (str/split (:out (tasks/shell {:out :string} "git diff --cached --name-status parsed-data/*")) #"\n")
        new-entries (filter #(str/starts-with? % "A") everything-that-changed)
        modified (filter #(re-find #"M|R" %) everything-that-changed)
        clean-git-output (fn [l]
                           (str/replace (apply str (interpose " -> " (-> l (str/split #"\t") rest ))) #"parsed-data/" "/"))
        archived (map #(-> % str (str/replace #"parsed-data/" "/" ))
                      (filter #(-> % str slurp read-string :archived-last-seen-at
                                   (= (str (fs/file-time->instant (fs/millis->file-time last-fetch-at-millis)))))
                              (all-parsed-files)))]
    (println "\nNew entries: " (count new-entries))
    (doseq [l new-entries]
      (println (clean-git-output l)))
    (println "\nArchived: " (count archived))
    (doseq [l archived]
      (println (-> l str (str/replace "parsed-data/" "/") )))
    (println "\nEdited:" (- (count modified) (count archived)))
    (doseq [l modified]
      (when (not ((set archived) (clean-git-output l)))
        (println (clean-git-output l))))))

(defn -main []

  (file-writer!)

  ;;remove duplicates
  (delete-old-duplicate)

  (add-archived-last-seen-at-key)

  (tasks/shell "git add parsed-data/*")

  (spit "last-fetch.txt" (.getTime now)))
