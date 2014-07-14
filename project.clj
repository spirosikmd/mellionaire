(defproject
  clojure-pta
  "0.2.0"
  :repl-options
  {:init-ns clojure-pta.repl}
  :dependencies
  [[ring-server "0.3.1"]
   [com.h2database/h2 "1.3.175"]
   [environ "0.4.0"]
   [markdown-clj "0.9.41"]
   [com.taoensso/timbre "3.1.6"]
   [korma "0.3.0-RC6"]
   [org.clojure/clojure "1.6.0"]
   [com.taoensso/tower "2.0.2"]
   [log4j
    "1.2.17"
    :exclusions
    [javax.mail/mail
     javax.jms/jms
     com.sun.jdmk/jmxtools
     com.sun.jmx/jmxri]]
   [selmer "0.6.4"]
   [lib-noir "0.8.1"]
   [compojure "1.1.6"]
   [clj-http "0.9.1"]
   [org.clojure/data.json "0.2.4"]
   [cljs-ajax "0.2.3"]]
  :ring
  {:handler clojure-pta.handler/app,
   :init clojure-pta.handler/init,
   :destroy clojure-pta.handler/destroy}
  :profiles
  {:uberjar {:aot :all},
   :production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}},
   :dev
   {:dependencies [[ring-mock "0.1.5"] [ring/ring-devel "1.2.2"]],
    :env {:dev true}}}
  :url
  "https://github.com/spirosikmd/mellionaire"
  :plugins
  [[lein-ring "0.8.10"] [lein-environ "0.4.0"]]
  :description
  "A simple demo event ticketing store built with luminus clojure web framework. The ticketing functionality is provided by Paylogic Ticketing API."
  :min-lein-version "2.0.0")
