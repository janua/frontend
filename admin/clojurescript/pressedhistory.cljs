(ns guardian-frontend.pressedhistory
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [guardian-frontend.components.paths :refer [pathsList]]
            [guardian-frontend.components.versions :refer [versionsList]]
            [guardian-frontend.components.utils :refer [getPaths]]))

(def stage (.-stage js/config))

(def previewLocation "http://localhost:9000")

(def app-state (atom {:paths []
                      :currentPath {:path nil :versions [] :selectedVersion nil}}))

(defn rootApp [data owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/div #js {:className "flex-wrap"}
      [(dom/h5 #js {:className "title"} "Restore History")
       (om/build pathsList data)
       (om/build versionsList (:currentPath data))
       (if-let [path (:path (:currentPath data))]
         (dom/iframe #js {:className "viewer" :frameBorder "0"
                        :src (str previewLocation "/version/" stage "/" path
                              (if-let [selectedVersion (:selectedVersion (:currentPath data))]
                                (str "?versionId=" selectedVersion)))}))]))))

(om/root rootApp app-state {:target (. js/document (getElementById "mainapp"))})

(getPaths (fn [r] (swap! app-state assoc :paths r)))
