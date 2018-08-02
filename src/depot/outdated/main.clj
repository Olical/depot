(ns depot.outdated.main
  (:require [clojure.pprint :as pprint]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.java.shell :as sh]
            [clojure.tools.cli :as cli]
            [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as reader]
            [clojure.tools.deps.alpha.util.maven :as maven]
            [clojure.tools.deps.alpha.extensions :as ext]
            [version-clj.core :as version])
  (:import [org.eclipse.aether.resolution VersionRangeRequest]))

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
  [lib coord {:keys [deps-map consider-types]}]
  (let [{:keys [types selected]} (coord->version-status lib coord deps-map)
        latest                   (find-latest types consider-types)]
    (when (and (not (str/blank? selected))
               (not (str/blank? latest))
               (= (version/version-compare latest selected) 1))
      {"Current" selected
       "Latest"  latest})))

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
  (let [{:keys [exit out err]} (sh/sh "git" "ls-remote" (:git/url coord))
        latest-remote-sha (get (parse-git-ls-remote out) "HEAD")]
    (when (and (= exit 0)
               (neg? (ext/compare-versions
                       lib coord (assoc coord :sha latest-remote-sha) {})))
      {"Current" (:sha coord)
       "Latest"  latest-remote-sha})))

(defn current-latest-map
  "Returns a map containing `'Current'` and `'Latest'` if the dependency has a
  newer version otherwise returns `nil`."
  [lib coord data]
  (-current-latest-map lib coord data))

(defn gather-outdated [consider-types aliases]
  (let [deps-map (-> (reader/clojure-env)
                     (:config-files)
                     (reader/read-deps))
        args-map (deps/combine-aliases deps-map aliases)
        all-deps (merge (:deps deps-map) (:extra-deps args-map))]
    (->> (for [[lib coord] all-deps
               :let [outdated (current-latest-map lib coord {:consider-types consider-types
                                                             :deps-map       deps-map})]]
           (when outdated
             (assoc outdated "Dependency" lib)))
         (keep identity)
         (sort-by #(get % "Dependency")))))

(defn comma-str->keywords-set [comma-str]
  (into #{} (map keyword) (str/split comma-str #",")))

(defn keywords-set->comma-str [kws]
  (str/join "," (map name kws)))

(def version-types-str (keywords-set->comma-str version-types))

(def cli-options
  [["-a" "--aliases ALIASES" "Comma list of aliases to use when reading deps.edn"
    :default #{}
    :default-desc ""
    :parse-fn comma-str->keywords-set]
   ["-t" "--consider-types TYPES" (str "Comma list of version types to consider out of " version-types-str)
    :default #{:release}
    :default-desc "release"
    :parse-fn comma-str->keywords-set
    :validate [#(set/subset? % version-types) (str "Must be subset of " version-types)]]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{{:keys [aliases consider-types help]} :options
         summary :summary} (cli/parse-opts args cli-options)]
    (if help
      (println summary)
      (let [outdated (gather-outdated consider-types aliases)]
        (if (empty? outdated)
          (println "All up to date!")
          (do (pprint/print-table ["Dependency" "Current" "Latest"] outdated)
              (println)))))
    (shutdown-agents)))
