(ns wakeru.scraping
  (:require [net.cgrand.enlive-html :as html]
            [org.httpkit.client :as http]
            [clojure.walk :as walk]
            [clojure.string :as string]))

(def table-a-url
  "https://www.city.sendai.jp/haiki-shido/kurashi/machi/genryo/gomi/yobi/gojuon-a.html")
(def table-ka-url
  "https://www.city.sendai.jp/haiki-shido/kurashi/machi/genryo/gomi/yobi/gojuon-ka.html")
(def table-sa-url
  "https://www.city.sendai.jp/haiki-shido/kurashi/machi/genryo/gomi/yobi/gojuon-sa.html")
(def table-ta-na-url
  "https://www.city.sendai.jp/haiki-shido/kurashi/machi/genryo/gomi/yobi/gojuon-ta.html")
(def table-ha-url
  "https://www.city.sendai.jp/haiki-shido/kurashi/machi/genryo/gomi/yobi/gojuon-ha.html")
(def table-ma-ya-ra-wa-url
  "https://www.city.sendai.jp/haiki-shido/kurashi/machi/genryo/gomi/yobi/gojuon-ma.html")

(def pages [table-a-url
            table-ka-url
            table-sa-url
            table-ta-na-url
            table-ha-url
            table-ma-ya-ra-wa-url])


(defn get-dom
  ""
  [url]
  (html/html-snippet
    (:body @(http/get url {:insecure? true}))))

(defn extract-tables
  ""
  [dom]
  (html/select dom [:tbody]))

(defn tr->map
  ""
  [[name category remark]]
  {:name     (-> name :content first)
   :category (-> category :content first)
   :remark   (-> remark :content)})

(defn table->list
  ""
  [tbody]
  (->> tbody
       :content
       (filter map?)
       (map (comp tr->map
               #(filter map? %)
               :content))
       (drop 1)))

(defn tables->list
  ""
  [tbody-list]
  (->> tbody-list
       (map table->list)
       (reduce concat)))

(defn pages->list
  ""
  [pages]
  (->> pages
       (map (comp tables->list
               extract-tables
               get-dom))
       (reduce concat)))

(defn write-edn
  ""
  []
  (spit "resources/graphql/database.edn" (pr-str (pages->list pages))))
