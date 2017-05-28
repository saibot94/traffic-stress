(ns trafic-stress-ui.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [org.httpkit.server :refer [with-channel on-close send!]]))


(def clients (atom {}))
(defn ws
  [req]
  (with-channel req con
    (swap! clients assoc con true)
    (println con " connected")
    (on-close con (fn [status]
                    (swap! clients dissoc con)
                    (println con " disconnected. status: " status)))))

(defn write-message [message]
  (doseq [client @clients]
    (send! (key client) message false)))


(defn curses-handler [_]
  (str "[\"**** ma-tii de boul ***i mele facut din laba trista si care nu stii sa conduci\",\"sa il/o *** dracu in gura si-n cur\",\"du-te-n *** mea\"]"))

(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/data" [] ws)
  (GET "/swears" [] curses-handler)
  (resources "/"))

(def dev-handler
  (-> #'routes
    wrap-reload))

(def handler routes)
