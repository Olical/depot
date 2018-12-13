# Depot [![Clojars Project](https://img.shields.io/clojars/v/olical/depot.svg)](https://clojars.org/olical/depot)

Find newer versions of your dependencies in your `deps.edn` file using the [Clojure CLI][cli]. This works for maven _and_ git dependencies.

## Usage

You can try it out easily with this one liner:

```bash
$ clojure -Sdeps '{:deps {olical/depot {:mvn/version "1.5.0"}}}' -m depot.outdated.main

|          Dependency | Current | Latest |
|---------------------+---------+--------|
| org.clojure/clojure |   1.8.0 |  1.9.0 |
```

I'd recommend adding depot as an alias in your own `deps.edn` file, this will allow it to check itself for updates:

> Note: Replace the ellipsis with the current version shown above.

```clojure
{:deps {}
 :aliases {:outdated {:extra-deps {olical/depot {:mvn/version "..."}}
                      :main-opts ["-m" "depot.outdated.main"]}}}
```

```bash
$ clojure -Aoutdated -a outdated

|   Dependency | Current | Latest |
|--------------+---------+--------|
| olical/depot |   ..... |  ..... |
```

### Updating `deps.edn`

To automatically update the versions in `deps.edn`, use `--update`.

```bash
$ clojure -m depot.outdated.main --update
Updating: deps.edn
  rewrite-clj {:mvn/version "0.6.0"} -> {:mvn/version "0.6.1"}
  cider/cider-nrepl {:mvn/version "0.17.0"} -> {:mvn/version "0.18.0"}
  clj-time {:mvn/version "0.14.4"} -> {:mvn/version "0.15.1"}
  olical/cljs-test-runner {:sha "5a18d41648d5c3a64632b5fec07734d32cca7671"} -> {:sha "da9710b389782d4637ef114176f6e741225e16f0"}
```

This will leave any formatting, whitespace, and comments intact. It will update
both the top level deps and any `:aliases` / `:extra-deps`. To prevent Depot
from touching certain parts of your `deps.edn`, mark them with the
`^:depot/ignore` metadata.

``` clojure
{:deps {...}

 :aliases
 {;; used for testing against older versions of Clojure
  :clojure-1.8 ^:depot/ignore {:extra-deps
                               {org.clojure/clojure {:mvn/version "1.8.0"}}}
  :clojure-1.9 ^:depot/ignore {:extra-deps
                               {org.clojure/clojure {:mvn/version "1.9.0"}}}}}
```

`--update` by default looks for `deps.edn` in the current working directory. You
can instead pass one or more filenames in explicitly.

``` bash
$ clojure -m depot.outdated.main --update ../my-project/deps.edn
```

## Existing work

This project is inspired by [lein-ancient][], it relies on [version-clj][] (by the same author, [xsc][]) for parsing and comparison of version numbers.

## Contributors

 * [@Olical](https://github.com/Olical) - Initial work and general maintenance.
 * [@seancorfield](https://github.com/seancorfield) - Support for `:override-deps`.
 * [@robert-stuttaford](https://github.com/robert-stuttaford) - Presenting results in a neat table.
 * [@kennyjwilli](https://github.com/kennyjwilli) - Git dependency support and table improvements.
 * [@plexus](https://github.com/plexus) - The entire `--update` system!

## Unlicenced

Find the full [unlicense][] in the `UNLICENSE` file, but here's a snippet.

>This is free and unencumbered software released into the public domain.
>
>Anyone is free to copy, modify, publish, use, compile, sell, or distribute this software, either in source code form or as a compiled binary, for any purpose, commercial or non-commercial, and by any means.

Do what you want. Learn as much as you can. Unlicense more software.

[unlicense]: http://unlicense.org/
[lein-ancient]: https://github.com/xsc/lein-ancient
[version-clj]: https://github.com/xsc/version-clj
[xsc]: https://github.com/xsc
[cli]: https://clojure.org/guides/deps_and_cli
