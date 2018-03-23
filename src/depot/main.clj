(ns depot.main
  (:require [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as reader]
            [clojure.tools.deps.alpha.util.maven :as maven])
  (:import [org.eclipse.aether.resolution VersionRangeRequest]
           [org.eclipse.aether.version Version]))

;; TODO Make this configurable.
(def aliases #{:test})

(defn coord->version-status [lib coord {:keys [mvn/repos mvn/local-repo]}]
  (let [local-repo (or local-repo maven/default-local-repo)
        remote-repos (mapv maven/remote-repo repos)
        system (maven/make-system)
        session (maven/make-session system local-repo)
        artifact (maven/coord->artifact lib coord)
        artifact-range (maven/coord->artifact lib (assoc coord :mvn/version "[0,)"))
        versions-req (doto (new VersionRangeRequest)
                       (.setArtifact artifact-range)
                       (.setRepositories remote-repos))
        versions (-> (.resolveVersionRange system session versions-req)
                     (.getVersions))]
    {:current (.getVersion artifact)
     :newer (map str versions)}))

(let [deps-map (-> (reader/clojure-env)
                   (:config-files)
                   (reader/read-deps))
      args-map (deps/combine-aliases deps-map aliases)
      resolved-universe (deps/resolve-deps deps-map args-map)
      direct-deps (keys (merge (:deps deps-map) (:extra-deps args-map)))
      resolved-local (select-keys resolved-universe direct-deps)]
  (doseq [[lib coord] resolved-local]
    (let [{:keys [current newer]} (coord->version-status lib coord deps-map)]
      (prn [lib current newer]))))
