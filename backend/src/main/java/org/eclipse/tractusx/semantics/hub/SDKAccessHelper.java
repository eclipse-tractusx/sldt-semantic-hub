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
package org.eclipse.tractusx.semantics.hub;

import java.util.List;

import org.eclipse.esmf.aspectmodel.generator.diagram.AspectModelDiagramGenerator;
import org.eclipse.esmf.aspectmodel.resolver.services.VersionedModel;
import org.eclipse.esmf.aspectmodel.shacl.violation.Violation;
import org.eclipse.tractusx.semantics.hub.model.AasFormat;
import org.eclipse.tractusx.semantics.hub.persistence.PersistenceLayer;
import org.eclipse.tractusx.semantics.hub.samm.SDKAccessHelperSAMM;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import io.vavr.control.Try;

@Component
public class SDKAccessHelper {

   SDKAccessHelperSAMM sdkAccessHelperSAMM = new SDKAccessHelperSAMM();

   public void setPersistenceLayer( PersistenceLayer persistenceLayer ) {
      sdkAccessHelperSAMM.setPersistenceLayer( persistenceLayer );
   }

   public List<Violation> validateModel( Try<VersionedModel> model ) {
      // SAMM validator should also process BAMM models so no switch here
      return sdkAccessHelperSAMM.validateModel( model );
   }

   public Try<byte[]> generateSvg( String urn ) {
      return sdkAccessHelperSAMM.generateDiagram( urn, AspectModelDiagramGenerator.Format.SVG );
   }

   public JsonNode getJsonSchema( String urn ) {
      return sdkAccessHelperSAMM.getJsonSchema( urn );
   }

   public Try<byte[]> getHtmlDocu( String urn ) {
      return sdkAccessHelperSAMM.getHtmlDocu( urn );
   }

   public String getOpenApiDefinitionJson( String urn, String baseUrl ) {
      return sdkAccessHelperSAMM.getOpenApiDefinitionJson( urn, baseUrl );
   }

   public Try<String> getExamplePayloadJson( String urn  ) {
      return sdkAccessHelperSAMM.getExamplePayloadJson( urn );
   }

   public Try getAasSubmodelTemplate( String urn, AasFormat aasFormat ) {
      return sdkAccessHelperSAMM.getAasSubmodelTemplate( urn, aasFormat );
   }

   public Try<VersionedModel> loadBammModel( String modelString ) {
      return sdkAccessHelperSAMM.loadSammModel( modelString );
   }
}
