(ns schools
  (:require [cheshire.core :as json]
            [clojure.pprint :as pprint]
            [clojure.walk :refer [postwalk]]
            [babashka.curl :as curl]
            [babashka.pods :as pods]))

(pods/load-pod 'retrogradeorbit/bootleg "0.1.9")

(require
 '[pod.retrogradeorbit.bootleg.utils :as bootleg])

(def d (-> "./hrefs.json"
           slurp
           (json/parse-string true)))

(defn parse-nec-new
  "pull :content value if string? or if the first element is a string"[el]
  (let [a (atom [])]
    (postwalk (fn [{:keys [content] :as el}]
                (when (string? content)
                  (swap! a conj content))
                (when (string? (first content))
                  (swap! a conj (first content)))
                el)
              el)
    (apply str @a)))

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
                 (swap! r assoc :nec-raw (parse-nec-new el)))
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
  (Thread/sleep (int (rand 250))))

(spit "./schools.edn" (with-out-str (pprint/pprint @results)))


(def schools (-> "schools.edn" slurp edn/read-string))
