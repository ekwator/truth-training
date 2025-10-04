pub mod node;
pub mod sync;
pub mod encryption;

// Переэкспортируем только то, что реально нужно внешним модулям
// pub use node::Node;
// pub use sync::sync_with_peer;
// pub use encryption::CryptoIdentity;

// Если ты пока не используешь эти re-export’ы — можно закомментировать их
#[allow(unused_imports)]
pub use node::Node;
#[allow(unused_imports)]
pub use sync::sync_with_peer;
#[allow(unused_imports)]
pub use encryption::CryptoIdentity;
