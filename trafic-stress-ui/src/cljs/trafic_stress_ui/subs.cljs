(ns trafic-stress-ui.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 :active-panel
 (fn [db _]
   (:active-panel db)))


(re-frame/reg-sub :socket-messages #(:socket-messages %))
(re-frame/reg-sub :menu-state #(:menu-state %))
(re-frame/reg-sub :curses #(:curses %))
(re-frame/reg-sub :colors #(:colors %))
(re-frame/reg-sub :stress-value #(:stress-value %))

(re-frame/reg-sub :stats #(:stats %))
