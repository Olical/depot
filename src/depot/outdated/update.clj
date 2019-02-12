(ns depot.outdated.update
  (:require [clojure.tools.deps.alpha.reader :as reader]
            [depot.outdated :as depot]
            [rewrite-clj.zip :as rzip]
            [clojure.zip :as zip]))

(defn zip-skip-ws
  "Skip whitespace, comments, and uneval nodes."
  [loc]
  (loop [loc loc]
    (if (and loc
             (not (rzip/end? loc))
             (#{:comment :whitespace :newline :comma :uneval} (rzip/tag loc)))
      (recur (zip/right loc))
      loc)))

(defn zright
  "Like [[rewrite-clj.zip/right]], but also skip over uneval nodes"
  [loc]
  (some-> loc rzip/right zip-skip-ws))

(defn zget
  "Like [[clojure.core/get]], but for a zipper over a map.

  Takes and returns a zipper (loc)."
  [loc key]
  (rzip/right
   (rzip/find-value (rzip/down loc) (comp zright zright) key)))

(defn update-loc?
  "Should the version at the current position be updated?

  Returns true unless any ancestor has the `^:depot/ignore` metadata."
  [loc]
  (not (rzip/find loc
                  rzip/up
                  (fn [loc]
                    (:depot/ignore (meta (rzip/sexpr loc)))))))

(defmacro with-print-namespace-maps [bool & body]
  (if (find-var 'clojure.core/*print-namespace-maps*)
    `(binding [*print-namespace-maps* ~bool]
       ~@body)
    ;; pre Clojure 1.9
    `(do ~@body)))

(defn- new-versions
  [deps consider-types repos]
  (into {}
        (pmap (fn [[artifact coords]]
                (let [[old-version version-key]
                      (or (some-> coords :mvn/version (vector :mvn/version))
                          (some-> coords :sha (vector :sha)))
                      new-version (-> (depot/current-latest-map artifact
                                                                coords
                                                                {:consider-types consider-types
                                                                 :deps-map repos})
                                      (get "Latest"))]
                  (when (and old-version
                             ;; ignore these Maven 2 legacy identifiers
                             (not (#{"RELEASE" "LATEST"} old-version))
                             new-version)
                    [artifact {:version-key version-key
                               :old-version old-version
                               :new-version new-version}])))
              deps)))

(defn- apply-new-version
  [loc new-versions]
  (let [artifact (rzip/sexpr loc)
        {version-key :version-key
         new-version :new-version
         old-version :old-version :as v} (get new-versions artifact)
        coords-loc (zright loc)
        version-loc (zget coords-loc version-key)]
    (if (and (update-loc? loc)
             v)
      (do
        (with-print-namespace-maps false
          (println " " artifact (pr-str {version-key old-version}) "->" (pr-str {version-key new-version})))
        (rzip/left
         (rzip/up
          (rzip/replace version-loc new-version))))
      loc)))

(defn update-deps
  "Update all deps in a `:deps` or `:extra-deps` map.

  `loc` points at the map."
  [loc consider-types repos]
  (let [new-versions (new-versions (rzip/sexpr loc) consider-types repos)]
    (rzip/map-keys
     (fn [loc]
       (apply-new-version loc new-versions))
     loc)))


(defn zmap-vals
  "Given a zipper pointing at a map, apply a tranformation to each value of the
  map."
  [loc f & args]
  (let [loc' (rzip/down loc)]
    (if loc'
      (loop [keyloc loc']
        (let [valloc (apply f (zright keyloc) args)]
          (if-let [next-keyloc (zright valloc)]
            (recur next-keyloc)
            (rzip/up valloc))))
      loc)))

(declare update-all)

(defn update-aliases
  "Update all `:aliases`, expects a loc pointing at the `:aliases` map."
  [loc consider-types repos]
  (zmap-vals loc update-all consider-types repos))

(defn update-all
  "Update all `:deps`, `:extra-deps`, and `:aliases`.

  Expects a loc to the top level `deps.edn` map.
  `consider-types` is a set, one of [[depot.outdated/version-types]].
  `repos` is a map with optional keys `:mvn/repos` and `:mvn/local-repo`."
  [loc consider-types repos]
  (let [update-key (fn [loc k]
                     (if-let [deps-loc (zget loc k)]
                       (rzip/up (update-deps deps-loc consider-types repos))
                       loc))
        loc (-> loc
                (update-key :deps)
                (update-key :extra-deps)
                (update-key :override-deps))]
    (if-let [aliases-loc (zget loc :aliases)]
      (rzip/up (update-aliases aliases-loc consider-types repos))
      loc)))

(defn update-deps-edn!
  "Destructively update a `deps.edn` file.

  Read a `deps.edn` file, update all dependencies in it to their latest version,
  unless marked with `^:depot/ignore` metadata, then overwrite the file with the
  updated version. Preserves whitespace and comments.

  This will consider user and system-wide `deps.edn` files for locating Maven
  repositories, but only considers the given file when determining current
  versions.

  `consider-types` is a set, one of [[depot.outdated/version-types]]. "
  [file consider-types]
  (println "Updating:" file)
  (let [deps (-> (reader/clojure-env)
                 :config-files
                 reader/read-deps)

        repos    (select-keys deps [:mvn/repos :mvn/local-repo])
        loc      (rzip/of-file file)
        old-deps (slurp file)
        loc'     (update-all loc consider-types repos)
        new-deps (rzip/root-string loc')]
    (when (and loc' new-deps) ;; defensive check to prevent writing an empty deps.edn
      (if (= old-deps new-deps)
        (println "  All up to date!")
        (try
          (spit file new-deps)
          (catch java.io.FileNotFoundException e
            (println "  [ERROR] Permission denied: " file)))))))
