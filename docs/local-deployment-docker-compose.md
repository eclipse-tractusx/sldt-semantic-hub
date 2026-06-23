<!--
    Copyright (c) 2026 Contributors to the Eclipse Foundation
    Copyright (c) 2026 Capgemini Deutschland GmbH

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Apache License, Version 2.0 which is available at
    https://www.apache.org/licenses/LICENSE-2.0.

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations
    under the License.

    SPDX-License-Identifier: Apache-2.0
-->

# Local Deployment with Docker Compose

This guide describes how to run the Semantic Hub and all of its dependencies
locally with Docker Compose, as an alternative to the Helm/minikube deployment
described in [INSTALL.md](../INSTALL.md). The [docker-compose.yml](../docker-compose.yml)
at the repository root bundles the same services the Helm chart deploys:

| Service        | Image                          | Purpose                                            | Host port |
|----------------|--------------------------------|----------------------------------------------------|-----------|
| `semantic-hub` | `semantic-hub:local` (built)   | The Semantic Hub backend                           | 4242      |
| `graphdb`      | `jena-fuseki-docker:5.3.0`     | Apache Jena Fuseki triple store (persistent, TDB2) | 3030      |

## Prerequisites

- Docker with the Compose plugin (`docker compose version` ≥ 2.x)
- `curl` and `unzip` for building the Fuseki image

Maven and a JDK are **not** required on the host — the Semantic Hub is built
inside a multi-stage Docker build.

## Step 1: Build the Fuseki image

The triple store image is not published on a public registry and must be built
locally once (same precondition as for the Helm deployment):

```bash
curl -LO https://repo1.maven.org/maven2/org/apache/jena/jena-fuseki-docker/5.3.0/jena-fuseki-docker-5.3.0.zip
unzip jena-fuseki-docker-5.3.0.zip
cd jena-fuseki-docker-5.3.0
docker build --build-arg JENA_VERSION=5.3.0 -t jena-fuseki-docker:5.3.0 .
cd ..
```

Verify the image exists:

```bash
docker image ls jena-fuseki-docker
```

## Step 2: Build and start the stack

From the repository root:

```bash
docker compose up -d --build
```

This builds the Semantic Hub image from [backend/Dockerfile](../backend/Dockerfile)
(Maven build inside the container, first run takes a few minutes) and starts:

- **graphdb** — Fuseki with a persistent TDB2 dataset `/ds`, started with
  `--tdb2 --update --loc databases/ /ds` (same arguments as the Helm chart).
  Data is persisted in the named Docker volume `fuseki-data`.
- **semantic-hub** — connected to Fuseki via `HUB_TRIPLE_STORE_BASE_URL=http://graphdb:3030/ds`.
  Authentication is disabled via the `local` Spring profile, exactly like the
  Helm chart does when `hub.authentication=false`.

## Step 3: Verify the deployment

Wait until both containers are up and the hub is healthy:

```bash
docker compose ps
curl http://localhost:4242/actuator/health
```

Expected output: `{"status":"UP", ...}`.

The hub needs a moment to start (the Helm probes allow ~100 s); follow the
logs if it is not ready yet:

```bash
docker compose logs -f semantic-hub
```

## Step 4: Use the API

- **Swagger UI**: <http://localhost:4242/>
- **API base URL**: `http://localhost:4242/api/v1`
- **Fuseki SPARQL endpoint** (direct triple store access): `http://localhost:3030/ds`

Example — upload an aspect model and read it back:

```bash
# Create a minimal SAMM aspect model
curl -X POST "http://localhost:4242/api/v1/models?type=SAMM&status=DRAFT" \
  -H "Content-Type: text/plain" \
  --data-binary @- <<'EOF'
@prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.2.0#> .
@prefix : <urn:samm:org.example:1.0.0#> .

:MyAspect a samm:Aspect ;
   samm:preferredName "My Aspect"@en ;
   samm:properties ( ) ;
   samm:operations ( ) ;
   samm:events ( ) .
EOF

# List all models
curl "http://localhost:4242/api/v1/models"
```

## Step 5: Tear down

```bash
docker compose down          # stop containers, keep triple store data
docker compose down -v       # stop containers and delete the Fuseki data volume
```

## Optional: In-memory triple store instead of Fuseki

For a quick, non-persistent setup you can use the hub's embedded triple store
and skip Fuseki entirely (equivalent to `hub.embeddedTripleStore=true` in the
Helm chart). Set in `docker-compose.yml`:

```yaml
  semantic-hub:
    environment:
      HUB_TRIPLE_STORE_EMBEDDED_ENABLED: "true"
```

and start only the hub: `docker compose up -d --build semantic-hub`.
All data is lost when the container restarts.

## Troubleshooting

- **`pull access denied for jena-fuseki-docker`** — the Fuseki image was not
  built locally; repeat step 1.
- **Hub logs connection errors to `graphdb`** — Fuseki may still be starting;
  the hub recovers once Fuseki is reachable. Check `docker compose logs graphdb`.
- **Port already in use** — adjust the host-side port mappings (`4242`, `3030`)
  in `docker-compose.yml`.
