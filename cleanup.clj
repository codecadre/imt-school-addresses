(ns cleanup
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [cheshire.core :as json]
            [clojure.set :as set]))

(def d (-> "schools.edn" slurp edn/read-string))

(defn sort-schools [data]
  (sort-by :imt-href data))

(defn clean-weirdness [s]
  (apply str (remove #(= % \ ) (seq s))))

(defn clean-name [s]
  (-> s
      (assoc :title-clean (:title-raw s))
      (update :title-clean #(clean-weirdness (str/trim %)))
      (dissoc :title-raw)
      ))

(defn clean-nec [s]
  (-> s
   (update :nec-raw
           (fn [nec-raw]
             (if nec-raw
               (edn/read-string (re-find #"\d+" nec-raw))
               nil)))))

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


(defn string->uuid
  "deterministic ID
  cljs version based on core/random-uuid with a few changes to use the seedrandom
  WARNING: This is different than just adding a #uuid to a string"
  [string]
  #?(:clj (java.util.UUID/fromString (.toString (java.util.UUID/nameUUIDFromBytes (.getBytes string))))
     :cljs (letfn [(random-fn [v] ((seedrandom (str string v))))
                   (det-rand-int [i n] (Math/floor (* (random-fn i) n)))
                   (hex [i] (.toString (det-rand-int i 16) 16))]
             (let [rhex (.toString (bit-or 0x8 (bit-and 0x3 (det-rand-int 0 16))) 16)]
               (uuid
                (str (hex 1)  (hex 2)  (hex 3)  (hex 4)
                     (hex 5)  (hex 6)  (hex 7)  (hex 8) "-"
                     (hex 9)  (hex 10) (hex 11) (hex 12) "-"
                     "4"      (hex 13) (hex 14) (hex 15) "-"
                     rhex     (hex 16) (hex 17) (hex 18) "-"
                     (hex 19) (hex 20) (hex 21) (hex 22)
                     (hex 23) (hex 24) (hex 25) (hex 26)
                     (hex 27) (hex 28) (hex 19) (hex 30)))))))

(defn add-uuid [{:keys [address-clean nec-raw title-clean] :as s}]
  (assoc s :id (string->uuid (str title-clean address-clean nec-raw))))

(defn overwrites [data-set]
  (reduce
   (fn [acc [update-id updates]]
     (map (fn [{:keys [id] :as school}]
            (if (= id update-id)
              (merge school updates)
              school)) acc)) data-set (-> "overwrites.edn" slurp edn/read-string)))

(def results (->> d
                  #_(take 304)
                  (map clean-name)
                  (map clean-nec)
                  (map clean-address)
                  (map add-uuid)
                  overwrites
                  (map parse-cp7)
                  (map #(set/rename-keys % {:nec-raw :nec :address-clean :address :title-clean :name :school-href :imt-href}))
                  (sort-by :cp7);;sort by cp7 and then name before sorting by nec "para desempatar" when nec is the equal
                  (sort-by :name)
                  (sort #(compare (:nec %1) (:nec %2)))
                  doall))

(def nec-duplicates
  (map first (filter #(> (last %) 1) (map #(vector (first %) (count (last %))) (group-by :nec results)))))
;; ([nil 3]
;;  [851 3]
;;  [964 2]
;;  [352 2]
;;  [1077 2]
;;  [131 2]
;;  [1314 2]
;;  [1079 2]
;;  [1318 2])

(def duplicates (filter #(or ((set nec-duplicates) (:nec %)) (nil? (:nec %))) results))

(def ks
  [:id
   :nec
   :name
   :address
   :distrito
   :concelho
   :cp7
   :concelho-href
   :imt-href])

(spit "./duplicates.txt" (with-out-str
                            (pprint/print-table [:id :nec :name :address :imt-href]
                                                (sort-schools duplicates))))

(spit "./parsed-data/db.edn" (with-out-str (pprint/pprint (sort-schools results))))
(spit "./parsed-data/db.json" (json/generate-string (sort-schools results) {:pretty true}))
(spit "./parsed-data/db.txt" (with-out-str (pprint/print-table ks (sort-schools results))))

(comment
  (count results) ;;1153

  (count (set results)) ;;1153

  (count (set (map :nec results)));; 1142
  )



(comment
  ;;sorting hrefs.json
  (def my-pretty-printer (cheshire.core/create-pretty-printer
                          (assoc cheshire.core/default-pretty-print-options
                                 :indent-arrays? true
                                 :object-field-value-separator ": "))
    )

 (-> (cheshire.core/parse-string (slurp "hrefs.json"))
     (as-> x (sort-by #(get % "school-href") x))
     (cheshire.core/generate-string {:pretty my-pretty-printer})
     (as-> xs (spit "hrefs.json" xs))
     )

 (-> [10 11]
     (conj 12)
     (as-> xs (map - xs [3 2 1]))
     (reverse))
 )

(comment
  (def schools (-> "schools.edn" slurp edn/read-string))

  (def sorted-data
    (->> schools
         (sort-by :school-href)
         (sort-by :concelho)
         (sort-by :distrito)
         ))

  (spit "./schools.edn" (with-out-str (pprint/pprint sorted-data)))

  )
>>>>>>> main
