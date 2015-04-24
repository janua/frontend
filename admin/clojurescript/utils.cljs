(ns guardian-frontend.components.utils
  (:require [om.core :as om :include-macros true]
            [ajax.core :refer [GET POST]]))

(defn getPaths [handler]
  (GET "/config"
    {:handler handler
     :error-handler #(.error js/console (pr-str %))
     :response-format :json
     :keywords? true}))

(defn getVersions [pressedPathId versionIdMarker handler]
  (GET (str "/history/list/" pressedPathId (if versionIdMarker (str "/" versionIdMarker)))
    {:handler handler
     :error-handler #(.error js/console (pr-str %))
     :response-format :json
     :keywords? true}))

(defn updateVersionsForPath [path currentPathCursor versionIdMarker]
  "Used for updating currentPath after an update"
  (getVersions path versionIdMarker
    #(om/transact! currentPathCursor (fn [v] (assoc v :path path :versions % :selectedVersion nil)))))

(defn restorePath [versionId currentPathCursor]
  (let [{:keys [path]} @currentPathCursor]
    (GET (str "/history/restore/" path "/" versionId)
      {:handler #(updateVersionsForPath path currentPathCursor)
       :error-handler #(.error js/console (pr-str %))
       :response-format :json
       :keywords? true})))

(defn updateVersion [currentPathCursor currentVersion]
  (om/transact! currentPathCursor (fn [m] (assoc m :selectedVersion currentVersion))))
