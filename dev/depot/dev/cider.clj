(ns depot.dev.cider
  (:require [cider-nrepl.main :as nrepl]))

(defn -main []
  (nrepl/init ["cider.nrepl/cider-middleware"]))
