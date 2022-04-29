(ns clean-details
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]))

(def d (-> "enrich-raw.edn" slurp edn/read-string))

(def results (take 10 d))

(def results d)

(spit "./enrich-clean.edn (with-out-str (pprint/pprint results))")
