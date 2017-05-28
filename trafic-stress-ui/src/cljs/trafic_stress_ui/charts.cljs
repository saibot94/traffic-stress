(ns trafic-stress-ui.charts
  (:require [cljsjs.highcharts]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]))


(def chart-config
  {:chart {:type "areaspline"
           :backgroundColor "transparent"}
   :title {:text "Historic World Population by Region"}
   :subtitle {:text "Source: Wikipedia.org"}
   :xAxis {:categories []
           :title {:text nil}}
   :yAxis {:min 0
           :title {:text "Population (millions)"
                   :align "high"}
           :labels {:overflow "justify"}}
   :tooltip {:valueSuffix " millions"}
   :plotOptions {:bar {:dataLabels {:enabled true}}}
   :legend {:layout "vertical"
            :align "right"
            :verticalAlign "top"
            :x -40
            :y 100
            :floating true
            :borderWidth 1
            :shadow true}
   :credits {:enabled false}
   :series [{:name "Speed"
             :data []}
            {:name "RPM"
             :data []}]})



(defn test-chart-render [stats]
  (fn []
    (println @stats)
    [:div {:style {:min-width "310px" :max-width "800px"
                   :height "400px" :margin "0 auto"}}]))

(defn test-chart-fn [stats]
  (fn [this]
    (let [stats (-> chart-config
                  (assoc-in [:series 0 :data] (map first @stats))
                  (assoc-in [:series 1 :data] (map second @stats)))]
      (js/Highcharts.Chart. (reagent/dom-node this) (clj->js stats)))))


(defn test-chart []
  (let [stats (re-frame/subscribe [:stats])]
    (reagent/create-class
      {:reagent-render (test-chart-render stats)
       :component-did-mount (test-chart-fn stats)
       :component-will-update (test-chart-fn stats)})))
