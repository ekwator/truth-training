.PHONY: doc-refactor-setup doc-refactor-run doc-refactor-test

PYTHON ?= python3

doc-refactor-setup:
	$(PYTHON) -m venv .venv
	. .venv/bin/activate && pip install -r requirements.txt

doc-refactor-run:
	$(PYTHON) scripts/doc_refactor/main.py run --phases all

doc-refactor-test:
	pytest scripts/doc_refactor/tests

