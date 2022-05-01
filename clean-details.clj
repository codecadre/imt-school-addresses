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
  (update s :nec-raw #(edn/read-string (re-find #"\d+" %))))

(comment
  (clean-weirdness "Av Dr. António Caldas, loja 139, Faquelo  4970-592 Arcos de"))

(defn clean-address [s]
  (->
   s
   (assoc :address-clean (:address-raw s))
   (update :address-clean #(clean-weirdness (str/trim %)))
   (dissoc :address-raw)))

(def results (->> d
                  (take 47)
                  (map clean-name)
                  (map clean-nec)
                  (map clean-address)))
(last (take 48 d))

#_(pprint/pprint results)

#_(map :title-raw results)


#_(spit "./enrich-clean.edn" (with-out-str (pprint/pprint results)))

#_(spit "./table.txt" (with-out-str (pprint/print-table results)))


(-> {:distrito "Bragança",
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
