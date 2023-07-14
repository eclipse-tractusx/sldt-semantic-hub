/********************************************************************************
 * Copyright (c) 2021-2023 Robert Bosch Manufacturing Solutions GmbH
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
package org.eclipse.tractusx.semantics.hub.bamm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.jena.rdf.model.Model;
import org.eclipse.esmf.aspectmodel.aas.AspectModelAASGenerator;
import org.eclipse.esmf.aspectmodel.generator.diagram.AspectModelDiagramGenerator;
import org.eclipse.esmf.aspectmodel.generator.diagram.AspectModelDiagramGenerator.Format;
import org.eclipse.esmf.aspectmodel.generator.docu.AspectModelDocumentationGenerator;
import org.eclipse.esmf.aspectmodel.generator.docu.AspectModelDocumentationGenerator.HtmlGenerationOption;
import org.eclipse.esmf.aspectmodel.generator.json.AspectModelJsonPayloadGenerator;
import org.eclipse.esmf.aspectmodel.generator.jsonschema.AspectModelJsonSchemaGenerator;
import org.eclipse.esmf.aspectmodel.generator.openapi.AspectModelOpenApiGenerator;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelResolver;
import org.eclipse.esmf.aspectmodel.resolver.services.TurtleLoader;
import org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel;
import org.eclipse.esmf.aspectmodel.shacl.violation.Violation;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.eclipse.esmf.metamodel.Aspect;
import org.eclipse.esmf.metamodel.AspectContext;
import org.eclipse.esmf.metamodel.loader.AspectModelLoader;
import org.eclipse.tractusx.semantics.hub.model.AasFormat;
import org.eclipse.tractusx.semantics.hub.persistence.PersistenceLayer;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.CharStreams;

import io.vavr.control.Try;

@Component
public class SDKAccessHelperSAMM {

   PersistenceLayer persistenceLayer;

   public void setPersistenceLayer( PersistenceLayer persistenceLayer ) {
      this.persistenceLayer = persistenceLayer;
   }


   public List<Violation> validateModel( Try<VersionedModel> model ) {
      final AspectModelValidator validator = new AspectModelValidator();
      return validator.validateModel( model );
   }

   public Try<byte[]> generatePng( String urn ) {
      VersionedModel versionedModel = getVersionedModel( urn );
      final AspectModelDiagramGenerator generator = new AspectModelDiagramGenerator( versionedModel );

      try {
         ByteArrayOutputStream output = new ByteArrayOutputStream();
         generator.generateDiagram( Format.PNG, Locale.ENGLISH, output );
         final byte[] bytes = output.toByteArray();

         return Try.success(bytes);
      } catch ( IOException e ) {
         return Try.failure( e );
      }
   }

   public JsonNode getJsonSchema( String urn ) {
      Aspect aspect = getBamAspect( urn );
      AspectModelJsonSchemaGenerator jsonSchemaGenerator = new AspectModelJsonSchemaGenerator();
      return jsonSchemaGenerator.apply( aspect, Locale.ENGLISH );
   }

   public Try<byte[]> getHtmlDocu( String urn ) {
      VersionedModel versionedModel = getVersionedModel( urn );
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      final Aspect aspect = AspectModelLoader.getAspects(versionedModel).get().get(0);
      
      AspectModelDocumentationGenerator documentationGenerator = new AspectModelDocumentationGenerator( new AspectContext(versionedModel, aspect) );

      Map<HtmlGenerationOption, String> options = new HashMap();

      try {
         InputStream ompCSS = getClass().getResourceAsStream( "/catena-template.css" );
         String defaultCSS = CharStreams.toString( new InputStreamReader( ompCSS ) );

         options.put( HtmlGenerationOption.STYLESHEET, defaultCSS );
      } catch ( IOException e ) {
         return Try.failure( e );
      }

      try {
         documentationGenerator.generate( ( String a ) -> {
            return output;
         }, options );

         return Try.success( output.toByteArray() );
      } catch ( IOException e ) {
         return Try.failure( e );
      }
   }

   public String getOpenApiDefinitionJson( String urn, String baseUrl ) {
      Aspect aspect = getBamAspect( urn );
      AspectModelOpenApiGenerator openApiGenerator = new AspectModelOpenApiGenerator();

      JsonNode resultJson = openApiGenerator.applyForJson( aspect, true, baseUrl, Optional.empty(), Optional.empty(), false, Optional.empty() );

      return resultJson.toString();
   }

   public Try<String> getExamplePayloadJson( String urn ) {
      Aspect aspect = getBamAspect( urn );
      AspectModelJsonPayloadGenerator payloadGenerator = new AspectModelJsonPayloadGenerator( aspect );

      return Try.of( payloadGenerator::generateJson );
   }

   public Try getAasSubmodelTemplate( String urn, AasFormat aasFormat ) {
      Aspect aspect = getBamAspect( urn );
      AspectModelAASGenerator aasGenerator = new AspectModelAASGenerator();
      ByteArrayOutputStream stream = new ByteArrayOutputStream();

      try {
         switch ( aasFormat ) {
         case FILE:
            aasGenerator.generateAASXFile( aspect, ( String s ) -> {
               return stream;
            } );
            return Try.of( stream::toByteArray );
         case XML:
            aasGenerator.generateAasXmlFile( aspect, ( String s ) -> {
               return stream;
            } );
            return Try.of( stream::toString );
         default:
            return Try.failure( new Exception( String.format( "Wrong AAS output format %s", aasFormat.toString() ) ) );

         }
      } catch ( IOException e ) {
         return Try.failure( e );
      }
   }

   private org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel getVersionedModel( String urn ) {
      final String modelDefinition = persistenceLayer.getModelDefinition( AspectModelUrn.fromUrn( urn ) );
      final Try<org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel> versionedModel =
            loadBammModel( modelDefinition );

      if ( versionedModel.isFailure() ) {
         throw new RuntimeException( "Failed to load versioned model", versionedModel.getCause() );
      }
      return versionedModel.get();
   }

   private org.eclipse.esmf.metamodel.Aspect getBamAspect( String urn ) {
      final Try<List<org.eclipse.esmf.metamodel.Aspect>> aspect =
            getAspectFromVersionedModel( getVersionedModel( urn ) );
      if ( aspect.isFailure() ) {
         throw new RuntimeException( "Failed to load aspect model", aspect.getCause() );
      }
      return aspect.get().get( 0 );
   }

   private Try<List<Aspect>> getAspectFromVersionedModel( VersionedModel versionedModel ) {
      return AspectModelLoader.getAspects( versionedModel );
   }

   public Try<VersionedModel> loadBammModel( String ttl ) {
      InputStream targetStream = new ByteArrayInputStream( ttl.getBytes() );

      Try<Model> model = TurtleLoader.loadTurtle( targetStream );

      StaticResolutionStrategy resolutionStrategy = new StaticResolutionStrategy( model );

      AspectModelResolver resolver = new AspectModelResolver();

      Try<VersionedModel> versionedModel = resolver.resolveAspectModel( resolutionStrategy, resolutionStrategy.getAspectModelUrn() );

      if ( resolutionStrategy.getResolvementCounter() > 1 ) {
         return Try.failure( new ResolutionException( "The definition must be self contained!" ) );
      }

      return versionedModel;
   }
}
