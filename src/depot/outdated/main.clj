(ns depot.outdated.main
  (:require [clojure.pprint :as pprint]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [clojure.tools.deps.alpha :as deps]
            [clojure.tools.deps.alpha.reader :as reader]
            [clojure.tools.deps.alpha.util.maven :as maven]
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
  (let [versions (->> (select-keys types consider-types)
                      (vals)
                      (apply concat))]
    (-> (sort version/version-compare versions)
        (last))))

(defn gather-outdated [consider-types aliases]
  (let [deps-map (-> (reader/clojure-env)
                     (:config-files)
                     (reader/read-deps))
        args-map (deps/combine-aliases deps-map aliases)
        resolved-universe (deps/resolve-deps deps-map args-map)
        direct-deps (->> (merge (:deps deps-map) (:extra-deps args-map))
                         (filter (comp :mvn/version val))
                         (map first))]
    (->> (for [[lib coord] (select-keys resolved-universe direct-deps)]
           (let [{:keys [types selected]} (coord->version-status lib coord deps-map)
                 latest                   (find-latest types consider-types)]
             (when (and (not (str/blank? selected))
                        (not (str/blank? latest))
                        (= (version/version-compare latest selected) 1))
               {"Dependency" lib
                "Current"    selected
                "Latest"     latest})))
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
          (do (pprint/print-table outdated)
              (println)))))
    (shutdown-agents)))
