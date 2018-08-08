# Depot [![Clojars Project](https://img.shields.io/clojars/v/olical/depot.svg)](https://clojars.org/olical/depot)

Find newer versions of your dependencies in your `deps.edn` file using the [Clojure CLI][cli]. This works for maven _and_ git dependencies.

## Usage

You can try it out easily with this one liner:

```bash
$ clojure -Sdeps '{:deps {olical/depot {:mvn/version "1.2.0"}}}' -m depot.outdated.main

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

## Existing work

This project is inspired by [lein-ancient][], it relies on [version-clj][] (by the same author, [xsc][]) for parsing and comparison of version numbers.

## Ideas

Here's a few things I'd like to add some day, feel free to discuss or suggest more:

 * Searching for dependencies.
 * `deps.edn` manipulation like npm with `package.json`.

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
