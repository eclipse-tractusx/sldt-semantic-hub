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
package org.eclipse.tractusx.semantics.hub.domain;

import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

public class ModelPackageUrn {
   private final String urn;

   public ModelPackageUrn( String urn ) {
      this.urn = urn;
   }

   public String getUrn() {
      return urn;
   }

   public static ModelPackageUrn fromUrn( AspectModelUrn aspectModelUrn ) {
      return new ModelPackageUrn( aspectModelUrn.getUrnPrefix() );
   }

   public static ModelPackageUrn fromUrn( String urn ) {
      return new ModelPackageUrn( urn );
   }
}
