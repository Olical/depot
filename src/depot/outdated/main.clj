(ns depot.outdated.main
  (:require [clojure.pprint :as pprint]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [depot.outdated :as depot]
            [depot.outdated.update]
            [depot.outdated.resolve-virtual]))

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
   ["-o" "--overrides" "Consider overrides for updates instead of pinning to them."]
   ["-e" "--every" "Expand search to all aliases, include overrides."]
   ["-w" "--write" "Instead of just printing changes, write them back to the file."]
   ["-r" "--resolve-virtual" "Convert -SNAPSHOT/RELEASE/LATEST versions into immutable references."]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{{:keys [aliases consider-types overrides every help write resolve-virtual]} :options
         files :arguments
         summary :summary} (cli/parse-opts args cli-options)]
    (cond
      help
      (do
        (println "USAGE: clojure -m depot.outdated.main [OPTIONS] [FILES]\n")
        (println " If no files are given, defaults to using \"deps.edn\".\n")
        (println summary))

      resolve-virtual
      (if (seq files)
        (run! depot.outdated.resolve-virtual/update-deps-edn! files)
        (depot.outdated.resolve-virtual/update-deps-edn! "deps.edn"))

      :else
      (let [files (if (seq files) files ["deps.edn"])
            overrides (or (when every true) overrides)
            check-alias? (if every (constantly true) (set aliases))]
        (when (and every aliases)
          (println "--every and --aliases are mutually exclusive.")
          (System/exit 1))
        (run! #(depot.outdated.update/apply-new-versions % consider-types check-alias? overrides write)
              files)))
    (shutdown-agents)))
