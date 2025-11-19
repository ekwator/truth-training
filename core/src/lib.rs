pub mod auth;
pub mod collective_intelligence;
pub mod config;
pub mod expert_simple;
pub mod knowledge;
pub mod models;
pub mod storage;
pub mod sync;
pub mod trust_propagation;
pub mod weights;

pub use crate::config::{
    default_ttl_for, validate_ttl, DiscoveryTimingConfig, CLEANUP_INTERVAL_SECS,
    DEFAULT_DISCOVERY_TIMING, GLOBAL_POLL_INTERVAL_SECS, GLOBAL_TTL_SECS, HEALTH_CHECK_RETRY_LIMIT,
    HEALTH_CHECK_TIMEOUT_SECS, LAN_DISCOVERY_INTERVAL_SECS, LAN_TTL_SECS,
    WIFI_DISCOVERY_INTERVAL_SECS, WIFI_TTL_SECS,
};
pub use crate::models::*;
pub use crate::storage::recalc_collective_truth;
pub use crate::storage::*;

pub fn add(left: u64, right: u64) -> u64 {
    left + right
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn it_works() {
        let result = add(2, 2);
        assert_eq!(result, 4);
    }
}
