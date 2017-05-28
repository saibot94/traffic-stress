(defproject trafic-stress-ui "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [http-kit "2.1.18"]
                 [reagent "0.6.0"]
                 [re-frame "0.9.2"]
                 [cljsjs/highcharts "5.0.4-0"]
                 [cljs-ajax "0.6.0"]
                 [day8.re-frame/http-fx "0.1.3"]
                 [secretary "1.2.3"]
                 [compojure "1.5.0"]
                 [yogthos/config "0.8"]
                 [ring "1.4.0"]
                 [puppetlabs/ring-middleware "1.0.0"]]

  :plugins [[lein-cljsbuild "1.1.4"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler trafic-stress-ui.handler/dev-handler
             :nrepl-port 7888}


  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.8.2"]
                   [com.cemerick/piggieback "0.2.1"]
                   [proto-repl "0.3.1"]]

    :plugins      [[lein-figwheel "0.5.9"]]}}


  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "trafic-stress-ui.core/mount-root"}
     :compiler     {:main                 trafic-stress-ui.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}}}


    {:id           "min"
     :source-paths ["src/cljs"]
     :jar true
     :compiler     {:main            trafic-stress-ui.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}]}




  :main trafic-stress-ui.server

  :aot [trafic-stress-ui.server]

  :uberjar-name "trafic-stress-ui.jar"

  :prep-tasks [["cljsbuild" "once" "min"] "compile"])
