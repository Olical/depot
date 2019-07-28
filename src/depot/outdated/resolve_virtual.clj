(ns depot.outdated.resolve-virtual
  (:require [depot.zip :as dzip]
            [depot.outdated :as outdated]
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

