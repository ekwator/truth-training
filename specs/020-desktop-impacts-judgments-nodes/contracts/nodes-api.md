# Nodes API Contract

**Feature**: Desktop Impacts, Judgments, and Network Nodes UI  
**Component**: Nodes API  
**Date**: 2025-01-XX

## Overview

Contract specification for Nodes API in Desktop UI, defining request/response formats, validation rules, and error handling.

## Tauri Command: `list_nodes`

### Request

```typescript
interface ListNodesRequest {
  nodeType?: string;      // "ALL", "LAN", "WIFI", "GLOBAL", "RELAY", "CLIENT"
  reachable?: boolean;    // Filter by reachability
}
```

### Response

**Success** (200):
```typescript
interface NodeRecord[] {
  id: number;
  address: string;
  node_type: string;      // "LAN" | "WIFI" | "GLOBAL" | "RELAY" | "CLIENT"
  reachable: boolean;
  ttl: number;           // Time-to-live in seconds
  last_seen: number;      // Unix timestamp (seconds)
  updated_at: number;     // Unix timestamp (seconds)
  source?: string;
  node_id?: string;
  expires_in: number;     // Calculated: (last_seen + ttl - now).max(0)
}
```

### Frontend Usage

```typescript
// ApiService.listNodes()
const nodes = await ApiService.listNodes(nodeType, reachable);
```

---

## Get Node by ID (TODO)

### Request

```typescript
interface GetNodeRequest {
  nodeId: number;
}
```

### Response

**Success** (200):
```typescript
interface NodeRecord {
  // Same as list_nodes response
}
```

### Implementation Status

**Current**: Not implemented in Tauri backend  
**Alternative**: Filter `list_nodes()` result by id in frontend  
**Future**: Add `get_node_by_id` Tauri command

### Alternative Implementation

```typescript
// Frontend workaround
const allNodes = await ApiService.listNodes();
const node = allNodes.find(n => n.id === nodeId);
```

---

## Node Type Mapping

### Technical Types

- "LAN": Local Area Network node
- "WIFI": Wi-Fi network node
- "GLOBAL": Global network node
- "RELAY": Relay node (Hub)
- "CLIENT": Client node (Leaf)

### User-Friendly Types

- "Hub": RELAY or GLOBAL nodes
- "Leaf": LAN, WIFI, or CLIENT nodes

### Mapping Function

```typescript
function mapToUserFriendly(nodeType: string): string {
  const upper = nodeType.toUpperCase();
  if (upper === "RELAY" || upper === "GLOBAL") return "Hub";
  if (upper === "LAN" || upper === "WIFI" || upper === "CLIENT") return "Leaf";
  return "Unknown";
}
```

---

## Calculated Fields

### expires_in

```typescript
const now = Math.floor(Date.now() / 1000);  // Unix timestamp (seconds)
const expires_in = Math.max(0, node.last_seen + node.ttl - now);
```

### age

```typescript
const now = Math.floor(Date.now() / 1000);
const age = now - node.last_seen;  // Seconds since last seen
```

---

## Test Scenarios

### TC-001: List All Nodes

**Given**: No filters  
**When**: Call `list_nodes()`  
**Then**: Returns all nodes in database

### TC-002: Filter by Node Type

**Given**: nodeType="RELAY"  
**When**: Call `list_nodes(nodeType="RELAY")`  
**Then**: Returns only RELAY nodes

### TC-003: Filter by Reachability

**Given**: reachable=true  
**When**: Call `list_nodes(reachable=true)`  
**Then**: Returns only reachable nodes

### TC-004: Get Node by ID (Alternative)

**Given**: nodeId=123  
**When**: Call `list_nodes()` and filter by id  
**Then**: Returns node with id=123

---

## References

- Tauri Command: `ui/desktop/src-tauri/src/discovery.rs`
- ApiService: `ui/desktop/src/services/api.ts`
- Android Implementation: `specs/019-android-node-details/spec.md`

