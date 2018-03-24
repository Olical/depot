(ns depot.outdated.main
  (:require [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as reader]
            [clojure.tools.deps.alpha.util.maven :as maven]
            [version-clj.core :as version]
            [clojure.set :as set])
  (:import [org.eclipse.aether.resolution VersionRangeRequest]))

(def version-types #{:snapshot :qualified :release})

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
    {:selected selected
     :types (group-by version-type versions)}))

(defn find-latest [types consider-types]
  (when-not (set/subset? consider-types version-types)
    (throw (Error. (str "Unrecognised version types "
                        (set/difference consider-types version-types)
                        " must be subset of " version-types))))
  (let [versions (->> (select-keys types consider-types)
                      (vals)
                      (apply concat))]
    (-> (sort version/version-compare versions)
        (last))))

(defn show-outdated! [consider-types aliases]
  (let [deps-map (-> (reader/clojure-env)
                     (:config-files)
                     (reader/read-deps))
        args-map (deps/combine-aliases deps-map aliases)
        resolved-universe (deps/resolve-deps deps-map args-map)
        direct-deps (keys (merge (:deps deps-map) (:extra-deps args-map)))
        resolved-local (select-keys resolved-universe direct-deps)]
    (doseq [[lib coord] resolved-local]
      (let [versions (coord->version-status lib coord deps-map)
            latest (find-latest (:types versions) consider-types)
            selected (-> versions :selected)]
        (when (= (version/version-compare latest selected) 1)
          (println (str lib ":") selected "=>" latest))))))

(defn -main [& args]
  (show-outdated! #{:release :qualified} #{:test})
  (shutdown-agents))
