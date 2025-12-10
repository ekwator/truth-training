# Quickstart: Desktop Impacts, Judgments, and Network Nodes

**Feature**: Desktop Impacts, Judgments, and Network Nodes UI  
**Date**: 2025-01-XX

## Overview

This document provides step-by-step instructions for using the Desktop UI features for adding impacts, submitting judgments, and viewing network node details.

## Prerequisites

- Desktop application installed and running
- At least one event created
- Network nodes discovered (for node details feature)

## Adding Impacts to Events

### Step 1: Open Event Summary

1. Launch Desktop application
2. Navigate to Events screen (Alt+4 or via menu)
3. Click on an event to open Event Summary screen

### Step 2: Add Impact

1. In Event Summary screen, locate "Impacts" section
2. Click "Add Impact" button (with emoji icon)
3. Add Impact modal dialog opens

### Step 3: Set Impact Level

1. Use slider to set impact level (1-5):
   - Level 1-3: Negative impact
   - Level 4-5: Positive impact
2. Current level is displayed (e.g., "Level 4")

### Step 4: Add Notes (Optional)

1. Enter notes in the text field (optional)
2. Notes can describe the impact in detail

### Step 5: Save Impact

1. Click "Save Impact" button
2. Modal closes
3. Impact appears in Impacts list:
   - Shows "Positive (Level 4-5)" or "Negative (Level 1-3)"
   - Displays notes if provided
   - Shows creation timestamp

**Expected Result**: Impact is saved and displayed in the Impacts list section.

---

## Submitting Judgments for Events

### Step 1: Open Event Summary

1. Navigate to Events screen
2. Click on an event to open Event Summary screen

### Step 2: Submit Judgment

1. In Event Summary screen, locate "Judgments" section
2. Click "Submit Judgment" button (with emoji icon)
3. Submit Judgment modal dialog opens

### Step 3: Select Assessment

1. Choose assessment:
   - **Confirm** (True): Event is confirmed/true
   - **Reject** (False): Event is rejected/false
   - **Abstain** (Uncertain): Uncertain about event

### Step 4: Set Confidence Level

1. Use slider to set confidence level (0.0-1.0):
   - 0.0: No confidence
   - 0.5: Medium confidence
   - 1.0: Full confidence
2. Current confidence is displayed as percentage (e.g., "80% confidence")

### Step 5: Add Reasoning (Optional)

1. Enter reasoning in the text field (optional)
2. Reasoning can explain your assessment

### Step 6: Submit Judgment

1. Click "Submit" button
2. Modal closes
3. Judgment appears in Judgments list:
   - Shows assessment (Confirm/Reject/Abstain)
   - Displays confidence percentage
   - Shows reasoning if provided
   - Shows submission timestamp

**Expected Result**: Judgment is submitted and displayed in the Judgments list section.

---

## Viewing Network Node Details

### Step 1: Access Nodes Panel

1. Navigate to Settings screen (Alt+8 or via menu)
2. Scroll to "Discovery Worker Settings" section
3. Click "Node Discovery" button
4. NodesPanel opens (or access via Dashboard if available)

### Step 2: View Node List

1. NodesPanel displays list of discovered nodes
2. Each node shows:
   - Address
   - Type (Hub or Leaf)
   - Status (Online/Offline)
   - TTL and expiration info

### Step 3: Open Node Details

1. Click on a node card
2. Node detail view opens (modal or side panel)

### Step 4: View Node Information

Node detail view displays:
- **Address**: Network address
- **Type**: Hub/Leaf (and technical type)
- **Status**: Reachable/Unreachable
- **Last Seen**: Timestamp of last contact
- **TTL**: Time-to-live value
- **Expires In**: Time until TTL expires (or "Expired")
- **Age**: Time since last seen
- **Source**: Discovery source (if available)
- **Node ID**: Unique node identifier (if available)
- **Created At**: Node creation timestamp
- **Updated At**: Last update timestamp

### Step 5: Refresh Node (Optional)

1. Click "Refresh" button in node detail view
2. Node data is reloaded from database

### Step 6: Close Detail View

1. Click "Close" or "Back" button
2. Return to NodesPanel

**Expected Result**: Node details are displayed correctly with all information.

---

## Validation Scenarios

### Scenario 1: Add Multiple Impacts

1. Add impact with level 1 (Negative)
2. Add impact with level 5 (Positive)
3. Verify both impacts appear in list
4. Verify correct level ranges displayed

### Scenario 2: Submit Multiple Judgments

1. Submit judgment with assessment "Confirm", confidence 0.9
2. Submit judgment with assessment "Reject", confidence 0.7
3. Verify both judgments appear in list
4. Verify correct assessments and confidence displayed

### Scenario 3: View Different Node Types

1. Find Hub node (RELAY or GLOBAL)
2. Verify displays as "Hub" in list
3. Open detail view, verify technical type shown
4. Find Leaf node (LAN, WIFI, or CLIENT)
5. Verify displays as "Leaf" in list
6. Open detail view, verify technical type shown

### Scenario 4: Offline Mode

1. Disconnect from network
2. Add impact - should work (offline-first)
3. Submit judgment - should work (offline-first)
4. View node details - should work (local data)
5. Verify data syncs when network restored

---

## Troubleshooting

### Impact Not Appearing

- Check impact level is valid (1-5)
- Verify event_id is correct
- Check browser console for errors
- Verify offline queue is processing

### Judgment Not Submitting

- Check assessment is selected
- Verify confidence level is 0.0-1.0
- Check browser console for errors
- Verify offline queue is processing

### Node Details Not Loading

- Verify node exists in database
- Check node_id is valid
- Try refreshing node list first
- Check browser console for errors

### Type Display Incorrect

- Verify node_type is valid (LAN/WIFI/GLOBAL/RELAY/CLIENT)
- Check NodeTypeMapper mapping logic
- Verify both Hub/Leaf and technical type displayed in details

---

## Keyboard Shortcuts

- **Alt+4**: Navigate to Event Summary
- **Alt+8**: Navigate to Settings
- **Escape**: Close modal/detail view
- **Enter**: Submit form (when focused)

---

## References

- Desktop Quickstart: `docs/quickstart_desktop.md:306-332`
- Android Implementation: `specs/018-android-impacts-judgments/spec.md`, `specs/019-android-node-details/spec.md`
- Desktop EventSummary: `ui/desktop/src/pages/EventSummary.tsx`
- Desktop NodesPanel: `ui/desktop/src/components/NodesPanel.tsx`

