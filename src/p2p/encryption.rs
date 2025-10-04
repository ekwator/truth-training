use ed25519_dalek::{SigningKey, VerifyingKey, Signature, Signer, Verifier};
use rand::rngs::OsRng;
use hex;

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

    pub fn verify(&self, data: &[u8], sig: &Signature) -> bool {
        self.verifying_key.verify(data, sig).is_ok()
    }

    pub fn public_key_hex(&self) -> String {
        hex::encode(self.verifying_key.to_bytes())
    }
}
