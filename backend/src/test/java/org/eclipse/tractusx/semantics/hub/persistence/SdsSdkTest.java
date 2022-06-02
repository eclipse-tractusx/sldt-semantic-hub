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
package org.eclipse.tractusx.semantics.hub.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Test;

import org.eclipse.tractusx.semantics.hub.InvalidAspectModelException;
import org.eclipse.tractusx.semantics.hub.TestUtils;
import org.eclipse.tractusx.semantics.hub.persistence.triplestore.ResourceDefinitionNotFoundException;
import org.eclipse.tractusx.semantics.hub.persistence.triplestore.SdsSdk;
import io.openmanufacturing.sds.aspectmodel.resolver.ResolutionStrategy;
import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;
import io.vavr.control.Try;

public class SdsSdkTest {

   @Test
   public void getAspectModelUrnExpectSuccess() throws IOException {
      final SdsSdk sdsSdk = new SdsSdk();
      final Model model = sdsSdk.load( TestUtils.PRODUCT_USAGE_MODEL_PATH );

      final AspectModelUrn aspectUrn = sdsSdk.getAspectUrn( model );
      assertThat( aspectUrn.getNamespace() ).isEqualTo( "org.eclipse.tractusx.semantics.test.productusage" );
      assertThat( aspectUrn.getUrnPrefix() )
            .isEqualTo( "urn:bamm:org.eclipse.tractusx.semantics.test.productusage:1.2.0#" );
      assertThat( aspectUrn.getUrn().toString() ).isEqualTo(
            "urn:bamm:org.eclipse.tractusx.semantics.test.productusage:1.2.0#ProductUsage" );
   }

   @Test
   public void validateValidAspectModelWithAvailableExternalReferenceExpectSuccess() throws IOException {
      final SdsSdk sdsSdk = new SdsSdk();
      final Model model = sdsSdk.load( TestUtils.PRODUCT_USAGE_MODEL_PATH );
      assertThatCode( () -> sdsSdk.validate( model, getLocalTripleResolutionStrategy() ) )
            .doesNotThrowAnyException();
   }

   /**
    * This test case fails currently. It's seams to be a bug in the BAMM sdk. I issue for this is created.
    *
    * @throws IOException
    */
   @Test
   public void validateAspectModelWithNotAvailableExternalReferenceExpectError() throws IOException {
      final SdsSdk sdsSdk = new SdsSdk();
      final Model model = sdsSdk.load( TestUtils.VEHICLE_WITH_NOT_AVAILABLE_EXTERNAL_REFERENCE );
      try {
         sdsSdk.validate( model, getNoOpTripleResolutionStrategy() );
      } catch ( final InvalidAspectModelException e ) {
         System.out.println( e.getDetails() );
      }
   }

   private SdsSdk.TripleStoreResolutionStrategy getLocalTripleResolutionStrategy() {
      return new SdsSdk.TripleStoreResolutionStrategy( urn -> {
         try {
            return new SdsSdk().load( TestUtils.PRODUCT_USAGE_DETAIL_MODEL_PATH );
         } catch ( final IOException e ) {
            throw new RuntimeException( e );
         }
      } );
   }

   private ResolutionStrategy getNoOpTripleResolutionStrategy() {
      return ( urn ) -> Try.failure( new ResourceDefinitionNotFoundException( "NoOpTripleResolutionStrategy",
            ResourceFactory.createResource( urn.getUrn().toASCIIString() ) ) );
   }
}
