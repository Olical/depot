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
    :default #{}
    :default-desc ""
    :parse-fn comma-str->keywords-set]
   ["-t" "--consider-types TYPES" (str "Comma list of version types to consider out of " version-types-str)
    :default #{:release}
    :default-desc "release"
    :parse-fn comma-str->keywords-set
    :validate [#(set/subset? % depot/version-types) (str "Must be subset of " depot/version-types)]]
   ["-o" "--overrides" "Consider overrides for updates instead of pinning to them."]
   ["-u" "--update" "Update deps.edn, or filenames given as additional command line arguments."]
   ["-r" "--resolve-virtual" "Convert -SNAPSHOT/RELEASE/LATEST versions into immutable references."]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{{:keys [aliases consider-types overrides help update resolve-virtual]} :options
         files :arguments
         summary :summary} (cli/parse-opts args cli-options)]
    (cond
      help
      (do
        (println "USAGE: clojure -m depot.outdated.main [OPTIONS] [FILES]\n")
        (println summary))

      update
      (if (seq files)
        (run! #(depot.outdated.update/update-deps-edn! % consider-types)
              files)
        (depot.outdated.update/update-deps-edn! "deps.edn" consider-types))


      resolve-virtual
      (if (seq files)
        (run! depot.outdated.resolve-virtual/update-deps-edn! files)
        (depot.outdated.resolve-virtual/update-deps-edn! "deps.edn"))

      :else
      (let [outdated (depot/gather-outdated consider-types aliases overrides)]
        (if (empty? outdated)
          (println "All up to date!")
          (do (pprint/print-table ["Dependency" "Current" "Latest"] outdated)
              (println)))))
    (shutdown-agents)))
