/*
 * Copyright (c) 2022 Bosch Software Innovations GmbH. All rights reserved.
 */

package org.eclipse.tractusx.semantics.hub;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.tractusx.semantics.hub.domain.ModelPackageStatus;
import org.eclipse.tractusx.semantics.hub.domain.ModelPackageUrn;
import org.eclipse.tractusx.semantics.hub.model.AasFormat;
import org.eclipse.tractusx.semantics.hub.model.SemanticModel;
import org.eclipse.tractusx.semantics.hub.model.SemanticModelList;
import org.eclipse.tractusx.semantics.hub.model.SemanticModelStatus;
import org.eclipse.tractusx.semantics.hub.model.SemanticModelType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.metamodel.Aspect;
import org.eclipse.tractusx.semantics.hub.bamm.BammHelper;
import org.eclipse.tractusx.semantics.hub.persistence.PersistenceLayer;
import io.vavr.control.Try;
import org.eclipse.tractusx.semantics.hub.api.ModelsApiDelegate;

public class AspectModelService implements ModelsApiDelegate {

   private final PersistenceLayer persistenceLayer;
   private final BammHelper bammHelper;

   public AspectModelService( final PersistenceLayer persistenceLayer, BammHelper bammHelper ) {
      this.persistenceLayer = persistenceLayer;
      this.bammHelper = bammHelper;
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
      final VersionedModel versionedModel = getVersionedModel( urn );
      final Try<byte[]>pngBytes = bammHelper.generatePng( versionedModel );
      if ( pngBytes.isFailure()  ) {
         throw new RuntimeException( String.format( "Failed to generate example payload for urn %s", urn ) );
      }
      return new ResponseEntity( pngBytes.get(), HttpStatus.OK );
   }

   @Override
   public ResponseEntity<Void> getModelJsonSchema( final String modelId ) {
      final JsonNode json = bammHelper.getJsonSchema( getBamAspect( modelId ) );
      return new ResponseEntity( json, HttpStatus.OK );
   }

   @Override
   public ResponseEntity<Void> getModelDocu( final String modelId ) {
      VersionedModel versionedModel = getVersionedModel( modelId );
      final Try<byte[]> docuResult = bammHelper.getHtmlDocu( versionedModel );
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
   public ResponseEntity<Void> getModelOpenApi( final String modelId, final String baseUrl ) {
      final Aspect bammAspect = getBamAspect( modelId );
      final String openApiJson = bammHelper.getOpenApiDefinitionJson( bammAspect, baseUrl );
      return new ResponseEntity( openApiJson, HttpStatus.OK );
   }

   @Override
   public ResponseEntity<Void> getModelExamplePayloadJson( final String modelId ) {
      final Aspect bammAspect = getBamAspect( modelId );
      final Try<String> result = bammHelper.getExamplePayloadJson( bammAspect );
      if ( result.isFailure() ) {
         throw new RuntimeException( String.format( "Failed to generate example payload for urn %s", modelId ) );
      }
      return new ResponseEntity( result.get(), HttpStatus.OK );
   }

   @Override
   public ResponseEntity getAasSubmodelTemplate(String urn, AasFormat aasFormat) {
      final Aspect bammAspect = getBamAspect( urn );
      final Try result = bammHelper.getAasSubmodelTemplate(bammAspect, aasFormat);
      if ( result.isFailure() ) {
         throw new RuntimeException( String.format( "Failed to generate AASX submodel template for model with urn %s", urn ) );
      }
      HttpHeaders responseHeaders = new HttpHeaders();

      if(aasFormat.equals(AasFormat.FILE)) {
         responseHeaders.set("Content-Type", "application/octet-stream");
      } else {
         responseHeaders.set("Content-Type", "application/xml");
      }

      return new ResponseEntity( result.get(), responseHeaders, HttpStatus.OK );
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

   private Aspect getBamAspect( String urn ) {
      final Try<Aspect> aspect = bammHelper.getAspectFromVersionedModel( getVersionedModel( urn ) );
      if ( aspect.isFailure() ) {
         throw new RuntimeException( "Failed to load aspect model", aspect.getCause() );
      }
      return aspect.get();
   }

   private VersionedModel getVersionedModel( String urn ) {
      final String modelDefinition = persistenceLayer.getModelDefinition( AspectModelUrn.fromUrn( urn ) );

      final Try<VersionedModel> versionedModel = bammHelper.loadBammModel( modelDefinition );

      if ( versionedModel.isFailure() ) {
         throw new RuntimeException( "Failed to load versioned model", versionedModel.getCause() );
      }
      return versionedModel.get();
   }
}
