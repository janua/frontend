(defproject om-clojurescript-template "0.1.0-SNAPSHOT"
  :description "OM Clojurescript Template"
  :url "http://www.github.com/janua"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2342"]
                 [om "0.7.3"]]
  :plugins [[lein-cljsbuild "1.0.3"]]
  :cljsbuild {
               :builds [
                         {:source-paths ["/Users/francis/Documents/frontend/admin/assets"]
                          :compiler {:output-to "/Users/francis/Documents/frontend/admin/assets/main.js"}}]})
