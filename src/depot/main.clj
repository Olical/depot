(ns depot.main
  (:require [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as reader]
            [clojure.tools.deps.alpha.util.maven :as maven]
            [version-clj.core :as version])
  (:import [org.eclipse.aether.resolution VersionRangeRequest]))

;; TODO Make this configurable.
(def aliases #{:test})

(defn version-type [v]
  (let [type (-> (version/version->seq v)
                 (last)
                 (first))]
    (cond
      (= type "snapshot") :snapshot
      (string? type) :qualified
      (int? type) :release
      :else :unrecognised)))

(defn coord->version-status [lib coord {:keys [mvn/repos mvn/local-repo]}]
  (let [local-repo (or local-repo maven/default-local-repo)
        remote-repos (mapv maven/remote-repo repos)
        system (maven/make-system)
        session (maven/make-session system local-repo)
        selected (:mvn/version coord)
        artifact (maven/coord->artifact lib (assoc coord :mvn/version "[0,)"))
        versions-req (doto (new VersionRangeRequest)
                       (.setArtifact artifact)
                       (.setRepositories remote-repos))
        versions (->> (.resolveVersionRange system session versions-req)
                      (.getVersions)
                      (map str))]
    (merge {:selected selected}
           (group-by version-type versions))))

(let [deps-map (-> (reader/clojure-env)
                   (:config-files)
                   (reader/read-deps))
      args-map (deps/combine-aliases deps-map aliases)
      resolved-universe (deps/resolve-deps deps-map args-map)
      direct-deps (keys (merge (:deps deps-map) (:extra-deps args-map)))
      resolved-local (select-keys resolved-universe direct-deps)]
  (doseq [[lib coord] resolved-local]
    (let [versions (coord->version-status lib coord deps-map)
          latest (-> versions :release last)
          selected (-> versions :selected)]
      (when (= (version/version-compare latest selected) 1)
        (println lib (str "(" selected ")") "can be updated to" latest)))))
