(ns depot.main
  (:require [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as reader]))

(let [deps-map (-> (reader/clojure-env)
                   (:config-files)
                   (reader/read-deps))
      args-map (deps/combine-aliases deps-map #{:test})]
  {:deps deps-map
   :args args-map
   :resolved (deps/resolve-deps deps-map args-map)})
