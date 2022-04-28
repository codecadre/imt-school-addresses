(ns example
  {:clj-kondo/config '{:lint-as {promesa.core/let clojure.core/let
                                 promesa.exec/schedule! promesa.exec/schedule!
                                 example/defp clojure.core/def}}}
  (:require
   ["puppeteer$default" :as puppeteer]
   [promesa.core :as p]
   ["fs" :as fs]
   [clojure.string :as str]))

(defn sleep-ms [] (js/Promise. (fn [r] (js/setTimeout r (int (* 1000 (rand)))))))

(defn escape-char
  "escape special characters"[s]
  (->>
   (str/split s "=" )
   (interpose "\\=")
   (apply str)))

(defn pull-schools-href-list [page {:keys [id href distrito concelho] :as concelho-map} results]
  (p/let [schools (.evaluate page "a = []; Array.from(document.querySelectorAll('.IMTTLinkTituloA')).forEach(el => a.push(el.href));a")
          schools (doall (map #(hash-map :school-href %
                                         :concelho-href href
                                         :distrito distrito
                                         :concelho concelho) schools))
          results (into results schools)]
    results))

#_(comment (defp browser (.launch puppeteer #js {:headless false}))
 (defp page (.newPage browser))

 (p/let [s (pull-schools-href-list page {:id 123 :distrito "distrito" :concelho "concelho"} [])]
   (prn "..." s))

 (p/let [browser (.launch puppeteer #js {:headless false})
         page (.newPage browser)
         concelho-map
         #_{:distrito "Lisboa"
            :concelho "Lisboa"
            :href
            "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx?Distrito=Lisboa&Concelho=Lisboa"}
         {:distrito "wtv"
          :concelho "wtv"
          :href "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx?Distrito=Braganca&Concelho=Bragan%u00e7a"}
         #_{:distrito "wtv"
            :concelho "wtv"
            :href
            "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx?Distrito=Braga&Concelho=Cabeiras%20Basto"}
         results (get-school-href page concelho-map [])]
   (prn results)))


(defn cycle-paginators [page {:keys [id href distrito concelho] :as concelho-map} page-count next-page results]
  (p/let [_ (.waitForSelector page ".divPaginacaoSemMenu > input")
          paginator-handlers (.$$ page ".divPaginacaoSemMenu > input")
          next-page-handler (nth paginator-handlers next-page)
          _ (.click next-page-handler)
          _ (.waitForSelector page ".divPaginacaoSemMenu > input")
          _ (.goBack page)
          _ (.goForward page)
          _ (sleep-ms)
          results (pull-schools-href-list page concelho-map results)]
    (if (> page-count next-page)
      (cycle-paginators page concelho-map page-count (inc next-page) results)
      results)))

(defn get-school-href [page concelho results]
  (p/let [_ (.goto page (:href concelho))
          _ (sleep-ms)
          paginator-handles (.$$ page ".divPaginacaoSemMenu > input")
          paginator-handles (drop-last paginator-handles)
          handlers-count (count paginator-handles)]
    (if (zero? handlers-count)
      (p/let [results (pull-schools-href-list page concelho results)]
        results)
      (cycle-paginators page concelho handlers-count 1 results))))

#_(p/let [browser (.launch puppeteer #js {:headless false})
        page (.newPage browser)
        concelho-map
        {:distrito "Lisboa"
           :concelho "Lisboa"
           :href
           "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx?Distrito=Lisboa&Concelho=Lisboa"}
        #_{:distrito "wtv"
         :concelho "wtv"
         :href "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx?Distrito=Braganca&Concelho=Bragan%u00e7a"}
        #_{:distrito "wtv"
           :concelho "wtv"
           :href
           "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx?Distrito=Braga&Concelho=Cabeiras%20Basto"}
        results (get-school-href page concelho-map [])]
  (prn results))

(defn pull-schools [page concelhos results]
  (p/let [concelho (first concelhos)
          rest-concelho (rest concelhos)
          _ (println "working on schools at " (:concelho concelho) "/" (:distrito concelho) "\n")
          _ (println (:href concelho) "\n")
          results (get-school-href page concelho results)
          _ (println "schools so far " (count results) "\n")]
    (if (empty? rest-concelho)
      results
      (pull-schools page rest-concelho results))))

(def anchors-query "var a = []; Array.from(document.querySelectorAll('a')).forEach(el => {a.push({href: el.href, id: el.id, html: el.innerHTML })});a")

(defn pull-concelhos [page districts results]
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

(def base-url "https://www.imt-ip.pt")

(def district-url "/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx")

(defn pull-districts [page]
 (p/let [_ (.goto page (str base-url district-url))
         _ (sleep-ms)
         anchors (.evaluate page anchors-query)
         districts (->> anchors
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

#_(p/let [browser (.launch puppeteer #js {:headless false})
        page (.newPage browser)
        districts (pull-districts page)]
  (prn "Districts: " (count districts)))


(p/let [browser (.launch puppeteer #js {:headless false})
        page (.newPage browser)
        districts (pull-districts page)
        #_#_districts (subvec (vec districts)  0 1)
        concelhos (pull-concelhos page districts [])
        schools (pull-schools page concelhos [])
        school-string (.stringify js/JSON (clj->js schools))]
  (.writeFileSync fs "./schools.json" school-string))

#_(comment
 (defp browser (.launch puppeteer #js {:headless false}))
 (defp page (.newPage browser))
 (p/let [browser (.launch puppeteer #js {:headless false})
         page (.newPage browser)]
   (pull-concelhos page
                   [{:href "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx?Distrito=Evora",
                     :id "Distrito\\=Evora",
                     :html "Évora"}])))



#_(comment
 (defp browser (.launch puppeteer #js {:headless false}))
 (defp page (.newPage browser))
 (.goto page "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx?Distrito=Lisboa&Concelho=Lisboa")
 (defp paginator-handles (.$$ page ".divPaginacaoSemMenu > input")#_(p/-> (.$$ page ".divPaginacaoSemMenu > input")
                                                                          #_rest
                                                                          #_drop-last))

 (get-school-href "page" {:href "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx?Distrito=Lisboa&Concelho=Lisboa"}))



#_(p/let [browser (.launch puppeteer #js {:headless false})
        page (.newPage browser)
        _ (.goto page base-url)
        anchors (.evaluate page s)

        braganca-id (escape-ids (get (first (filter #(str/includes? (get % "id") "Distrito=Braganca")  anchors)) "id"))
        braganca-el (.$ page (str "#" braganca-id))]
  (.click braganca-el)
  #_(prn district-anchors)
  #_(.close browser))




;; (defp browser (.launch puppeteer #js {:headless false}))

;; (defp page (.newPage browser))

;; (defp content (.content page))

;; (def a (atom []))

;; (defp hrefs-dom (.$$ page "a"))
;; (def hrefs (js/Array.from hrefsdom))

;; (prn hrefs)

;; #_(defp foo (.evaluate page (fn [sel]
;;                             (reset! a (.querySelectorAll js/document  sel))) "a"))




;; (defp bar (.evaluate page (fn [el] (prn (.-innerHTML el)))))
;; (count hrefs)

;; (j/get (first hrefs) "href")

;; (.-href (first hrefs))

;; (.getAttributes (first hrefs) "href")

;; (js/typeof (first hrefs))

;; (keys (js->clj (first hrefs)))

;; #_(p/let [browser (.launch puppeteer #js {:headless false})
;;         page (.newPage browser)])
#_(defmacro defp
    "Define var when promise is resolved"
    [binding expr]
    `(-> ~expr (.then (fn [val]
                        (def ~binding val)))))






(comment


  (browser-test)
  ;; evaluate to continue after pause
  (@continue)
  ;; inspect content captured during test
  ;; (subs c 0 10)

  (defmacro defp
    "Define var when promise is resolved"
    [binding expr]
    `(-> ~expr (.then (fn [val]
                        (def ~binding val)))))

  (defp browser (.launch puppeteer #js {:headless false}))
  (defp page (.newPage browser))
  (.goto page "https://clojure.org")

  )
