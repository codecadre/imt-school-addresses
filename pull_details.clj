(ns pull-details
  (:require [cheshire.core :as json]
            [clojure.pprint :as pprint]))

(require '[babashka.pods :as pods])
(pods/load-pod 'retrogradeorbit/bootleg "0.1.9")
(require '[clojure.walk :refer [postwalk prewalk]])
(require '[babashka.curl :as curl])

(require
 '[babashka.fs :as fs]
 '[selmer.parser :as selmer] ; provided by Babashka
 '[pod.retrogradeorbit.bootleg.utils :as bootleg])

(def d (-> "./schools.json"
           slurp
           (json/parse-string true)))

#_(defn parse-nec
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

#_(def first-level (-> el :content rest first :content first))

#_#_(def el
      {:type :element,
       :attrs
       {:id "ctl00_PlaceHolderMain_ObservacoesDiv",
        :class "paginaConteudoDivTextoSemImagem"},
       :tag :div,
       :content
       ["\r\n\t\t\t"
        {:type :element,
         :attrs
         {:id
          "ctl00_PlaceHolderMain_Observacoes__ControlWrapper_RichHtmlField",
          :style "display:inline"},
         :tag :div,
         :content ;;first-level
         [{:type :element,
           :attrs {:color "#808184", :face "Arial"},
           :tag :font,
           :content
           ["Nº Alvará: "
            {:type :element,
             :attrs {:color "#808184", :face "Arial"},
             :tag :font,
             :content ["1356"]}
            " "]}]}
        "\r\n\t\t"]})

(parse-nec-new [el])

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
  (Thread/sleep (int (rand 500))))

(spit "./enrich-raw.edn" (with-out-str (pprint/pprint @results)))

#_(->
 "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/K%c3%a9ris.aspx"
 href->hc!
  hiccup->school-raw)
