use actix_web::{HttpRequest, HttpResponse};

#[derive(Debug)]
pub struct TransportEnvelope {
    pub public_key: String,
    pub signature: String,
    pub timestamp: String,
}

/// Extracts minimal transport envelope headers. Anti-replay and verification are handled by callers.
pub fn extract_envelope(req: &HttpRequest) -> Result<TransportEnvelope, HttpResponse> {
    let pk = req
        .headers()
        .get("X-Public-Key")
        .and_then(|v| v.to_str().ok());
    let sig = req
        .headers()
        .get("X-Signature")
        .and_then(|v| v.to_str().ok());
    let ts = req
        .headers()
        .get("X-Timestamp")
        .and_then(|v| v.to_str().ok());
    match (pk, sig, ts) {
        (Some(pk), Some(sig), Some(ts)) if !pk.is_empty() && !sig.is_empty() && !ts.is_empty() => {
            Ok(TransportEnvelope {
                public_key: pk.to_string(),
                signature: sig.to_string(),
                timestamp: ts.to_string(),
            })
        }
        _ => Err(HttpResponse::BadRequest().body("Missing transport envelope headers")),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use actix_web::test;

    #[actix_web::test]
    async fn extract_envelope_missing_headers() {
        let req = test::TestRequest::default().to_http_request();
        let err = extract_envelope(&req).unwrap_err();
        assert_eq!(err.status(), actix_web::http::StatusCode::BAD_REQUEST);
    }
}
