(ns util
  (:import [java.util UUID])
  (:require [clojure.string :as str]
            [babashka.fs :as fs]))

(defn rand-int-from-to
  "returns a random integer from to"
  [from to]
  (let [min-value from
        max-value to]
    (+ min-value (rand-int  (- max-value min-value)))))

;;UUID type 3
;;Version-3 UUIDs are based on an MD5 hash of the name and namespace.

(defn string->uuid [string]
  (UUID/nameUUIDFromBytes (.getBytes string)))

(defn string->filesystem-path-ready
  "clean up a string for the filesystem. ex:
  ' Faro Bla' -> 'faro-bla' "
  [string]
  (let [lower-case (-> string str/trim str/lower-case)
        no-diacritics (reduce (fn [s [pat repl]]
                                (str/replace s pat repl))
                              lower-case
                              [[#"[éèê]" "e"]
                               [#"[àáâãª]" "a"]
                               [#"[ú]" "u"]
                               [#"[í]" "i"]
                               [#"[óôõº]" "o"]
                               [#"[ç]" "c"]
                               [#"[&–-]" " "]
                               [#"[.,’]" ""]])
        word-array (-> no-diacritics (str/split #" "))
        word-array (remove empty? word-array)]
    (apply str (interpose #"-" word-array ))))

;;TODO maybe tests
(comment
  (map string->filesystem-path-ready ["idanha-a-nova"
                                      "A Desportiva - Espinho"
                                      "Saturno, Ldª"
                                      "Boavista - Feira"
                                      "A Mourinha de Stº Teotónio"
                                      "Prova Real- Arco de Baúlhe"
                                      "Arões"
                                      "Proença – A - Nova"
                                      "Moleiro, Amaro e Oliveira"
                                      "D’El Rei"
                                      "Infante de Sagres - Vila Real de Stº António"
                                      "Lago Azul –Figueiró dos Vinhos"
                                      "Infante de Sagres -Cascais"
                                      "Paço D’ Arcos"
                                      "Feira Nova – Ensino da Condução Automóvel"
                                      "A Desportiva – Palácio"
                                      "Escola de Condução SOUSA & BATISTA"]))

(defn now-dd-mm-yyyy []
  (.format
   (java.time.LocalDateTime/now)
   (java.time.format.DateTimeFormatter/ofPattern "dd_MM_yyyy")))

(defn sort-files-by-latest-first [files]
  (sort #(> (-> %1 fs/creation-time fs/file-time->millis)
            (-> %2 fs/creation-time fs/file-time->millis)) files))

(defn latest-file [files]
  (first (sort-files-by-latest-first files)))
