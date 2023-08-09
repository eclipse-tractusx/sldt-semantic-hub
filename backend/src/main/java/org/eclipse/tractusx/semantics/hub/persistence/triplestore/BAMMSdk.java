///********************************************************************************
// * Copyright (c) 2021-2023 Robert Bosch Manufacturing Solutions GmbH
// * Copyright (c) 2021-2023 Contributors to the Eclipse Foundation
// *
// * See the NOTICE file(s) distributed with this work for additional
// * information regarding copyright ownership.
// *
// * This program and the accompanying materials are made available under the
// * terms of the Apache License, Version 2.0 which is available at
// * https://www.apache.org/licenses/LICENSE-2.0.
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// * License for the specific language governing permissions and limitations
// * under the License.
// *
// * SPDX-License-Identifier: Apache-2.0
// ********************************************************************************/
//package org.eclipse.tractusx.semantics.hub.persistence.triplestore;
//
//import io.openmanufacturing.sds.aspectmodel.MissingMetaModelVersionException;
//import io.openmanufacturing.sds.aspectmodel.MultipleMetaModelVersionsException;
//import io.openmanufacturing.sds.aspectmodel.UnsupportedVersionException;
//import io.openmanufacturing.sds.aspectmodel.VersionNumber;
//import io.openmanufacturing.sds.aspectmodel.resolver.AspectMetaModelResourceResolver;
//import io.openmanufacturing.sds.aspectmodel.resolver.AspectModelResolver;
//import io.openmanufacturing.sds.aspectmodel.resolver.EitherStrategy;
//import io.openmanufacturing.sds.aspectmodel.resolver.ResolutionStrategy;
//import io.openmanufacturing.sds.aspectmodel.resolver.services.SdsAspectMetaModelResourceResolver;
//import io.openmanufacturing.sds.aspectmodel.resolver.services.VersionedModel;
//import io.openmanufacturing.sds.aspectmodel.shacl.violation.Violation;
//import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;
//import io.openmanufacturing.sds.aspectmodel.validation.services.AspectModelValidator;
//import io.vavr.control.Try;
//import org.apache.jena.rdf.model.*;
//import org.apache.jena.vocabulary.RDF;
//import org.eclipse.tractusx.semantics.hub.InvalidAspectModelException;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.Spliterators;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//import java.util.stream.StreamSupport;
//
//import static java.util.Spliterator.ORDERED;
//
///**
// * @deprecated (This class will be deprecated when we switch all to BAMM model to SAMM models.)
// */
//@Deprecated
//public class BAMMSdk {
//
//   private static final String MESSAGE_MISSING_METAMODEL_VERSION = "Unable to parse metamodel version";
//   private static final String MESSAGE_MULTIPLE_METAMODEL_VERSIONS = "Multiple metamodel versions detected, unable to parse";
//   private static final String MESSAGE_BAMM_VERSION_NOT_SUPPORTED = "The used meta model version is not supported";
//
//   private final AspectMetaModelResourceResolver aspectMetaModelResourceResolver;
//   private final AspectModelResolver aspectModelResolver;
//   private final AspectModelValidator aspectModelValidator;
//
//   public BAMMSdk() {
//      aspectMetaModelResourceResolver = new SdsAspectMetaModelResourceResolver();
//      aspectModelResolver = new AspectModelResolver();
//      aspectModelValidator = new AspectModelValidator();
//   }
//
//   public boolean isApplicable(final Model model){
//      Try<?> version = aspectMetaModelResourceResolver.getBammVersion( model );
//      return !version.isFailure();
//   }
//
//   public void validate( final Model model, final Function<String, Model> tripleStoreRequester ) {
//      final io.openmanufacturing.sds.aspectmodel.resolver.ResolutionStrategy resolutionStrategy = new BAMMSdk.TripleStoreResolutionStrategy( tripleStoreRequester );
//
//      final VersionNumber knownVersion = getKnownVersion( model );
//      final Try<VersionedModel> versionedModel = aspectMetaModelResourceResolver
//            .mergeMetaModelIntoRawModel( model, knownVersion );
//      final ResolutionStrategy firstPayloadThenTripleStore = new EitherStrategy(
//            new SelfResolutionStrategy( versionedModel.get().getRawModel() ),
//            resolutionStrategy );
//
//      final AspectModelUrn modelUrn = getAspectUrn( model );
//      final Try<VersionedModel> resolvedModel = versionedModel.flatMap( loadedModel ->
//            aspectModelResolver.resolveAspectModel( firstPayloadThenTripleStore, modelUrn ) );
//
//      if ( resolvedModel.isFailure() ) {
//         throw new InvalidAspectModelException( resolvedModel.getCause().getMessage() );
//      }
//      final List<Violation> violations = aspectModelValidator.validateModel( resolvedModel );
//      if ( !violations.isEmpty() ) {
//         final Map<String, String> detailsMap=violations.stream().collect( Collectors.toMap( Violation::errorCode,Violation::message ) );
//         throw new InvalidAspectModelException( detailsMap );
//      }
//   }
//
//   public AspectModelUrn getAspectUrn( final Model model ) {
//      final StmtIterator stmtIterator = model.listStatements( null, RDF.type, (RDFNode) null );
//      return StreamSupport.stream( Spliterators.spliteratorUnknownSize( stmtIterator, ORDERED ), false )
//            .filter( statement -> statement.getObject().isURIResource() )
//            .filter( statement -> statement.getObject().asResource().toString().matches( SparqlQueries.BAMM_ASPECT_URN_REGEX ) )
//            .map( Statement::getSubject )
//            .map( Resource::toString )
//            .map( AspectModelUrn::fromUrn )
//            .findAny()
//            .orElseThrow( () -> new InvalidAspectModelException( "Unable to parse Aspect Model URN" ) );
//   }
//
//   public VersionNumber getKnownVersion( final Model rawModel ) {
//      return aspectMetaModelResourceResolver
//            .getBammVersion( rawModel )
//            .onFailure( MissingMetaModelVersionException.class,
//                  e -> {
//                     throw new InvalidAspectModelException( MESSAGE_MISSING_METAMODEL_VERSION );
//                  } )
//            .onFailure( MultipleMetaModelVersionsException.class,
//                  e -> {
//                     throw new InvalidAspectModelException( MESSAGE_MULTIPLE_METAMODEL_VERSIONS );
//                  } )
//            .onFailure( UnsupportedVersionException.class,
//                  e -> {
//                     throw new InvalidAspectModelException( MESSAGE_BAMM_VERSION_NOT_SUPPORTED );
//                  } ).get();
//   }
//
//   public static class TripleStoreResolutionStrategy implements ResolutionStrategy {
//      private final Function<String, Model> tripleStoreRequester;
//      private final List<String> alreadyLoadedNamespaces = new ArrayList<>();
//
//      public TripleStoreResolutionStrategy( final Function<String, Model> tripleStoreRequester ) {
//         this.tripleStoreRequester = tripleStoreRequester;
//      }
//
//      @Override
//      public Try<Model> apply( final AspectModelUrn aspectModelUrn ) {
//         final String namespace = aspectModelUrn.getUrn().toString();
//         if ( alreadyLoadedNamespaces.contains( namespace ) ) {
//            return Try.success( ModelFactory.createDefaultModel() );
//         }
//         alreadyLoadedNamespaces.add( namespace );
//
//         final Resource resource = ResourceFactory.createResource( aspectModelUrn.getUrn().toASCIIString() );
//         final Model model = tripleStoreRequester.apply( aspectModelUrn.getUrn().toString() );
//         if ( model == null ) {
//            return Try.failure( new ResourceDefinitionNotFoundException( getClass().getSimpleName(), resource ) );
//         }
//         return model.contains( resource, RDF.type, (RDFNode) null ) ?
//               Try.success( model ) :
//               Try.failure( new ResourceDefinitionNotFoundException( getClass().getSimpleName(), resource ) );
//      }
//   }
//
//   private static class SelfResolutionStrategy implements ResolutionStrategy {
//      private final Model model;
//      public SelfResolutionStrategy( final Model model ) {
//         this.model = model;
//      }
//
//      @Override
//      public Try<Model> apply( final AspectModelUrn aspectModelUrn ) {
//         final Resource resource = ResourceFactory.createResource( aspectModelUrn.getUrn().toString() );
//         return model.contains( resource, RDF.type, (RDFNode) null ) ?
//               Try.success( model ) :
//               Try.failure( new ResourceDefinitionNotFoundException( getClass().getSimpleName(), resource ) );
//      }
//   }
//}
