#!/usr/bin/env bash

# Remove trailing whitespace from src/api.rs
# Creates a backup file src/api.rs.bak before applying changes

set -e

FILE="src/api.rs"

if [ ! -f "$FILE" ]; then
  echo "Error: $FILE not found."
  exit 1
fi

echo "Creating backup: $FILE.bak"
cp "$FILE" "$FILE.bak"

echo "Removing trailing whitespace from $FILE ..."
# Remove trailing whitespace only
sed -i 's/[ \t]*$//' "$FILE"

echo "Done. Trailing whitespace removed from $FILE"
