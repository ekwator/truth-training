use ed25519_dalek::{SigningKey, VerifyingKey, Signature, Signer, Verifier};
use rand::rngs::OsRng;
use hex;
use crate::api::VerifyError; // либо продублировать VerifyError в другом месте/реэкспортировать

pub struct CryptoIdentity {
    pub signing_key: SigningKey,
    pub verifying_key: VerifyingKey,
}

impl CryptoIdentity {
    pub fn new() -> Self {
        let mut rng = OsRng;
        let signing_key = SigningKey::generate(&mut rng);
        let verifying_key = signing_key.verifying_key();
        Self { signing_key, verifying_key }
    }

    pub fn sign(&self, data: &[u8]) -> Signature {
        self.signing_key.sign(data)
    }

    pub fn public_key_hex(&self) -> String {
        hex::encode(self.verifying_key.to_bytes())
    }
    
    /// Проверка, возвращающая Result
    pub fn verify(&self, data: &[u8], sig: &Signature) -> Result<(), VerifyError> {
        self.verifying_key
            .verify(data, sig)
            .map_err(|e| VerifyError::VerificationFailed(e.to_string()))
    }
}
