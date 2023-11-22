/********************************************************************************
 * Copyright (c) 2021-2022 Robert Bosch Manufacturing Solutions GmbH
 * Copyright (c) 2021-2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/
package org.eclipse.tractusx.semantics;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class FusekiTestContainer {
   private static final int PORT = 3030;
   private static final String IMAGE = "jena-fuseki-docker:4.7.0";
   private static final String CONTAINER_NAME = "fuseki-container";

   public static final GenericContainer<?> FUSEKI_CONTAINER =
         new GenericContainer<>( IMAGE )
               .withExposedPorts( PORT )
               .withNetworkAliases( CONTAINER_NAME )
               .withEnv( "JVM_ARGS", "-Xmx2048M" )
               .withCommand( "--tdb2", "--update", "--mem", "/ds" )
               .waitingFor( Wait.forListeningPort() );

   static {
      FUSEKI_CONTAINER.start();
   }

   @DynamicPropertySource
   static void databaseProperties( DynamicPropertyRegistry registry ) {
      String baseUrl = String.format( "http://localhost:%s/ds", FUSEKI_CONTAINER.getMappedPort( PORT ) );
      registry.add( "hub.triple-store.baseUrl", () -> baseUrl );
      registry.add( "hub.triple-store.username", () -> "admin" );
      registry.add( "hub.triple-store.password", () -> "admin" );
      registry.add( "hub.triple-store.queryEndpoint", () -> "query" );
      registry.add( "hub.triple-store.updateEndpoint", () -> "update" );
   }
}
