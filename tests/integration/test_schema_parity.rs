//! Integration Scenario T011 – Schema/migration parity across CLI, Server, Desktop, Android.
//! This test is intentionally failing (panic) until the unified node schema + migrations are wired up.

use anyhow::Result;

#[tokio::test(flavor = "multi_thread")]
async fn scenario_t011_schema_parity_pending() -> Result<()> {
    panic!(
        "T011 pending: create DB via CLI, open via server/desktop/android, verify canonical node schema and indexes."
    );
}
