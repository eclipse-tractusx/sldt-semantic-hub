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
package org.eclipse.tractusx.semantics.hub;

import java.net.URL;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties( "hub.triple-store" )
public class TripleStoreProperties {

   private final EmbeddedTripleStore embedded = new EmbeddedTripleStore();

   /**
    * The base url for the tripe store connection.
    * <p>
    * Optional. If the embedded server is enabled the url for the embedded server is used.
    */
   private URL baseUrl;

   /**
    * The query endpoint
    */
   private String queryEndpoint;
   /**
    * The update endpoint
    */
   private String updateEndpoint;
   /**
    * The username to authenticate against the triplestore
    */
   private String username;
   /**
    * The password to authenticate against the triplestore
    */
   private String password;

   public URL getBaseUrl() {
      return baseUrl;
   }

   public void setBaseUrl( final URL baseUrl ) {
      this.baseUrl = baseUrl;
   }

   public EmbeddedTripleStore getEmbedded() {
      return embedded;
   }

   public static class EmbeddedTripleStore {
      /**
       * The local port for the Fuseki server.
       * If the port is 0, a random free port will be used
       * <p>
       * Default is 0
       */
      private int port = 3335;

      /**
       * Is the embedded fuseki server enabled?
       * <p>
       * Default is {false}
       */
      private boolean enabled = false;
      private String defaultDataset = "/data";
      private String contextPath = "/fuseki";

      public String getContextPath() {
         return contextPath;
      }

      public void setContextPath( final String contextPath ) {
         this.contextPath = contextPath;
      }

      public String getDefaultDataset() {
         return defaultDataset;
      }

      public void setDefaultDataset( final String defaultDataset ) {
         this.defaultDataset = defaultDataset;
      }

      public boolean isEnabled() {
         return enabled;
      }

      public void setEnabled( final boolean enabled ) {
         this.enabled = enabled;
      }

      public void setPort( final int port ) {
         this.port = port;
      }

      public int getPort() {
         return port;
      }
   }

   public String getUsername() {
      return username;
   }

   public void setUsername( String username ) {
      this.username = username;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword( String password ) {
      this.password = password;
   }

   public String getQueryEndpoint() {
      return queryEndpoint;
   }

   public void setQueryEndpoint( String queryEndpoint ) {
      this.queryEndpoint = queryEndpoint;
   }

   public String getUpdateEndpoint() {
      return updateEndpoint;
   }

   public void setUpdateEndpoint( String updateEndpoint ) {
      this.updateEndpoint = updateEndpoint;
   }
}
