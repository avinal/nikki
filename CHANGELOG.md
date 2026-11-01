# Changelog

All notable changes to Nikki are documented here.

Sections per release: **Breaking**, Added, Changed, Fixed, Removed.
A `### Breaking` section means existing users must take action before or after upgrading.

## [Unreleased]

## [1.5.0] - 2026-11-01

### Breaking
- Requires Memos 26.10.0+ — REST API paths changed from `/api/v1/...` to `/api/...`; pre-26.10.0 servers will stop working

### Added
- Onboarding flow: welcome, permissions, and sign-in screens with back/next navigation
- Calendar permission requested during onboarding alongside notifications and battery
- Link preview image toggle in settings (off by default, avoids third-party connections)
- Per-ABI APK splits — install only the ~1.85 MB relevant to your device

### Fixed
- API compatibility with Memos 26.10.0 (new path prefix, renamed comments response field)
- Expired access tokens now trigger re-login instead of silent failure
- Link preview caching no longer blocks retries when server URL is not yet loaded
- Calendar sync skipped when permission not granted instead of silently failing

## [1.4.2] - 2026-10-15

### Fixed
- Reproducible build configuration for F-Droid

## [1.4.1] - 2026-09-30

### Fixed
- Share link URL corrected to use the Memos frontend route

## [1.4.0] - 2026-09-30

### Breaking
- Application ID changed from `com.avinal.memos` to `space.avinal.nikki` — uninstall the old version before installing this one

### Added
- Memos v0.31 support: Spaces visibility, updated reaction and timestamp APIs
- Server-side link previews replace client-side HTML fetching
- Share links for public access to individual memos
- Configurable memo preview lines in feed (Settings → memos → preview lines)
- Accent-colored date gutter on memo cards with adaptive labels
- Task screen: left-edge urgency colors, memo filter dropdown
- Feed shows one link preview per card, all shown when expanded

## [1.3.0] - 2026-07-28

### Added
- Memos v0.30 compatibility
- Relative timestamps and timestamp tests
- Dependency updates

## [1.2.0] - 2026-05-15

### Added
- Offline-first memo cache with pending sync queue
- Link previews for URLs in memos (7-day Room cache)
- Pull-to-refresh and configurable sync interval

### Fixed
- Various UI polish and stability improvements

## [1.1.1] - 2026-03-10

### Fixed
- Compiler warnings
- Minor stability fixes

## [1.1.0] - 2026-03-01

### Added
- Task reminders via calendar integration
- Priority notification channels (p1 bypasses DND)
- Parser doctor with inline error detection

## [1.0.0] - 2026-01-15

### Added
- Initial release
- Full Memos API integration with offline sync
- Task parsing from markdown checkboxes with due dates, times, priorities, reminders, tags
- 4-channel priority notifications
- 20 accent colors, 3 themes (dark, light, AMOLED)
- Activity calendar, tag browser, search, archived memos
- Share intent receiver for quick capture
- JSON backup and restore
