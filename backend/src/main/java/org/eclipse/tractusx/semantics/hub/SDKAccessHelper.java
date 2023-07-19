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

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import org.eclipse.esmf.aspectmodel.VersionNumber;
import org.eclipse.tractusx.semantics.hub.bamm.SDKAccessHelperBAMM;
import org.eclipse.tractusx.semantics.hub.model.AasFormat;
import org.eclipse.tractusx.semantics.hub.persistence.PersistenceLayer;
import org.eclipse.tractusx.semantics.hub.samm.SDKAccessHelperSAMM;
import org.springframework.stereotype.Component;

import org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel;
import org.eclipse.esmf.aspectmodel.shacl.violation.Violation;

import io.vavr.control.Try;

@Component
public class SDKAccessHelper {

   SDKAccessHelperBAMM sdkAccessHelperBAMM = new SDKAccessHelperBAMM();
   SDKAccessHelperSAMM sdkAccessHelperSAMM = new SDKAccessHelperSAMM();


   public void setPersistenceLayer( PersistenceLayer persistenceLayer ) {
      sdkAccessHelperSAMM.setPersistenceLayer( persistenceLayer );
      sdkAccessHelperBAMM.setPersistenceLayer( persistenceLayer );
   }

   private boolean isBAMM(String urn){
      String BAMM_IDENTIFICATION_STRING = "bamm:";
      return urn.contains( BAMM_IDENTIFICATION_STRING );
   }

   public List<Violation> validateModel( Try<VersionedModel> model ) {
      // SAMM validator should also process BAMM models so no switch here
      return sdkAccessHelperSAMM.validateModel( model );
   }

   public Try<byte[]> generatePng( String urn ) {
      if( isBAMM( urn )) {
         return sdkAccessHelperBAMM.generatePng( urn );
      }else {
         return sdkAccessHelperSAMM.generatePng( urn );
      }
   }

   public JsonNode getJsonSchema( String urn ) {
      if( isBAMM( urn )) {
         return sdkAccessHelperBAMM.getJsonSchema( urn );
      }else {
         return sdkAccessHelperSAMM.getJsonSchema( urn );
      }
   }

   public Try<byte[]> getHtmlDocu( String urn ) {
      if( isBAMM( urn )) {
         return sdkAccessHelperBAMM.getHtmlDocu( urn );
      }else {
         return sdkAccessHelperSAMM.getHtmlDocu( urn );
      }
   }

   public String getOpenApiDefinitionJson( String urn, String baseUrl ) {
      if( isBAMM( urn )) {
         return sdkAccessHelperBAMM.getOpenApiDefinitionJson( urn, baseUrl );
      }else {
         return sdkAccessHelperSAMM.getOpenApiDefinitionJson( urn, baseUrl );
      }
   }

   public Try<String> getExamplePayloadJson( String urn  ) {
      if( isBAMM( urn )) {
         return sdkAccessHelperBAMM.getExamplePayloadJson( urn );
      }else {
         return sdkAccessHelperSAMM.getExamplePayloadJson( urn );
      }
   }

   public Try getAasSubmodelTemplate( String urn, AasFormat aasFormat ) {
      if( isBAMM( urn )) {
         return sdkAccessHelperBAMM.getAasSubmodelTemplate( urn, aasFormat );
      }else {
         return sdkAccessHelperSAMM.getAasSubmodelTemplate( urn, aasFormat );
      }
   }

   public Try<VersionedModel> loadBammModel( String modelString ) {
      if( isBAMM( modelString )) {
         Try<io.openmanufacturing.sds.aspectmodel.resolver.services.VersionedModel> versionModel =
               sdkAccessHelperBAMM.loadBammModel( modelString );
         if ( versionModel.isFailure() ) {
            throw new RuntimeException( "Failed to load aspect model", versionModel.getCause() );
         }

         VersionNumber versionNumber =
               new VersionNumber(  versionModel.get().getMetaModelVersion().getMajor(),
                     versionModel.get().getMetaModelVersion().getMinor(),
                     versionModel.get().getMetaModelVersion().getMicro());

         return Try.of( () -> new VersionedModel(
               versionModel.get().getModel(),
               versionNumber,
               versionModel.get().getRawModel()
         ));

      }else {
         return sdkAccessHelperSAMM.loadSammModel( modelString );
      }
   }
}
