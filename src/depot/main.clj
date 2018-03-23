(ns depot.main
  (:require [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as reader]
            [clojure.tools.deps.alpha.util.maven :as maven])
  (:import [org.eclipse.aether.resolution VersionRangeRequest]))

;; TODO Make this configurable.
(def aliases #{:test})

(defn coord->versions [[lib coord] {:keys [mvn/repos mvn/local-repo]}]
  (let [local-repo (or local-repo maven/default-local-repo)
        remote-repos (mapv maven/remote-repo repos)
        system (maven/make-system)
        session (maven/make-session system local-repo)
        artifact (maven/coord->artifact lib (assoc coord :mvn/version "[0,)"))
        versions-req (doto (new VersionRangeRequest)
                       (.setArtifact artifact)
                       (.setRepositories remote-repos))
        versions-res (.resolveVersionRange system session versions-req)]
    (.getVersions versions-res)))

(let [deps-map (-> (reader/clojure-env)
                   (:config-files)
                   (reader/read-deps))
      args-map (deps/combine-aliases deps-map aliases)
      resolved-universe (deps/resolve-deps deps-map args-map)
      direct-deps (keys (merge (:deps deps-map) (:extra-deps args-map)))
      resolved-local (select-keys resolved-universe direct-deps)]
  (last (coord->versions (first resolved-local) deps-map)))
