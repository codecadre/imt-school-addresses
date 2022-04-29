(ns clean-details
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.string :as str]))

(def d (-> "enrich-raw.edn" slurp edn/read-string))

(defn clean-weirdness [s]
  (apply str (remove #(= % \ ) (seq s))))

(defn clean-name [s]
  (update s :title-raw #(clean-weirdness (str/trim %))))

(defn clean-nec [s]
  (update s :nec-raw #(edn/read-string (re-find #"\d+" %))))

(comment
  (clean-weirdness "Av Dr. António Caldas, loja 139, Faquelo  4970-592 Arcos de"))

(defn clean-address [s]
  (update s :address-raw #(clean-weirdness (str/trim %))))

(def results (->> d
                  (take 10)
                  (map clean-name)
                  (map clean-nec)
                  (map clean-address)))


#_(pprint/pprint results)

#_(map :title-raw results)


(spit "./enrich-clean.edn" (with-out-str (pprint/pprint results)))

(spit "./table.txt" (with-out-str (pprint/print-table results)))
