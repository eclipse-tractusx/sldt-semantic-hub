/********************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.tractusx.semantics.hub.api.ModelsApiDelegate;
import org.eclipse.tractusx.semantics.hub.domain.ModelPackageStatus;
import org.eclipse.tractusx.semantics.hub.domain.ModelPackageUrn;
import org.eclipse.tractusx.semantics.hub.model.AasFormat;
import org.eclipse.tractusx.semantics.hub.model.SemanticModel;
import org.eclipse.tractusx.semantics.hub.model.SemanticModelList;
import org.eclipse.tractusx.semantics.hub.model.SemanticModelStatus;
import org.eclipse.tractusx.semantics.hub.model.SemanticModelType;
import org.eclipse.tractusx.semantics.hub.persistence.PersistenceLayer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import io.vavr.control.Try;

public class AspectModelService implements ModelsApiDelegate {

   private final PersistenceLayer persistenceLayer;
   private final SDKAccessHelper sdkHelper;

   public AspectModelService( final PersistenceLayer persistenceLayer, SDKAccessHelper sdkHelper ) {
      this.persistenceLayer = persistenceLayer;
      this.sdkHelper = sdkHelper;
      sdkHelper.setPersistenceLayer( persistenceLayer );
   }

   @Override
   public ResponseEntity<SemanticModelList> getModelList(Integer pageSize,
                                                         Integer page,
                                                         String namespaceFilter,
                                                         SemanticModelStatus status ) {

      try {
         final String decodedNamespace = URLDecoder.decode( namespaceFilter,
               StandardCharsets.UTF_8.name() );

         ModelPackageStatus modelPackageStatus = null;
         if ( status != null ) {
            modelPackageStatus = ModelPackageStatus.valueOf( status.name() );
         }
         final SemanticModelList list = persistenceLayer.getModels( decodedNamespace,
               modelPackageStatus, page,
               pageSize );

         return new ResponseEntity<>( list, HttpStatus.OK );
      } catch ( final java.io.UnsupportedEncodingException uee ) {
         return new ResponseEntity<>( HttpStatus.BAD_REQUEST );
      }
   }

   @Override
   public ResponseEntity<SemanticModel> getModelByUrn(final String urn ) {
      final SemanticModel model = persistenceLayer.getModel( AspectModelUrn.fromUrn( urn ) );

      if ( model == null ) {
         return new ResponseEntity<>( HttpStatus.NOT_FOUND );
      }

      return new ResponseEntity<>( model, HttpStatus.OK );
   }

   @Override
   public ResponseEntity<SemanticModel> createModelWithUrn(final SemanticModelType type, final String newModel, final SemanticModelStatus status ) {
      final SemanticModel resultingModel = persistenceLayer.save( type, newModel, status );
      return new ResponseEntity<>( resultingModel, HttpStatus.OK );
   }

   @Override
   public ResponseEntity<org.springframework.core.io.Resource> getModelDiagram( final String urn ) {
      final Try<byte[]>pngBytes = sdkHelper.generateSvg( urn );
      if ( pngBytes.isFailure()  ) {
         throw new RuntimeException( String.format( "Failed to generate example payload for urn %s", urn ) );
      }
      return new ResponseEntity( pngBytes.get(), HttpStatus.OK );
   }

   @Override
   public ResponseEntity<Void> getModelJsonSchema( final String modelId ) {
      final JsonNode json = sdkHelper.getJsonSchema( modelId );
      return new ResponseEntity( json, HttpStatus.OK );
   }

   @Override
   public ResponseEntity<Void> getModelDocu( final String modelId ) {
      final Try<byte[]> docuResult = sdkHelper.getHtmlDocu( modelId );
      if ( docuResult.isFailure() ) {
         throw new RuntimeException( String.format( "Failed to generate documentation for urn %s", modelId ) );
      }
      final HttpHeaders headers = new HttpHeaders();
      headers.setContentType( MediaType.TEXT_HTML );
      return new ResponseEntity( docuResult.get(), headers, HttpStatus.OK );
   }

   @Override
   public ResponseEntity<Void> getModelFile( final String modelId ) {
      String modelDefinition = persistenceLayer.getModelDefinition( AspectModelUrn.fromUrn( modelId ) );
      return new ResponseEntity( modelDefinition, HttpStatus.OK );
   }

   @Override
   public ResponseEntity<Void> deleteModel( final String modelId ) {
      persistenceLayer.deleteModelsPackage( ModelPackageUrn.fromUrn( modelId ) );
      return new ResponseEntity<>( HttpStatus.NO_CONTENT );
   }

   @Override
   public ResponseEntity<SemanticModel> modifyModel( final SemanticModelType type, final String newModel, final SemanticModelStatus status  ) {
      final SemanticModel resultingModel = persistenceLayer.save( type, newModel, status );
      return new ResponseEntity<>( resultingModel, HttpStatus.OK );
   }

   @Override
   public ResponseEntity<SemanticModel> updateModel( String urn,
         SemanticModelStatus status ) {
      SemanticModel semanticModel = persistenceLayer.updateModel(  urn , status );
      return new ResponseEntity<>( semanticModel, HttpStatus.OK );

   }

   @Override
   public ResponseEntity<Void> getModelOpenApi( final String modelId, final String baseUrl ) {
      final String openApiJson = sdkHelper.getOpenApiDefinitionJson( modelId, baseUrl );
      return new ResponseEntity( openApiJson, HttpStatus.OK );
   }

   @Override
   public ResponseEntity<Void> getModelExamplePayloadJson( final String modelId ) {
      final Try<String> result = sdkHelper.getExamplePayloadJson( modelId );
      if ( result.isFailure() ) {
         throw new RuntimeException( String.format( "Failed to generate example payload for urn %s", modelId ) );
      }
      return new ResponseEntity( result.get(), HttpStatus.OK );
   }

   @Override
   public ResponseEntity getAasSubmodelTemplate(String urn, AasFormat aasFormat) {
      final Try result = sdkHelper.getAasSubmodelTemplate(urn, aasFormat);
      if ( result.isFailure() ) {
         throw new RuntimeException( String.format( "Failed to generate AASX submodel template for model with urn %s", urn ) );
      }
      HttpHeaders responseHeaders = new HttpHeaders();

      responseHeaders.setContentType(getMediaType( aasFormat ));

      return new ResponseEntity( result.get(), responseHeaders, HttpStatus.OK );
   }

   /**
    * Determines the MediaType based on the AasFormat
    * @param aasFormat
    * @return MediaType
    */
   private MediaType getMediaType( AasFormat aasFormat ) {
      MediaType mediaType = switch ( aasFormat ) {
         case FILE -> MediaType.APPLICATION_OCTET_STREAM;
         case XML -> MediaType.APPLICATION_XML;
         case JSON -> MediaType.APPLICATION_JSON;
      };
      return mediaType;
   }

   @Override
   public ResponseEntity<SemanticModelList> getModelListByUrns(Integer pageSize, Integer page, List<String> requestBody) {
      List<AspectModelUrn> urnList = Lists.transform(requestBody, (String urn) -> AspectModelUrn.fromUrn(urn));

      final SemanticModelList models = persistenceLayer.findModelListByUrns( urnList, page, pageSize );

      if ( models == null ) {
         return new ResponseEntity<>( HttpStatus.NOT_FOUND );
      }

      return new ResponseEntity<SemanticModelList>( models, HttpStatus.OK );
   }

}
