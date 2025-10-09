use ed25519_dalek::{SigningKey, VerifyingKey, Signature, Signer, Verifier};
use rand::rngs::OsRng;
use hex;
use std::convert::TryInto;
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

    /// Создание CryptoIdentity из hex-строки публичного ключа
    pub fn from_public_key_hex(public_key_hex: &str) -> Result<Self, VerifyError> {
        // 1) decode public key hex
        let public_key_bytes = hex::decode(public_key_hex).map_err(VerifyError::PublicKeyHex)?;
        if public_key_bytes.len() != 32 {
            return Err(VerifyError::PublicKeyLength(public_key_bytes.len()));
        }

        // 2) convert Vec<u8> -> [u8; 32]
        let public_key_array: [u8; 32] = public_key_bytes
            .as_slice()
            .try_into()
            .map_err(|_| VerifyError::PublicKeyLength(public_key_bytes.len()))?;

        // 3) parse VerifyingKey
        let verifying_key = VerifyingKey::from_bytes(&public_key_array)
            .map_err(|e| VerifyError::PublicKeyParse(e.to_string()))?;

        // Создаем CryptoIdentity только с публичным ключом (для верификации)
        // Приватный ключ будет пустым, но это не важно для верификации
        Ok(Self {
            signing_key: SigningKey::from_bytes(&[0u8; 32]), // dummy key
            verifying_key,
        })
    }

    /// Проверка подписи из hex-строк
    pub fn verify_from_hex(&self, data: &[u8], signature_hex: &str) -> Result<(), VerifyError> {
        // decode signature hex
        let signature_bytes = hex::decode(signature_hex).map_err(VerifyError::SignatureHex)?;

        // parse Signature
        let signature = Signature::try_from(signature_bytes.as_slice())
            .map_err(|e| VerifyError::SignatureParse(e.to_string()))?;

        // verify using the existing verify method
        self.verify(data, &signature)
    }

    /// Создание CryptoIdentity из пары ключей в hex (для CLI)
    pub fn from_keypair_hex(private_key_hex: &str, public_key_hex: &str) -> Result<Self, String> {
        let sk_bytes = hex::decode(private_key_hex).map_err(|e| e.to_string())?;
        if sk_bytes.len() != 32 { return Err(format!("invalid private key length: {}", sk_bytes.len())); }
        let pk_bytes = hex::decode(public_key_hex).map_err(|e| e.to_string())?;
        if pk_bytes.len() != 32 { return Err(format!("invalid public key length: {}", pk_bytes.len())); }

        let signing_key = SigningKey::from_bytes(&sk_bytes.try_into().map_err(|_| "bad sk".to_string())?);
        let verifying_key = VerifyingKey::from_bytes(&pk_bytes.try_into().map_err(|_| "bad pk".to_string())?)
            .map_err(|e| e.to_string())?;
        Ok(Self { signing_key, verifying_key })
    }
}

impl Default for CryptoIdentity {
    fn default() -> Self { Self::new() }
}