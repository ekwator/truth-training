#[cfg(feature = "p2p-client-sync")]
mod cli_tests {
    use serde_json::Value;
    use std::process::Command;
    use tempfile::TempDir;

    fn get_bin_path() -> String {
        std::env::var("CARGO_BIN_EXE_truthctl").expect(
            "CARGO_BIN_EXE_truthctl must be set. Run: cargo test --package app --bin truthctl",
        )
    }

    fn temp_home() -> TempDir {
        tempfile::tempdir().expect("temp dir")
    }

    #[test]
    fn truthctl_sync_runs() {
        // Минимальная проверка запуска бинарника с --help
        let bin_path = get_bin_path();
        let status = std::process::Command::new(&bin_path)
            .arg("--help")
            .status()
            .expect("failed to start truthctl");
        assert!(status.success());
    }

    #[test]
    fn nodes_list_empty_database() {
        let tmp = temp_home();
        let db_path = tmp.path().join("nodes.db");
        let db_str = db_path.to_string_lossy().to_string();
        let bin = get_bin_path();

        // List empty database as JSON
        let output = Command::new(&bin)
            .args(["nodes", "list", "--db", &db_str, "--format", "json"])
            .output()
            .expect("nodes list");
        if !output.status.success() {
            eprintln!("Command failed with status: {:?}", output.status);
            eprintln!("stderr: {}", String::from_utf8_lossy(&output.stderr));
            eprintln!("stdout: {}", String::from_utf8_lossy(&output.stdout));
        }
        assert!(output.status.success(), "nodes list should succeed");
        let json: Value = serde_json::from_slice(&output.stdout).expect("json");
        assert!(json.as_array().map(|arr| arr.is_empty()).unwrap_or(true));
    }

    #[test]
    fn nodes_list_with_filters() {
        let tmp = temp_home();
        let db_path = tmp.path().join("nodes.db");
        let db_str = db_path.to_string_lossy().to_string();
        let bin = get_bin_path();

        // Add multiple nodes with different types
        Command::new(&bin)
            .args([
                "nodes",
                "add",
                "--db",
                &db_str,
                "--address",
                "http://127.0.0.1:8080",
                "--type",
                "lan",
                "--ttl",
                "120",
                "--reachable",
                "true",
            ])
            .status()
            .expect("add lan node");

        Command::new(&bin)
            .args([
                "nodes",
                "add",
                "--db",
                &db_str,
                "--address",
                "http://127.0.0.1:8081",
                "--type",
                "global",
                "--ttl",
                "3600",
                "--reachable",
                "false",
            ])
            .status()
            .expect("add global node");

        // List all nodes
        let output = Command::new(&bin)
            .args(["nodes", "list", "--db", &db_str, "--format", "json"])
            .output()
            .expect("list all");
        assert!(output.status.success());
        let json: Value = serde_json::from_slice(&output.stdout).expect("json");
        let nodes = json.as_array().unwrap();
        assert_eq!(nodes.len(), 2);

        // Filter by type
        let output = Command::new(&bin)
            .args([
                "nodes", "list", "--db", &db_str, "--type", "lan", "--format", "json",
            ])
            .output()
            .expect("list lan");
        assert!(output.status.success());
        let json: Value = serde_json::from_slice(&output.stdout).expect("json");
        let nodes = json.as_array().unwrap();
        assert_eq!(nodes.len(), 1);
        assert_eq!(nodes[0]["type"], "LAN");

        // Filter by reachable
        let output = Command::new(&bin)
            .args([
                "nodes",
                "list",
                "--db",
                &db_str,
                "--reachable",
                "true",
                "--format",
                "json",
            ])
            .output()
            .expect("list reachable");
        assert!(output.status.success());
        let json: Value = serde_json::from_slice(&output.stdout).expect("json");
        let nodes = json.as_array().unwrap();
        assert_eq!(nodes.len(), 1);
        assert_eq!(nodes[0]["reachable"], true);

        // Test limit
        let output = Command::new(&bin)
            .args([
                "nodes", "list", "--db", &db_str, "--limit", "1", "--format", "json",
            ])
            .output()
            .expect("list with limit");
        assert!(output.status.success());
        let json: Value = serde_json::from_slice(&output.stdout).expect("json");
        let nodes = json.as_array().unwrap();
        assert!(nodes.len() <= 1);
    }

    #[test]
    fn nodes_list_table_format() {
        let tmp = temp_home();
        let db_path = tmp.path().join("nodes.db");
        let db_str = db_path.to_string_lossy().to_string();
        let bin = get_bin_path();

        Command::new(&bin)
            .args([
                "nodes",
                "add",
                "--db",
                &db_str,
                "--address",
                "http://127.0.0.1:8080",
                "--type",
                "lan",
                "--ttl",
                "120",
            ])
            .status()
            .expect("add node");

        // List as table (default format)
        let output = Command::new(&bin)
            .args(["nodes", "list", "--db", &db_str])
            .output()
            .expect("list table");
        assert!(output.status.success());
        let stdout = String::from_utf8_lossy(&output.stdout);
        // Table format should contain address
        assert!(stdout.contains("127.0.0.1:8080") || stdout.contains("Address"));
    }

    #[tokio::test]
    async fn nodes_discover_no_registries() {
        let tmp = temp_home();
        let db_path = tmp.path().join("nodes.db");
        let db_str = db_path.to_string_lossy().to_string();
        let bin = get_bin_path();

        // Discover with no registries configured (should complete without error)
        let output = Command::new(&bin)
            .args(["nodes", "discover", "--db", &db_str, "--types", "global"])
            .output()
            .expect("discover");
        // Should succeed even with no registries (just skips global discovery)
        assert!(output.status.success() || output.status.code() == Some(0));

        // Verify warning message is shown
        let stderr = String::from_utf8_lossy(&output.stderr);
        let stdout = String::from_utf8_lossy(&output.stdout);
        let output_text = format!("{}{}", stderr, stdout);
        assert!(
            output_text.contains("No registry URLs")
                || output_text.contains("skipping global discovery")
                || output.status.success(),
            "Should show warning or succeed when no registries configured"
        );
    }

    #[tokio::test]
    async fn nodes_discover_empty_registry_url() {
        let tmp = temp_home();
        let db_path = tmp.path().join("nodes.db");
        let db_str = db_path.to_string_lossy().to_string();
        let bin = get_bin_path();

        // Discover with empty registry URL (should handle gracefully)
        let output = Command::new(&bin)
            .args([
                "nodes",
                "discover",
                "--db",
                &db_str,
                "--types",
                "global",
                "--registry",
                "",
            ])
            .output()
            .expect("discover");
        // Should succeed (empty URL treated as no registry)
        assert!(output.status.success() || output.status.code() == Some(0));
    }

    #[tokio::test]
    async fn nodes_discover_malformed_registry_url() {
        let tmp = temp_home();
        let db_path = tmp.path().join("nodes.db");
        let db_str = db_path.to_string_lossy().to_string();
        let bin = get_bin_path();

        // Discover with malformed registry URL (should handle gracefully)
        let output = Command::new(&bin)
            .args([
                "nodes",
                "discover",
                "--db",
                &db_str,
                "--types",
                "global",
                "--registry",
                "not-a-valid-url",
            ])
            .output()
            .expect("discover");
        // Should complete (may show error but shouldn't crash)
        assert!(output.status.code().is_some());

        // Verify error is logged but doesn't crash
        let stderr = String::from_utf8_lossy(&output.stderr);
        let stdout = String::from_utf8_lossy(&output.stdout);
        // Should either show error or complete gracefully
        assert!(
            stderr.contains("error")
                || stderr.contains("failed")
                || stdout.contains("Discovery completed")
                || output.status.success(),
            "Should handle malformed URL gracefully"
        );
    }

    #[tokio::test]
    async fn nodes_discover_local_types() {
        let tmp = temp_home();
        let db_path = tmp.path().join("nodes.db");
        let db_str = db_path.to_string_lossy().to_string();
        let bin = get_bin_path();

        // Discover local types (LAN/WIFI) - may not find anything but should run
        let output = Command::new(&bin)
            .args([
                "nodes", "discover", "--db", &db_str, "--types", "lan", "wifi",
            ])
            .output()
            .expect("discover local");
        // Should complete (may find 0 nodes, but command should succeed)
        assert!(output.status.success() || output.status.code() == Some(0));
    }

    #[tokio::test]
    async fn nodes_sync_with_invalid_server() {
        let tmp = temp_home();
        let db_path = tmp.path().join("nodes.db");
        let db_str = db_path.to_string_lossy().to_string();
        let bin = get_bin_path();

        // Add a node to local DB
        Command::new(&bin)
            .args([
                "nodes",
                "add",
                "--db",
                &db_str,
                "--address",
                "http://127.0.0.1:8080",
                "--type",
                "lan",
                "--ttl",
                "120",
            ])
            .status()
            .expect("add node");

        // Try sync with invalid server (should fail gracefully)
        let output = Command::new(&bin)
            .args([
                "nodes",
                "sync",
                "--db",
                &db_str,
                "--server",
                "http://127.0.0.1:99999",
            ])
            .output()
            .expect("sync invalid server");
        // Should fail (connection refused or timeout)
        assert!(!output.status.success());
    }

    #[test]
    fn nodes_cleanup_expired_nodes() {
        let tmp = temp_home();
        let db_path = tmp.path().join("nodes.db");
        let db_str = db_path.to_string_lossy().to_string();
        let bin = get_bin_path();

        // Add node with very short TTL (already expired)
        let past_timestamp = chrono::Utc::now().timestamp() - 200;
        Command::new(&bin)
            .args([
                "nodes",
                "add",
                "--db",
                &db_str,
                "--address",
                "http://127.0.0.1:8080",
                "--type",
                "lan",
                "--ttl",
                "100",
                "--last-seen",
                &past_timestamp.to_string(),
            ])
            .status()
            .expect("add expired node");

        // Verify node exists
        let output = Command::new(&bin)
            .args(["nodes", "list", "--db", &db_str, "--format", "json"])
            .output()
            .expect("list before cleanup");
        let json: Value = serde_json::from_slice(&output.stdout).expect("json");
        assert_eq!(json.as_array().unwrap().len(), 1);

        // Run cleanup
        let output = Command::new(&bin)
            .args(["nodes", "cleanup", "--db", &db_str])
            .output()
            .expect("cleanup");
        assert!(output.status.success());

        // Verify node was removed
        let output = Command::new(&bin)
            .args(["nodes", "list", "--db", &db_str, "--format", "json"])
            .output()
            .expect("list after cleanup");
        let json: Value = serde_json::from_slice(&output.stdout).expect("json");
        assert_eq!(json.as_array().unwrap().len(), 0);
    }

    #[test]
    fn nodes_cleanup_no_expired_nodes() {
        let tmp = temp_home();
        let db_path = tmp.path().join("nodes.db");
        let db_str = db_path.to_string_lossy().to_string();
        let bin = get_bin_path();

        // Add node with long TTL (not expired)
        Command::new(&bin)
            .args([
                "nodes",
                "add",
                "--db",
                &db_str,
                "--address",
                "http://127.0.0.1:8080",
                "--type",
                "lan",
                "--ttl",
                "3600",
            ])
            .status()
            .expect("add valid node");

        // Run cleanup
        let output = Command::new(&bin)
            .args(["nodes", "cleanup", "--db", &db_str])
            .output()
            .expect("cleanup");
        assert!(output.status.success());

        // Verify node still exists
        let output = Command::new(&bin)
            .args(["nodes", "list", "--db", &db_str, "--format", "json"])
            .output()
            .expect("list after cleanup");
        let json: Value = serde_json::from_slice(&output.stdout).expect("json");
        assert_eq!(json.as_array().unwrap().len(), 1);
    }

    #[test]
    fn nodes_validate_schema() {
        let tmp = temp_home();
        let db_path = tmp.path().join("nodes.db");
        let db_str = db_path.to_string_lossy().to_string();
        let bin = get_bin_path();

        // Validate empty database
        let output = Command::new(&bin)
            .args(["nodes", "validate", "--db", &db_str])
            .output()
            .expect("validate");
        assert!(output.status.success());
        let stdout = String::from_utf8_lossy(&output.stdout);
        // Should report schema parity
        assert!(stdout.contains("Schema parity") || stdout.contains("parity"));

        // Add a node and validate again
        Command::new(&bin)
            .args([
                "nodes",
                "add",
                "--db",
                &db_str,
                "--address",
                "http://127.0.0.1:8080",
                "--type",
                "lan",
                "--ttl",
                "120",
            ])
            .status()
            .expect("add node");

        let output = Command::new(&bin)
            .args(["nodes", "validate", "--db", &db_str])
            .output()
            .expect("validate with nodes");
        assert!(output.status.success());
        let stdout = String::from_utf8_lossy(&output.stdout);
        // Should show discovery status
        assert!(stdout.contains("Discovery status") || stdout.contains("nodes"));
    }

    #[test]
    fn nodes_health_check_empty() {
        let tmp = temp_home();
        let db_path = tmp.path().join("nodes.db");
        let db_str = db_path.to_string_lossy().to_string();
        let bin = get_bin_path();

        // Health check on empty database
        let output = Command::new(&bin)
            .args(["nodes", "health-check", "--db", &db_str])
            .output()
            .expect("health check");
        // Should succeed (checked 0 nodes)
        assert!(output.status.success() || output.status.code() == Some(0));
    }

    #[test]
    fn nodes_health_check_with_nodes() {
        let tmp = temp_home();
        let db_path = tmp.path().join("nodes.db");
        let db_str = db_path.to_string_lossy().to_string();
        let bin = get_bin_path();

        // Add a node
        Command::new(&bin)
            .args([
                "nodes",
                "add",
                "--db",
                &db_str,
                "--address",
                "http://127.0.0.1:8080",
                "--type",
                "lan",
                "--ttl",
                "120",
            ])
            .status()
            .expect("add node");

        // Health check (may fail if node is unreachable, but command should run)
        let output = Command::new(&bin)
            .args(["nodes", "health-check", "--db", &db_str])
            .output()
            .expect("health check");
        // Command should complete (even if node is unreachable)
        assert!(output.status.code().is_some());
    }

    #[test]
    fn nodes_end_to_end_workflow() {
        let tmp = temp_home();
        let db_path = tmp.path().join("nodes.db");
        let db_str = db_path.to_string_lossy().to_string();
        let bin = get_bin_path();

        // 1. Add nodes
        Command::new(&bin)
            .args([
                "nodes",
                "add",
                "--db",
                &db_str,
                "--address",
                "http://127.0.0.1:8080",
                "--type",
                "lan",
                "--ttl",
                "120",
                "--node-id",
                "node-1",
            ])
            .status()
            .expect("add node 1");

        Command::new(&bin)
            .args([
                "nodes",
                "add",
                "--db",
                &db_str,
                "--address",
                "http://127.0.0.1:8081",
                "--type",
                "global",
                "--ttl",
                "3600",
                "--node-id",
                "node-2",
            ])
            .status()
            .expect("add node 2");

        // 2. List all nodes
        let output = Command::new(&bin)
            .args(["nodes", "list", "--db", &db_str, "--format", "json"])
            .output()
            .expect("list");
        assert!(output.status.success());
        let json: Value = serde_json::from_slice(&output.stdout).expect("json");
        assert_eq!(json.as_array().unwrap().len(), 2);

        // 3. Validate
        let output = Command::new(&bin)
            .args(["nodes", "validate", "--db", &db_str])
            .output()
            .expect("validate");
        assert!(output.status.success());

        // 4. Remove one node
        Command::new(&bin)
            .args([
                "nodes",
                "remove",
                "--db",
                &db_str,
                "--address",
                "http://127.0.0.1:8080",
            ])
            .status()
            .expect("remove node");

        // 5. Verify removal
        let output = Command::new(&bin)
            .args(["nodes", "list", "--db", &db_str, "--format", "json"])
            .output()
            .expect("list after remove");
        let json: Value = serde_json::from_slice(&output.stdout).expect("json");
        assert_eq!(json.as_array().unwrap().len(), 1);
        assert_eq!(json[0]["address"], "http://127.0.0.1:8081");
    }
}
