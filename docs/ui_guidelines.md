# UI/UX Guidelines — Truth Training v1.0.0

This document provides comprehensive user interface and user experience guidelines for Truth Training applications across all platforms.

## Core UX Principles

### 1. No Business Logic in UI
- All business logic must reside in the API or FFI layer
- UI components should be pure presentation layers
- Data validation and processing happens server-side or in the core

### 2. Expert Wizard Interface
- Display expert questions with clear rationale
- Show confidence levels and scoring explanations
- Guide users through complex decision-making processes
- Provide contextual help and tooltips

### 3. Progress Visualization
- Display clear progress trends and metrics
- Show synchronization status across devices
- Provide real-time feedback on operations
- Use consistent progress indicators

## Platform-Specific Guidelines

### CLI Interface (truthctl)

#### Command Structure
- Subcommands mirror domain objects: `peers add/list`, `sync`
- Use consistent flags: `--db truth_db.sqlite`, `--peers peers.json`, `--verbose`
- Support `--pull-only` for sync operations
- Human-readable output by default, JSON available with `--json`

#### Command Examples
```bash
# Peer management
truthctl peers add --address 192.168.1.100
truthctl peers list --verbose

# Synchronization
truthctl sync --pull-only
truthctl sync --peers peers.json --verbose

# Database operations
truthctl events list --db custom.sqlite
truthctl metrics show --json
```

### Desktop UI Guidelines

#### Layout Principles
- Clean, minimal interface with clear navigation
- Consistent spacing and typography
- Responsive design for different screen sizes
- Accessible color schemes and contrast ratios

#### Navigation Structure
- Main navigation: Events, Contexts, Judgments, Sync, Settings
- Breadcrumb navigation for deep hierarchies
- Quick actions accessible from main screens
- Search functionality prominently placed

#### Data Display
- Tabular data with sorting and filtering
- Card-based layouts for complex information
- Progressive disclosure for detailed views
- Real-time updates without page refresh

### Android UI Guidelines

#### Material Design Compliance
- Follow Material Design 3 guidelines
- Use system colors and typography
- Implement proper touch targets (48dp minimum)
- Support dark/light theme switching

#### Mobile-Specific Patterns
- Bottom navigation for main sections
- Floating Action Button for primary actions
- Pull-to-refresh for data updates
- Swipe gestures for common actions

#### Performance Considerations
- Lazy loading for large datasets
- Efficient RecyclerView implementations
- Background sync with progress indicators
- Offline-first data handling

## Interaction Patterns

### Event Creation Flow
1. **Context Selection**: Choose from existing or create new context
2. **Event Details**: Description, category, impact assessment
3. **Expert Questions**: Guided wizard with explanations
4. **Review & Submit**: Final confirmation with summary
5. **Feedback**: Success confirmation with next steps

### Judgment Submission
1. **Event Review**: Display full event context
2. **Rating Interface**: Clear scoring mechanism (-1 to +1)
3. **Confidence Level**: Slider or selection (0 to 1)
4. **Rationale**: Optional text explanation
5. **Submit**: Immediate feedback and collective score update

### Synchronization Status
- **Connected**: Green indicator with last sync time
- **Syncing**: Animated progress with transfer details
- **Offline**: Yellow indicator with queue count
- **Error**: Red indicator with error message and retry option

## Visual Design Standards

### Color Palette
- **Primary**: Truth blue (#1976D2)
- **Secondary**: Accent green (#4CAF50)
- **Warning**: Amber (#FF9800)
- **Error**: Red (#F44336)
- **Success**: Green (#4CAF50)
- **Neutral**: Gray scale (#757575, #BDBDBD, #F5F5F5)

### Typography
- **Headers**: Roboto Bold, 24/20/16px
- **Body**: Roboto Regular, 14px
- **Captions**: Roboto Light, 12px
- **Code**: Roboto Mono, 14px

### Iconography
- Use Material Design icons consistently
- 24dp standard size for interface icons
- 16dp for inline icons
- Consistent style across platforms

## Accessibility Requirements

### WCAG 2.1 Compliance
- Level AA compliance minimum
- Color contrast ratio 4.5:1 for normal text
- Color contrast ratio 3:1 for large text
- Keyboard navigation support

### Screen Reader Support
- Proper semantic markup
- Alt text for images and icons
- ARIA labels for complex interactions
- Logical tab order

### Mobile Accessibility
- Touch targets minimum 48dp
- Voice control support
- High contrast mode compatibility
- Text scaling support (up to 200%)

## Error Handling & Feedback

### Error Messages
- Clear, actionable error descriptions
- Suggest specific solutions when possible
- Avoid technical jargon
- Provide contact information for complex issues

### Loading States
- Skeleton screens for content loading
- Progress bars for file operations
- Spinner for quick operations
- Timeout handling with retry options

### Success Feedback
- Immediate confirmation of actions
- Toast notifications for background operations
- Visual state changes for completed actions
- Undo options where appropriate

## Performance Guidelines

### Response Times
- Navigation: < 200ms
- Pagination: < 100ms
- Search results: < 500ms
- Sync operations: Progress indication required

### Data Loading
- Lazy loading for large lists
- Pagination for datasets > 50 items
- Caching for frequently accessed data
- Background prefetching for predictable navigation

### Offline Behavior
- Clear offline indicators
- Queue operations for later sync
- Cached data availability
- Graceful degradation of features

## Testing Requirements

### Usability Testing
- User journey validation
- A/B testing for critical flows
- Accessibility testing with assistive technologies
- Cross-platform consistency verification

### Performance Testing
- Load testing with large datasets
- Memory usage monitoring
- Battery impact assessment (mobile)
- Network efficiency testing

## Implementation Notes

### Cross-Platform Consistency
- Shared design tokens and components
- Consistent behavior across platforms
- Platform-specific adaptations where appropriate
- Regular design system updates

### Development Guidelines
- Component-based architecture
- Reusable UI libraries
- Automated testing for UI components
- Design system documentation

---

*This document aligns with [[spec/09-ux-guidelines.md](spec/09-ux-guidelines.md)](../spec/09-ux-guidelines.md) and is maintained for Truth Training v1.0.0.*

## Related Documentation

- [Technical Specification](Technical_Specification.md) - System architecture
- [API Reference](api_reference/API_REFERENCE.md) - Backend integration
- [CLI Usage](CLI_Usage.md) - Command-line interface
- [Desktop UI Guide](UI_Desktop.md) - Desktop application specifics
