#!/bin/bash
set -e

PLIST="com.truth.training.server.plist"
DEST="/Library/LaunchDaemons/${PLIST}"

cp "${PLIST}" "${DEST}"
launchctl load "${DEST}" || true
launchctl start com.truth.training.server || true

exit 0


