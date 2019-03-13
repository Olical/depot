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

(deftest update-deps-test
  (is (= '{:deps {org.clojure/algo.monads {:mvn/version "0.1.6"}}}

         (-> (rzip/edn (node/coerce '{:deps {org.clojure/algo.monads {:mvn/version "0.1.4"}}}))
             (rzip/down)
             (rzip/right)
             (u/update-deps CONSIDER_TYPES_RELEASES REPOS)
             (rzip/root)
             (node/sexpr))))

  (testing "it skips over uneval nodes"
    (is (= '{:deps {org.clojure/algo.monads {:mvn/version "0.1.6"}}}
           (-> (rzip/of-string "{:deps {org.clojure/algo.monads #_foo {:mvn/version \"0.1.4\"}}}")
               (rzip/down)
               (rzip/right)
               (u/update-deps CONSIDER_TYPES_RELEASES REPOS)
               (rzip/root)
               (node/sexpr)))))

  (testing "it ignores empty maps"
    (is (= '{:deps {}}
           (-> (rzip/edn (node/coerce '{:deps {}}))
               (rzip/down)
               (rzip/right)
               (u/update-deps CONSIDER_TYPES_RELEASES REPOS)
               (rzip/root)
               (node/sexpr))))))
