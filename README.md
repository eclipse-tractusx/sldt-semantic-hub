<!--
    Copyright (c) 2021-2023 Robert Bosch Manufacturing Solutions GmbH
    Copyright (c) 2021-2023 Contributors to the Eclipse Foundation

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

# Semantic Hub
The Semantic Hub is a logical and architectural component of Tractus-X.
The source code under this folder contains reference implementations of the SLDT Semantic Hub

## Build Packages

Run `mvn install` to run unit tests, build and install the package.

## Run Package Locally
To check whether the build was successful, you can start the resulting JAR file from the build process by running `java -jar target/semantic-hub-backend-{current-version}.jar`.

## Precondition
Build fuseki docker image by following the below steps :
- Download [jena-fuseki-docker-4.7.0.zip](https://repo1.maven.org/maven2/org/apache/jena/jena-fuseki-docker/4.7.0/jena-fuseki-docker-4.7.0.zip)
- Unzip the jena-fuseki-docker-4.7.0.zip.
- Build the docker image by running the command - `docker build --build-arg JENA_VERSION=4.7.0 -t jena-fuseki-docker:4.7.0 .`

This docker image `jena-fuseki-docker:4.7.0` will be used in the Helm deployment - [values.yaml](charts/semantic-hub/values.yaml) (graphdb.image).

## Build Docker
Run `docker build -t semantic-hub .`

## Install Instructions
For detailed instructions please refer to our [INSTALL.md](INSTALL.md)

## Notice for Docker image

This application provides container images for demonstration purposes.

DockerHub: [Docker.io:tractusx-semantic-hub](https://hub.docker.com/r/tractusx/sldt-semantic-hub). <br>

The Helm chart can be found at the GitHub repository at: [Releases](https://github.com/eclipse-tractusx/sldt-semantic-hub/releases)

Eclipse Tractus-X product(s) installed within the image:

- GitHub: https://github.com/eclipse-tractusx/sldt-semantic-hub
- Project home: https://projects.eclipse.org/projects/automotive.tractusx
- Dockerfile: https://github.com/eclipse-tractusx/sldt-semantic-hub/blob/main/backend/Dockerfile
- Project license: [Apache License, Version 2.0](https://github.com/eclipse-tractusx/sldt-semantic-hub/blob/main/LICENSE)

**Used base image**
- [eclipse-temurin:17-jre-alpine](https://github.com/adoptium/containers)
- Official Eclipse Temurin DockerHub page: https://hub.docker.com/_/eclipse-temurin  
- Eclipse Temurin Project: https://projects.eclipse.org/projects/adoptium.temurin  
- Additional information about the Eclipse Temurin images: https://github.com/docker-library/repo-info/tree/master/repos/eclipse-temurin

As with all Docker images, these likely also contain other software which may be under other licenses (such as Bash, etc from the base distribution, along with any direct or indirect dependencies of the primary software being contained).

As for any pre-built image usage, it is the image user's responsibility to ensure that any use of this image complies with any relevant licenses for all software contained within.

### NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023 Robert Bosch Manufacturing Solutions GmbH
- SPDX-FileCopyrightText: 2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/sldt-semantic-hub.git