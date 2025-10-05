## Expert System (Heuristics)

Implementation: `core_lib::expert_simple`
- Questions: TriState (yes/no/unknown), weights, truth_bias per question.
- Evaluation: maps answers to [0..1], normalizes to score [-1..1], computes confidence from answered ratio with unknown penalty, suggests detected if confidence >= 0.6.
- CLI: `app` exposes `assess` command; can auto-apply detected=TRUE when confident.

Open improvements
- Calibrate weights per context; persist suggestions and rationale to statements.
- Integrate with rating protocol (use score/confidence as prior for S_e).
