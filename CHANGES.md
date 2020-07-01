# Depot changes

## v2.0.1

 * Fix `pom.xml`, there was no source code in `v2.0.0`.

## v2.0.0

 * **Breaking** Replaced the `--update` flag with the `--write` flag.
 * **Breaking** Changed scoping rules to be the same with or without the `--write` flag
 * **Breaking** Remove support for the `--overrides` flag. `:override-deps` will be checked by default.
   To ignore them, use the `:depot/ignore` metadata.
 * **Breaking** Changed scoping rules to be the same with or without the `--resolve-virtual` flag
 * **Breaking** Changed `--resolve-virtual` to be read-only unless combined with `--write`
 * Fixed inconsistent output styles
 * Added the `--every` flag which allows checking all aliases at once
 * Bump various dependencies.

## v1.8.4

 * Republish v1.8.3 without the `target` directory which contained some old AOTed classes. Thanks for the heads up, Sean Corfield!

## v1.8.3

 * Merge [#24](https://github.com/Olical/depot/pull/24) - Replace calls to `clojure-env` with new API.

## v1.8.2

 * Merge [#21](https://github.com/Olical/depot/pull/21) - Handle uneval nodes, fixing [#20](https://github.com/Olical/depot/issues/20).

`--resolve-virtual` should now be stable, all thanks to [@plexus](https://github.com/plexus).

## v1.8.1

 * Merge [#19](https://github.com/Olical/depot/pull/19) - Fix depot.zip/left, clean up whitespace.

## v1.8.0

 * Merge [#17](https://github.com/Olical/depot/pull/17) - Resolve snapshots.

## v1.7.0

 * Merge [#16](https://github.com/Olical/depot/pull/16) - Reduce the runtime by making multiple requests in parallel.

## v1.6.0

 * Merge [#15](https://github.com/Olical/depot/pull/15) - Consider overrides in the outdated check mode too.

## v1.5.1

 * Merge [#13](https://github.com/Olical/depot/pull/13) - Fix a couple of issues with `--update`.

## v1.5.0

 * Merge [#9](https://github.com/Olical/depot/pull/9) - Ignore "RELEASE" and "LATEST", bring back Clojure 1.8 compatibility (as mentioned in [#7](https://github.com/Olical/depot/issues/7)).
 * Merge [#10](https://github.com/Olical/depot/pull/10) - Let --update also check :override-deps.

Even more improvements from [@plexus](https://github.com/plexus)! I also sorted out some formatting and deleted some unused forms, but that's all.

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
