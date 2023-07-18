/********************************************************************************
 * Copyright (c) 2021-2023 Robert Bosch Manufacturing Solutions GmbH
 * Copyright (c) 2021-2023 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.semantics.hub;

import java.net.Authenticator;
import java.net.http.HttpClient;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdfconnection.LibSec;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.eclipse.tractusx.semantics.hub.persistence.PersistenceLayer;
import org.eclipse.tractusx.semantics.hub.persistence.triplestore.SdsSdk;
import org.eclipse.tractusx.semantics.hub.persistence.triplestore.TripleStorePersistence;

@Configuration
@EnableConfigurationProperties( TripleStoreProperties.class )
public class TripleStoreConfiguration {

   @Bean
   public AspectModelService aspectModelService( final PersistenceLayer persistenceLayer ) {
      return new AspectModelService( persistenceLayer, new SDKAccessHelper() );
   }

   @Bean
   public PersistenceLayer tripleStorePersistence( final RDFConnectionRemoteBuilder rdfConnectionRemoteBuilder ) {
      return new TripleStorePersistence( rdfConnectionRemoteBuilder, new SdsSdk() );
   }

   /**
    * Embedded Fuseki triple store for local dev and tests.
    */
   @ConditionalOnProperty( prefix = "hub.triple-store.embedded", name = "enabled", havingValue = "true",
                           matchIfMissing = false )
   @Bean( destroyMethod = "stop" )
   public FusekiServer fusekiServer( final TripleStoreProperties properties ) {
      final TripleStoreProperties.EmbeddedTripleStore embedded = properties.getEmbedded();
      return FusekiServer.create().port( embedded.getPort() )
                         .add( embedded.getDefaultDataset(), DatasetFactory.create() )
                         .verbose( true )
                         .contextPath( embedded.getContextPath() )
                         .enableStats( true )
                         .enableCors( true )
                         .build().start();
   }

   @Bean
   public RDFConnectionRemoteBuilder rdfConnectionBuilder( final TripleStoreProperties properties ) {
      if ( properties.getEmbedded().isEnabled() ) {
         return RDFConnectionRemote.create().destination( localDestination( properties.getEmbedded() ) );
      }
      Credentials credentials = new UsernamePasswordCredentials( properties.getUsername(), properties.getPassword() );
      BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials( AuthScope.ANY, credentials );
      Authenticator basicAuth = LibSec.authenticator( properties.getUsername(), properties.getPassword() );
      return RDFConnectionRemote
            .create()
            .httpClient(  HttpClient.newBuilder().authenticator(basicAuth).build())
            .destination( properties.getBaseUrl().toString() )
            .queryEndpoint( properties.getQueryEndpoint() )
            .updateEndpoint( properties.getUpdateEndpoint() );
   }

   private static String localDestination( final TripleStoreProperties.EmbeddedTripleStore embedded ) {
      return "http://localhost:" + embedded.getPort() + embedded.getContextPath() + embedded.getDefaultDataset();
   }
}
