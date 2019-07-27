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

(defn- new-versions
  "Find all deps in a `:deps` or `:extra-deps` or `:override-deps` map to be updated,
  at the top level and in aliases.

  `loc` points at the top level map."
  [loc {:keys [consider-types repos]}]
  (let [deps (->> (dzip/lib-loc-seq loc)
                  (filter (fn [loc]
                            (and (not (dzip/ignore-loc? loc))
                                 (not (dzip/ignore-loc? (dzip/right loc))))))
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
        coords-loc (-> loc
                       dzip/right
                       dzip/enter-meta)
        {version-key :version-key
         new-version :new-version
         old-version :old-version :as v} (get new-versions artifact)]
    (->
     (if (not v)
       coords-loc
       (do (with-print-namespace-maps false
             (println " "
                      artifact
                      (pr-str {version-key old-version})
                      "->"
                      (pr-str {version-key new-version})))
           (dzip/zassoc coords-loc version-key new-version)))
     dzip/exit-meta
     dzip/left)))

(defn- apply-to-deps-map
  "Given a `loc` pointing at a map and f apply
  to every dependency in the map."
  [loc f]
  (dzip/map-keys f loc))

(defn- apply-top-level-deps
  "Given a root `loc`, and a new-versions map, apply f
   to the top-level dependencies."
  [loc f]
  (-> loc
      (dzip/zget :deps)
      dzip/enter-meta
      (apply-to-deps-map f)
      dzip/exit-meta
      rzip/up))

(defn- apply-alias-deps
  [loc include-override-deps? f]
  (cond-> loc
    (dzip/zget loc :extra-deps)
    (-> (dzip/zget :extra-deps)
        dzip/enter-meta
        (apply-to-deps-map f)
        dzip/exit-meta
        rzip/up)

    (and include-override-deps? (dzip/zget loc :override-deps))
    (-> (dzip/zget :override-deps)
        dzip/enter-meta
        (apply-to-deps-map f)
        dzip/exit-meta
        rzip/up)))

(defn- apply-aliases-deps
  "`loc` points to the root of the deps.edn file."
  [loc include-alias? include-override-deps? f]
  (let [alias-map (dzip/zget loc :aliases)]
    (dzip/map-keys (fn [loc]
                     (let [alias-name (rzip/sexpr loc)]
                       (if (include-alias? alias-name)
                         (-> loc
                             rzip/right
                             dzip/enter-meta
                             (apply-alias-deps include-override-deps? f)
                             dzip/exit-meta
                             rzip/left)
                         loc)))
                   alias-map)))

(defn apply-new-versions
  [file consider-types include-alias? include-override-deps? write? messages]
  (let [start-message ((if write? :start-write :start-read-only) messages)]
    (printf (str start-message "\n") file))
  (let [deps (reader/read-deps (reader/default-deps))
        repos    (select-keys deps [:mvn/repos :depot/local-maven-repo])
        loc      (rzip/of-file file)
        old-deps (slurp file)
        new-versions (new-versions loc {:consider-types consider-types
                                        :repos repos})
        loc'     (-> loc
                     (apply-top-level-deps (partial apply-new-version new-versions))
                     (apply-aliases-deps include-alias?
                                         include-override-deps?
                                         (partial apply-new-version new-versions)))
        new-deps (rzip/root-string loc')]
    (when (and loc' new-deps) ;; defensive check to prevent writing an empty deps.edn
      (if (= old-deps new-deps)
        (println (:no-changes messages))
        (try
          (when write? (spit file new-deps))
          (catch java.io.FileNotFoundException e
            (println "  [ERROR] Permission denied: " file)))))))
