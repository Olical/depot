(ns depot.zip-test
  (:require [depot.zip :as u]
            [rewrite-clj.zip :as rzip]
            [rewrite-clj.node :as node]
            [clojure.test :refer :all]))

(deftest zip-skip-ws-test
  (is (= :foo
         (-> (rzip/of-string "   ,,, ;;;\n#_123 :foo")
             (#'u/zip-skip-ws)
             rzip/sexpr))))

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

    (is (= "{:foo :bar,\n :baz :baq,\n :baq 123}"
           (-> loc
               (u/zassoc :baq 123)
               rzip/root-string)))))
