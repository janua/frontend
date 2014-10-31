(ns guardian-frontend.components.versions
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [guardian-frontend.components.utils :refer [restorePath]]))

(defn versionsList [currentPath owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/div #js {:className "versions"}
        (dom/h3 nil (or (:path currentPath) "No Selected Path"))
        (let [etagsList (map #(:etag %) (:versions currentPath))]
          (map
            (fn [version]
              (let [{:keys [etag]} version
                    etagOccursMoreThanOnce (> (count (filter #{etag} etagsList)) 1)]
                (om/build versionItem
                  (assoc version
                    :etagOccursMoreThanOnce etagOccursMoreThanOnce
                    :currentPath currentPath))))
            (:versions currentPath)))))))

(defn versionItem [version owner]
  (reify
    om/IRender
    (render [this]
      (let [{:keys [currentPath isLatest key versionId bytes dateTime humanDateTime etag etagOccursMoreThanOnce]} version]
        (dom/div #js {:className (str "version" (if isLatest " latest"))}
          (dom/div nil "Key: " key)
          (dom/div nil "Version ID: " versionId (if isLatest " (Latest)"))
          (dom/div nil "Size: " bytes " bytes")
          (dom/div nil "Date (Long): " dateTime)
          (dom/div nil "Date (Human): " humanDateTime)
          (dom/div nil "ETag: "
            (dom/span (if etagOccursMoreThanOnce #js {:className "highlight"}) etag))
          (if-not isLatest
            (dom/button #js {:className "btn btn-sm btn-primary":onClick #(restorePath versionId currentPath)} "Restore")))))))