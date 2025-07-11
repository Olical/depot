(ns depot.outdated.main
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [depot.outdated :as depot]
            [depot.outdated.update :as update]
            [depot.outdated.resolve-virtual :as resolve-virtual]))

(defn comma-str->keywords-set [comma-str]
  (into #{} (map keyword) (str/split comma-str #",")))

(defn keywords-set->comma-str [kws]
  (str/join "," (map name kws)))

(def version-types-str (keywords-set->comma-str depot/version-types))

(def cli-options
  [["-a" "--aliases ALIASES" "Comma list of aliases to use when reading deps.edn"
    :parse-fn comma-str->keywords-set]
   ["-t" "--consider-types TYPES" (str "Comma list of version types to consider out of " version-types-str)
    :default #{:release}
    :default-desc "release"
    :parse-fn comma-str->keywords-set
    ;; TODO: check the :errors after parsing for this error
    :validate [#(set/subset? % depot/version-types) (str "Must be subset of " depot/version-types)]]
   ["-e" "--every" "Expand search to all aliases."]
   ["-w" "--write" "Instead of just printing changes, write them back to the file."]
   ["-r" "--resolve-virtual" "Convert -SNAPSHOT/RELEASE/LATEST versions into immutable references."]
   ["-f" "--fail" "Return a non-zero exit status if anything was out of date."]
   ["-h" "--help"]])

(def ^:private messages
  {:resolve-virtual {:start-read-only "Checking virtual versions in: %s"
                     :start-write "Resolving virtual versions in: %s"
                     :no-changes "  No virtual versions found"}
   :update-old {:start-read-only "Checking for old versions in: %s"
                :start-write "Updating old versions in: %s"
                :no-changes "  All up to date!"}})

(defn- exit [code]
  (shutdown-agents)
  (System/exit code))

(defn -main [& args]
  (let [{{:keys [aliases consider-types every help write resolve-virtual fail]} :options
         files :arguments
         summary :summary} (cli/parse-opts args cli-options)]
    (cond
      help
      (do
        (println "USAGE: clojure -m depot.outdated.main [OPTIONS] [FILES]\n")
        (println " If no files are given, defaults to using \"deps.edn\".\n")
        (println summary))

      :else
      (let [files (if (seq files) files ["deps.edn"])
            check-alias? (if every (constantly true) (set aliases))
            messages (if resolve-virtual
                       (:resolve-virtual messages)
                       (:update-old messages))
            new-versions (if resolve-virtual
                           resolve-virtual/pinned-versions
                           depot/newer-versions)]
        (when (and every aliases)
          (println "--every and --aliases are mutually exclusive.")
          (exit 1))
        (let [application-results
              (mapv #(update/apply-new-versions
                      % consider-types check-alias?
                      write messages new-versions)
                    files)]
          (when (and fail (some :newer-deps? application-results))
            (exit 1)))))
    (exit 0)))
