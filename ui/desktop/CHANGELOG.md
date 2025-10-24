# Changelog

All notable changes to the Truth Training Desktop UI will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.1] - UI Desktop Integration Phase Finalization

### Added
- Integration tests with Truth Core API
- Comprehensive FFI bridge validation tests
- Data structure validation for all API endpoints
- Error handling and performance requirement tests
- CI/CD workflow integration for validation

### Changed
- Updated CI/CD pipeline to include integration test validation
- Enhanced test coverage for desktop UI components
- Improved error handling in API communication layer

### Fixed
- Resolved Tauri configuration issues for production builds
- Fixed icon generation for Linux DEB/RPM packages
- Corrected FFI bridge command structure validation

### Technical Details
- **Frontend**: React/TypeScript with Vite build system
- **Backend**: Rust/Tauri with FFI bridge integration
- **Testing**: Jest with comprehensive integration test suite
- **Build**: Production-ready DEB/RPM packages for Linux
- **CI/CD**: GitHub Actions workflow with automated testing

## [0.1.0] - Initial Desktop UI Implementation

### Added
- Complete Tauri desktop application setup
- React/TypeScript frontend with modern UI components
- Rust backend with FFI bridge to frontend
- Zustand state management for offline-first architecture
- Comprehensive test suite (unit, integration, E2E)
- Production build configuration for Linux (DEB/RPM)
- Documentation and build instructions

### Technical Stack
- **Frontend**: React 18, TypeScript, Vite, Zustand
- **Backend**: Rust, Tauri 2.x
- **Testing**: Jest, React Testing Library, Playwright
- **Build**: Vite, Cargo, Tauri CLI
- **CI/CD**: GitHub Actions

### Features
- Event creation and management
- Judgment submission and consensus calculation
- Offline-first architecture with sync capabilities
- Health monitoring and status reporting
- Cross-platform desktop support (Linux/Windows/macOS)
