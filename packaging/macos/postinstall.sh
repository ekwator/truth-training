#!/bin/bash
set -e

# Get the installing user's home directory
INSTALL_USER="${3:-$USER}"
USER_HOME=$(eval echo ~"$INSTALL_USER")
if [ -z "$USER_HOME" ] || [ "$USER_HOME" = "~$INSTALL_USER" ]; then
    USER_HOME="$HOME"
fi

# LaunchAgent directory (user-specific, no root required)
LAUNCH_AGENTS_DIR="$USER_HOME/Library/LaunchAgents"
PLIST="com.truth.training.server.plist"
DEST="$LAUNCH_AGENTS_DIR/$PLIST"

# Create LaunchAgents directory if it doesn't exist
mkdir -p "$LAUNCH_AGENTS_DIR"

# Copy plist to LaunchAgents directory
if [ -f "$PLIST" ]; then
    cp "$PLIST" "$DEST"
    chmod 644 "$DEST"
    
    # Load and start the LaunchAgent
    launchctl load "$DEST" 2>/dev/null || true
    launchctl start com.truth.training.server 2>/dev/null || true
    
    echo "truth-core-server LaunchAgent has been installed for user $INSTALL_USER"
    echo "Service location: $DEST"
    echo "Manage with: launchctl [load|unload|start|stop] $DEST"
else
    echo "ERROR: Plist file not found: $PLIST"
    exit 1
fi

exit 0
