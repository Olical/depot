(ns depot.outdated.update
  (:require [clojure.tools.deps.alpha.reader :as reader]
            [depot.zip :as dzip]
            [rewrite-clj.zip :as rzip]))

(defmacro with-print-namespace-maps [bool & body]
  (if (find-var 'clojure.core/*print-namespace-maps*)
    `(binding [*print-namespace-maps* ~bool]
       ~@body)
    ;; pre Clojure 1.9
    `(do ~@body)))

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
  "Given an `loc` pointing at a map, navigate to the sub-map at k, and apply f
  to every key in it. Returns an `loc` at the 'same' point in the tree."
  [loc k f]
  (-> loc
      (dzip/zget k)
      dzip/enter-meta
      (as-> % (dzip/map-keys f %))
      dzip/exit-meta
      rzip/up))

(defn- apply-top-level-deps
  "Given a root `loc`, and a new-versions map, apply f
   to the top-level dependencies."
  [loc f]
  (apply-to-deps-map loc :deps f))

(defn- apply-alias-deps
  [loc include-override-deps? f]
  (cond-> loc
    (dzip/zget loc :extra-deps)
    (apply-to-deps-map :extra-deps f)

    (and include-override-deps? (dzip/zget loc :override-deps))
    (apply-to-deps-map :override-deps f)))

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
  [file consider-types include-alias? include-override-deps? write? messages new-versions]
  (let [start-message ((if write? :start-write :start-read-only) messages)]
    (printf (str start-message "\n") file))
  (let [deps (reader/read-deps (reader/default-deps))
        config (-> deps
                   (select-keys [:mvn/repos :mvn/local-repo])
                   (assoc :consider-types consider-types))
        loc      (rzip/of-file file)
        old-deps (slurp file)
        new-versions (new-versions loc config)
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
