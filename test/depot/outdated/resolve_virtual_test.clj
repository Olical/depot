(ns depot.outdated.resolve-virtual-test
  (:require [depot.outdated.resolve-virtual :as r]
            [clojure.test :refer :all]
            [rewrite-clj.zip :as rzip]
            [clojure.tools.deps.alpha.util.maven :as maven]
            [clojure.string :as str]
            [depot.zip :as dzip]))

(deftest resolve-version-test
  (is
   (str/starts-with? (r/resolve-version 'cider/piggieback
                                        {:mvn/version "0.4.1-SNAPSHOT"}
                                        {:mvn/repos maven/standard-repos})
                     "0.4.1-20")))
