use std::collections::HashMap;

pub use crate::models::*;
pub use crate::storage::*;

// Заготовка для P2P синхронизации
pub fn start_sync() {
    println!("Starting P2P sync...");
}

/// Deterministic merge of node inventories (FR-011).
/// Returns the merged list plus counts of local additions/updates.
pub fn merge_node_lists(local: &[Node], incoming: &[Node]) -> (Vec<Node>, usize, usize) {
    let mut map: HashMap<String, Node> = HashMap::new();
    for node in local {
        map.insert(node.address.clone(), node.clone());
    }

    let mut added = 0usize;
    let mut updated = 0usize;

    for candidate in incoming {
        if let Some(existing) = map.get_mut(&candidate.address) {
            if should_replace(existing, candidate) {
                *existing = candidate.clone();
                updated += 1;
            }
        } else {
            map.insert(candidate.address.clone(), candidate.clone());
            added += 1;
        }
    }

    let mut merged: Vec<Node> = map.into_values().collect();
    merged.sort_by(|a, b| b.last_seen.cmp(&a.last_seen));
    (merged, added, updated)
}

fn should_replace(existing: &Node, candidate: &Node) -> bool {
    let existing_priority = existing.node_type.priority();
    let candidate_priority = candidate.node_type.priority();

    if candidate_priority > existing_priority {
        return true;
    }
    if candidate_priority < existing_priority {
        return false;
    }
    if candidate.last_seen > existing.last_seen {
        return true;
    }
    if candidate.last_seen < existing.last_seen {
        return false;
    }
    candidate.address < existing.address
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::config::LAN_TTL_SECS;

    fn sample_node(address: &str, node_type: NodeType, last_seen: i64) -> Node {
        Node {
            id: last_seen,
            address: address.to_string(),
            node_type,
            reachable: true,
            last_seen,
            ttl: LAN_TTL_SECS,
            source: Some(NodeSource::LocalBroadcast),
            node_id: Some(address.to_string()),
            created_at: last_seen,
            updated_at: last_seen,
        }
    }

    #[test]
    fn merge_prefers_local_over_global() {
        let local = vec![sample_node("http://node", NodeType::Lan, 100)];
        let incoming = vec![sample_node("http://node", NodeType::Global, 200)];
        let (merged, _added, updated) = merge_node_lists(&local, &incoming);
        assert_eq!(merged.len(), 1);
        assert_eq!(merged[0].node_type, NodeType::Lan);
        assert_eq!(updated, 0, "Global should not override LAN");
    }

    #[test]
    fn merge_uses_last_seen_for_same_priority() {
        let local = vec![sample_node("http://node", NodeType::Lan, 100)];
        let incoming = vec![sample_node("http://node", NodeType::Lan, 200)];
        let (merged, _, updated) = merge_node_lists(&local, &incoming);
        assert_eq!(merged[0].last_seen, 200);
        assert_eq!(updated, 1);
    }

    #[test]
    fn merge_adds_new_nodes() {
        let local = vec![sample_node("http://node-a", NodeType::Lan, 100)];
        let incoming = vec![sample_node("http://node-b", NodeType::Global, 150)];
        let (merged, added, updated) = merge_node_lists(&local, &incoming);
        assert_eq!(merged.len(), 2);
        assert_eq!(added, 1);
        assert_eq!(updated, 0);
    }
}
