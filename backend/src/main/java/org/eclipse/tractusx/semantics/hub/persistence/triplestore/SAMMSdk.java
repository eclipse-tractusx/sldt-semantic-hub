/********************************************************************************
 * Copyright (c) 2021-2025 Robert Bosch Manufacturing Solutions GmbH
 * Copyright (c) 2021-2025 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.semantics.hub.persistence.triplestore;

import static java.util.Spliterator.ORDERED;
import static org.eclipse.esmf.aspectmodel.resolver.AspectModelFileLoader.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.function.Function;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.esmf.aspectmodel.AspectModelFile;
import org.eclipse.esmf.aspectmodel.loader.AspectModelLoader;
import org.eclipse.esmf.aspectmodel.resolver.ResolutionStrategy;
import org.eclipse.esmf.aspectmodel.resolver.ResolutionStrategySupport;
import org.eclipse.esmf.aspectmodel.resolver.exceptions.ModelResolutionException;
import org.eclipse.esmf.aspectmodel.shacl.violation.Violation;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.eclipse.esmf.metamodel.AspectModel;
import org.eclipse.tractusx.semantics.hub.InvalidAspectModelException;
import org.eclipse.tractusx.semantics.hub.model.SemanticModelType;

public class SAMMSdk {

   private final AspectModelValidator aspectModelValidator;

   public SAMMSdk() {
      this.aspectModelValidator = new AspectModelValidator();
   }

   public void validate( final String modelData, final Function<String, Model> tripleStoreRequester, SemanticModelType type ) {
      InputStream inputStream = new ByteArrayInputStream( modelData.getBytes( StandardCharsets.UTF_8 ) );
      AspectModel aspectModel;
      final ResolutionStrategy resolutionStrategy = new TripleStoreResolutionStrategy( tripleStoreRequester, type );
      try {
         aspectModel = new AspectModelLoader( resolutionStrategy ).load( inputStream );
      } catch (Exception e) {
         throw new InvalidAspectModelException( e.getMessage() );
      }
      final List<Violation> violations = aspectModelValidator.validateModel( aspectModel );
      if (!violations.isEmpty()) {
         Map<String, String> detailsMap = violations.stream()
               .collect(
                     Collectors.groupingBy(
                           Violation::errorCode,
                           Collectors.mapping( Violation::message, Collectors.joining( "," ) )
                     )
               );

         throw new InvalidAspectModelException( detailsMap );
      }
   }

   public AspectModelUrn getAspectUrn( final Model model ) {
      final StmtIterator stmtIterator = model.listStatements( null, RDF.type, (RDFNode) null );
      return StreamSupport.stream( Spliterators.spliteratorUnknownSize( stmtIterator, ORDERED ), false )
            .filter( statement -> statement.getObject().isURIResource() )
            .filter( statement -> statement.getObject().asResource().toString().matches( SparqlQueries.SAMM_ASPECT_URN_REGEX ) )
            .map( Statement::getSubject )
            .map( Resource::toString )
            .map( AspectModelUrn::fromUrn )
            .findAny()
            .orElseThrow( () -> new InvalidAspectModelException( "Unable to parse Aspect Model URN" ) );
   }

   public static class TripleStoreResolutionStrategy implements ResolutionStrategy {
      private final Function<String, Model> tripleStoreRequester;
      private final SemanticModelType type;
      private AspectModelFile aspectModelFile;

      public TripleStoreResolutionStrategy( Function<String, Model> tripleStoreRequester, SemanticModelType type ) {
         this.tripleStoreRequester = tripleStoreRequester;
         this.type = type;
      }


      @Override
      public AspectModelFile apply( AspectModelUrn aspectModelUrn, ResolutionStrategySupport resolutionStrategySupport ) throws ModelResolutionException {
         final Resource resource = ResourceFactory.createResource( checkAndReplaceToBammPrefix( aspectModelUrn.getUrn().toASCIIString() ) );
         try {
            final Model model = tripleStoreRequester.apply( checkAndReplaceToBammPrefix( aspectModelUrn.getUrn().toString() ) );
            if (model == null) {
               throw new ResourceDefinitionNotFoundException( getClass().getSimpleName(), resource );
            }
            if (!model.contains( resource, RDF.type, (RDFNode) null )) {
               throw new ResourceDefinitionNotFoundException( getClass().getSimpleName(), resource );
            }
            aspectModelFile = load( model );
            return aspectModelFile;
         } catch (Exception e) {
            throw new ResourceDefinitionNotFoundException( getClass().getSimpleName(), resource );
         }
      }

      @Override
      public Stream<URI> listContents() {
         return aspectModelFile.sourceLocation().stream();
      }


      @Override
      public Stream<URI> listContentsForNamespace( final AspectModelUrn aspectModelUrn ) {
         return aspectModelFile.namespace().urn().equals( aspectModelUrn )
               ? aspectModelFile.sourceLocation().stream()
               : Stream.empty();
      }

      @Override
      public Stream<AspectModelFile> loadContents() {
         return Stream.of( aspectModelFile );
      }

      @Override
      public Stream<AspectModelFile> loadContentsForNamespace( final AspectModelUrn aspectModelUrn ) {
         return aspectModelFile.namespace().urn().equals( aspectModelUrn )
               ? Stream.of( aspectModelFile )
               : Stream.empty();
      }

      private String checkAndReplaceToBammPrefix( String value ) {
         return isBamm() ? value.replace( "samm", "bamm" ) : value;
      }

      private boolean isBamm() {
         return type.equals( SemanticModelType.BAMM );
      }
   }
}
