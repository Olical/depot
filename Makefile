.PHONY: dev test pom

dev:
	clojure -Adev

test:
	clojure -Atest -m depot.outdated.main -a test -t release,qualified

pom:
	clj -Spom; and xmllint --format pom.xml -o pom.xml
