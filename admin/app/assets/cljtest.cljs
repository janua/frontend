(ns om-clojurescript-template.core
    (:require [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]))


(.log js/console "Test From Assets!")
(.log js/console "Test From Assets!")

(def items (atom []))

(defn pushToItems [e] (swap! items (fn [l] (conj (take 5 l) {:text (str (rand-int 30) " - " (.-timeStamp e))}))))

(defn itemsList [data owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/div #js {:id "items"}
        (cons (dom/button #js {:onClick pushToItems} "Click")
          (vec (map (fn [item] (dom/div #js {:id "item"} (:text item))) data)))))))
(om/root itemsList items {:target (. js/document (getElementById "mainapp"))})