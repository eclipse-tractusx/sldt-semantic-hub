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

   private final SAMMSdk sammSdk;
   private final BAMMSdk bammSdk;

   public SdsSdk() {
      bammSdk = new BAMMSdk();
      sammSdk = new SAMMSdk();
   }

   private boolean isBAMM (Model model) {
      return bammSdk.isApplicable( model );
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
    * Resolves the given model with the provided resolution strategy and validates the conformidity using the SAMM
    * tooling.
    *
    * @param model - the model to validate
    */
   public void validate( final Model model, final Function<String, Model> tripleStoreRequester ) {
      if(isBAMM( model )) {
         bammSdk.validate( model, tripleStoreRequester );
      } else {
         sammSdk.validate( model, tripleStoreRequester );
      }

   }

   /**
    * @throws InvalidAspectModelException
    */
   public AspectModelUrn getAspectUrn( final Model model ) {
      if(isBAMM( model )) {
         io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn aspectModelUrn = bammSdk.getAspectUrn( model );
         org.eclipse.esmf.aspectmodel.urn.AspectModelUrn sammAspectModelUrn =
               org.eclipse.esmf.aspectmodel.urn.AspectModelUrn.fromUrn( aspectModelUrn.getUrn() );
         return sammAspectModelUrn;
      } else {
         return sammSdk.getAspectUrn( model );
      }
   }

   public VersionNumber getKnownVersion( final Model rawModel ) {
      if(isBAMM( rawModel )) {
         io.openmanufacturing.sds.aspectmodel.VersionNumber versionNumber = bammSdk.getKnownVersion( rawModel );
         org.eclipse.esmf.aspectmodel.VersionNumber sammVersionNumber =
               new VersionNumber( versionNumber.getMajor(), versionNumber.getMinor(), versionNumber.getMicro() );
         return sammVersionNumber;
      } else {
         return sammSdk.getKnownVersion( rawModel );
      }
   }
}
