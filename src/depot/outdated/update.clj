(ns depot.outdated.update
  (:require [clojure.tools.deps.alpha :as deps.alpha]
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
     (if (and v (seq (rzip/sexpr coords-loc)))
       (with-print-namespace-maps false
         (println " "
                  artifact
                  (pr-str {version-key old-version})
                  "->"
                  (pr-str {version-key new-version}))
         (dzip/zassoc coords-loc version-key new-version))
       coords-loc)
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
      rzip/up
      (or loc)))

(defn- apply-top-level-deps
  "Given a root `loc`, and a new-versions map, apply f
   to the top-level dependencies."
  [loc f]
  (apply-to-deps-map loc :deps f))

(defn- apply-alias-deps
  [loc f]
  (-> loc
      (apply-to-deps-map :extra-deps f)
      (apply-to-deps-map :override-deps f)))

(defn- apply-aliases-deps
  "`loc` points to the root of the deps.edn file."
  [loc include-alias? f]
  (if-let [alias-map (dzip/zget loc :aliases)]
    (dzip/map-keys (fn [loc]
                     (let [alias-name (rzip/sexpr loc)]
                       (if (include-alias? alias-name)
                         (-> loc
                             rzip/right
                             dzip/enter-meta
                             (apply-alias-deps f)
                             dzip/exit-meta
                             rzip/left)
                         loc)))
                   alias-map)
    loc))

(defn apply-new-versions
  [file consider-types include-alias? write? messages new-versions]
  (let [start-message ((if write? :start-write :start-read-only) messages)]
    (printf (str start-message "\n") file))
  (let [{:keys [root-edn user-edn project-edn]} (deps.alpha/find-edn-maps)
        deps (deps.alpha/merge-edns [root-edn user-edn project-edn])
        config (-> deps
                   (select-keys [:mvn/repos :mvn/local-repo])
                   (assoc :consider-types consider-types))
        loc      (rzip/of-file file)
        old-deps (slurp file)
        new-versions (new-versions loc config)
        loc'     (-> loc
                     (apply-top-level-deps (partial apply-new-version new-versions))
                     (apply-aliases-deps include-alias?
                                         (partial apply-new-version new-versions)))
        new-deps (rzip/root-string loc')
        newer-deps? (not= old-deps new-deps)]

    (when (and loc' new-deps) ;; defensive check to prevent writing an empty deps.edn
      (if newer-deps?
        (try
          (when write? (spit file new-deps))
          (catch java.io.FileNotFoundException e
            (println "  [ERROR] Permission denied: " file)))
        (println (:no-changes messages))))

    {:newer-deps? newer-deps?}))
