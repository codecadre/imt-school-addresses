(ns util
  (:import [java.util UUID])
  (:require [clojure.string :as str]))

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
                               [#"[àáâã]" "a"]
                               [#"[ú]" "u"]
                               [#"[í]]" "i"]
                               [#"[óô]" "o"]
                               [#"[ç]" "c"]])]
    (apply str (interpose #"-" (-> no-diacritics (str/split #" ") )))))
