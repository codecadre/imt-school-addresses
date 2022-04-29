(ns clean-details
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.string :as str]))

(def d (-> "enrich-raw.edn" slurp edn/read-string))

(defn clean-name [s]
  (update s :title-raw str/trim))

(defn clean-nec [s]
  (update s :nec-raw #(edn/read-string (re-find #"\d+" %))))

(defn clean-address [s]
  (update s :address-raw str/trim))

(def results (->> d
                  (take 10)
                  (map clean-name)
                  (map clean-nec)
                  (map clean-address)
                  (map clean-name)))


#_(pprint/pprint results)

#_(map :title-raw results)


(spit "./enrich-clean.edn" (with-out-str (pprint/pprint results)))

(spit "./table.edn" (with-out-str (pprint/print-table results)))
