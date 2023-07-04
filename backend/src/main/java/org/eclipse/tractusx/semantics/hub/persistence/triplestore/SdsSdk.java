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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.eclipse.esmf.aspectmodel.resolver.services.TurtleLoader;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.tractusx.semantics.hub.InvalidAspectModelException;

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
    * Resolves the given model with the provided resolution strategy and validates the conformidity using the SAMM tooling.
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

   public AspectModelUrn getAspectUrn( final Model model ) {
      if(isBAMM( model )) {
         io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn aspectModelUrn = bammSdk.getAspectUrn( model );
         org.eclipse.esmf.aspectmodel.urn.AspectModelUrn sammAspectModelUrn = org.eclipse.esmf.aspectmodel.urn.AspectModelUrn.fromUrn( aspectModelUrn.getUrn() );
         return sammAspectModelUrn;
      } else {
         return sammSdk.getAspectUrn( model );
      }
   }
}
