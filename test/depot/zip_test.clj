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
