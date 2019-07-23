(ns depot.outdated.update-test
  (:require [clojure.test :refer :all]
            [depot.outdated.update :as u]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip :as rzip]
            [clojure.tools.deps.alpha.util.maven :as maven]))

(def CONSIDER_TYPES_RELEASES #{:release})

(def REPOS {:mvn/repos maven/standard-repos})

(deftest update-loc?-test
  (testing "don't update when tagged with :depot/ignore"
    (is (false? (u/update-loc?
                 (rzip/find-value (rzip/of-string "{:aliases {:dev ^:depot/ignore {:deps {foo/bar {}}}}} :test {:deps {baz/baq {}}}")
                                  rzip/next
                                  'foo/bar)))))

  (testing "do update by default"
    (is (true? (u/update-loc?
                (rzip/find-value (rzip/of-string "{:aliases {:dev ^:depot/ignore {:deps {foo/bar {}}}}} :test {:deps {baz/baq {}}}")
                                 rzip/next
                                 'baz/baq))))))
