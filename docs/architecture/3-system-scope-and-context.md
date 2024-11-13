## 3 System scope and context

### Business Context

```mermaid
%%{init: {"flowchart": {"curve": "linear"} }}%%
flowchart LR
    DC(Data Consumer)
    DP(Data Provider)
    K(Keycloak)

    subgraph Semantic Hub
    SHB(Semantic Hub Backend)
    SH[(Sematic Hub postgres)]
    end
    
    SHB <-->|Find submodels / metadata| DC
    SHB <-->|Submodel creation \n provide metadata| DP

    SHB <--> SH

    K -->|Public key for token validation| SHB

    DC <-->|Token request| K
    DP <-->|Token request| K
```

### NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023 Robert Bosch Manufacturing Solutions GmbH
- SPDX-FileCopyrightText: 2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/sldt-semantic-hub.git