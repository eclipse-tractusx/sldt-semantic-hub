## 6 Concept

### Overall Concept

#The overall concept can be found under **2 Architecture and
constraints**.

### Semantic Hub

Example Aspect Model
```
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix bamm: <urn:bamm:io.openmanufacturing:meta-model:1.0.0#> .
@prefix : <urn:bamm:org.idtwin:1.0.0#> .


:DocumentationSimple a bamm:Aspect;
bamm:name "ManufacturerDocumentationSimple";
bamm:preferredName "ManufacturerDocumentation"@en;
bamm:description "The Submodel defines a simplified set of manufacturer documentation to bring about information from manufacturer to operator of industrial equipment."@en;

:documents a bamm:Property;
bamm:name "documents";
bamm:preferredName "documents"@en;
bamm:description "Set of documents"@en;
```

The Semantic Hub will add the release status as triple upon upload:
Release Status
```
@prefix aux: <urn:bamm:io.openmanufacturing:aspect-model:aux#>

<urn:bamm:org.idtwin:1.0.0#> aux:releaseStatus aux:DRAFT .
```

## Package
| No | Rule                                                                                                              | Example                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|----|-------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1. | A package is defined by the urn prefix until "#".                                                                 | net.catenax.semantics.product:1.2.0#                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| 2. | A package can contain one or multiple aspects.                                                                    | Example 1: net.catenax.semantics.traceability:1.2.0#Traceability Example 2: net.catenax.semantics.product:1.2.0#ProductDescription net.catenax.semantics.product:1.2.0#ProductUsage net.catenax.semantics.product:1.2.0#ProductDetails                                                                                                                                                                                                                                                                                |
| 3. | Multiple versions of a package can exists.                                                                        | Possible: net.catenax.semantics.product:1.2.0 net.catenax.semantics.product:4.2.0                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| 4. | The versioning applies to the package.  All aspects and model elements scoped to a package have the same version. | Possible: net.catenax.semantics.product:1.2.0#ProductDescription  net.catenax.semantics.product:1.2.0#ProductUsage net.catenax.semantics.product:1.2.0#ProductDetails  Possible:net.catenax.semantics.product:4.3.0#ProductDescription  net.catenax.semantics.product:4.3.0#ProductUsage net.catenax.semantics.product:4.3.0#ProductDetails  Not Possible: net.catenax.semantics.product:1.3.0#ProductDescription net.catenax.semantics.product:1.2.0#ProductUsage net.catenax.semantics.product:3.2.0#ProductDetails |
| 5. | All aspect models and model elements scoped to a package have the same status.                                    | Possible: net.catenax.semantics.product:1.2.0#ProductDescription → RELEASE, net.catenax.semantics.product:1.2.0#ProductUsage → RELEASE net.catenax.semantics.product:1.2.0#ProductDetails → RELEASE Not Possible: net.catenax.semantics.product:1.2.0#ProductDescription → RELEASE, net.catenax.semantics.product:1.2.0#ProductUsage → DRAFT                                                                                                                                                                          |


### NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023 Robert Bosch Manufacturing Solutions GmbH
- SPDX-FileCopyrightText: 2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/sldt-semantic-hub.git