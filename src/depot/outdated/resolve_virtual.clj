(ns depot.outdated.resolve-virtual
  (:require [clojure.tools.deps.alpha.reader :as reader]
            [depot.zip :as dzip]
            [depot.outdated :as outdated]
            [rewrite-clj.zip :as rzip]
            [clojure.zip :as zip]
            [clojure.tools.deps.alpha.util.maven :as maven]
            [clojure.string :as str]))

(defn resolve-version [lib coord {:keys [mvn/repos depot/local-maven-repo]}]
  (let [artifact     (maven/coord->artifact lib coord)
        system       (maven/make-system)
        session      (outdated/make-session system (or local-maven-repo maven/default-local-repo))
        remote-repos (mapv maven/remote-repo repos)
        request      (org.eclipse.aether.resolution.VersionRequest. artifact remote-repos nil)]
    (.getVersion (.resolveVersion system session request))))

(defn pinned-versions
  [loc config]
  (let [deps (->> (dzip/lib-loc-seq loc)
                  (filter (fn [loc]
                            (and (not (dzip/ignore-loc? loc))
                                 (not (dzip/ignore-loc? (dzip/right loc))))))
                  (map dzip/loc->lib)
                  doall)]
    (into {}
          (map (fn [[artifact coords]]
                 (when-let [mvn-version (:mvn/version coords)]
                   (when (some (partial str/ends-with? mvn-version) ["-SNAPSHOT" "LATEST" "RELEASE"])
                     [artifact {:version-key :mvn/version
                                :old-version mvn-version
                                :new-version (resolve-version artifact coords config)}]))))
          deps)))

(defn resolve-all
  [loc config]
  (dzip/transform-libs
   loc
   (fn [loc]
     (let [artifact (rzip/sexpr loc)
           coords-loc (dzip/right loc)
           coords (rzip/sexpr coords-loc)]
       (if-let [mvn-version (:mvn/version coords)]
         (if (some (partial str/ends-with? mvn-version) ["-SNAPSHOT" "LATEST" "RELEASE"])
           (let [version (resolve-version artifact coords config)]
             (println "   " artifact (:mvn/version coords) "-->" version)
             (dzip/zassoc coords-loc :mvn/version version))
           coords-loc)
         coords-loc)))))

(defn update-deps-edn! [file]
  (println "Resolving:" file)
  (let [deps     (-> (reader/default-deps) reader/read-deps)
        loc      (rzip/of-file file)
        old-deps (slurp file)
        loc'     (resolve-all loc (select-keys deps [:mvn/repos :depot/local-maven-repo]))
        new-deps (rzip/root-string loc')]
    (when (and loc' new-deps) ;; defensive check to prevent writing an empty deps.edn
      (if (= old-deps new-deps)
        (println "  All up to date!")
        (try
          (spit file new-deps)
          (catch java.io.FileNotFoundException e
            (println "  [ERROR] Permission denied: " file)))))))
