.PHONY: cider prepl test pom

cider:
	clojure -Acider

prepl:
	clojure -Aprepl

test:
	clojure -Atest -m depot.outdated.main -a test -t release,qualified

pom:
	clojure -Spom
	xmllint --format pom.xml -o pom.xml
