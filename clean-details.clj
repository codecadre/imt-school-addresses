(ns clean-details
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.string :as str]))

(def d (-> "enrich-raw.edn" slurp edn/read-string))

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
                     (or (re-find #"\d{4}-\s\d{3}" address)
                         (re-find #"\d{4}-\d{3}" address)
                         (re-find #"\d{4}\s-\d{3}" address)
                         (re-find #"\d{4}\s-\s\d{3}" address))))))

#_(->> d
     (take 10)
     (map clean-address)
     (map parse-cp7)
     (map :cp7))



(def results (->> d
                  #_(take 304)
                  (map clean-name)
                  (map clean-nec)
                  (map clean-address)
                  (map parse-cp7)
                  (sort #(compare (:nec-raw %1) (:nec-raw %2)))
                  doall))

#_(->> [(nth d 303)]
     (map clean-name)
     (map clean-nec)
     #_(map clean-address)
     doall)

#_(last (take 48 d))

#_(pprint/pprint results)

#_(map :title-raw results)


(spit "./enrich-clean.edn" (with-out-str (pprint/pprint results)))

(spit "./table.txt" (with-out-str (pprint/print-table results)))


#_(-> {:distrito "Bragança",
 :concelho "Carrazeda de Ansiâes",
 :school-href
 "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/K%c3%a9ris.aspx",
 :concelho-href
 "https://www.imt-ip.pt/sites/IMTT/Portugues/EnsinoConducao/LocalizacaoEscolasConducao/Paginas/LocalizacaoEscolasConducao.aspx?Distrito=Braganca&Concelho=Carrazeda%20Ans%C3%A3es",
 :title-raw "\r\n\t        Kéris \r\n\t        ",
 :address-raw
 "\r\n\t\t\tRua Luis de Camões, n.º 319, r/c B 5140-080 Carrazeda de Ansiães \r\n\t\t",
 :nec-raw
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
    :content
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
   "\r\n\t\t"]}}
    clean-name
    clean-address
    clean-nec
    )