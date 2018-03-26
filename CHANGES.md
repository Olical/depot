# Depot changes

## 1.0.1

 * Only try to check dependencies that use the `:mvn/version` key, attempting to parse git shas was causing an error. [#1](https://github.com/Olical/depot/issues/1)

## 1.0.0

 * Initial release with `depot.outdated.main` that checks for outdated dependencies in your `deps.edn` file.
