use base64::{engine::general_purpose, Engine as _};
use ed25519_dalek::{Signature, Verifier, VerifyingKey};
use serde_json::Value;

#[derive(Debug)]
pub enum VerifyError {
    InvalidJson(serde_json::Error),
    MissingSignature,
    MissingPublicKey,
    MissingPayload,
    Base64(base64::DecodeError),
    SignatureParse(ed25519_dalek::SignatureError),
    PublicKeyLength(usize),
    PublicKeyConstruct(String),
    VerificationFailed(String),
}

impl From<serde_json::Error> for VerifyError { fn from(e: serde_json::Error) -> Self { Self::InvalidJson(e) } }
impl From<base64::DecodeError> for VerifyError { fn from(e: base64::DecodeError) -> Self { Self::Base64(e) } }
impl From<ed25519_dalek::SignatureError> for VerifyError { fn from(e: ed25519_dalek::SignatureError) -> Self { Self::SignatureParse(e) } }

pub struct TrustedMessage {
    pub verified: bool,
    pub payload: Value,
    pub sender_key_b64: String,
}

/// Verify signed Android JSON envelope of the form:
/// { "node_id": "...", "payload": { ... }, "signature": "base64", "public_key": "base64" }
/// The signature is computed over the canonical JSON of `payload`.
pub fn verify_json_message(raw_json: &str) -> Result<TrustedMessage, VerifyError> {
    let v: Value = serde_json::from_str(raw_json)?;

    let signature_b64 = v.get("signature").and_then(|s| s.as_str()).ok_or(VerifyError::MissingSignature)?;
    let pubkey_b64 = v.get("public_key").and_then(|s| s.as_str()).ok_or(VerifyError::MissingPublicKey)?;
    let payload = v.get("payload").ok_or(VerifyError::MissingPayload)?.clone();

    let data = serde_json::to_vec(&payload)?;

    let sig_bytes = general_purpose::STANDARD.decode(signature_b64)?;
    let pk_bytes = general_purpose::STANDARD.decode(pubkey_b64)?;

    if pk_bytes.len() != 32 { return Err(VerifyError::PublicKeyLength(pk_bytes.len())); }

    let sig_arr: [u8; 64] = match sig_bytes.as_slice().try_into() {
        Ok(a) => a,
        Err(_) => return Err(VerifyError::VerificationFailed("invalid_signature_length".to_string())),
    };
    let signature = Signature::from_bytes(&sig_arr);

    // Directly construct verifying key and verify signature without p2p dependency
    let pk_arr: [u8; 32] = match pk_bytes.as_slice().try_into() {
        Ok(a) => a,
        Err(_) => return Err(VerifyError::PublicKeyLength(pk_bytes.len())),
    };
    let verifying_key = match VerifyingKey::from_bytes(&pk_arr) {
        Ok(vk) => vk,
        Err(e) => return Err(VerifyError::PublicKeyConstruct(e.to_string())),
    };

    if let Err(e) = verifying_key.verify(&data, &signature) {
        return Err(VerifyError::VerificationFailed(e.to_string()));
    }

    Ok(TrustedMessage { verified: true, payload, sender_key_b64: pubkey_b64.to_string() })
}


