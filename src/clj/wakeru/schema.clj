(ns wakeru.schema
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia.util :as util]
            [com.walmartlabs.lacinia.schema :as schema]
            [clojure.edn :as edn]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.pedestal :as lp]
            [io.pedestal.http :as http]
            [clojure.java.browse :refer [browse-url]]))

(def database
  (-> (io/resource "graphql/database.edn")
      slurp
      edn/read-string))

(defn resolve-keyword-search
  ""
  [_ {:keys [keyword]} _]
  (filter
    #(clojure.string/includes? (:name %) keyword)
    database))

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
