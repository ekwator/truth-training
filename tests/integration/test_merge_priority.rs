//! Integration Scenario T017 – Merge priority (Local > Global).

use anyhow::Result;

#[tokio::test(flavor = "multi_thread")]
async fn scenario_t017_merge_priority_pending() -> Result<()> {
    panic!(
        "T017 pending: seed duplicate LAN/GLOBAL entries, execute merge workflow, assert LAN/Wi-Fi records win per FR-011."
    );
}
