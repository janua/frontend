(defproject guardian-frontend "0.1.0-SNAPSHOT"
  :description "Guardian Frontend Clojurescript"
  :url "http://www.github.com/guardian/frontend"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2342"]
                 [om "0.7.3"]]
  :plugins [[lein-cljsbuild "1.0.3"]]
  :cljsbuild {
                 :builds [
                           {:source-paths ["admin/public/clojurescript"]
                            :compiler {:output-to "admin/public/clojurescript/main.js"}}]})
