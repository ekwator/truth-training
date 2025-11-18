# API Contracts: Node Discovery & Address Exchange

This directory contains OpenAPI 3.0 specifications for the Node Discovery API.

## Files

- `nodes-api.yaml`: Complete OpenAPI specification for node discovery, synchronization, and health check endpoints.

## Endpoints Summary

### Node Management
- `GET /nodes` - List all known nodes (with filters)
- `POST /nodes` - Register a new node manually
- `GET /nodes/{id}` - Get node by ID
- `PUT /nodes/{id}` - Update node
- `DELETE /nodes/{id}` - Delete node

### Discovery & Sync
- `POST /nodes/discover` - Trigger discovery cycle
- `POST /nodes/sync` - Synchronize node lists with peer

### Health Checks
- `GET /nodes/health` - Check reachability of nodes

## Contract Testing

Contract tests should be generated from this specification to ensure:
1. Request/response schemas match specification
2. Required fields are validated
3. Enum values are enforced
4. Error responses follow schema

See `tests/contract/` for generated contract tests.

## Usage

### Generate Client SDKs
```bash
# Using openapi-generator
openapi-generator generate -i contracts/nodes-api.yaml -g rust -o clients/rust
openapi-generator generate -i contracts/nodes-api.yaml -g kotlin -o clients/kotlin
openapi-generator generate -i contracts/nodes-api.yaml -g typescript -o clients/typescript
```

### Validate Specification
```bash
# Using swagger-cli
swagger-cli validate contracts/nodes-api.yaml
```

### View Documentation
```bash
# Using swagger-ui
docker run -p 8080:8080 -e SWAGGER_JSON=/specs/nodes-api.yaml -v $(pwd)/contracts:/specs swaggerapi/swagger-ui
```

