(ns depot.zip
  "Extra zipper helpers."
  (:require [rewrite-clj.zip :as rzip]
            [clojure.zip :as zip]))

(defn- zip-skip-ws
  "Skip whitespace, comments, and uneval nodes."
  [loc next-fn skip-fn end?-fn]
  (loop [loc loc]
    (cond
      (nil? loc) loc
      (end?-fn loc) loc

      (#{:comment :whitespace :newline :comma} (rzip/tag loc))
      (recur (next-fn loc))

      (= :uneval (rzip/tag loc))
      (recur (skip-fn loc))

      :else loc)))

(defn left
  "Like [[rewrite-clj.zip/left], but also skip over uneval nodes"
  [loc]
  (some-> loc rzip/left (zip-skip-ws rzip/left rzip/left (constantly false))))

(defn right
  "Like [[rewrite-clj.zip/right]], but also skip over uneval nodes"
  [loc]
  (some-> loc rzip/right (zip-skip-ws rzip/right rzip/right (constantly false))))

(defn znext
  "Like [[rewrite-clj.zip/next]], but also skip over uneval nodes"
  [loc]
  (some-> loc rzip/next (zip-skip-ws rzip/next rzip/right rzip/end?)))

(defn enter-meta
  "If the given `loc` is a meta node, navigate down to the value to which it is attached,
  else return the `loc`."
  [loc]
  (if (not= :meta (rzip/tag loc))
    loc
    (-> loc
        rzip/down
        right
        (recur))))

(defn exit-meta
  "If the given `loc`'s parent is a meta node, return the first ancestor whose parent is not,
  else return the `loc`."
  [loc]
  (let [loc' (rzip/up loc)]
    (if (= :meta (rzip/tag loc'))
      (recur loc')
      loc)))

(defn zget
  "Like [[clojure.core/get]], but for a zipper over a map.

  Takes and returns a zipper (loc)."
  [loc key]
  {:pre [(rzip/map? loc)]}
  (right
   (rzip/find-value (rzip/down loc) (comp right right) key)))

(defn zassoc
  "Like [[clojure.core/assoc]], but for a zipper over a map.

  New keys will be added at the end, preserving indentation."
  [loc k v]
  {:pre [(rzip/map? loc)]}
  (if-let [vloc (zget loc k)]
    ;; key found, just replace the value
    (rzip/up (rzip/replace vloc v))

    ;; key not found, add it to the end of the map
    (let [;; loc to the last value in the map
          loc (rzip/rightmost (rzip/down loc))
          ;; whitespace nodes preceding the last key in the map. This way we
          ;; maintain the same use of commas, newlines, whitespace
          ws-nodes (->> loc
                        left
                        zip/lefts
                        reverse
                        (take-while rewrite-clj.reader/whitespace?)
                        reverse)
          ;; copy the whitespace nodes into the end of the map
          loc (reduce (fn [loc node]
                        (zip/right (zip/insert-right loc node)))
                      loc
                      ws-nodes)]
      ;; insert key and value
      (-> loc
          (rzip/insert-right k)
          (rzip/right)
          (rzip/insert-right v)
          (rzip/up)))))

(defn map-vals
  "Like [[rewrite-clj.zip/map-vals]], but account for uneval nodes, and can take extra args."
  [f loc & args]
  (let [loc' (rzip/down loc)]
    (if loc'
      (loop [keyloc loc']
        (let [valloc (apply f (right keyloc) args)]
          (if-let [next-keyloc (right valloc)]
            (recur next-keyloc)
            (rzip/up valloc))))
      loc)))

(defn map-keys
  "Like [[rewrite-clj.zip/map-keys]], but account for uneval nodes."
  [f zloc]
  (loop [loc (rzip/down zloc)
         parent zloc]
    (if-not (and loc (rzip/node loc))
      parent
      (if-let [v (f loc)]
        (recur (right (right v)) (rzip/up v))
        (recur (right (right loc)) parent)))))

(defn ignore-loc?
  "Should the version at the current position be ignored?
  Returns true if any ancestor has the `^:depot/ignore` metadata."
  [loc]
  (boolean
   (rzip/find loc
              rzip/up
              (fn [loc]
                (:depot/ignore (meta (rzip/sexpr loc)))))))

;; TODO make sure this only matches map keys
(defn lib?
  "Is the loc at a library name."
  [loc]
  (and loc
       (= :token (rzip/tag loc))
       (rzip/map? (rzip/up loc))
       (#{:deps :default-deps :extra-deps :override-deps} (some-> loc
                                                                  rzip/up
                                                                  left
                                                                  rzip/sexpr))))

(defn next-lib
  "Find the next loc, depth first, that is a library name."
  [loc]
  (rzip/find-next loc znext lib?))

(defn lib-loc-seq
  "A sequence of zippers each pointing at a library name."
  [loc]
  (->> loc
       next-lib
       (iterate next-lib)
       (take-while identity)))

(defn loc->lib
  "Given a zipper pointing at a library name, return a pair of [name
  coordinate-map]"
  [loc]
  [(rzip/sexpr loc)
   (rzip/sexpr (right loc))])

(defn lib-seq
  "A sequence of all libraries in the given zipper over deps.edn, returning a seq
  of [name, coordinate-map] pairs."
  [loc]
  (map loc->lib (lib-loc-seq loc)))

(defn transform-libs
  "Transform all coordinate maps in the given zipper over deps.edn. The function f
  takes a loc pointing at the library name, and returns a new loc."
  [loc f & args]
  (loop [loc loc
         loc' (next-lib loc)]
    (if loc'
      (let [loc (-> (apply f loc' args) znext)]
        (recur loc (next-lib loc)))
      loc)))

(defn mapped-libs
  "Find every unignored dep in a `:deps` or `:extra-deps` or `:override-deps` map, at
   the top level and in aliases, and return a map of artifact to output of
  (f artifact coords). f must be free of side-effects.

  `loc` points at the top level map."
  [loc f]
  (->> (lib-loc-seq loc)
       (filter (fn [loc]
                 (and (not (ignore-loc? loc))
                      (not (ignore-loc? (right loc))))))
       (map loc->lib)
       (pmap (fn [[artifact coords]]
               [artifact (f artifact coords)]))
       (into {})))
