/********************************************************************************
 * Copyright (c) 2025 Robert Bosch Manufacturing Solutions GmbH
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.semantics.hub.samm;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.CharStreams;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.eclipse.esmf.aspectmodel.aas.*;
import org.eclipse.esmf.aspectmodel.generator.diagram.AspectModelDiagramGenerator;
import org.eclipse.esmf.aspectmodel.generator.diagram.DiagramArtifact;
import org.eclipse.esmf.aspectmodel.generator.diagram.DiagramGenerationConfig;
import org.eclipse.esmf.aspectmodel.generator.docu.AspectModelDocumentationGenerator;
import org.eclipse.esmf.aspectmodel.generator.docu.DocumentationGenerationConfig;
import org.eclipse.esmf.aspectmodel.generator.docu.DocumentationGenerationConfigBuilder;
import org.eclipse.esmf.aspectmodel.generator.json.AspectModelJsonPayloadGenerator;
import org.eclipse.esmf.aspectmodel.generator.jsonschema.AspectModelJsonSchemaGenerator;
import org.eclipse.esmf.aspectmodel.generator.jsonschema.JsonSchemaGenerationConfig;
import org.eclipse.esmf.aspectmodel.generator.jsonschema.JsonSchemaGenerationConfigBuilder;
import org.eclipse.esmf.aspectmodel.generator.openapi.AspectModelOpenApiGenerator;
import org.eclipse.esmf.aspectmodel.generator.openapi.OpenApiSchemaGenerationConfig;
import org.eclipse.esmf.aspectmodel.generator.openapi.OpenApiSchemaGenerationConfigBuilder;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.metamodel.AspectModel;
import org.eclipse.tractusx.semantics.hub.persistence.PersistenceLayer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Stream;

@Setter
@RequiredArgsConstructor
public class SDKAccessHelperSAMM {

   private PersistenceLayer persistenceLayer;
   private AspectModel aspectModel;

   public Try<byte[]> generateDiagram( String urn, DiagramGenerationConfig.Format format ) {
      return Try.of( () -> {
         String modelData = persistenceLayer.getModelDefinition( AspectModelUrn.fromUrn( urn ) );
         aspectModel = loadAspectModel( modelData ).get();
         DiagramGenerationConfig config = new DiagramGenerationConfig( Locale.ENGLISH, format );
         final AspectModelDiagramGenerator generator = new AspectModelDiagramGenerator( aspectModel.aspect(), config );

         // Generate the diagram and retrieve the content
         return generator.generate()
               .findFirst()
               .map( DiagramArtifact::getContent )
               .orElseThrow( () -> new IllegalStateException( "No artifact was generated." ) );
      } );
   }

   public JsonNode getJsonSchema( String urn ) {
      String modelData = persistenceLayer.getModelDefinition( AspectModelUrn.fromUrn( urn ) );
      aspectModel = loadAspectModel( modelData ).get();
      final JsonSchemaGenerationConfig config = JsonSchemaGenerationConfigBuilder.builder()
            .locale( Locale.ENGLISH )
            .build();
      final AspectModelJsonSchemaGenerator generator = new AspectModelJsonSchemaGenerator( aspectModel.aspect(), config );
      return generator.getContent();
   }

   public Try<byte[]> getHtmlDocument( String urn ) {
      return Try.of( () -> {
         String modelData = persistenceLayer.getModelDefinition( AspectModelUrn.fromUrn( urn ) );
         aspectModel = loadAspectModel( modelData ).get();
         InputStream ompCSS = getClass().getResourceAsStream( "/catena-template.css" );
         if (ompCSS == null) {
            throw new IOException( "CSS resource not found" );
         }
         String defaultCSS = CharStreams.toString( new InputStreamReader( ompCSS ) );


         final DocumentationGenerationConfig config = DocumentationGenerationConfigBuilder.builder()
               .stylesheet( defaultCSS )
               .build();
         final AspectModelDocumentationGenerator generator =
               new AspectModelDocumentationGenerator( aspectModel.aspect(), config );

         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         generator.generate( aspectFileName -> outputStream );

         return outputStream.toByteArray();
      } );
   }

   public String getOpenApiDefinitionJson( String urn, String baseUrl ) {
      String modelData = persistenceLayer.getModelDefinition( AspectModelUrn.fromUrn( urn ) );
      aspectModel = loadAspectModel( modelData ).get();

      final OpenApiSchemaGenerationConfig config = OpenApiSchemaGenerationConfigBuilder.builder().baseUrl( baseUrl ).build();
      final AspectModelOpenApiGenerator generator = new AspectModelOpenApiGenerator( aspectModel.aspect(), config );

      return generator.generateJson();
   }

   public Try<String> getExamplePayloadJson( String urn ) {
      return Try.of( () -> {
         String modelData = persistenceLayer.getModelDefinition( AspectModelUrn.fromUrn( urn ) );
         aspectModel = loadAspectModel( modelData ).get();
         final AspectModelJsonPayloadGenerator generator = new AspectModelJsonPayloadGenerator( aspectModel.aspect() );
         return generator.generateJson();
      } );
   }

   public Try<byte[]> getAasSubmodelTemplate( String urn, AasFileFormat aasFormat ) {
      return Try.of( () -> {
         String modelData = persistenceLayer.getModelDefinition( AspectModelUrn.fromUrn( urn ) );
         aspectModel = loadAspectModel( modelData ).get();
         AasGenerationConfig config = AasGenerationConfigBuilder.builder()
               .format( aasFormat )
               .build();

         Stream<AasArtifact> artifacts = new AspectModelAasGenerator( aspectModel.aspect(), config ).generate();

         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

         artifacts.forEach( artifact -> {
            try {
               outputStream.write( artifact.getContent() );
            } catch (IOException e) {
               throw new RuntimeException( "Error writing artifact content to stream", e );
            }
         } );
         return outputStream.toByteArray();
      } );
   }

   public Try<AspectModel> loadAspectModel( String modelUrn ) {
      return Try.of( () -> {
         InputStream inputStream = new ByteArrayInputStream( modelUrn.getBytes( StandardCharsets.UTF_8 ) );
         return new AspectModelLoader().load( inputStream );
      } );
   }
}
