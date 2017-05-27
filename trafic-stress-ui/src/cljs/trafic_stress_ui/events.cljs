(ns trafic-stress-ui.events
    (:require [re-frame.core :as re-frame]
              [trafic-stress-ui.db :as db]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(re-frame/reg-event-db
 :socket-message
 (fn [db [_ data]]
   (update-in db [:socket-messages] conj data)))
