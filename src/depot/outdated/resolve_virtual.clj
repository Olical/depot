(ns depot.outdated.resolve-virtual
  (:require [clojure.tools.deps.alpha.reader :as reader]
            [depot.zip :as dzip]
            [depot.outdated :as outdated]
            [rewrite-clj.zip :as rzip]
            [clojure.zip :as zip]
            [clojure.tools.deps.alpha.util.maven :as maven]
            [clojure.string :as str]))

(defn resolve-version [lib coord repos]
  (let [artifact     (maven/coord->artifact lib coord)
        system       (maven/make-system)
        session      (outdated/make-session system maven/default-local-repo)
        remote-repos (mapv maven/remote-repo repos)
        request      (org.eclipse.aether.resolution.VersionRequest. artifact remote-repos nil)]
    (.getVersion (.resolveVersion system session request))))

(defn resolve-all
  [loc repos]
  (dzip/transform-libs
   loc
   (fn [loc]
     (let [artifact (rzip/sexpr loc)
           coords-loc (dzip/right loc)
           coords (rzip/sexpr coords-loc)]
       (if-let [mvn-version (:mvn/version coords)]
         (if (some (partial str/ends-with? mvn-version) ["-SNAPSHOT" "LATEST" "RELEASE"])
           (let [version (resolve-version artifact coords repos)]
             (println "   " artifact (:mvn/version coords) "-->" version)
             (dzip/zassoc coords-loc :mvn/version version))
           coords-loc)
         coords-loc)))))

(defn update-deps-edn! [file]
  (println "Resolving:" file)
  (let [deps     (-> (reader/clojure-env) :config-files reader/read-deps)
        loc      (rzip/of-file file)
        old-deps (slurp file)
        loc'     (resolve-all loc (:mvn/repos deps))
        new-deps (rzip/root-string loc')]
    (when (and loc' new-deps) ;; defensive check to prevent writing an empty deps.edn
      (if (= old-deps new-deps)
        (println "  All up to date!")
        (try
          (spit file new-deps)
          (catch java.io.FileNotFoundException e
            (println "  [ERROR] Permission denied: " file)))))))

#_
(resolve-version 'cider/piggieback {:mvn/version "0.4.1-SNAPSHOT"} maven/standard-repos)
