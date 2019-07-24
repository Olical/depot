(ns depot.outdated.update-test
  (:require [clojure.test :refer :all]
            [depot.outdated.update :as u]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip :as rzip]
            [clojure.tools.deps.alpha.util.maven :as maven]))

(def CONSIDER_TYPES_RELEASES #{:release})

(def REPOS {:mvn/repos maven/standard-repos})

