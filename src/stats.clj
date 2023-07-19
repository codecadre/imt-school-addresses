(ns stats
  (:require [clojure.edn :as edn :refer [read-string]]
            [babashka.fs :as fs]
            [clojure.string :as str]
            [babashka.tasks :as tasks]))

(def last-fetch-at-millis (edn/read-string (str/trim (slurp "last-fetch.txt"))))

(defn all-parsed-files []
  (fs/glob "parsed-data" "**/*.edn"))

(defn -main []
  (let [everything-that-changed (str/split (:out (tasks/shell {:out :string} "git diff --cached --name-status parsed-data/*")) #"\n")
        new-entries (filter #(str/starts-with? % "A") everything-that-changed)
        modified (filter #(re-find #"M|R" %) everything-that-changed)
        clean-git-output (fn [l]
                           (str/replace (apply str (interpose " -> " (-> l (str/split #"\t") rest ))) #"parsed-data/" "/"))
        archived (map #(-> % str (str/replace #"parsed-data/" "/" )) (filter #(-> % str slurp read-string :archived-last-seen-at
                                                                                  (= (str (fs/file-time->instant (fs/millis->file-time last-fetch-at-millis)))))
                                                                             (all-parsed-files)))]
    (println "New entries: " (count new-entries))
    (doseq [l new-entries]
      (println (clean-git-output l)))
    (println "\nArchived: " (count archived))
    (doseq [l archived]
      (println (-> l str (str/replace "parsed-data/" "/") )))
    (println "\nEdited:" (- (count modified) (count archived)))
    (doseq [l modified]
      (when (not ((set archived) (clean-git-output l)))
        (println (clean-git-output l))))))
