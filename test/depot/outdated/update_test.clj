(ns depot.outdated.update-test
  (:require [clojure.test :refer :all]
            [depot.outdated.update :as u]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip :as rzip]
            [rewrite-clj.node :as node]
            [clojure.tools.deps.alpha.util.maven :as maven]))

(def CONSIDER_TYPES_RELEASES #{:release})

(def REPOS {:mvn/repos maven/standard-repos})

(deftest missing-top-level-deps
  (let [input (rzip/edn (node/coerce {:aliases {:test {:extra-deps {'midje {:mvn/version "1.9.8"}}}}}))]
    (is (= input
           (#'u/apply-top-level-deps
            input
            identity)))))
