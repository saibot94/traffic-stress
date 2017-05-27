(ns trafic-stress-ui.views
    (:require [re-frame.core :as re-frame]))


;; home

(defn home-panel []
  (let [name (re-frame/subscribe [:name])
        messages (re-frame/subscribe [:socket-messages])]
    (fn []
      [:div (str "Hello from " @name ". This is the Home Page.")
       (map (fn [m] [:div (str m)]) @messages)])))


(defn- panels [panel-name]
  (case panel-name
    :home-panel [home-panel]
    [:div]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [show-panel @active-panel])))
