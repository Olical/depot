# Depot [![Clojars Project](https://img.shields.io/clojars/v/olical/depot.svg)](https://clojars.org/olical/depot)

> Warning: I'm not actively maintaining this project, I'm finally releasing `v2+` since it's been sitting around for far too long. It contains a bunch of breaking changes that are mentioned in `CHANGES.md`.
>
> You may have better luck with [liquidz/antq](https://github.com/liquidz/antq) or [slipset/deps-ancient](https://github.com/slipset/deps-ancient). If you'd like to see this project developed and maintained further I'm open to inviting other maintainers or promoting forks. I'm reluctant to transfer ownership and publishing rights to the artifact since that's been used for malicious purposes in recent times.
>
> If this version isn't to your liking you can still rely on `v1.8.4` (or earlier) and the legacy documentation in `README-v1.md`.

Find newer versions of your dependencies in your `deps.edn` file using the [Clojure CLI][cli]. This works for maven _and_ git dependencies.

## Usage

You can try it out easily with this one liner:

```bash
$ clojure -Sdeps '{:deps {olical/depot {:mvn/version "RELEASE"}}}' -M -m depot.outdated.main
Checking for old versions in: deps.edn
  org.clojure/clojure {:mvn/version "1.9.0"} -> {:mvn/version "1.12.1"}
```

I'd recommend adding depot as an alias in your own `deps.edn` file, this will allow it to check itself for updates:

```clojure
{:deps {}
 :aliases {:outdated {:replace-deps {olical/depot {:mvn/version "2.4.1"}}
                      :main-opts ["-m" "depot.outdated.main"]}}}
```

```bash
$ clojure -Aoutdated --aliases outdated
Checking for old versions in: deps.edn
  olical/depot {:mvn/version "..."} -> {:mvn/version "..."}
```

### Controlling which files are checked

By default Depot looks for `deps.edn` in the current working directory. You
can instead pass one or more filenames in explicitly.

```bash
$ clojure -Aoutdated ../my-project/deps.edn
```

### Controlling which part of the file are checked

By default, only dependencies under the top-level `:deps` are considered.

To also consider `:default-deps`, `:extra-deps` and `:override-deps` under aliases,
use the `--aliases alias1,alias2` to specify alias,
or the `--every` flag to consider all aliases.

To prevent Depot from touching certain parts of your `deps.edn`, mark
them with the `^:depot/ignore` metadata.

```clojure
{:deps {...}

 :aliases
 {;; used for testing against older versions of Clojure
  :clojure-1.8 ^:depot/ignore {:extra-deps
                               {org.clojure/clojure {:mvn/version "1.8.0"}}}
  :clojure-1.9 ^:depot/ignore {:extra-deps
                               {org.clojure/clojure {:mvn/version "1.9.0"}}}}}
```

The metadata can be placed on the artifact name, the coord map or on
any map containing the dependency in question.

### Updating `deps.edn`

By default, depot only prints the new versions. To update the file in
place, include the `--write` flag. This will leave any formatting,
whitespace, and comments intact.

```bash
$ clojure -Aoutdated --write
Updating old versions in: deps.edn
  rewrite-clj {:mvn/version "0.6.0"} -> {:mvn/version "0.6.1"}
  cider/cider-nrepl {:mvn/version "0.17.0"} -> {:mvn/version "0.18.0"}
  clj-time {:mvn/version "0.14.4"} -> {:mvn/version "0.15.1"}
  olical/cljs-test-runner {:git/sha "5a18d41648d5c3a64632b5fec07734d32cca7671"} -> {:git/sha "da9710b389782d4637ef114176f6e741225e16f0"}
```

### Freezing snapshots

Maven has a concept called "virtual" versions, these are similar to Git branches, they are pointers to another version, and the version they point to can change over time. The best known example are snapshot releases. When your `deps.edn` refers to a version `0.4.1-SNAPSHOT`, the version that actually gets installed will look like `0.4.1-20190222.154954-1`.

A maintainer can publish as many snapshots as they like, all with the same version string. This means that re-running the same code twice might yield different results, if in the meanwhile a new snapshot was released. So installing `0.4.1-SNAPSHOT` again later on may install a completely different version.

For the sake of stability and reproducibility it may be desirable to "lock" this version. This is what the `--resolve-virtual` flag is for. The `--resolve-virtual` flag will resolve the virtual version to the current timestamped version that the SNAPSHOT is an alias of, so that your code is once again deterministic.

Besides `SNAPSHOT` versions `--resolve-virtual` will also handle the special version strings `"RELEASE"` and `"LATEST"`

```
% clojure -Aoutdated --resolve-virtual
Checking virtual versions in: deps.edn
   cider/piggieback {:mvn/version "0.4.1-SNAPSHOT"} -> {:mvn/version "0.4.1-20190222.154954-1"}
```

## Existing work

This project is inspired by [lein-ancient][], it relies on [version-clj][] (by the same author, [xsc][]) for parsing and comparison of version numbers.

## Contributors

- [@Olical](https://github.com/Olical) - Initial work and general maintenance.
- [@daaku](https://github.com/daaku) - Ensuring `:override-deps` is adhered to in the non-mutating mode.
- [@kennyjwilli](https://github.com/kennyjwilli) - Git dependency support and table improvements.
- [@lverns](https://github.com/lverns) - Reducing the runtime significantly by making multiple requests in parallel.
- [@plexus](https://github.com/plexus) - Both the `--update` and `--resolve-virtual` systems, so many improvements!
- [@robert-stuttaford](https://github.com/robert-stuttaford) - Presenting results in a neat table.
- [@seancorfield](https://github.com/seancorfield) - Support for `:override-deps`.
- [@dharrigan](https://github.com/dharrigan) - Bump dependencies, fixing warnings.
- [@dotemacs](https://github.com/dotemacs) - Updating dependencies, supporting newer `tools.deps.alpha` versions.
- [@timothypratley](https://github.com/timothypratley) - Adding `:default-deps` support.

## Unlicenced

Find the full [unlicense][] in the `UNLICENSE` file, but here's a snippet.

> This is free and unencumbered software released into the public domain.
>
> Anyone is free to copy, modify, publish, use, compile, sell, or distribute this software, either in source code form or as a compiled binary, for any purpose, commercial or non-commercial, and by any means.

Do what you want. Learn as much as you can. Unlicense more software.

[unlicense]: http://unlicense.org/
[lein-ancient]: https://github.com/xsc/lein-ancient
[version-clj]: https://github.com/xsc/version-clj
[xsc]: https://github.com/xsc
[cli]: https://clojure.org/guides/deps_and_cli
