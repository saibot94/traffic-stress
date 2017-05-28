(ns trafic-stress-ui.events
    (:require [re-frame.core :as re-frame]
              [trafic-stress-ui.db :as db]
              [day8.re-frame.http-fx]
              [ajax.core :as ajax]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
 :set-active-panel
 (fn [db [_ active-panel]]
   (-> db
     (assoc :active-panel active-panel)
     (assoc :menu-state false))))

(re-frame/reg-event-db
 :socket-message
 (fn [db [_ data]]
   (update-in db [:socket-messages] conj data)))

(re-frame/reg-event-db
   :close-menu
   (fn [db _]
     (assoc db :menu-state false)))

(re-frame/reg-event-db
  :open-menu
  (fn [db _]
    (assoc db :menu-state true)))

(re-frame/reg-event-db
  :curses-received
  (fn [db [_ curses]]
    (assoc db :curses curses)))

(re-frame/reg-event-db
  :curses-transmission-error
  (fn [db [_ curses]]
    (assoc db :curses nil)))

(defn fetch-curses []
   {:method          :get
    :uri             "/swears"
    :timeout         8000                                           ;; optional see API docs
    :response-format (ajax/json-response-format {:keywords? true})  ;; IMPORTANT!: You must provide this.
    :on-success      [:curses-received]
    :on-failure      [:curses-transmission-error]})

(defn stress-level-event [db event]
  {:db db
   :http-xhrio (fetch-curses)})


(re-frame/reg-event-db
  :stats-recevied
  (fn [db [_ stats]]
    (println (count (:stats db)))
    (update db :stats conj (->> stats
                              (map (juxt :speed :rpm))))))



(re-frame/reg-event-fx
  :stress-level-change
  (fn [{:keys [db]} [_ new-value]]
    (let [base-evt {:db (-> db
                          (assoc :stress-value new-value))}]
      (cond
        (and (> new-value 50)
             (:server-call db)) (-> base-evt
                                  (assoc-in [:db :server-call] false)
                                  (assoc :http-xhrio (fetch-curses)))
        (< new-value 50) (-> base-evt
                            (assoc-in [:db :server-call] true)
                            (assoc-in [:db :curses] []))

        :else base-evt))))
