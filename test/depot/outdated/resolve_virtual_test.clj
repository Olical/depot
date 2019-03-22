(ns depot.outdated.resolve-virtual-test
  (:require [depot.outdated.resolve-virtual :as r]
            [clojure.test :refer :all]
            [rewrite-clj.zip :as rzip]
            [clojure.tools.deps.alpha.util.maven :as maven]))

(deftest resolve-all-test
  (is (= '{:deps
           {org.clojure/clojure {:mvn/version "1.10.0"},
            cider/piggieback    {:mvn/version "0.4.1-20190222.154954-1"}}}
         (with-redefs [r/resolve-version (fn [lib _ _]
                                           ('{org.clojure/clojure "1.10.0"
                                              cider/piggieback    "0.4.1-20190222.154954-1"} lib))]
           (-> "{:deps {org.clojure/clojure {:mvn/version \"LATEST\"}
cider/piggieback {:mvn/version \"0.4.1-SNAPSHOT\"}}}"
               rzip/of-string
               (r/resolve-all maven/standard-repos)
               rzip/root
               rewrite-clj.node.protocols/sexpr)))))
