(ns depot.outdated
  (:require [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.tools.deps.alpha] ;; need for multimethods
            [clojure.tools.deps.alpha.extensions :as ext]
            [clojure.tools.deps.alpha.util.maven :as maven]
            [depot.zip :as dzip]
            [version-clj.core :as version])
  (:import org.apache.maven.repository.internal.MavenRepositorySystemUtils
           [org.eclipse.aether RepositorySystem RepositorySystemSession]
           org.eclipse.aether.resolution.VersionRangeRequest))

(def version-types #{:snapshot :qualified :release})

(defn version-type [v]
  (let [type (-> (version/version->seq v)
                 (last)
                 (first))]
    (cond
      (= type "snapshot") :snapshot
      (string? type) :qualified
      (integer? type) :release
      :else :unrecognised)))

(defn make-session
  ^RepositorySystemSession [^RepositorySystem system local-repo]
  (let [session (MavenRepositorySystemUtils/newSession)
        local-repo-mgr (.newLocalRepositoryManager system session (maven/make-local-repo local-repo))]
    (.setLocalRepositoryManager session local-repo-mgr)
    session))

(defn coord->version-status [lib coord {:keys [mvn/repos depot/local-maven-repo]}]
  (let [local-repo (or local-maven-repo maven/default-local-repo)
        remote-repos (mapv maven/remote-repo repos)
        system (maven/make-system)
        session (make-session system local-repo)
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
  (let [versions (->> (select-keys types consider-types)
                      (vals)
                      (apply concat))]
    (-> (sort version/version-compare versions)
        (last))))

(defmulti -current-latest-map (fn [_ coord _] (ext/coord-type coord)))

(defmethod -current-latest-map :default
  [_ _ _]
  nil)

(defmethod -current-latest-map :mvn
  [lib coord {:keys [consider-types] :as config}]
  (let [{:keys [types selected]} (coord->version-status lib coord config)
        latest                   (find-latest types consider-types)]
    (when (and (not (str/blank? selected))
               (not (str/blank? latest))
               (= (version/version-compare latest selected) 1))
      {:current selected
       :latest  latest})))

(defn- parse-git-ls-remote
  "Returns a map of ref name to the latest sha for that ref name."
  [s]
  (let [lines (-> s
                  (str/trim-newline)
                  (str/split #"\n"))]
    (into {}
          (comp
            (map (fn [x]
                   (-> x
                       (str/triml)
                       (str/split #"\t")
                       (reverse)
                       (vec))))
            (filter #(= 2 (count %))))
          lines)))

(defmethod -current-latest-map :git
  [lib coord _]
  (let [{:keys [exit out]} (sh/sh "git" "ls-remote" (:git/url coord))
        latest-remote-sha (get (parse-git-ls-remote out) "HEAD")]
    (when (and (= exit 0)
               (neg? (ext/compare-versions
                       lib coord (assoc coord :sha latest-remote-sha) {})))
      {:current (:sha coord)
       :latest  latest-remote-sha})))

(defn current-latest-map
  "Returns a map containing `:current` and `:latest` if the dependency has a
  newer version otherwise returns `nil`."
  [lib coord config]
  (-current-latest-map lib coord config))

(defn newer-versions
  "Find all deps in a `:deps` or `:extra-deps` or `:override-deps` map to be updated,
  at the top level and in aliases.

  `loc` points at the top level map."
  [loc config]
  (dzip/mapped-libs
   loc
   (fn [artifact coords]
     (let [[old-version version-key]
           (or (some-> coords :mvn/version (vector :mvn/version))
               (some-> coords :sha (vector :sha)))
           new-version (-> (current-latest-map artifact
                                                     coords
                                                     config)
                           (get :latest))]
       (when (and old-version
                  ;; ignore these Maven 2 legacy identifiers
                  (not (#{"RELEASE" "LATEST"} old-version))
                  new-version)
         {:version-key version-key
          :old-version old-version
          :new-version new-version})))))
