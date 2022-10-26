# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 0.1.0-M2
- [ ] doublecheck the correct version

### Added
- Swagger UI now integrated with Portal Authentication
- Filter endpoint for search of list of URNs
- Support of BAMM 2.0.0
- Helm Charts available via helm Repository at Eclipse Foundation

 ### fixed
- Update jackson-databind to version 2.13.3 (A1SLDT-952)
- Update snakeyaml to version to 1.33 (A1SLDT-954)
- Update commons-text (apache-org.apache.commons) to 1.10.0

### Changed
- Update of Spring Boot version to 2.7.3

## 0.1.0-M1
- [ ] doublecheck the correct version

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