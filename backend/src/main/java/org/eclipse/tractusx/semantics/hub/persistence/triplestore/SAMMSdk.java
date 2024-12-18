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
package org.eclipse.tractusx.semantics.hub.persistence.triplestore;

import static java.util.Spliterator.ORDERED;

import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelResolver;
import org.eclipse.esmf.aspectmodel.resolver.ResolutionStrategy;
import org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel;
import org.eclipse.esmf.aspectmodel.shacl.violation.Violation;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import org.eclipse.tractusx.semantics.hub.InvalidAspectModelException;
import org.eclipse.tractusx.semantics.hub.model.SemanticModelType;

import io.vavr.control.Try;

public class SAMMSdk {

   private final AspectModelValidator aspectModelValidator;

   public SAMMSdk() {
      aspectModelValidator = new AspectModelValidator();
   }

   public void validate( final Model model, final Function<String, Model> tripleStoreRequester, SemanticModelType type ) {
      final ResolutionStrategy resolutionStrategy =
            new SAMMSdk.TripleStoreResolutionStrategy( tripleStoreRequester, type );

      final Try<VersionedModel> resolvedModel = new AspectModelResolver().resolveAspectModel( resolutionStrategy, model );

      final List<Violation> violations = aspectModelValidator.validateModel( resolvedModel );
      if ( !violations.isEmpty() ) {
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
            .filter( statement -> statement.getObject().asResource().toString().matches( SparqlQueries.SAMM_ASPECT_URN_REGEX ))
            .map( Statement::getSubject )
            .map( Resource::toString )
            .map( AspectModelUrn::fromUrn )
            .findAny()
            .orElseThrow( () -> new InvalidAspectModelException( "Unable to parse Aspect Model URN" ) );
   }

   public static class TripleStoreResolutionStrategy implements ResolutionStrategy {

      private final Function<String, Model> tripleStoreRequester;
      private final SemanticModelType type;

      public TripleStoreResolutionStrategy( final Function<String, Model> tripleStoreRequester, SemanticModelType type ) {
         this.tripleStoreRequester = tripleStoreRequester;
         this.type = type;
      }

      @Override
      public Try<Model> apply( final AspectModelUrn aspectModelUrn ) {
         final Resource resource = ResourceFactory.createResource( checkAndReplaceToBammPrefix(aspectModelUrn.getUrn().toASCIIString()));
         final Model model = tripleStoreRequester.apply( checkAndReplaceToBammPrefix(aspectModelUrn.getUrn().toString()));

         if ( model == null ) {
            return Try.failure( new ResourceDefinitionNotFoundException( getClass().getSimpleName(), resource ) );
         }
         return model.contains( resource, RDF.type, (RDFNode) null ) ?
               Try.success( model ) :
               Try.failure( new ResourceDefinitionNotFoundException( getClass().getSimpleName(), resource ) );
      }

      private String checkAndReplaceToBammPrefix(String value){
         return isBamm() ? value.replace( "samm", "bamm" ) : value ;
      }

      private boolean isBamm(){
         return type.equals( SemanticModelType.BAMM );
      }
   }
}
