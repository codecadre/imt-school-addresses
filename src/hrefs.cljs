;; Not very smart but does the job

(ns hrefs
  {:clj-kondo/config '{:lint-as {promesa.core/let clojure.core/let
                                 example/defp clojure.core/def}}}
  (:require
   ["puppeteer$default" :as puppeteer]
   [promesa.core :as p]
   ["fs" :as fs]
   [clojure.string :as str]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resources
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def base-url "https://www.imt-ip.pt")

(def district-url "/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx")

;; It's a query to get the links in a page 'a'
(def anchors-query "var a = []; Array.from(document.querySelectorAll('a')).forEach(el => {a.push({href: el.href, id: el.id, html: el.innerHTML })});a")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro defp
  "Define var when promise is resolved"
  [binding expr]
  `(-> ~expr (.then (fn [val]
                      (def ~binding val)))))



(defn rand-int-from-to
  "returns a random integer from to"
  [from to]
  (let [min-value from
        max-value to]
    (+ min-value (rand-int  (- max-value min-value)))))

;;TODO require from util
(defn sleep-ms
  ([]
   (sleep-ms (rand-int-from-to 250 500)))
  ([time-in-ms]
   (js/Promise. (fn [r] (js/setTimeout r time-in-ms)))))

(defn escape-char
  "escape special characters"
  [s]
  (->>
   (str/split s "=" )
   (interpose "\\=")
   (apply str)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; actual script logic
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pull-schools-href-list
  "Scrapes the actual href in each 'concelho' paginated page."
  [page {:keys [id href distrito concelho] :as concelho-map} results]
  (p/let [schools (.evaluate page "a = []; Array.from(document.querySelectorAll('.IMTTLinkTituloA')).forEach(el => a.push(el.href));a")
          schools (doall (map #(hash-map :school-href %
                                         :concelho-href href
                                         :distrito distrito
                                         :concelho concelho) schools))
          results (into results schools)]
    results))

(defn get-paginator-values
  "Given a puppeteer page, returns a promise that contains the paginator values. Ex:

  [1 2 3 >]
  [< 1 2 3 >]
  [< 1 2 3] "
  [page]
  (p/let [values (.$$eval page ".divPaginacaoSemMenu > input" (js/eval "(elements) => {return elements.map(el => el.value)}"))]
    (mapv js->clj values)))

(defn resolve-next-paginator-index
  "given an array with the values representing the paginator (ex: [1 2 3 >])
  and a human page number (ex: 1, 2 3),
  returns the next paginator index or nil"
  [paginator-values current-human-page]
  (let [current-index (.indexOf paginator-values (str current-human-page))]
    (if (< (inc current-index) (count paginator-values))
      (inc current-index)
      nil)))

;; (resolve-next-paginator-index ["<" "1" "2" "3"] 3) => nil
;; (resolve-next-paginator-index ["1" "2" "3" ">"] 1) => 1
;; (resolve-next-paginator-index ["<" "1" "2" "3" ">"] 2) => 3

(defn paginate-through-concelho
  "given a page and a concelho map, paginate and fill the results array with each school link"
  [page concelho-map current-human-page results]
  (p/let [results (pull-schools-href-list page concelho-map results)
          ;; no idea why I have to sleep and wait here...
          _ (sleep-ms 1000)
          ;; the paginator might never come if we have small concelhos...
          #_#__ (.waitForSelector page ".divPaginacaoSemMenu > input")
          paginator-elements (.$$ page ".divPaginacaoSemMenu > input")
          paginator-values (get-paginator-values page)
          next-paginator-index (resolve-next-paginator-index paginator-values current-human-page)
          next-page (when next-paginator-index
                      (nth paginator-elements next-paginator-index))]
    (if next-page
      (do (.click next-page)
          (paginate-through-concelho page concelho-map (inc current-human-page) results))
      results)))

(defn pull-schools
  "Get the schools for a list of concelhos"
  [page concelhos results]
  (p/let [concelho (first concelhos)
          rest-concelho (rest concelhos)
          _ (println "working on schools at " (:concelho concelho) "/" (:distrito concelho) "\n")
          _ (println (:href concelho) "\n")
          page-number 1 ;; actuall page in the paginator 1, 2 3, etc. Don't confuse it with an index it's just the human page number
          - (.goto page (:href concelho))
          results (paginate-through-concelho page concelho page-number results)
          _ (println "schools so far " (count results) "\n")]
    (if (empty? rest-concelho)
      results
      (pull-schools page rest-concelho results))))

(defn pull-concelhos
  "Recursively pulls all concelhos for a list of districts. Returns all concelhos in 'districts'."
  [page districts results]
  (p/let [{:keys [id href html]} (first districts)
          districts (rest districts)
          _ (.goto page href)
          _ (sleep-ms)
          anchors (.evaluate page anchors-query)
          concelhos (->> anchors
                         (map js->clj)
                         (map (fn [el]
                                (->> el vec (map (fn [[f l]]
                                                   [(keyword f) l]) )
                                     (into {}))))
                         (filter #(str/includes? (:id %) "Concelho"))
                         (map #(assoc %
                                      :distrito html
                                      :concelho (:html %)
                                      :id (escape-char (:id %))))
                         doall)
          results (into results concelhos)]
    (if (empty? districts)
      results
      (pull-concelhos page districts results))))

(defn pull-districts
  "Given a puppeteer page for the IMT schools, extracts a list of districts."
  [page]
 (p/let [_ (.goto page (str base-url district-url))
         _ (sleep-ms)
         anchors (.evaluate page anchors-query)
         districts (->> anchors ;;gets all links each page
                        (map js->clj)
                        (map (fn [el]
                               (->> el vec (map (fn [[f l]]
                                                  [(keyword f) l]) )
                                    (into {}))))
                        (filter #(str/includes? (:id %) "Distrito"))
                        (map #(assoc %
                                     :distrito (:html %)
                                     :id (escape-char (:id %))))
                        doall)]
   districts))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(p/let [browser (.launch puppeteer #js {:headless false #_ true})
        page (.newPage browser)
        districts (pull-districts page)
        #_#_districts (subvec (vec districts)  0 1)
        concelhos (pull-concelhos page districts [])
        schools (pull-schools page concelhos [])
        schools-sorted (sort-by :school-href schools)
        school-string (.stringify js/JSON (clj->js schools-sorted) nil "  ")]
  (.close browser)
  (.writeFileSync fs "./temp/hrefs.json" school-string))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Examples for debug and other
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; npx nbb nrepl-server/


(comment

  ;; Util functions


  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; 1. Get a list of schools with pull-schools-href-list
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (do
    ;; Open a browser and navigate to page first
    (defp browser (.launch puppeteer #js {:headless true}))
    (defp page (.newPage browser))
    (def imt-page-with-schools
      "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx?Distrito=Faro&Concelho=Loul%C3%A9")

    (.goto page imt-page-with-schools)

    (defp hrefs-list-example (pull-schools-href-list page {:id 123 :distrito "distrito" :concelho "concelho"} [])))

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; 2. Paginator logic cycle-paginators and get-school-href
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (defp browser (.launch puppeteer #js {:headless false}))
  (defp page (.newPage browser))
  (def imt-page-with-schools
    ;; small concelho with 1 page
    #_"https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx?Distrito=Viseu&Concelho=Cinf%C3%A3es"
    ;; lisbon
    "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx?Distrito=Lisboa&Concelho=Lisboa")
  (.goto page imt-page-with-schools)

  (def concelho-map {:id 123 :distrito "distrito" :concelho "concelho" :href imt-page-with-schools})

  (defp test (paginate-through-concelho page concelho-map 1 []))

  (identity test)

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; 3. pull-concelhos
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (defp browser (.launch puppeteer #js {:headless false}))
  (defp page (.newPage browser))
  (p/let [browser (.launch puppeteer #js {:headless false})
          page (.newPage browser)]
    (pull-concelhos page
                    [{:href "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx?Distrito=Faro&Concelho=Loul%C3%A9",
                      :id "Distrito\\=Loule",
                      :html "Loule"}]))

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; 4 pull-chools
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (p/let [browser (.launch puppeteer #js {:headless false})
          page (.newPage browser)
          r (pull-schools page
                          [{:href "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx?Distrito=Faro&Concelho=Loul%C3%A9",
                            :concelho "wtv"
                            :distrito "wtv"}]
                          [])]
    (prn (set (map :school-href r))))
  )
