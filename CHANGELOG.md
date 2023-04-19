# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 0.2.3-M1
### Added

### Fixed
- Adjust deployment to contain security context

## 0.2.2-M1
### Added

### Fixed
- Fix cve-2022-45688 (update json to 20230227)

### Changed

## 0.2.1-M1
### Added

### Fixed
- Update application to springboot 3.0.5 to fix vulnerability spring-expression

### Changed

## 0.2.0-M1
### Added
- Add --no-cache to run apk in Dockerfile
- Update application to springboot 3.0.1

### Fixed

### Changed

## 0.1.0-M3
### Added

### Fixed
- Update jackson-databind to version 2.14.0
- Update commons-text to version 1.10.0
- Update snakeyaml to version 1.33

### Changed
- Update BAMM SDK from 2.0.0 to 2.0.6

## 0.1.0-M2
### Added
- Swagger UI now integrated with Portal Authentication
- Filter endpoint for search of list of URNs
- Helm Charts available via helm Repository at Eclipse Foundation

### Fixed
- Update jackson-databind to version 2.13.3
- Update snakeyaml to version to 1.31

### Changed
- Update of Spring Boot version to 2.7.3

## 0.1.0-M1
### Fixed
- Adjust tests to avoid duplicates
### Changed
- Deactivate name search

## 0.0.1-M2
### Added
- The Semantic Hub now allows the generation and export of AAS files
### Changed
- Switched to BAMM SDK version 2.0.0

## 0.0.1-M1
### Added
- The Semantic Hub allows the creation, update, deletion of Semantic models in the RDF based format BAMM Aspect Meta Model (short BAMM)
- Users can use the Semantic Hub API to view models and their meta information (such as version and current status, e.g. whether it is ready to be used)
- The Semantic Hub provides endpoints to generate artifacts from the BAMM models:
  - example payload JSON
  - detailed HTML documentation describing the model
  - a diagram of the model
  - an OpenAPI description
  - an Asset Administration Shell representation of the model as a submodel template
  - a JSON schema representation of the model
- The Semantic Hub allows the reuse of model components between different BAMM aspects and can resolve these dependencies
- The Semantic Hub prevents unauthenticated access by checking whether an access token is provided by a CX user
- The Semantic Hub enforces that only users with the correct role can read/create/update/delete semantic models
