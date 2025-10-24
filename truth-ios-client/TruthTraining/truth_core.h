#ifndef TRUTH_CORE_H
#define TRUTH_CORE_H

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

// Core initialization
void* truth_core_init(void);
void truth_core_free(void* core);

// JSON processing
char* truth_core_process_json(const char* json, size_t json_len);
void truth_core_free_string(char* str);

// Signature verification
bool truth_core_verify_signature(const char* message, const char* signature, const char* public_key);

// P2P operations
int32_t truth_core_sync_with_peer(void* core, const char* peer_url);
int32_t truth_core_get_peer_count(void* core);

#ifdef __cplusplus
}
#endif

#endif // TRUTH_CORE_H
