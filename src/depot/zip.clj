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
