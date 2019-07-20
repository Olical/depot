(ns depot.outdated.update
  (:require [clojure.tools.deps.alpha.reader :as reader]
            [depot.outdated :as depot]
            [depot.zip :as dzip]
            [rewrite-clj.zip :as rzip]))

(defmacro with-print-namespace-maps [bool & body]
  (if (find-var 'clojure.core/*print-namespace-maps*)
    `(binding [*print-namespace-maps* ~bool]
       ~@body)
    ;; pre Clojure 1.9
    `(do ~@body)))

(defn update-loc?
  "Should the version at the current position be updated?
  Returns true unless any ancestor has the `^:depot/ignore` metadata."
  [loc]
  (not (rzip/find loc
                  rzip/up
                  (fn [loc]
                    (:depot/ignore (meta (rzip/sexpr loc)))))))

(defn- new-versions
  "Find all deps in a `:deps` or `:extra-deps` or `:override-deps` map to be updated,
  at the top level and in aliases.

  `loc` points at the top level map."
  [loc consider-types repos]
  (let [deps (->> (dzip/lib-loc-seq loc)
                  (filter (fn [loc]
                            (and (update-loc? loc)
                                 (update-loc? (dzip/right loc)))))
                  (map dzip/loc->lib)
                  doall)]
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
                deps))))

(defn- apply-new-version
  [new-versions loc]
  (let [artifact (rzip/sexpr loc)
        coords-loc (dzip/right loc)
        {version-key :version-key
         new-version :new-version
         old-version :old-version :as v} (get new-versions artifact)]
    (if v
      (do
        (with-print-namespace-maps false
          (println " " artifact (pr-str {version-key old-version}) "->" (pr-str {version-key new-version})))
        (dzip/zassoc coords-loc version-key new-version))
      coords-loc)))

(defn update-deps
  "Update all deps in a `:deps` or `:extra-deps` or `:override-deps` map, at the
  top level and in aliases.

  `new-versions` is a map of artifact the to-be-applied updates.

  `loc` points at the top level map."
  [loc new-versions]
  (dzip/transform-libs loc (partial apply-new-version new-versions)))

(defn- update-deps-edn*
  [file consider-types write?]
  (let [deps (-> (reader/default-deps)
                 reader/read-deps)

        repos    (select-keys deps [:mvn/repos :mvn/local-repo])
        loc      (rzip/of-file file)
        old-deps (slurp file)
        new-versions (new-versions loc consider-types repos)
        loc'     (update-deps loc new-versions)
        new-deps (rzip/root-string loc')]
    (when (and loc' new-deps) ;; defensive check to prevent writing an empty deps.edn
      (if (= old-deps new-deps)
        (println "  All up to date!")
        (try
          (when write? (spit file new-deps))
          (catch java.io.FileNotFoundException e
            (println "  [ERROR] Permission denied: " file)))))))

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
  (update-deps-edn* file consider-types true))

(defn check-deps-edn
  "Check for updates to a `deps.edn` file.

  Read a `deps.edn` file, find newer versions of all dependencies in it unless
  marked with `^:depot/ignore` metadata.

  This will consider user and system-wide `deps.edn` files for locating Maven
  repositories, but only considers the given file when determining current
  versions.

  `consider-types` is a set, one of [[depot.outdated/version-types]]. "
  [file consider-types]
  (println "Checking:" file)
  (update-deps-edn* file consider-types false))
