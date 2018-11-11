# Depot changes

## v1.4.0

 * Merge [#6](https://github.com/Olical/depot/pull/6) - Updating `deps.edn` automatically with `--update`.

Thank you very much, [@plexus](https://github.com/plexus)!

## v1.3.0

 * Merge [#5](https://github.com/Olical/depot/pull/5) - Support `:override-deps`.
 * Bump `org.clojure/tools.cli` to `0.4.1`.
 * Bump `org.clojure/tools.deps.alpha` to `0.5.460`.

## v1.2.0

 * Merge [#4](https://github.com/Olical/depot/pull/4) - Add support for git dependencies.

Thank you, [@kennyjwilli](https://github.com/kennyjwilli), great contribution.

## v1.1.0

 * Merge [#3](https://github.com/Olical/depot/pull/3)
   * Only consider changes when selected and latest are non-blank strings.
   * Gather changes and print a nice table rather than printing as differences are found.
   * Print a message if no changes can be found.

Thank you very much, [@robert-stuttaford](https://github.com/robert-stuttaford)!

## v1.0.1

 * Only try to check dependencies that use the `:mvn/version` key, attempting to parse git shas was causing an error. [#1](https://github.com/Olical/depot/issues/1)

## v1.0.0

 * Initial release with `depot.outdated.main` that checks for outdated dependencies in your `deps.edn` file.
