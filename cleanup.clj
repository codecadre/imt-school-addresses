(ns cleanup
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [cheshire.core :as json]))

(def d (-> "schools.edn" slurp edn/read-string))

(defn clean-weirdness [s]
  (apply str (remove #(= % \ ) (seq s))))

(defn clean-name [s]
  (-> s
      (assoc :title-clean (:title-raw s))
      (update :title-clean #(clean-weirdness (str/trim %)))
      (dissoc :title-raw)
      ))

(defn clean-nec [s]
  (update s :nec-raw
          (fn [nec-raw]
            (if nec-raw
              (edn/read-string (re-find #"\d+" nec-raw))
              nil))))

(comment
  (clean-weirdness "Av Dr. António Caldas, loja 139, Faquelo  4970-592 Arcos de"))

(defn clean-address [s]
  (->
   s
   (assoc :address-clean (:address-raw s))
   (update :address-clean #(clean-weirdness (str/trim %)))
   (dissoc :address-raw)))

(defn parse-cp7 [s]
  (-> s
      (assoc :cp7 (:address-clean s))
      (update :cp7 (fn [address]
                     (let  [cp7 (first (re-find #"\d{4}(–|-|\s-\s|\s-|-\s)(\d{3}|\d{2})" address))
                            cp7-4 (when cp7 (re-find #"^\d{4}(?=\D)" cp7))
                            cp7-3 (when cp7 (first (re-find #"(?<=\D)(\d{3}|\d{2})" cp7)))]
                       (if cp7
                         (apply str [cp7-4 "-" cp7-3])
                         (re-find #"\d{4}" address)))))))

(def results (->> d
                  #_(take 304)
                  (map clean-name)
                  (map clean-nec)
                  (map clean-address)
                  (map parse-cp7)
                  (sort #(compare (:nec-raw %1) (:nec-raw %2)))
                  doall))


(spit "./parsed-data/db.edn" (with-out-str (pprint/pprint results)))
(spit "./parsed-data/db.json" (json/generate-string results {:pretty true}))
(spit "./parsed-data/db.txt" (with-out-str (pprint/print-table results)))
