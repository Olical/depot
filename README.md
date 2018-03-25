# Depot [![Clojars Project](https://img.shields.io/clojars/v/olical/depot.svg)](https://clojars.org/olical/depot)

Find newer versions of your dependencies in your `deps.edn` file using the [Clojure CLI][cli].

## Usage

You can try it out easily with this one liner:

```bash
$ clojure -Sdeps '{:deps {olical/depot {:mvn/version "1.0.0"}}}' -m depot.outdated.main
org.clojure/clojure: 1.8.0 => 1.9.0
```

You can add the following alias to your `deps.edn`:

```clojure
{:aliases {:outdated {:extra-deps {olical/depot {:mvn/version "1.0.0"}}
                      :main-opts ["-m" "depot.outdated.main"]}}}
```

Now you may execute the namespace with a smaller command:

```bash
$ clojure -Aoutdated --help
  -a, --aliases ALIASES                Comma list of aliases to use when reading deps.edn
  -t, --consider-types TYPES  release  Comma list of version types to consider out of qualified,release,snapshot
  -h, --help
```

## Existing work

This project is inspired by [lein-ancient][], it relies on [version-clj][] (by the same author, [xsc][]) for parsing and comparison of version numbers.

## Ideas?

This is a very young project and as such it's ripe for new features, feel free to suggest them! Things I would like to do:

 * More options to configure which `deps.edn` file to check.
 * Git support.
 * Automatically rewriting your `deps.edn` file.
 * General `deps.edn` manipulation like npm. `depot add some-awesome-dependency` for example.
 * Searching Clojars and adding the ones you want, like Arch Linux's pacman.

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
