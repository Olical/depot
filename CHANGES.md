# Depot changes

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
