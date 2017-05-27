(ns trafic-stress-ui.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [trafic-stress-ui.events]
              [trafic-stress-ui.subs]
              [trafic-stress-ui.routes :as routes]
              [trafic-stress-ui.views :as views]
              [trafic-stress-ui.config :as config]))


(defn send-socket-message [m]
  (let [data (aget m "data")
        json-data (.parse js/JSON data)
        clj-data (js->clj json-data :keywordize-keys true)]
    (re-frame/dispatch [:socket-message clj-data])))

(defn connect-socket []
  (let [ws (js/WebSocket. "ws://localhost:3449/data")]
    (aset ws "onmessage" send-socket-message)
    ws))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))




(defonce socket (connect-socket))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))




(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
