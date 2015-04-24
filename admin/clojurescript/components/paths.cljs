(ns guardian-frontend.components.paths
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ajax.core :refer [GET POST]]
            [guardian-frontend.components.utils :refer [updateVersionsForPath]]))

(defn pathsList [data owner]
  (reify
    om/IRender
    (render [this]
      (let [{:keys [currentPath]} data]
        (dom/div #js {:className "paths-list"}
          (dom/h3 nil "Paths List")
          (apply dom/div nil
            (map
              (fn [path]
                (dom/div #js {:onClick #(updateVersionsForPath path currentPath nil)} path))
              (:paths data))))))))
