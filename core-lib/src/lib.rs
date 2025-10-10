pub mod auth;
pub mod expert_simple;
pub mod knowledge;
pub mod models;
pub mod storage;
pub mod sync;
pub mod trust_propagation;

pub use crate::models::*;
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
