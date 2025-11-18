#[cfg(feature = "desktop")]
use crate::p2p::encryption::CryptoIdentity;

/// Load or generate node identity (placeholder implementation).
/// Future versions can persist keys to disk; for now we delegate to CryptoIdentity::new().
#[cfg(feature = "desktop")]
pub fn load_or_generate_identity() -> CryptoIdentity {
    CryptoIdentity::new()
}
