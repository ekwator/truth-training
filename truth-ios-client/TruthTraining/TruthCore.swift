import Foundation

public class TruthCore: ObservableObject {
    private let core: OpaquePointer?
    
    public init() {
        self.core = truth_core_init()
    }
    
    deinit {
        truth_core_free(core)
    }
    
    public func processJson(_ json: String) -> String? {
        let result = json.withCString { cString in
            truth_core_process_json(cString, strlen(cString))
        }
        
        guard let result = result else { return nil }
        
        let swiftString = String(cString: result)
        truth_core_free_string(result)
        
        return swiftString
    }
    
    public func verifySignature(_ message: String, signature: String, publicKey: String) -> Bool {
        return message.withCString { messagePtr in
            signature.withCString { sigPtr in
                publicKey.withCString { keyPtr in
                    truth_core_verify_signature(messagePtr, sigPtr, keyPtr)
                }
            }
        }
    }
    
    public func syncWithPeer(_ peerUrl: String) -> Int32 {
        return peerUrl.withCString { urlPtr in
            truth_core_sync_with_peer(core, urlPtr)
        }
    }
    
    public func getPeerCount() -> Int32 {
        return truth_core_get_peer_count(core)
    }
}

public enum TruthCoreError: Error {
    case initializationFailed
    case invalidJson
    case signatureVerificationFailed
    case networkError(Int32)
    case unknownError
}

extension TruthCore {
    public func processJsonSafely(_ json: String) throws -> String {
        guard let result = processJson(json) else {
            throw TruthCoreError.invalidJson
        }
        return result
    }
    
    public func verifySignatureSafely(_ message: String, signature: String, publicKey: String) throws -> Bool {
        let result = verifySignature(message, signature: signature, publicKey: publicKey)
        if !result {
            throw TruthCoreError.signatureVerificationFailed
        }
        return result
    }
}
