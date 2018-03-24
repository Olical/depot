# Depot [![Clojars Project](https://img.shields.io/clojars/v/olical/depot.svg)](https://clojars.org/olical/depot)

Find newer versions of your dependencies in your `deps.edn` file.

## Usage

You can try it out easily with this one liner:

```bash
$ clojure -Sdeps '{:deps {olical/depot {:mvn/version "1.0.0"}}}' -m depot.outdated.main --consider-types release,qualified --aliases test
org.clojure/clojure: 1.9.0 => 1.10.0-alpha4
clj-time/clj-time: 0.13.0 => 0.14.2
```

This will check for any new release or qualified (alpha, beta etc) versions in your base deps with the test alias applied on top. Use `--help` to see more information on the arguments.

You may want to add this dependency and the `-m depot.outdated.main` argument to your `~/.clojure/deps.edn` file for convenience.

## Existing work

This project is inspired by [lein-ancient][], it actually relies on [version-clj][] (by the same author, [xsc][]) for parsing and comparison of version numbers.

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
