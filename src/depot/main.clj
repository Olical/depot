(ns depot.main
  (:require [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as reader]))

;; TODO Make this configurable.
(def aliases #{:test})

(let [deps-map (-> (reader/clojure-env)
                   (:config-files)
                   (reader/read-deps))
      args-map (deps/combine-aliases deps-map aliases)
      resolved (deps/resolve-deps deps-map args-map)
      direct-deps (keys (merge (:deps deps-map) (:extra-deps args-map)))]
  (select-keys resolved direct-deps))
