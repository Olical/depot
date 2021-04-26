(ns depot.outdated.resolve-virtual
  (:require [depot.zip :as dzip]
            [depot.outdated :as outdated]
            [clojure.tools.deps.alpha.util.maven :as maven]
            [clojure.string :as str]))

(defn resolve-version [lib coord {:keys [mvn/repos mvn/local-repo]}]
  (let [artifact     (maven/coord->artifact lib coord)
        system       (maven/make-system)
        session      (outdated/make-session system (or local-repo maven/default-local-repo))
        remote-repos (mapv maven/remote-repo repos)
        request      (org.eclipse.aether.resolution.VersionRequest. artifact remote-repos nil)]
    (.getVersion (.resolveVersion system session request))))

(defn pinned-versions
  "Find all deps in a `:deps` or `:default-deps` or `:extra-deps` or `:override-deps` map to be pinned,
  at the top level and in aliases.

  `loc` points at the top level map."
  [loc config]
  (dzip/mapped-libs
   loc
   (fn [artifact coords]
     (when-let [mvn-version (:mvn/version coords)]
       (when (some (partial str/ends-with? mvn-version) ["-SNAPSHOT" "LATEST" "RELEASE"])
         {:version-key :mvn/version
          :old-version mvn-version
          :new-version (resolve-version artifact coords config)})))))

