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
package org.eclipse.tractusx.semantics.hub.persistence.triplestore;

import static java.util.Spliterator.ORDERED;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.tractusx.semantics.hub.InvalidAspectModelException;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import org.eclipse.esmf.aspectmodel.MissingMetaModelVersionException;
import org.eclipse.esmf.aspectmodel.MultipleMetaModelVersionsException;
import org.eclipse.esmf.aspectmodel.UnsupportedVersionException;
import org.eclipse.esmf.aspectmodel.VersionNumber;
import org.eclipse.esmf.aspectmodel.resolver.AspectMetaModelResourceResolver;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelResolver;
import org.eclipse.esmf.aspectmodel.resolver.EitherStrategy;
import org.eclipse.esmf.aspectmodel.resolver.ResolutionStrategy;
import org.eclipse.esmf.aspectmodel.resolver.services.SammAspectMetaModelResourceResolver;
import org.eclipse.esmf.aspectmodel.resolver.services.TurtleLoader;
import org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel;
import org.eclipse.esmf.aspectmodel.shacl.violation.Violation;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.validation.services.AspectModelValidator;
import io.vavr.control.Try;

public class SdsSdk {

   private static final String MESSAGE_MISSING_METAMODEL_VERSION = "Unable to parse metamodel version";
   private static final String MESSAGE_MULTIPLE_METAMODEL_VERSIONS = "Multiple metamodel versions detected, unable to parse";
   private static final String MESSAGE_BAMM_VERSION_NOT_SUPPORTED = "The used meta model version is not supported";

   private final AspectMetaModelResourceResolver aspectMetaModelResourceResolver;
   private final AspectModelResolver aspectModelResolver;
   private final AspectModelValidator aspectModelValidator;

   public SdsSdk() {
      aspectMetaModelResourceResolver = new SammAspectMetaModelResourceResolver();
      aspectModelResolver = new AspectModelResolver();
      aspectModelValidator = new AspectModelValidator();
   }

   public Model load( final String resourceName ) throws IOException {
      return load( IOUtils.resourceToByteArray( resourceName, getClass().getClassLoader() ) );
   }

   public Model load( final byte[] bytes ) {
      return TurtleLoader
            .loadTurtle( new ByteArrayInputStream( bytes ) )
            .getOrElseThrow( cause -> new InvalidAspectModelException( cause.getMessage() ) );
   }

   /**
    * Resolves the given model with the provided resolution strategy and validates the conformidity using the BAMM
    * tooling.
    *
    * @param model - the model to validate
    * @param resolutionStrategy - the resolution strategry to use for validation. @see
    *       TripleStoreResolutionStrategy
    */
   public void validate( final Model model, final ResolutionStrategy resolutionStrategy ) {
      final VersionNumber knownVersion = getKnownVersion( model );
      final Try<VersionedModel> versionedModel = aspectMetaModelResourceResolver
            .mergeMetaModelIntoRawModel( model, knownVersion );
      final ResolutionStrategy firstPayloadThenTripleStore = new EitherStrategy(
            new SelfResolutionStrategy( versionedModel.get().getRawModel() ),
            resolutionStrategy
      );

      final AspectModelUrn modelUrn = getAspectUrn( model );
      final Try<VersionedModel> resolvedModel = versionedModel.flatMap( loadedModel ->
            aspectModelResolver.resolveAspectModel( firstPayloadThenTripleStore, modelUrn ) );

      if ( resolvedModel.isFailure() ) {
         throw new InvalidAspectModelException( resolvedModel.getCause().getMessage() );
      }
      final List<Violation> violations = aspectModelValidator.validateModel( resolvedModel );
      if ( !violations.isEmpty() ) {
         final Map<String, String> detailsMap=violations.stream().collect( Collectors.toMap( Violation::errorCode,Violation::message ) );
         throw new InvalidAspectModelException( detailsMap );
      }
   }

   /**
    * @throws InvalidAspectModelException
    */
   public AspectModelUrn getAspectUrn( final Model model ) {
      final StmtIterator stmtIterator = model.listStatements( null, RDF.type, (RDFNode) null );
      return StreamSupport.stream( Spliterators.spliteratorUnknownSize( stmtIterator, ORDERED ), false )
            .filter( statement -> statement.getObject().isURIResource() )
            .filter( statement -> statement.getObject().asResource().toString()
                  .matches( SparqlQueries.BAMM_ASPECT_URN_REGEX ) )
            .map( Statement::getSubject )
            .map( Resource::toString )
            .map( AspectModelUrn::fromUrn )
            .findAny()
            .orElseThrow( () -> new InvalidAspectModelException( "Unable to parse Aspect Model URN" ) );
   }

   public VersionNumber getKnownVersion( final Model rawModel ) {
      return aspectMetaModelResourceResolver
            .getBammVersion( rawModel )
            .onFailure( MissingMetaModelVersionException.class,
                  e -> {
                     throw new InvalidAspectModelException( MESSAGE_MISSING_METAMODEL_VERSION );
                  } )
            .onFailure( MultipleMetaModelVersionsException.class,
                  e -> {
                     throw new InvalidAspectModelException( MESSAGE_MULTIPLE_METAMODEL_VERSIONS );
                  } )
            .onFailure( UnsupportedVersionException.class,
                  e -> {
                     throw new InvalidAspectModelException( MESSAGE_BAMM_VERSION_NOT_SUPPORTED );
                  } ).get();
   }

   public static class TripleStoreResolutionStrategy implements ResolutionStrategy {

      private final Function<AspectModelUrn, Model> tripleStoreRequester;
      private final List<String> alreadyLoadedNamespaces = new ArrayList<>();

      public TripleStoreResolutionStrategy( final Function<AspectModelUrn, Model> tripleStoreRequester ) {
         this.tripleStoreRequester = tripleStoreRequester;
      }

      @Override
      public Try<Model> apply( final AspectModelUrn aspectModelUrn ) {
         final String namespace = aspectModelUrn.getNamespace();
         if ( alreadyLoadedNamespaces.contains( namespace ) ) {
            return Try.success( ModelFactory.createDefaultModel() );
         }
         alreadyLoadedNamespaces.add( namespace );

         final Resource resource = ResourceFactory.createResource( aspectModelUrn.getUrn().toASCIIString() );
         final Model model = tripleStoreRequester.apply( aspectModelUrn );
         if ( model == null ) {
            return Try.failure(
                  new ResourceDefinitionNotFoundException( getClass().getSimpleName(), resource ) );
         }
         return model.contains( resource, RDF.type, (RDFNode) null ) ?
               Try.success( model ) :
               Try.failure(
                     new ResourceDefinitionNotFoundException( getClass().getSimpleName(), resource ) );
      }
   }

   private static class SelfResolutionStrategy implements ResolutionStrategy {

      private final Model model;

      public SelfResolutionStrategy( final Model model ) {
         this.model = model;
      }

      @Override
      public Try<Model> apply( final AspectModelUrn aspectModelUrn ) {
         final Resource resource = ResourceFactory.createResource( aspectModelUrn.getUrn().toString() );
         return model.contains( resource, RDF.type, (RDFNode) null ) ?
               Try.success( model ) :
               Try.failure( new ResourceDefinitionNotFoundException( getClass().getSimpleName(), resource ) );
      }
   }
}
