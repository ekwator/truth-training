#!/bin/bash
set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
MAIN_VERSION=$(cargo metadata --no-deps --format-version=1 | jq -r '.packages[0].version')

# Registry file
echo "📘 Updating docs/VERSION_REGISTRY.md, spec/README.md, and docs/UI_Desktop.md to v${MAIN_VERSION}..."

cat > "$ROOT_DIR/docs/VERSION_REGISTRY.md" <<EOF
# Truth Training – Version Registry (v${MAIN_VERSION})

| Component         | Version | Description |
|-------------------|----------|--------------|
| Core Library      | v${MAIN_VERSION} | Main logic layer |
| Server            | v${MAIN_VERSION} | HTTP API service |
| CLI (truthctl)    | v${MAIN_VERSION} | Command-line interface |
| Desktop UI        | v0.1.3   | Text-based desktop interface |
| Spec Document     | v0.4.0   | Reference specification |

_Last updated: $(date -u +"%Y-%m-%d %H:%M UTC")_
EOF

# Update spec/README.md very first version string
sed -i "1s/v[0-9.]\+/v${MAIN_VERSION}/" "$ROOT_DIR/spec/README.md"

# Update UI_Desktop.md reference to core/server version
if grep -q "Core/Server version:" "$ROOT_DIR/docs/UI_Desktop.md"; then
  sed -i "s/Core\/Server version: v[0-9.]\+/Core\/Server version: v${MAIN_VERSION}/" "$ROOT_DIR/docs/UI_Desktop.md"
else
  echo "Core/Server version: v${MAIN_VERSION}" >> "$ROOT_DIR/docs/UI_Desktop.md"
fi
