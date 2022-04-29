(ns pull-details
  (:require [cheshire.core :as json]
            [clojure.pprint :as pprint]))

(require '[babashka.pods :as pods])
(pods/load-pod 'retrogradeorbit/bootleg "0.1.9")
(require '[clojure.walk :refer [postwalk]])
(require '[babashka.curl :as curl])

(require
 '[babashka.fs :as fs]
 '[selmer.parser :as selmer] ; provided by Babashka
 '[pod.retrogradeorbit.bootleg.utils :as bootleg])

(def d (-> "./schools.json"
           slurp
           (json/parse-string true)))

(defn parse-nec
  "nec is problematic and shows up in different ways.
  These rules are 'low effort' and subject to change"
  [el]
  (let [first-level (-> el :content rest first :content first)
        parsed (cond
                 (string? first-level) first-level
                 (-> first-level :content count (= 1)) (-> first-level :content first)
                 :else (-> first-level :content last :content first))]
    (if parsed
      parsed
      el)))

(defn hiccup->school-raw [hc]
  (let [r (atom {})]
   (postwalk (fn [{:keys [attrs] :as el}]
               (when (= (:id attrs) "paginaConteudoDivTitulo")
                 (swap! r assoc :title-raw (-> el :content (nth 4))))
               (when (and (= (:class attrs) "paginaConteudoDivTextoSemImagem")
                          (= 1 (count (keys attrs))))
                 (swap! r assoc :address-raw (-> el :content first)))
               (when (and (= (:class attrs) "paginaConteudoDivTextoSemImagem")
                          (= 2 (count (keys attrs))))
                 (swap! r assoc :nec-raw (parse-nec el)))
               el) hc)
   @r))


(defn href->hc! [href]
  (let [html (curl/get href {:raw-args ["-k"]})
        hc (bootleg/convert-to (:body html) :hickory)]
    hc))

(def results (atom []))
(doseq [s d]
  (swap! results conj (merge s
                             (-> s
                                 :school-href
                                 href->hc!
                                 hiccup->school-raw)))
  (println "processed " (count @results) " schools.")
  (Thread/sleep (int (rand 500))))

(spit "./enrich-raw.edn" (with-out-str (pprint/pprint @results)))
