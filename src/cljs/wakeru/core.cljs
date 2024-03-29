(ns wakeru.core
  (:require
   [reagent.core :as reagent]
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [wakeru.events :as events]
   [wakeru.routes :as routes]
   [wakeru.views :as views]
   [wakeru.config :as config]
   ))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (routes/app-routes)
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
