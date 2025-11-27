.PHONY: doc-refactor-run doc-refactor-test

PYTHON ?= python3

doc-refactor-run:
	$(PYTHON) scripts/doc_refactor/main.py run --phases all

doc-refactor-test:
	pytest scripts/doc_refactor/tests

