(ns wakeru.schema
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia.util :as util]
            [com.walmartlabs.lacinia.schema :as schema]
            [clojure.edn :as edn]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.pedestal :as lp]
            [io.pedestal.http :as http]
            [clojure.java.browse :refer [browse-url]]
            [clj-fuzzy.metrics :as metrics]
            [clojure.string :as string]))

(def database
  (-> (io/resource "graphql/database.edn")
      slurp
      edn/read-string))

(defn n-gram
  "N-gram インデックスの作成"
  [n s]
  (->> (partition n 1 s)
       (map clojure.string/join)
       (into #{})))

(def bigram (partial n-gram 2))

(defn bigram-score
  "bigram検索"
  [keyword target]
  (count (clojure.set/intersection
           (bigram keyword)
           (bigram target))))

(defn partial-match
  "部分一致検索"
  [keyword]
  (->> database
       (filter #(string/includes? (:name %) keyword))
       (sort-by #(metrics/levenshtein keyword (:name %)))))

(defn fizzy-match
  "曖昧検索"
  [keyword]
  (->> database
       (filter #(< 1 (bigram-score keyword (:name %))))
       (sort-by #(metrics/levenshtein keyword (:name %)))))


(defn resolve-keyword-search
  "キーワード検索"
  [_ {:keys [keyword]} _]
  (if (< (count keyword) 3)
    (partial-match keyword)
    (fizzy-match keyword)))

(def resolver-map {:query/keyword_search resolve-keyword-search})

(defn load-schema
  ""
  []
  (-> (io/resource "graphql/schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers resolver-map)
      schema/compile))

(defonce server nil)

(defn start-server
  [_]
  (let [server (-> (load-schema)
                   (lp/service-map {:graphiql true})
                   (merge {::http/allowed-origins (constantly true)})
                   http/create-server
                   http/start)]
    server))

(defn stop-server
  [server]
  (http/stop server)
  nil)

(defn start
  []
  (alter-var-root #'server start-server)
  :started)

(defn stop
  []
  (alter-var-root #'server stop-server)
  :stopped)

(defn restart
  ""
  []
  (if server
    (stop))
  (start))
