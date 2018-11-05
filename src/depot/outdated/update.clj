(ns depot.outdated.update
  (:require [clojure.tools.deps.alpha.reader :as reader]
            [depot.outdated :as depot]
            [rewrite-clj.zip :as rzip]))

(defn zget [loc key]
  (rzip/right
   (rzip/find-value (rzip/down loc) (comp rzip/right rzip/right) key)))

(defn update-loc? [loc]
  (not (rzip/find loc
                  rzip/up
                  (fn [loc]
                    (:depot/ignore (meta (rzip/sexpr loc)))))))

(defn try-update-artifact [loc consider-types repos]
  (if (update-loc? loc)
    (let [artifact (rzip/sexpr loc)
          coords-loc (rzip/right loc)]
      (if-let [latest (get (depot/current-latest-map artifact
                                                     (rzip/sexpr coords-loc)
                                                     {:consider-types consider-types
                                                      :deps-map repos})
                           "Latest")]
        (if-let [[version-loc version-key]
                 (or (some-> (zget coords-loc :mvn/version) (vector :mvn/version))
                     (some-> (zget coords-loc :sha) (vector :sha)))]
          (do
            (binding [*print-namespace-maps* false]
              (println " " artifact (pr-str {version-key (rzip/sexpr version-loc)}) "->" (pr-str {version-key latest})))
            (rzip/left
             (rzip/up
              (rzip/replace version-loc latest))))
          loc)
        loc))
    loc))

(defn update-deps [loc consider-types repos]
  (rzip/up
   (loop [loc (rzip/down loc)]
     (let [loc' (try-update-artifact loc consider-types repos)
           loc'' (rzip/right (rzip/right loc'))]
       (if loc''
         (recur loc'')
         loc')))))

(defn zmap-vals [loc f & args]
  (let [loc' (rzip/down loc)]
    (if loc'
      (loop [keyloc loc']
        (let [valloc (apply f (rzip/right keyloc) args)]
          (if-let [next-keyloc (rzip/right valloc)]
            (recur next-keyloc)
            (rzip/up valloc))))
      loc)))

(declare update-all)

(defn update-aliases
  "Update all :aliases, expects a loc pointing at the :aliases map."
  [loc consider-types repos]
  (zmap-vals loc update-all consider-types repos))

(defn assert= [x y]
  (when (not= x y)
    (let [msg `(~'not= ~x ~y)]
      (throw (ex-info (prn-str msg) {:error msg})))))

(defn update-all [loc consider-types repos]
  (let [deps-loc (zget loc :deps)
        loc (if deps-loc
              (rzip/up (update-deps deps-loc consider-types repos))
              loc)
        extra-deps-loc (zget loc :extra-deps)
        loc (if extra-deps-loc
              (rzip/up (update-deps extra-deps-loc consider-types repos))
              loc)
        aliases-loc (zget loc :aliases)
        loc (if aliases-loc
              (rzip/up (update-aliases aliases-loc consider-types repos))
              loc)]
    loc))

(defn update-deps-edn! [file consider-types]
  (println "Updating:" file)
  (let [deps  (-> (reader/clojure-env)
                  :config-files
                  reader/read-deps)
        repos (select-keys deps [:mvn/repos :mvn/local-repo])
        loc   (rzip/of-file file)
        old-deps (slurp file)
        new-deps (rzip/root-string
                  (update-all loc consider-types repos))]
    (if (= old-deps new-deps)
      (println "  All up to date!")
      (try
        (spit file new-deps)
        (catch java.io.FileNotFoundException e
          (println "  [ERROR] Permission denied: " file))))))
