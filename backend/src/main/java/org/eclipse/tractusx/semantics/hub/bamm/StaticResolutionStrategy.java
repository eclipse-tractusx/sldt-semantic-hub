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
package org.eclipse.tractusx.semantics.hub.bamm;

import org.eclipse.esmf.samm.KnownVersion;
import org.eclipse.esmf.aspectmodel.resolver.AbstractResolutionStrategy;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.vocabulary.SAMM;
import io.vavr.NotImplementedError;
import io.vavr.control.Try;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import java.util.List;
import java.util.Optional;

/**
 * @deprecated (This class will be deprecated when we switch all to BAMM model to SAMM models.)
 */
@Deprecated
@SuppressWarnings("java:S106")
public class StaticResolutionStrategy extends AbstractResolutionStrategy {
   private int counter;
   private final Try<Model> model;

   public StaticResolutionStrategy( Try<Model> model ) {
      this.model = model;
   }

   @Override
   public Try<Model> apply( AspectModelUrn t ) {
      counter++;
      return this.model;
   }

   public int getResolvementCounter() {
      return counter;
   }

   public AspectModelUrn getAspectModelUrn() {
      final Optional<StmtIterator> stmtIterator = getStmtIterator();

      final String aspectModelUrn = stmtIterator.orElseThrow(
                  () -> new NotImplementedError( "AspectModelUrn cannot be found." ) )
            .next().getSubject().getURI();

      return AspectModelUrn.fromUrn( aspectModelUrn );
   }

   private Optional<StmtIterator> getStmtIterator() {
      for ( final KnownVersion version : KnownVersion.getVersions() ) {
         final SAMM bamm = new SAMM( version );
         final List<Resource> resources = List.of( bamm.Aspect(), bamm.Property(), bamm.Entity(), bamm.Characteristic() );
         final Optional<StmtIterator> stmtIterator = resources.stream().filter(
                     resource -> model.get().listStatements( null, RDF.type, resource ).hasNext() ).findFirst()
               .map( resource -> model.get().listStatements( null, RDF.type, resource ) );
         if ( stmtIterator.isPresent() ) {
            return stmtIterator;
         }
      }
      return Optional.empty();
   }
}
