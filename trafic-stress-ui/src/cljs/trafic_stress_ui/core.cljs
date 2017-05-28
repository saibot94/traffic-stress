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
    (println clj-data)
    (cond
      (= (:event clj-data) "stress") (re-frame/dispatch [:stress-level-change (:payload clj-data)])

      :else (re-frame/dispatch [:stats-recevied clj-data]))))

(def heroku "ws://shrouded-journey-12474.herokuapp.com/data/3")
(def local "ws://localhost:3449/data")

(defn connect-socket []
  (let [ws (js/WebSocket. heroku)]
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
                  (.getElementById js/document "app"))
  (re-frame/dispatch [:server-event-received {:type :stress-event}]))





(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
