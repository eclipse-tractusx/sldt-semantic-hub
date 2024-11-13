## 4 Runtime-view

### Upload of an aspect model
```mermaid
sequenceDiagram
    actor ModelAdmin as Model Admin
    participant SemanticHub as Semantic hub
    participant TripleStore as TripleStore

    ModelAdmin ->>+ SemanticHub: Upload aspect model file (.ttl)
    loop validation
    SemanticHub ->>+ SemanticHub: Check if aspect model is complaint with bamm/samm (using sdk)
    SemanticHub ->>+ TripleStore: Check if aspect model already exists
    Note over SemanticHub, TripleStore: If the aspect model exists and is in release status, <br> the upload will be rejected. <br>If the aspect model exists in draft status,<br> upload will be accepted.
    SemanticHub ->>+ TripleStore: Check if external referenceses exists
    Note over SemanticHub, TripleStore: If an external reference does not exists,<br> the upload will be rejected.<br>Should we allow to reference namespaces,<br> in DRAFT state?
    TripleStore -->> SemanticHub: response ok
    end
    note over ModelAdmin, SemanticHub: The model admin provides the release status <br> (RELEASED/DRAFT) upon upload
    SemanticHub ->>+ SemanticHub: Add the release status as triple to the model
    SemanticHub ->>+ TripleStore: Write all triples to TripleStore
    TripleStore -->> SemanticHub: response ok
    SemanticHub -->> ModelAdmin: Response upload successful
```
| Validation | Description Value | 
|---|---|
| BAMM compliance  | Checks if the model is compliant with the BAMM. The BAMM SDK does provide the validation logic. |
| Model Status check (RELEASE vs DRAFT)  | Uploads will always accepted when there are no existing namespace:version combination in the TripleStore. For a model in DRAFT state, uploads will always be accepted. For a model in RELEASE state, uploads will be denied. RELEASED models are immutable. |
|  External reference check | It will be checked if all exernal references are available in the TripleStore. The BAMM SDK does provide a mechanisim where the resolving against the TripleStore can be integrated. |


## Download of the documentation of an Aspect Model
```mermaid
sequenceDiagram
    actor ModelAdmin as Model Admin
    participant SemanticHub as Semantic hub
    participant TripleStore as TripleStore

    ModelAdmin ->>+ SemanticHub: Get documentation for aspect model com.catenax:0.0.1:Sample
    SemanticHub ->>+ TripleStore: Resolve aspect model with all references based on provided urn com.catenax:0.0.1:Sample
    TripleStore -->> SemanticHub: response 
    SemanticHub ->>+ SemanticHub: generate documentation based on the response from TripleStore
    SemanticHub -->> ModelAdmin: respond with generated documenation
```
```

Example queries to resolve an aspect model with all references:
Construct Query
```
@prefix ns: <urn:bamm:org.idtwin:1.0.0#DocumentationSimple>

CONSTRUCT {
?s ?p ?o .
} WHERE {
bind( ns: as ?aspect)
?aspect (<>|!<>)* ?s . // resolves all references
?s ?p ?o .
}
Search for Aspect Models
The current search API can stay as is. Below is an example query for selecting bamm properties:
Search Queries
CONSTRUCT {
?s ?p ?o .
} WHERE {   
FILTER ( $param == ?o )  // Custom filter can be added here.
?s ?p ?o .
}


### NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023 Robert Bosch Manufacturing Solutions GmbH
- SPDX-FileCopyrightText: 2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/sldt-semantic-hub.git