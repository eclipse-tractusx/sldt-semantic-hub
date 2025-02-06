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

## Precondition
Build fuseki docker image by following the below steps :
- Download [jena-fuseki-docker-5.0.0.zip](https://repo1.maven.org/maven2/org/apache/jena/jena-fuseki-docker/5.0.0/jena-fuseki-docker-5.0.0.zip)
- Unzip the jena-fuseki-docker-5.0.0.zip.
- Build the docker image by running the command - `docker build --build-arg JENA_VERSION=5.0.0 -t jena-fuseki-docker:5.0.0 .`

This docker image `jena-fuseki-docker:5.0.0` will be used in the Helm deployment and test - [values.yaml](charts/semantic-hub/values.yaml) (graphdb.image).

## Build Packages

Run `mvn install` to run unit tests, build and install the package.

## Run Package Locally
To check whether the build was successful, you can start the resulting JAR file from the build process by running `java -jar target/semantic-hub-backend-{current-version}.jar`.

## Build Docker
Run `docker build -t semantic-hub .`

## Install Instructions
For detailed instructions please refer to our [INSTALL.md](INSTALL.md)