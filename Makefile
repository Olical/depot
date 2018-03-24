.PHONY: dev

dev:
	clojure -Adev

test:
	clojure -Atest -m depot.outdated.main -a test -t release,qualified
