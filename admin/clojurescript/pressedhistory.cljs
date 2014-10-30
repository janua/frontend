(ns guardian-frontend.pressedhistory
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [guardian-frontend.components.paths :refer [pathsList]]
            [guardian-frontend.components.versions :refer [versionsList]]
            [guardian-frontend.components.utils :refer [getPaths]]))

(def app-state (atom {:paths []
                      :currentPath {:path "No Selected Path" :versions []}}))

(defn rootApp [data owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/div #js {:className "flex-wrap"}
      [(dom/h5 #js {:className "title"} "Restore History")
       (om/build pathsList data)
       (om/build versionsList (:currentPath data))]))))

(om/root rootApp app-state {:target (. js/document (getElementById "mainapp"))})

(getPaths (fn [r] (swap! app-state assoc :paths r)))