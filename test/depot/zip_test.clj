(ns depot.zip-test
  (:require [depot.zip :as u]
            [rewrite-clj.zip :as rzip]
            [rewrite-clj.node :as node]
            [clojure.test :refer :all]))

(deftest enter-meta-test
  (are [input] (= :map (-> (rzip/of-string input)
                           (rzip/get :d)
                           (u/enter-meta)
                           (rzip/tag)))
    "{:d {:a 0 :b 1}}"
    "{:d ^:depot/ignore {:a 0 :b 1}}"
    "{:d ^:depot/ignore ^:foo/bar {:a 0 :b 1}}"))

(deftest exit-meta-test
  (are [input] (= {:d {:a 0 :b 1}}
                  (-> (rzip/of-string input)
                      (rzip/get :d)
                      (u/enter-meta)
                      (u/exit-meta)
                      (rzip/up)
                      (rzip/sexpr)))
    "{:d {:a 0 :b 1}}"
    "{:d ^:depot/ignore {:a 0 :b 1}}"
    "{:d ^:depot/ignore ^:foo/bar {:a 0 :b 1}}"))

(deftest right-test
  (is (= :y
         (-> (rzip/of-string "[:x   ,,#_123\n  :y]")
             rzip/down
             u/right
             rzip/sexpr))))

(deftest left-test
  (is (= :x
         (-> (rzip/of-string "[:x   ,,#_123\n  :y]")
             rzip/down
             rzip/rightmost
             u/left
             rzip/sexpr))))

(deftest zget-test
  (is (= :bar
         (-> (rzip/edn (node/coerce {:foo :bar
                                     :bar :baz}))
             (u/zget :foo)
             (rzip/sexpr))))

  (is (nil?
       (-> (rzip/edn (node/coerce {:foo :bar
                                   :bar :baz}))
           (u/zget :unkown))))

  (is (= :baz
         (-> (rzip/of-string "{:foo :bar #_uneval :bar :baz}")
             (u/zget :bar)
             (rzip/sexpr)))))

(deftest zassoc-test
  (let [loc (rzip/of-string "{:foo :bar,\n :baz :baq}")]
    (is (= "{:foo 123,\n :baz :baq}"
           (-> loc
               (u/zassoc :foo 123)
               rzip/root-string)))

    (is (= "{:foo :bar,\n :baz 123}"
           (-> loc
               (u/zassoc :baz 123)
               rzip/root-string)))

    (is (= "{:foo :bar,\n :baz :baq :baq 123}"
           (-> loc
               (u/zassoc :baq 123)
               rzip/root-string)))))

(deftest lib-loc-seq-test
  (let [loc (rzip/of-string (pr-str '{:deps
                                      {foo {:mvn/version "1.0"}}
                                      :aliases
                                      {:test
                                       {:extra-deps {bar {:mvn/version "1.9.8"}}}}}))
        loc-seq (u/lib-loc-seq loc)]
    (is (= 2
           (count loc-seq)))

    (is (= (-> loc
               rzip/down
               rzip/right
               rzip/down)
           (first loc-seq)))

    (is (= (-> loc
               rzip/down
               rzip/right
               rzip/right
               rzip/right
               rzip/down
               rzip/right
               rzip/down
               rzip/right
               rzip/down)
           (second loc-seq)))))

(deftest ignore-loc?-test
  (testing "don't update when tagged with :depot/ignore"
    (is (true? (u/ignore-loc?
                (rzip/find-value (rzip/of-string "{:aliases {:dev ^:depot/ignore {:deps {foo/bar {}}}}} :test {:deps {baz/baq {}}}")
                                 rzip/next
                                 'foo/bar)))))

  (testing "do update by default"
    (is (false? (u/ignore-loc?
                 (rzip/find-value (rzip/of-string "{:aliases {:dev ^:depot/ignore {:deps {foo/bar {}}}}} :test {:deps {baz/baq {}}}")
                                  rzip/next
                                  'baz/baq))))))

(deftest mapped-libs-test
  (let [loc (rzip/edn
             (node/coerce
              '{:deps {org.clojure/clojure {:mvn/version "1.10.1"}}
                :aliases {:dev {:extra-deps {foo.bar {:git/sha "abiglonghash"}}}
                          :test {:extra-deps {foo.baz {:mvn/version "1.2.3"}}
                                 :override-deps {foo.baz.sublib {:mvn/version "1.2.2"}}}}}))]
    (is (= '{org.clojure/clojure [org.clojure/clojure {:mvn/version "1.10.1"}]
             foo.bar [foo.bar {:git/sha "abiglonghash"}]
             foo.baz [foo.baz {:mvn/version "1.2.3"}]
             foo.baz.sublib [foo.baz.sublib {:mvn/version "1.2.2"}]}
           (u/mapped-libs loc (fn [& args] args))))
    (is (every? nil? (vals (u/mapped-libs loc (constantly nil)))))))
