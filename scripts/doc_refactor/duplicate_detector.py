"""Duplicate detection using TF-IDF cosine similarity."""

from __future__ import annotations

import json
import os
from pathlib import Path
from typing import List, Optional

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

from .models import DocumentationFile

DEFAULT_REPORT_DIR = Path(os.environ.get("DOC_REFACTOR_REPORT_DIR", "reports/doc_refactor"))


def run_duplicate_detection(
    root: Path,
    records: List[DocumentationFile],
    *,
    dry_run: bool = False,
    report_dir: Optional[Path] = None,
    threshold: float = 0.8,
) -> dict:
    report_directory = report_dir or DEFAULT_REPORT_DIR
    report_directory.mkdir(parents=True, exist_ok=True)

    candidate_records = [record for record in records if record.role in {"DETAIL", "SPEC"}]
    if len(candidate_records) < 2:
        payload = {"actions": []}
        (report_directory / "dedupe.json").write_text(json.dumps(payload, indent=2), encoding="utf-8")
        return payload

    texts = [_read_text(record.path) for record in candidate_records]
    vectorizer = TfidfVectorizer(stop_words="english")
    matrix = vectorizer.fit_transform(texts)
    similarities = cosine_similarity(matrix)

    actions = []
    for i in range(len(candidate_records)):
        for j in range(i + 1, len(candidate_records)):
            score = similarities[i, j]
            if score < threshold:
                continue
            category = _categorize_score(score)
            if category == "safe":
                continue
            actions.append(
                {
                    "pair": [
                        str(candidate_records[i].path.relative_to(root)),
                        str(candidate_records[j].path.relative_to(root)),
                    ],
                    "score": round(float(score), 3),
                    "category": category,
                }
            )

    payload = {"actions": actions}
    (report_directory / "dedupe.json").write_text(json.dumps(payload, indent=2), encoding="utf-8")
    return payload


def _categorize_score(score: float) -> str:
    if score >= 0.92:
        return "probable_duplicate"
    if score >= 0.85:
        return "needs_merge"
    return "safe"


def _read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="latin-1")

