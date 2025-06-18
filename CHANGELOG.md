# Change Log

All notable changes to this project will be documented in this file. This change log follows the conventions
of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

## 3.4.2 - 2025-06-17

### Added

- Apply schema coercion with in `schema-body-in-interceptor`.

## 2.4.2 - 2024-11-17

### Fixed

- Use `:http-request-in-handle-timing-v2` metric.

## 1.4.2 - 2024-11-16

### Added

- Added `service-component.interceptors/http-request-in-handle-timing-interceptor` interceptor.

## 1.3.2 - 2024-11-10

### Added

- Bump Pedestal version to 0.7.2.

## 1.3.1 - 2024-11-09

### Fixed

- Added missing pedestal interceptor dependencies.

## 1.3.0 - 2024-11-09

### Fixed

- Fixed error handling interceptors.

## 0.3.0 - 2024-11-09

### Added

- Added body request schema validation.

## 0.2.0 - 2024-11-08

### Added

- Added default interceptors to the service component.

## 0.1.0 - 2024-11-08

### Added

- Initial implementation of the service-component library.
