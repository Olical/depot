(ns depot.zip
  "Extra zipper helpers."
  (:require [rewrite-clj.zip :as rzip]
            [clojure.zip :as zip]))

(defn- zip-skip-ws
  "Skip whitespace, comments, and uneval nodes."
  [loc]
  (loop [loc loc]
    (if (and loc
             (not (rzip/end? loc))
             (#{:comment :whitespace :newline :comma :uneval} (rzip/tag loc)))
      (recur (zip/right loc))
      loc)))

(defn right
  "Like [[rewrite-clj.zip/right]], but also skip over uneval nodes"
  [loc]
  (some-> loc rzip/right zip-skip-ws))

(defn zget
  "Like [[clojure.core/get]], but for a zipper over a map.

  Takes and returns a zipper (loc)."
  [loc key]
  (rzip/right
   (rzip/find-value (rzip/down loc) (comp right right) key)))

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

;; TODO make sure this only matches map keys
(defn lib?
  "Is the loc at a library name."
  [loc]
  (and (= :token (rzip/tag loc))
       (rzip/map? (rzip/up loc))
       (#{:deps :extra-deps :override-deps} (some-> loc
                                                    rzip/up
                                                    rzip/left
                                                    rzip/sexpr))))

(defn next-lib
  "Find the next loc, depth first, that is a library name."
  [loc]
  (rzip/find-next-depth-first loc lib?))

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
   (rzip/sexpr (rzip/right loc))])

(defn lib-seq
  "A sequence of all libraries in the given zipper over deps.edn, returning a seq
  of [name, coordinate-map] pairs."
  [loc]
  (map loc->lib (lib-loc-seq loc)))

(defn transform-coords
  "Transform all coordinate maps in the given zipper over deps.edn. The function f
  takes a pair of [library-name coordinate-map] and returns a coordinate map."
  [loc f & args]
  (loop [loc loc
         loc' (next-lib loc)]
    (if loc'
      (let [coords (apply f (loc->lib loc') args)
            loc (-> loc'
                    (rzip/right)
                    (rzip/replace coords))]
        (recur loc (next-lib loc)))
      loc)))
