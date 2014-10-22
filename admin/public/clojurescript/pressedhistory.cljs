(ns guardian-frontend.pressedhistory
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ajax.core :refer [GET POST]]))

(def versions (atom []))

(defn getVersions [pressedPathId handler]
  (GET "/history/list/uk"
    {:handler handler
     :response-format :json
     :keywords? true}))

(defn versionsList [data owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/div #js {:className "versions"}
        (map
          (fn [version]
            (dom/div #js {:className "version"}
              (dom/div nil (:key version))
              (dom/div nil (:versionId version))
              (dom/div nil (:bytes version) " bytes")
              (dom/div nil (:dateTime version))
              (dom/div nil (:humanDateTime version))))
          data)))))

(om/root versionsList versions {:target (. js/document (getElementById "mainapp"))})

(getVersions "ignored" (fn [r] (swap! versions (fn [v] (vec (concat v r))))))