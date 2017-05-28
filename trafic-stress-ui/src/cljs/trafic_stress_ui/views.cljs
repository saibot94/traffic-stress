(ns trafic-stress-ui.views
    (:require [re-frame.core :as re-frame]
              [trafic-stress-ui.charts :as charts]))


(defn stress-css [class stress-value]
  (let [stress-0 (str class "-0 ")
        stress-1 (str class "-1 ")
        stress-2 (str class "-2 ")]
    (cond
      (<= stress-value 33) stress-0
      (<= stress-value 66) (str stress-0 stress-1)
      :else (str stress-0 stress-1 stress-2))))

;; home

(defn nav-to [path]
  (let [location (aget js/window "location")]
    (set! (.-href location) path)))

(defn menu-item [{:keys [active? path text stress-value]}]
  [:li {:class (str
                (stress-css "menu-item" stress-value)
                (when active?
                  (stress-css "active" stress-value)))}
      [:a {:href path} text]])



(defn menu [{:keys [open? on-close stress-value]}]
  [:div (when-not open? {:style {:display "none"}})
    [:div {:on-click #(on-close %) :class "modal w3-animate-fade"}]
    [:nav {:class "menu w3-card w3-animate-left"}
      [:span {:class "menu-art"}]
      [:ul {:class "nav-menu"}
        [menu-item {:text "Home"
                    :active? true
                    :path "#/"
                    :stress-value stress-value}]
        [menu-item {:text "Stats"
                    :active? false
                    :path "#/stats"
                    :stress-value stress-value}]
        [menu-item {:text "Settings"
                    :active? false
                    :path "#/settings"
                    :stress-value stress-value}]]]])




(defn header []
  (let [menu-open? (re-frame/subscribe [:menu-state])
        stress-value (re-frame/subscribe [:stress-value])]
    (fn []
      [:div
        [:div {:class "w3-panel header"}
          [:i {:on-click #(re-frame/dispatch [:open-menu]) :class "hamburger icon"}]
          [:i {:class "icon logo"}]]
        [menu {:open? @menu-open?
               :on-close #(re-frame/dispatch [:close-menu])
               :stress-value @stress-value}]])))


(defn curse-item [{:keys [curse]}]
  [:div {:key (:id curse) :class "w3-animate-left w3-card curse-item"}
    [:span
      [:p curse]]])

(defn curses-list []
  (let [curses (re-frame/subscribe [:curses])]
    (fn []
      [:div {:class "curses-list"}
        (->> @curses
          (map-indexed (fn [i x] {:curse x :id i}))
          (map (fn [curse]
                  [curse-item curse])))])))



(defn app-wrapper [{:keys [stress-value ]} & content]
  [:div {:class "app"}
    [:div {:class (str "background " (stress-css "stress" stress-value))}]
    [header]
    [:div {:class "w3-container content"}
      content]
    [:div {:class "w3-container"}
        [:input {:value stress-value :on-change #(re-frame/dispatch [:stress-level-change (-> % (.-target) (.-value))]) :type "range"}]]])



(defn home-panel []
  (let [stress-value (re-frame/subscribe [:stress-value])]
    (fn []
      [app-wrapper {:stress-value @stress-value}
        [curses-list]])))


(defn stats-panel []
  (let [stress-value (re-frame/subscribe [:stress-value])]
    (fn []
      [app-wrapper {:stress-value @stress-value}
        [:div {:class "w3-container w3-card-2 stats-panel"}
          [:h1 {:class (str (stress-css "menu-item" @stress-value))} "Stats"]
          [charts/test-chart]]])))



(defn- panels [panel-name]
  (case panel-name
    :home-panel [home-panel]
    :stats-panel [stats-panel]
    [:div]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [show-panel @active-panel])))
