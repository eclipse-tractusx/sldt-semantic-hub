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
package org.eclipse.tractusx.semantics.hub;

import static org.eclipse.tractusx.semantics.hub.TestUtils.AAS_JSON_FILE;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

@TestInstance( TestInstance.Lifecycle.PER_CLASS )
@DirtiesContext( classMode = DirtiesContext.ClassMode.AFTER_CLASS )
public class ModelsApiTest extends AbstractModelsApiTest{

    @BeforeAll
    public void init() {
        deleteAllData();
   }

   @Test
   public void testWithoutAuthenticationTokenProvidedExpectForbidden() throws Exception {
      mvc.perform(
                      MockMvcRequestBuilders
                              .get("/api/v1/models")
                              .accept(MediaType.APPLICATION_JSON)
              )
              .andDo(MockMvcResultHandlers.print())
              .andExpect(status().isUnauthorized());
   }

   @Test
   public void testWithAuthenticationTokenProvidedExpectSuccess() throws Exception {
      mvc.perform(
                      MockMvcRequestBuilders
                              .get("/api/v1/models")
                              .accept(MediaType.APPLICATION_JSON)
                              .with(jwtTokenFactory.allRoles())
              )
              .andDo(MockMvcResultHandlers.print())
              .andExpect(status().isOk());
   }

   @Test
   public void testGetModelsExpectSuccess() throws Exception {
      String urnPrefix = "urn:samm:org.eclipse.tractusx:1.0.0#";
      mvc.perform(
               post( TestUtils.createValidModelRequest(urnPrefix),"DRAFT")
         )
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( status().isOk() );

      mvc.perform(
               MockMvcRequestBuilders.get( "/api/v1/models" )
                                     .accept( MediaType.APPLICATION_JSON )
                                     .with(jwtTokenFactory.allRoles())
         )
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( jsonPath( "$.items" ).isArray() )
         .andExpect( jsonPath( "$.items[*].urn", hasItem( toMovementUrn(urnPrefix) ) ) )
         .andExpect( jsonPath( "$.items[*].version", hasItem( "1.0.0" ) ) )
         .andExpect( jsonPath( "$.items[*].name", hasItem( "Movement" ) ) )
         .andExpect( jsonPath( "$.items[*].type", hasItem( "SAMM" ) ) )
         .andExpect( jsonPath( "$.items[*].status", hasItem( "DRAFT" ) ) )
         .andExpect( jsonPath( "$.totalItems", greaterThan( 0 ) ) )
         .andExpect( jsonPath( "$.itemCount", greaterThan( 0 ) ) )
         .andExpect( status().isOk() );
   }

   @Test
   public void testSaveValidModelExpectSuccess() throws Exception {
      String urnPrefix = "urn:samm:org.eclipse.tractusx.valid.save:2.0.0#";
      mvc.perform(
               post( TestUtils.createValidModelRequest(urnPrefix),"RELEASED")
         )
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( jsonPath( "$.urn", is( toMovementUrn(urnPrefix) ) ) )
         .andExpect( jsonPath( "$.version", is( "2.0.0" ) ) )
         .andExpect( jsonPath( "$.name", is( "Movement" ) ) )
         .andExpect( jsonPath( "$.type", is( "SAMM" ) ) )
         .andExpect( jsonPath( "$.status", is( "RELEASED" ) ) )
         .andExpect( status().isOk() );
   }

   @Test
   public void testSaveInvalidModelExpectSuccess() throws Exception {
      String insertModelJson = "@prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:1.0.0#> .\n @prefix bamm-c: <urn:samm:org.eclipse.esmf.samm:characteristicX:1.0.0#> .\n @prefix bamm-e: <urn:samm:org.eclipse.esmf.samm:entity:1.0.0#> .\n @prefix unit: <urn:samm:org.eclipse.esmf.samm:unit:1.0.0#> .\n @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n @prefix : <urn:samm:org.eclipse.tractusx:1.0.0#> .\n \n :Movement a samm:Aspect;\n samm:name \"Movement\";\n samm:preferredName \"Movement\"@en;\n samm:description \"Aspect for movement information\"@en;\n samm:propertiesX (:isMoving :speedLimitWarning :position);\n samm:operations ().\n :isMoving a samm:Property;\n samm:name \"isMoving\";\n samm:preferredName \"Moving\"@en;\n samm:description \"Flag indicating whether the asset is currently moving\"@en;\n samm:characteristic bamm-c:Boolean.\n :speedLimitWarning a samm:Property;\n samm:name \"speedLimitWarning\";\n samm:preferredName \"Speed Limit Warning\"@en;\n samm:description \"Indicates if the speed limit is adhered to.\"@en;\n samm:characteristic :TrafficLight.\n :position a samm:Property;\n samm:name \"position\";\n samm:preferredName \"Position\"@en;\n samm:description \"Indicates a position\"@en;\n samm:characteristic :SpatialPositionCharacteristic.\n :TrafficLight a bamm-c:Enumeration;\n samm:name \"TrafficLight\";\n samm:preferredName \"Warning Level\"@en;\n samm:description \"Represents if speed of position change is within specification (green), within tolerance (yellow), or outside specification (red).\"@en;\n samm:dataType xsd:string;\n bamm-c:values (\"green\" \"yellow\" \"red\").\n :SpatialPosition a samm:Entity;\n samm:name \"SpatialPosition\";\n samm:preferredName \"Spatial Position\"@en;\n samm:description \"Position in space, described along three axis, with the third axis optional, if all positions are in a plane.\"@en;\n samm:properties (:x :y :z).\n :x a samm:Property;\n samm:name \"x\";\n samm:preferredName \"x\"@en;\n samm:description \"x coordinate in space\"@en;\n samm:characteristic :Coordinate.\n :y a samm:Property;\n samm:name \"y\";\n samm:preferredName \"y\"@en;\n samm:description \"y coordinate in space\"@en;\n samm:characteristic :Coordinate.\n :z a samm:Property;\n samm:name \"z\";\n samm:preferredName \"z\"@en;\n samm:description \"z coordinate in space\"@en;\n samm:characteristic :Coordinate;\n samm:optional \"true\"^^xsd:boolean.\n :Coordinate a bamm-c:Measurement;\n samm:name \"Coordinate\";\n samm:preferredName \"Coordinate\"@en;\n samm:description \"Represents a coordinate along an axis in space.\"@en;\n samm:dataType xsd:float;\n bamm-c:unit unit:metre.\n :SpatialPositionCharacteristic a bamm-c:SingleEntity;\n samm:name \"SpatialPositionCharacteristic\";\n samm:preferredName \"Spatial Position Characteristic\"@en;\n samm:description \"Represents a single position in space with optional z coordinate.\"@en;\n samm:dataType :SpatialPosition.";

      mvc.perform(post( insertModelJson ))
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( jsonPath( "$.error.details.ERR_PROCESSING", containsString(
               "Validation succeeded, but an error was found while processing the model. This indicates an error in the model validation; please consider reporting this issue including the model at https://github.com/eclipse-esmf/esmf-semantic-aspect-meta-model/issues -- Could not load Aspect model, please make sure the model is valid: Could not load urn:samm:org.eclipse.tractusx:1.0.0#SpatialPositionCharacteristic: Unknown type urn:samm:org.eclipse.esmf.samm:characteristicX:1.0.0#SingleEntity" ) ) )
         .andExpect( status().is4xxClientError() );
   }

   @Test
   public void testGenerateJsonSchemaExpectSuccess() throws Exception {
      String urnPrefix = "urn:samm:org.eclipse.tractusx.model.status.transition:2.0.0#";
      mvc.perform(
               post( TestUtils.createValidModelRequest(urnPrefix),"RELEASED")
         )
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( status().isOk() );

      mvc.perform(
               MockMvcRequestBuilders.get(
                     "/api/v1/models/{urn}/json-schema", toMovementUrn(urnPrefix))
                                    .with(jwtTokenFactory.allRoles())
         )
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( content().json(
               "{\"$schema\":\"http://json-schema.org/draft-04/schema\",\"description\":\"Aspect for movement information\",\"type\":\"object\",\"components\":{\"schemas\":{\"urn_samm_org.eclipse.esmf.samm_characteristic_2.0.0_Boolean\":{\"type\":\"boolean\",\"description\":\"Represents a boolean value (i.e. a \\\"flag\\\").\"},\"urn_samm_org.eclipse.tractusx.model.status.transition_2.0.0_WarningLevel\":{\"type\":\"string\",\"description\":\"Represents if speed of position change is within specification (green), within tolerance (yellow), or outside specification (red).\",\"enum\":[\"green\",\"yellow\",\"red\"]},\"urn_samm_org.eclipse.tractusx.model.status.transition_2.0.0_Coordinate\":{\"type\":\"number\",\"description\":\"Represents a coordinate along an axis in space.\"},\"urn_samm_org.eclipse.tractusx.model.status.transition_2.0.0_SpatialPosition\":{\"description\":\"Represents a single location in space.\",\"type\":\"object\",\"properties\":{\"x\":{\"description\":\"x coordinate in space\",\"$ref\":\"#/components/schemas/urn_samm_org.eclipse.tractusx.model.status.transition_2.0.0_Coordinate\"},\"y\":{\"description\":\"y coordinate in space\",\"$ref\":\"#/components/schemas/urn_samm_org.eclipse.tractusx.model.status.transition_2.0.0_Coordinate\"},\"z\":{\"description\":\"z coordinate in space\",\"$ref\":\"#/components/schemas/urn_samm_org.eclipse.tractusx.model.status.transition_2.0.0_Coordinate\"}},\"required\":[\"x\",\"y\"]}}},\"properties\":{\"moving\":{\"description\":\"Flag indicating if the position is changing\",\"$ref\":\"#/components/schemas/urn_samm_org.eclipse.esmf.samm_characteristic_2.0.0_Boolean\"},\"speedLimitWarning\":{\"description\":\"Indicats if speed limit is adhered to.\",\"$ref\":\"#/components/schemas/urn_samm_org.eclipse.tractusx.model.status.transition_2.0.0_WarningLevel\"},\"position\":{\"description\":\"Indicates a position\",\"$ref\":\"#/components/schemas/urn_samm_org.eclipse.tractusx.model.status.transition_2.0.0_SpatialPosition\"}},\"required\":[\"moving\",\"speedLimitWarning\",\"position\"]}" ) )
         .andExpect( status().isOk() );
   }

   @Test
   public void testDeleteEndpointWithNotExistingModelExpectNotFound() throws Exception {
      mvc.perform(
               MockMvcRequestBuilders.delete(
                     "/api/v1/models/{urn}",
                     "urn:samm:org.eclipse.tractusx.notexistingpackage:2.0.0#" )
                       .with(jwtTokenFactory.allRoles())
         )
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( status().isNotFound() );
   }

   @Test
   public void testGenerateOpenApiEndpointSpecExpectSuccess() throws Exception {
      String urnPrefix = "urn:samm:org.eclipse.tractusx.testopenapi:1.0.0#";
      mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"DRAFT") )
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( status().isOk() );

      mvc.perform(
               MockMvcRequestBuilders.get( "/api/v1/models/{urn}/openapi?baseUrl=example.com",
                     toMovementUrn(urnPrefix) ).with(jwtTokenFactory.allRoles()))
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( status().isOk() )
         .andExpect( content().json(
               "{\"openapi\":\"3.0.3\",\"info\":{\"title\":\"Movement\",\"version\":\"v1.0.0\"},\"servers\":[{\"url\":\"example.com/api/v1.0.0\",\"variables\":{\"api-version\":{\"default\":\"v1.0.0\"}}}],\"paths\":{\"/{tenant-id}/movement\":{\"get\":{\"tags\":[\"Movement\"],\"operationId\":\"getMovement\",\"parameters\":[{\"name\":\"tenant-id\",\"in\":\"path\",\"description\":\"The ID of the tenant owning the requested Twin.\",\"required\":true,\"schema\":{\"type\":\"string\",\"format\":\"uuid\"}}],\"responses\":{\"200\":{\"$ref\":\"#/components/responses/Movement\"},\"401\":{\"$ref\":\"#/components/responses/ClientError\"},\"402\":{\"$ref\":\"#/components/responses/Unauthorized\"},\"403\":{\"$ref\":\"#/components/responses/Forbidden\"},\"404\":{\"$ref\":\"#/components/responses/NotFoundError\"}}}}},\"components\":{\"schemas\":{\"ErrorResponse\":{\"type\":\"object\",\"required\":[\"error\"],\"properties\":{\"error\":{\"$ref\":\"#/components/schemas/Error\"}}},\"Error\":{\"type\":\"object\",\"required\":[\"details\"],\"properties\":{\"message\":{\"type\":\"string\",\"minLength\":1},\"path\":{\"type\":\"string\",\"minLength\":1},\"details\":{\"type\":\"object\",\"minLength\":1,\"additionalProperties\":{\"type\":\"object\"}},\"code\":{\"type\":\"string\",\"nullable\":true}}},\"urn_samm_org.eclipse.esmf.samm_characteristic_2.0.0_Boolean\":{\"type\":\"boolean\",\"description\":\"Represents a boolean value (i.e. a \\\"flag\\\").\"},\"urn_samm_org.eclipse.tractusx.testopenapi_1.0.0_WarningLevel\":{\"type\":\"string\",\"description\":\"Represents if speed of position change is within specification (green), within tolerance (yellow), or outside specification (red).\",\"enum\":[\"green\",\"yellow\",\"red\"]},\"urn_samm_org.eclipse.tractusx.testopenapi_1.0.0_Coordinate\":{\"type\":\"number\",\"description\":\"Represents a coordinate along an axis in space.\"},\"urn_samm_org.eclipse.tractusx.testopenapi_1.0.0_SpatialPosition\":{\"description\":\"Represents a single location in space.\",\"type\":\"object\",\"properties\":{\"x\":{\"description\":\"x coordinate in space\",\"$ref\":\"#/components/schemas/urn_samm_org.eclipse.tractusx.testopenapi_1.0.0_Coordinate\"},\"y\":{\"description\":\"y coordinate in space\",\"$ref\":\"#/components/schemas/urn_samm_org.eclipse.tractusx.testopenapi_1.0.0_Coordinate\"},\"z\":{\"description\":\"z coordinate in space\",\"$ref\":\"#/components/schemas/urn_samm_org.eclipse.tractusx.testopenapi_1.0.0_Coordinate\"}},\"required\":[\"x\",\"y\"]},\"Movement\":{\"description\":\"Aspect for movement information\",\"type\":\"object\",\"properties\":{\"moving\":{\"description\":\"Flag indicating if the position is changing\",\"$ref\":\"#/components/schemas/urn_samm_org.eclipse.esmf.samm_characteristic_2.0.0_Boolean\"},\"speedLimitWarning\":{\"description\":\"Indicats if speed limit is adhered to.\",\"$ref\":\"#/components/schemas/urn_samm_org.eclipse.tractusx.testopenapi_1.0.0_WarningLevel\"},\"position\":{\"description\":\"Indicates a position\",\"$ref\":\"#/components/schemas/urn_samm_org.eclipse.tractusx.testopenapi_1.0.0_SpatialPosition\"}},\"required\":[\"moving\",\"speedLimitWarning\",\"position\"]}},\"responses\":{\"Unauthorized\":{\"description\":\"The requesting user or client is not authenticated.\"},\"Forbidden\":{\"description\":\"The requesting user or client is not authorized to access resources for the given tenant.\"},\"NotFoundError\":{\"description\":\"The requested Twin has not been found.\"},\"ClientError\":{\"description\":\"Payload or user input is invalid. See error details in the payload for more.\",\"content\":{\"application/json\":{\"schema\":{\"$ref\":\"#/components/schemas/ErrorResponse\"}}}},\"Movement\":{\"content\":{\"application/json\":{\"schema\":{\"$ref\":\"#/components/schemas/Movement\"}}},\"description\":\"The request was successful.\"}},\"requestBodies\":{\"Movement\":{\"content\":{\"application/json\":{\"schema\":{\"$ref\":\"#/components/schemas/Movement\"}}}}}}}" ) )
      ;
   }

  @Test
  public void testAasxEndpointExpectSuccess() throws Exception {
    String urnPrefix = "urn:samm:org.eclipse.tractusx.testaas:1.0.0#";
    mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"DRAFT") )
        .andDo( MockMvcResultHandlers.print() )
        .andExpect( status().isOk() );

    mvc.perform(MockMvcRequestBuilders.get("/api/v1/models/{urn}/aas?aasFormat=XML", toMovementUrn(urnPrefix)).with(jwtTokenFactory.allRoles()))
      .andDo( MockMvcResultHandlers.print())
      .andExpect( status().isOk() )
      .andExpect(content().xml("<?xml version='1.0' encoding='UTF-8'?>\n<aas:environment xmlns:aas=\"https://admin-shell.io/aas/3/0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"https://admin-shell.io/aas/3/0 AAS.xsd\">\n  <aas:assetAdministrationShells>\n    <aas:assetAdministrationShell>\n      <idShort>id_defaultAdminShell</idShort>\n      <aas:description>\n        <aas:langStringTextType>\n          <aas:language>en</aas:language>\n          <aas:text>defaultAdminShell</aas:text>\n        </aas:langStringTextType>\n      </aas:description>\n      <administration/>\n      <id>urn:samm:org.eclipse.tractusx.testaas:1.0.0#Movement</id>\n      <aas:embeddedDataSpecifications>\n        <aas:embeddedDataSpecification>\n          <aas:dataSpecification>\n            <aas:type>ExternalReference</aas:type>\n            <aas:keys>\n              <aas:key>\n                <aas:type>GlobalReference</aas:type>\n                <aas:value>Optional[urn:samm:org.eclipse.tractusx.testaas:1.0.0#Movement]</aas:value>\n              </aas:key>\n            </aas:keys>\n          </aas:dataSpecification>\n          <aas:dataSpecificationContent>\n            <aas:dataSpecificationIec61360>\n              <aas:preferredName>\n                <aas:langStringPreferredNameTypeIec61360>\n                  <aas:language>en</aas:language>\n                  <aas:text>Movement</aas:text>\n                </aas:langStringPreferredNameTypeIec61360>\n              </aas:preferredName>\n              <aas:shortName>\n                <aas:langStringShortNameTypeIec61360>\n                  <aas:language>EN</aas:language>\n                  <aas:text>Movement</aas:text>\n                </aas:langStringShortNameTypeIec61360>\n              </aas:shortName>\n              <aas:definition>\n                <aas:langStringDefinitionTypeIec61360>\n                  <aas:language>en</aas:language>\n                  <aas:text>Aspect for movement information</aas:text>\n                </aas:langStringDefinitionTypeIec61360>\n              </aas:definition>\n            </aas:dataSpecificationIec61360>\n          </aas:dataSpecificationContent>\n        </aas:embeddedDataSpecification>\n      </aas:embeddedDataSpecifications>\n      <assetInformation>\n        <assetKind>Type</assetKind>\n      </assetInformation>\n    </aas:assetAdministrationShell>\n  </aas:assetAdministrationShells>\n  <aas:submodels>\n    <aas:submodel>\n      <idShort>Movement</idShort>\n      <aas:description>\n        <aas:langStringTextType>\n          <aas:language>en</aas:language>\n          <aas:text>Aspect for movement information</aas:text>\n        </aas:langStringTextType>\n      </aas:description>\n      <administration/>\n      <id>Optional[urn:samm:org.eclipse.tractusx.testaas:1.0.0#Movement]/submodel</id>\n      <kind>Template</kind>\n      <aas:semanticId>\n        <aas:type>ModelReference</aas:type>\n        <aas:keys>\n          <aas:key>\n            <aas:type>ConceptDescription</aas:type>\n            <aas:value>urn:samm:org.eclipse.tractusx.testaas:1.0.0#Movement</aas:value>\n          </aas:key>\n        </aas:keys>\n      </aas:semanticId>\n      <aas:submodelElements>\n        <aas:property>\n          <idShort>id_moving</idShort>\n          <aas:displayName>\n            <aas:langStringNameType>\n              <aas:language>en</aas:language>\n              <aas:text>Moving</aas:text>\n            </aas:langStringNameType>\n          </aas:displayName>\n          <aas:description>\n            <aas:langStringTextType>\n              <aas:language>en</aas:language>\n              <aas:text>Flag indicating if the position is changing</aas:text>\n            </aas:langStringTextType>\n          </aas:description>\n          <aas:semanticId>\n            <aas:type>ExternalReference</aas:type>\n            <aas:keys>\n              <aas:key>\n                <aas:type>ConceptDescription</aas:type>\n                <aas:value>urn:samm:org.eclipse.tractusx.testaas:1.0.0#moving</aas:value>\n              </aas:key>\n            </aas:keys>\n          </aas:semanticId>\n          <valueType>xs:boolean</valueType>\n          <value>Unknown</value>\n        </aas:property>\n        <aas:property>\n          <idShort>id_speedLimitWarning</idShort>\n          <aas:displayName>\n            <aas:langStringNameType>\n              <aas:language>en</aas:language>\n              <aas:text>Speed Limit Warning</aas:text>\n            </aas:langStringNameType>\n          </aas:displayName>\n          <aas:description>\n            <aas:langStringTextType>\n              <aas:language>en</aas:language>\n              <aas:text>Indicats if speed limit is adhered to.</aas:text>\n            </aas:langStringTextType>\n          </aas:description>\n          <aas:semanticId>\n            <aas:type>ExternalReference</aas:type>\n            <aas:keys>\n              <aas:key>\n                <aas:type>ConceptDescription</aas:type>\n                <aas:value>urn:samm:org.eclipse.tractusx.testaas:1.0.0#speedLimitWarning</aas:value>\n              </aas:key>\n            </aas:keys>\n          </aas:semanticId>\n          <valueType>xs:string</valueType>\n          <value>Unknown</value>\n        </aas:property>\n        <aas:submodelElementCollection>\n          <idShort>id_SpatialPosition</idShort>\n          <aas:displayName>\n            <aas:langStringNameType>\n              <aas:language>en</aas:language>\n              <aas:text>Spatial Position</aas:text>\n            </aas:langStringNameType>\n          </aas:displayName>\n          <aas:description>\n            <aas:langStringTextType>\n              <aas:language>en</aas:language>\n              <aas:text>Position in space, described along three axis, with the third axis optional, if all positions are in a plane.</aas:text>\n            </aas:langStringTextType>\n          </aas:description>\n          <aas:value>\n            <aas:property>\n              <idShort>id_x</idShort>\n              <aas:displayName>\n                <aas:langStringNameType>\n                  <aas:language>en</aas:language>\n                  <aas:text>x</aas:text>\n                </aas:langStringNameType>\n              </aas:displayName>\n              <aas:description>\n                <aas:langStringTextType>\n                  <aas:language>en</aas:language>\n                  <aas:text>x coordinate in space</aas:text>\n                </aas:langStringTextType>\n              </aas:description>\n              <aas:semanticId>\n                <aas:type>ExternalReference</aas:type>\n                <aas:keys>\n                  <aas:key>\n                    <aas:type>ConceptDescription</aas:type>\n                    <aas:value>urn:samm:org.eclipse.tractusx.testaas:1.0.0#x</aas:value>\n                  </aas:key>\n                </aas:keys>\n              </aas:semanticId>\n              <valueType>xs:float</valueType>\n              <value>Unknown</value>\n            </aas:property>\n            <aas:property>\n              <idShort>id_y</idShort>\n              <aas:displayName>\n                <aas:langStringNameType>\n                  <aas:language>en</aas:language>\n                  <aas:text>y</aas:text>\n                </aas:langStringNameType>\n              </aas:displayName>\n              <aas:description>\n                <aas:langStringTextType>\n                  <aas:language>en</aas:language>\n                  <aas:text>y coordinate in space</aas:text>\n                </aas:langStringTextType>\n              </aas:description>\n              <aas:semanticId>\n                <aas:type>ExternalReference</aas:type>\n                <aas:keys>\n                  <aas:key>\n                    <aas:type>ConceptDescription</aas:type>\n                    <aas:value>urn:samm:org.eclipse.tractusx.testaas:1.0.0#y</aas:value>\n                  </aas:key>\n                </aas:keys>\n              </aas:semanticId>\n              <valueType>xs:float</valueType>\n              <value>Unknown</value>\n            </aas:property>\n            <aas:property>\n              <idShort>id_z</idShort>\n              <aas:displayName>\n                <aas:langStringNameType>\n                  <aas:language>en</aas:language>\n                  <aas:text>z</aas:text>\n                </aas:langStringNameType>\n              </aas:displayName>\n              <aas:description>\n                <aas:langStringTextType>\n                  <aas:language>en</aas:language>\n                  <aas:text>z coordinate in space</aas:text>\n                </aas:langStringTextType>\n              </aas:description>\n              <aas:semanticId>\n                <aas:type>ExternalReference</aas:type>\n                <aas:keys>\n                  <aas:key>\n                    <aas:type>ConceptDescription</aas:type>\n                    <aas:value>urn:samm:org.eclipse.tractusx.testaas:1.0.0#z</aas:value>\n                  </aas:key>\n                </aas:keys>\n              </aas:semanticId>\n              <valueType>xs:float</valueType>\n              <value>Unknown</value>\n            </aas:property>\n          </aas:value>\n        </aas:submodelElementCollection>\n      </aas:submodelElements>\n    </aas:submodel>\n  </aas:submodels>\n  <aas:conceptDescriptions>\n    <aas:conceptDescription>\n      <category>APPLICATION_CLASS</category>\n      <idShort>id_Movement</idShort>\n      <aas:displayName>\n        <aas:langStringNameType>\n          <aas:language>en</aas:language>\n          <aas:text>Movement</aas:text>\n        </aas:langStringNameType>\n      </aas:displayName>\n      <aas:description>\n        <aas:langStringTextType>\n          <aas:language>en</aas:language>\n          <aas:text>Aspect for movement information</aas:text>\n        </aas:langStringTextType>\n      </aas:description>\n      <id>urn:samm:org.eclipse.tractusx.testaas:1.0.0#Movement</id>\n      <aas:embeddedDataSpecifications>\n        <aas:embeddedDataSpecification>\n          <aas:dataSpecification>\n            <aas:type>ExternalReference</aas:type>\n            <aas:keys>\n              <aas:key>\n                <aas:type>GlobalReference</aas:type>\n                <aas:value>Optional[urn:samm:org.eclipse.tractusx.testaas:1.0.0#Movement]</aas:value>\n              </aas:key>\n            </aas:keys>\n          </aas:dataSpecification>\n          <aas:dataSpecificationContent>\n            <aas:dataSpecificationIec61360>\n              <aas:preferredName>\n                <aas:langStringPreferredNameTypeIec61360>\n                  <aas:language>en</aas:language>\n                  <aas:text>Movement</aas:text>\n                </aas:langStringPreferredNameTypeIec61360>\n              </aas:preferredName>\n              <aas:shortName>\n                <aas:langStringShortNameTypeIec61360>\n                  <aas:language>EN</aas:language>\n                  <aas:text>Movement</aas:text>\n                </aas:langStringShortNameTypeIec61360>\n              </aas:shortName>\n              <aas:definition>\n                <aas:langStringDefinitionTypeIec61360>\n                  <aas:language>en</aas:language>\n                  <aas:text>Aspect for movement information</aas:text>\n                </aas:langStringDefinitionTypeIec61360>\n              </aas:definition>\n            </aas:dataSpecificationIec61360>\n          </aas:dataSpecificationContent>\n        </aas:embeddedDataSpecification>\n      </aas:embeddedDataSpecifications>\n    </aas:conceptDescription>\n    <aas:conceptDescription>\n      <idShort>id_Boolean</idShort>\n      <aas:displayName>\n        <aas:langStringNameType>\n          <aas:language>en</aas:language>\n          <aas:text>Boolean</aas:text>\n        </aas:langStringNameType>\n      </aas:displayName>\n      <id>urn:samm:org.eclipse.tractusx.testaas:1.0.0#moving</id>\n      <aas:embeddedDataSpecifications>\n        <aas:embeddedDataSpecification>\n          <aas:dataSpecification>\n            <aas:type>ExternalReference</aas:type>\n            <aas:keys>\n              <aas:key>\n                <aas:type>GlobalReference</aas:type>\n                <aas:value>Optional[urn:samm:org.eclipse.tractusx.testaas:1.0.0#moving]</aas:value>\n              </aas:key>\n            </aas:keys>\n          </aas:dataSpecification>\n          <aas:dataSpecificationContent>\n            <aas:dataSpecificationIec61360>\n              <aas:preferredName>\n                <aas:langStringPreferredNameTypeIec61360>\n                  <aas:language>en</aas:language>\n                  <aas:text>Moving</aas:text>\n                </aas:langStringPreferredNameTypeIec61360>\n              </aas:preferredName>\n              <aas:shortName>\n                <aas:langStringShortNameTypeIec61360>\n                  <aas:language>EN</aas:language>\n                  <aas:text>moving</aas:text>\n                </aas:langStringShortNameTypeIec61360>\n              </aas:shortName>\n              <aas:dataType>BOOLEAN</aas:dataType>\n              <aas:definition>\n                <aas:langStringDefinitionTypeIec61360>\n                  <aas:language>en</aas:language>\n                  <aas:text>Represents a boolean value (i.e. a \"flag\").</aas:text>\n                </aas:langStringDefinitionTypeIec61360>\n              </aas:definition>\n            </aas:dataSpecificationIec61360>\n          </aas:dataSpecificationContent>\n        </aas:embeddedDataSpecification>\n      </aas:embeddedDataSpecifications>\n    </aas:conceptDescription>\n    <aas:conceptDescription>\n      <idShort>id_WarningLevel</idShort>\n      <aas:displayName>\n        <aas:langStringNameType>\n          <aas:language>en</aas:language>\n          <aas:text>Warning Level</aas:text>\n        </aas:langStringNameType>\n      </aas:displayName>\n      <id>urn:samm:org.eclipse.tractusx.testaas:1.0.0#speedLimitWarning</id>\n      <aas:embeddedDataSpecifications>\n        <aas:embeddedDataSpecification>\n          <aas:dataSpecification>\n            <aas:type>ExternalReference</aas:type>\n            <aas:keys>\n              <aas:key>\n                <aas:type>GlobalReference</aas:type>\n                <aas:value>Optional[urn:samm:org.eclipse.tractusx.testaas:1.0.0#speedLimitWarning]</aas:value>\n              </aas:key>\n            </aas:keys>\n          </aas:dataSpecification>\n          <aas:dataSpecificationContent>\n            <aas:dataSpecificationIec61360>\n              <aas:preferredName>\n                <aas:langStringPreferredNameTypeIec61360>\n                  <aas:language>en</aas:language>\n                  <aas:text>Speed Limit Warning</aas:text>\n                </aas:langStringPreferredNameTypeIec61360>\n              </aas:preferredName>\n              <aas:shortName>\n                <aas:langStringShortNameTypeIec61360>\n                  <aas:language>EN</aas:language>\n                  <aas:text>speedLimitWarning</aas:text>\n                </aas:langStringShortNameTypeIec61360>\n              </aas:shortName>\n              <aas:dataType>STRING</aas:dataType>\n              <aas:definition>\n                <aas:langStringDefinitionTypeIec61360>\n                  <aas:language>en</aas:language>\n                  <aas:text>Represents if speed of position change is within specification (green), within tolerance (yellow), or outside specification (red).</aas:text>\n                </aas:langStringDefinitionTypeIec61360>\n              </aas:definition>\n              <aas:valueList>\n                <aas:valueReferencePairs>\n                  <aas:valueReferencePair>\n                    <aas:value>DefaultScalarValue[value=green, typeUri='DefaultScalar[metaModelVersion=SAMM_2_0_0, urn='http://www.w3.org/2001/XMLSchema#string']']</aas:value>\n                    <aas:valueId>\n                      <aas:type>ModelReference</aas:type>\n                      <aas:keys>\n                        <aas:key>\n                          <aas:type>DataElement</aas:type>\n                          <aas:value>urn:samm:org.eclipse.tractusx.testaas:1.0.0#WarningLevel:DefaultScalarValue[value=green, typeUri='DefaultScalar[metaModelVersion=SAMM_2_0_0, urn='http://www.w3.org/2001/XMLSchema#string']']</aas:value>\n                        </aas:key>\n                      </aas:keys>\n                    </aas:valueId>\n                  </aas:valueReferencePair>\n                  <aas:valueReferencePair>\n                    <aas:value>DefaultScalarValue[value=yellow, typeUri='DefaultScalar[metaModelVersion=SAMM_2_0_0, urn='http://www.w3.org/2001/XMLSchema#string']']</aas:value>\n                    <aas:valueId>\n                      <aas:type>ModelReference</aas:type>\n                      <aas:keys>\n                        <aas:key>\n                          <aas:type>DataElement</aas:type>\n                          <aas:value>urn:samm:org.eclipse.tractusx.testaas:1.0.0#WarningLevel:DefaultScalarValue[value=yellow, typeUri='DefaultScalar[metaModelVersion=SAMM_2_0_0, urn='http://www.w3.org/2001/XMLSchema#string']']</aas:value>\n                        </aas:key>\n                      </aas:keys>\n                    </aas:valueId>\n                  </aas:valueReferencePair>\n                  <aas:valueReferencePair>\n                    <aas:value>DefaultScalarValue[value=red, typeUri='DefaultScalar[metaModelVersion=SAMM_2_0_0, urn='http://www.w3.org/2001/XMLSchema#string']']</aas:value>\n                    <aas:valueId>\n                      <aas:type>ModelReference</aas:type>\n                      <aas:keys>\n                        <aas:key>\n                          <aas:type>DataElement</aas:type>\n                          <aas:value>urn:samm:org.eclipse.tractusx.testaas:1.0.0#WarningLevel:DefaultScalarValue[value=red, typeUri='DefaultScalar[metaModelVersion=SAMM_2_0_0, urn='http://www.w3.org/2001/XMLSchema#string']']</aas:value>\n                        </aas:key>\n                      </aas:keys>\n                    </aas:valueId>\n                  </aas:valueReferencePair>\n                </aas:valueReferencePairs>\n              </aas:valueList>\n            </aas:dataSpecificationIec61360>\n          </aas:dataSpecificationContent>\n        </aas:embeddedDataSpecification>\n      </aas:embeddedDataSpecifications>\n    </aas:conceptDescription>\n    <aas:conceptDescription>\n      <idShort>id_Coordinate</idShort>\n      <aas:displayName>\n        <aas:langStringNameType>\n          <aas:language>en</aas:language>\n          <aas:text>Coordinate</aas:text>\n        </aas:langStringNameType>\n      </aas:displayName>\n      <id>urn:samm:org.eclipse.tractusx.testaas:1.0.0#x</id>\n      <aas:embeddedDataSpecifications>\n        <aas:embeddedDataSpecification>\n          <aas:dataSpecification>\n            <aas:type>ExternalReference</aas:type>\n            <aas:keys>\n              <aas:key>\n                <aas:type>GlobalReference</aas:type>\n                <aas:value>Optional[urn:samm:org.eclipse.tractusx.testaas:1.0.0#x]</aas:value>\n              </aas:key>\n            </aas:keys>\n          </aas:dataSpecification>\n          <aas:dataSpecificationContent>\n            <aas:dataSpecificationIec61360>\n              <aas:preferredName>\n                <aas:langStringPreferredNameTypeIec61360>\n                  <aas:language>en</aas:language>\n                  <aas:text>x</aas:text>\n                </aas:langStringPreferredNameTypeIec61360>\n              </aas:preferredName>\n              <aas:shortName>\n                <aas:langStringShortNameTypeIec61360>\n                  <aas:language>EN</aas:language>\n                  <aas:text>x</aas:text>\n                </aas:langStringShortNameTypeIec61360>\n              </aas:shortName>\n              <aas:unit>metre</aas:unit>\n              <aas:dataType>REAL_MEASURE</aas:dataType>\n              <aas:definition>\n                <aas:langStringDefinitionTypeIec61360>\n                  <aas:language>en</aas:language>\n                  <aas:text>Represents a coordinate along an axis in space.</aas:text>\n                </aas:langStringDefinitionTypeIec61360>\n              </aas:definition>\n            </aas:dataSpecificationIec61360>\n          </aas:dataSpecificationContent>\n        </aas:embeddedDataSpecification>\n      </aas:embeddedDataSpecifications>\n    </aas:conceptDescription>\n    <aas:conceptDescription>\n      <idShort>id_Coordinate</idShort>\n      <aas:displayName>\n        <aas:langStringNameType>\n          <aas:language>en</aas:language>\n          <aas:text>Coordinate</aas:text>\n        </aas:langStringNameType>\n      </aas:displayName>\n      <id>urn:samm:org.eclipse.tractusx.testaas:1.0.0#y</id>\n      <aas:embeddedDataSpecifications>\n        <aas:embeddedDataSpecification>\n          <aas:dataSpecification>\n            <aas:type>ExternalReference</aas:type>\n            <aas:keys>\n              <aas:key>\n                <aas:type>GlobalReference</aas:type>\n                <aas:value>Optional[urn:samm:org.eclipse.tractusx.testaas:1.0.0#y]</aas:value>\n              </aas:key>\n            </aas:keys>\n          </aas:dataSpecification>\n          <aas:dataSpecificationContent>\n            <aas:dataSpecificationIec61360>\n              <aas:preferredName>\n                <aas:langStringPreferredNameTypeIec61360>\n                  <aas:language>en</aas:language>\n                  <aas:text>y</aas:text>\n                </aas:langStringPreferredNameTypeIec61360>\n              </aas:preferredName>\n              <aas:shortName>\n                <aas:langStringShortNameTypeIec61360>\n                  <aas:language>EN</aas:language>\n                  <aas:text>y</aas:text>\n                </aas:langStringShortNameTypeIec61360>\n              </aas:shortName>\n              <aas:unit>metre</aas:unit>\n              <aas:dataType>REAL_MEASURE</aas:dataType>\n              <aas:definition>\n                <aas:langStringDefinitionTypeIec61360>\n                  <aas:language>en</aas:language>\n                  <aas:text>Represents a coordinate along an axis in space.</aas:text>\n                </aas:langStringDefinitionTypeIec61360>\n              </aas:definition>\n            </aas:dataSpecificationIec61360>\n          </aas:dataSpecificationContent>\n        </aas:embeddedDataSpecification>\n      </aas:embeddedDataSpecifications>\n    </aas:conceptDescription>\n    <aas:conceptDescription>\n      <idShort>id_Coordinate</idShort>\n      <aas:displayName>\n        <aas:langStringNameType>\n          <aas:language>en</aas:language>\n          <aas:text>Coordinate</aas:text>\n        </aas:langStringNameType>\n      </aas:displayName>\n      <id>urn:samm:org.eclipse.tractusx.testaas:1.0.0#z</id>\n      <aas:embeddedDataSpecifications>\n        <aas:embeddedDataSpecification>\n          <aas:dataSpecification>\n            <aas:type>ExternalReference</aas:type>\n            <aas:keys>\n              <aas:key>\n                <aas:type>GlobalReference</aas:type>\n                <aas:value>Optional[urn:samm:org.eclipse.tractusx.testaas:1.0.0#z]</aas:value>\n              </aas:key>\n            </aas:keys>\n          </aas:dataSpecification>\n          <aas:dataSpecificationContent>\n            <aas:dataSpecificationIec61360>\n              <aas:preferredName>\n                <aas:langStringPreferredNameTypeIec61360>\n                  <aas:language>en</aas:language>\n                  <aas:text>z</aas:text>\n                </aas:langStringPreferredNameTypeIec61360>\n              </aas:preferredName>\n              <aas:shortName>\n                <aas:langStringShortNameTypeIec61360>\n                  <aas:language>EN</aas:language>\n                  <aas:text>z</aas:text>\n                </aas:langStringShortNameTypeIec61360>\n              </aas:shortName>\n              <aas:unit>metre</aas:unit>\n              <aas:dataType>REAL_MEASURE</aas:dataType>\n              <aas:definition>\n                <aas:langStringDefinitionTypeIec61360>\n                  <aas:language>en</aas:language>\n                  <aas:text>Represents a coordinate along an axis in space.</aas:text>\n                </aas:langStringDefinitionTypeIec61360>\n              </aas:definition>\n            </aas:dataSpecificationIec61360>\n          </aas:dataSpecificationContent>\n        </aas:embeddedDataSpecification>\n      </aas:embeddedDataSpecifications>\n    </aas:conceptDescription>\n    <aas:conceptDescription>\n      <idShort>id_PositionSingleEntity</idShort>\n      <aas:displayName>\n        <aas:langStringNameType>\n          <aas:language>en</aas:language>\n          <aas:text>Spatial Position Characteristic</aas:text>\n        </aas:langStringNameType>\n      </aas:displayName>\n      <id>urn:samm:org.eclipse.tractusx.testaas:1.0.0#position</id>\n      <aas:embeddedDataSpecifications>\n        <aas:embeddedDataSpecification>\n          <aas:dataSpecification>\n            <aas:type>ExternalReference</aas:type>\n            <aas:keys>\n              <aas:key>\n                <aas:type>GlobalReference</aas:type>\n                <aas:value>Optional[urn:samm:org.eclipse.tractusx.testaas:1.0.0#position]</aas:value>\n              </aas:key>\n            </aas:keys>\n          </aas:dataSpecification>\n          <aas:dataSpecificationContent>\n            <aas:dataSpecificationIec61360>\n              <aas:preferredName>\n                <aas:langStringPreferredNameTypeIec61360>\n                  <aas:language>en</aas:language>\n                  <aas:text>Position</aas:text>\n                </aas:langStringPreferredNameTypeIec61360>\n              </aas:preferredName>\n              <aas:shortName>\n                <aas:langStringShortNameTypeIec61360>\n                  <aas:language>EN</aas:language>\n                  <aas:text>position</aas:text>\n                </aas:langStringShortNameTypeIec61360>\n              </aas:shortName>\n              <aas:dataType>STRING</aas:dataType>\n              <aas:definition>\n                <aas:langStringDefinitionTypeIec61360>\n                  <aas:language>en</aas:language>\n                  <aas:text>Represents a single location in space.</aas:text>\n                </aas:langStringDefinitionTypeIec61360>\n              </aas:definition>\n            </aas:dataSpecificationIec61360>\n          </aas:dataSpecificationContent>\n        </aas:embeddedDataSpecification>\n      </aas:embeddedDataSpecifications>\n    </aas:conceptDescription>\n  </aas:conceptDescriptions>\n</aas:environment>\n"));


    mvc.perform(MockMvcRequestBuilders.get("/api/v1/models/{urn}/aas?aasFormat=FILE", toMovementUrn(urnPrefix)).with(jwtTokenFactory.allRoles()))
        .andDo( MockMvcResultHandlers.print())
        .andExpect( header().string("Content-Type", "application/octet-stream") )
        .andExpect( status().isOk() );

     mvc.perform( MockMvcRequestBuilders.get( "/api/v1/models/{urn}/aas?aasFormat=JSON", toMovementUrn( urnPrefix ) ).with( jwtTokenFactory.allRoles() ) )
           .andDo( MockMvcResultHandlers.print() )
           .andExpect( status().isOk() )
           .andExpect( content().json( TestUtils.loadModelFromResources( AAS_JSON_FILE )));

  }

    @Test
    public void testAasxEndpointExpectError() throws Exception {
        String urnPrefix = "urn:samm:org.eclipse.tractusx.testaas:2.0.0#";
        mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"DRAFT") )
            .andDo( MockMvcResultHandlers.print() )
            .andExpect( status().isOk() );

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/models/{urn}/aas?aasFormat=WRONG_FORMAT", toMovementUrn(urnPrefix)).with(jwtTokenFactory.allRoles()))
        .andDo( MockMvcResultHandlers.print())
        .andExpect( status().is4xxClientError() );
    }

   @Test
   public void testExampleGenerateExamplePayloadJsonExpectSuccess() throws Exception {
      String urnPrefix = "urn:samm:org.eclipse.tractusx.testjsonschema:2.0.0#";
      mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"DRAFT")
                      .with(jwtTokenFactory.allRoles()))
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( status().isOk() );

      mvc.perform(
               MockMvcRequestBuilders.get( "/api/v1/models/{urn}/example-payload",
                     toMovementUrn(urnPrefix) )
                       .with(jwtTokenFactory.allRoles())
         )
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( jsonPath( "$.moving" ).exists() )
         .andExpect( jsonPath( "$.speedLimitWarning" ).exists() )
         .andExpect( jsonPath( "$.position.x" ).exists() )
         .andExpect( jsonPath( "$.position.y" ).exists() )
         .andExpect( jsonPath( "$.position.z" ).exists() )
         .andExpect( status().isOk() );
   }

   /**
    * This test verifies that existing triples e.g. characteristic can be referenced.
    */
   @Test
   public void testSaveModelWithExternalReferencesExpectSuccess() throws Exception {
      // save the model with external reference to a traceability characteristic
      // this will fail because traceability does not exist yet
      String modelWithReferenceToTraceability = TestUtils.loadModelFromResources(
            TestUtils.MODEL_WITH_REFERENCE_TO_TRACEABILITY_MODEL_PATH );
      mvc.perform( post( modelWithReferenceToTraceability,"DRAFT" ) )
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( status().isBadRequest() )
         .andExpect( jsonPath( "$.error.message", is( "Validation failed." ) ) )
         .andExpect( jsonPath( "$.error.details.validationError",
               containsString( "urn:samm:org.eclipse.tractusx.traceability:0.1.1#PartDataCharacteristic" ) ) );

      // save the traceability aspect model
      String traceabilityModel = TestUtils.loadModelFromResources(
            TestUtils.TRACEABILITY_MODEL_PATH );
      mvc.perform( post( traceabilityModel, "DRAFT" ) )
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( status().isOk() );

      // save again the model with external reference and validate the result
      mvc.perform( post(modelWithReferenceToTraceability, "DRAFT" ) )
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( status().isOk() );

      mvc.perform(
               MockMvcRequestBuilders.get( "/api/v1/models/{urn}/example-payload",
                     "urn:samm:org.eclipse.tractusx.modelwithreferencetotraceability:0.1.1#ModelWithReferenceToTraceability" )
                       .with(jwtTokenFactory.allRoles())
         )
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( jsonPath( "$.staticData" ).exists() )
         .andExpect( jsonPath( "$.staticData.customerId" ).exists() )
         .andExpect( status().isOk() );

      // verify that the turtle file contains a complete resolved model
      String traceabilityBaseUrn = "urn:samm:org.eclipse.tractusx.traceability:0.1.1";
      String modelExtBaseUrn = "urn:samm:org.eclipse.tractusx.modelwithreferencetotraceability:0.1.1";
      mvc.perform(
               MockMvcRequestBuilders.get( "/api/v1/models/{urn}/file",
                     "urn:samm:org.eclipse.tractusx.modelwithreferencetotraceability:0.1.1#ModelWithReferenceToTraceability" )
                       .with(jwtTokenFactory.allRoles())
         )
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( status().isOk() )
         .andExpect( content().string( containsString( modelExtBaseUrn + "#ModelWithReferenceToTraceability" ) ) )
         .andExpect( content().string( containsString( modelExtBaseUrn + "#staticData" ) ) )
         .andExpect( content().string( containsString( traceabilityBaseUrn + "#customerId" ) ) );
   }

   /**
    * With the introduction of references between models, the existing model status process needs to be redesigned.
    * The current implementation is based on the old process. This test ensures the correctness of the old process,
    * and will be updated as soon as the new model status process is clarified and implemented.
    */
   @Nested
   @DisplayName("State Transition of Models")
   public class StateTransitionTests{
      @Test
      public void testModelStatusTransitionForPost() throws Exception {
         String urnPrefix = "urn:samm:org.eclipse.tractusx.model.status.transition.post:2.0.0#";

         // draft state, deletes and modifications are allowed
         mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"DRAFT") )
                 .andDo( MockMvcResultHandlers.print() )
                 .andExpect(status().isOk());

         mvc.perform(delete(urnPrefix))
                 .andDo( MockMvcResultHandlers.print() )
                 .andExpect(status().isNoContent());

         mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"DRAFT") )
                 .andDo( MockMvcResultHandlers.print() )
                 .andExpect(status().isOk());

         // Transition from draft to standardized is not allowed
         mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"STANDARDIZED") )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(status().isBadRequest());

         // transition from draft to release is allowed, delete is not allowed
         mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"RELEASED") )
                 .andDo( MockMvcResultHandlers.print() )
                 .andExpect(status().isOk());

         mvc.perform(delete(urnPrefix))
                 .andDo( MockMvcResultHandlers.print() )
                 .andExpect(status().isBadRequest())
                 .andExpect( jsonPath( "$.error.message", is(
                         "The package urn:samm:org.eclipse.tractusx.model.status.transition.post:2.0.0# is already in status RELEASED and cannot be deleted." ) ) );

         // transition from released to draft is not allowed
         mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"DRAFT") )
                 .andExpect( jsonPath( "$.error.message", is(
                         "The package urn:samm:org.eclipse.tractusx.model.status.transition.post:2.0.0# is already in status RELEASED and cannot be modified. Only a transition to STANDARDIZED or DEPRECATED is possible." ) ) )
                 .andExpect( status().isBadRequest() );

         // transition from released to standardized is allowed
         mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"STANDARDIZED") )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(status().isOk());

         // transition from standardized to draft is not allowed
         mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"DRAFT") )
               .andExpect( jsonPath( "$.error.message", is(
                     "The package urn:samm:org.eclipse.tractusx.model.status.transition.post:2.0.0# is already in status STANDARDIZED and cannot be modified. Only a transition to DEPRECATED is possible." ) ) )
               .andExpect( status().isBadRequest() );

         // transition from standardized to deprecated is allowed
         mvc.perform(
                         post( TestUtils.createValidModelRequest(urnPrefix),"DEPRECATED")
                 )
                 .andDo( MockMvcResultHandlers.print() )
                 .andExpect(status().isOk());

         // delete deprecated model is allowed
         mvc.perform(
                         delete( urnPrefix )
                 )
                 .andDo( MockMvcResultHandlers.print() )
                 .andExpect(status().isNoContent());
      }
      @Test
      public void testModelStatusTransitionForPut() throws Exception {
         String urnPrefix = "urn:samm:org.eclipse.tractusx.model.status.transition.put:2.0.0#";

         // draft state, deletes and modifications are allowed
         mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"DRAFT") )
                 .andDo( MockMvcResultHandlers.print() )
                 .andExpect(status().isOk());

         mvc.perform(delete(urnPrefix))
                 .andDo( MockMvcResultHandlers.print() )
                 .andExpect(status().isNoContent());

         mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix), "DRAFT") )
                 .andDo( MockMvcResultHandlers.print() )
                 .andExpect(status().isOk());

         // transition from draft to release is allowed, delete is not allowed
         mvc.perform(put( TestUtils.createValidModelRequest(urnPrefix),"RELEASED"))
                 .andDo( MockMvcResultHandlers.print() )
                 .andExpect(status().isOk());

         mvc.perform(delete(urnPrefix))
                 .andDo( MockMvcResultHandlers.print() )
                 .andExpect(status().isBadRequest())
                 .andExpect( jsonPath( "$.error.message", is(
                         "The package urn:samm:org.eclipse.tractusx.model.status.transition.put:2.0.0# is already in status RELEASED and cannot be deleted." ) ) );

         // transition from released to draft is not allowed
         mvc.perform(put( TestUtils.createValidModelRequest(urnPrefix), "DRAFT") )
                 .andExpect( jsonPath( "$.error.message", is(
                         "The package urn:samm:org.eclipse.tractusx.model.status.transition.put:2.0.0# is already in status RELEASED and cannot be modified. Only a transition to STANDARDIZED or DEPRECATED is possible." ) ) )
                 .andExpect( status().isBadRequest() );

         // transition from released to standardized is allowed
         mvc.perform(put( TestUtils.createValidModelRequest(urnPrefix),"STANDARDIZED") )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(status().isOk());

         // transition from standardized to deprecated is allowed
         mvc.perform(put( TestUtils.createValidModelRequest(urnPrefix),"DEPRECATED") )
                 .andDo( MockMvcResultHandlers.print() )
                 .andExpect(status().isOk());

         // delete deprecated model is allowed
         mvc.perform(delete( urnPrefix ))
                 .andDo( MockMvcResultHandlers.print() )
                 .andExpect(status().isNoContent());
      }

      @Test
      public void testDependentModelTransition() throws Exception {
        String urnPrefix = "urn:samm:org.eclipse.tractusx.model.status.transitionWithDependency:1.0.0#";

        mvc.perform(post( TestUtils.createModelDependency(), "DRAFT" ))
            .andDo( MockMvcResultHandlers.print() )
            .andExpect(status().isOk());

        mvc.perform(post( TestUtils.createDependentModel(urnPrefix), "DRAFT") )
            .andDo( MockMvcResultHandlers.print() )
            .andExpect(status().isOk());

        mvc.perform(put( TestUtils.createDependentModel(urnPrefix), "RELEASED") )
            .andDo( MockMvcResultHandlers.print() )
            .andExpect(jsonPath( "$.error.message", is(
                "It is not allowed to release an aspect that has dependencies in DRAFT state." ) ) );
      }
   }

    @Test
    public void testGetModelListByMultipleUrns() throws Exception {
        String urnPrefixPattern = "urn:samm:org.eclipse.tractusx.test_model_list_by_urns_%s:1.0.0#";

        List<String> urnSearchArrayEvenNumbers = new ArrayList<String>();
        List<String> urnSearchArrayOddNumbers = new ArrayList<String>();
        List<String> urnSearchArrayNonExistingEntry = new ArrayList<String>();

        for(int i = 1; i <= 11; i++) {
            String urnPrefix = String.format(urnPrefixPattern, i);
            mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"DRAFT") )
            .andDo( MockMvcResultHandlers.print() )
            .andExpect( status().isOk() );

            if((i % 2) == 0) {
                urnSearchArrayEvenNumbers.add(toMovementUrn(urnPrefix));
            } else {
                urnSearchArrayOddNumbers.add(toMovementUrn(urnPrefix));
            }
        }

        urnSearchArrayNonExistingEntry.add("urn:samm:org.eclipse.tractusx.test_model_list_by_urns_50:1.0.0#Movement");

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/models/lookup" )
        .param("pageSize", "2")
        .param("page", "0")
        .content(new JSONArray(urnSearchArrayEvenNumbers).toString())
        .contentType(MediaType.APPLICATION_JSON)
        .with(jwtTokenFactory.allRoles()))
        .andDo( MockMvcResultHandlers.print() )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentPage", equalTo(0)))
        .andExpect(jsonPath("$.totalItems", equalTo(5)))
        .andExpect(jsonPath("$.totalPages", equalTo(3)))
        .andExpect(jsonPath("$.items[0].urn", equalTo("urn:samm:org.eclipse.tractusx.test_model_list_by_urns_10:1.0.0#Movement")))
        .andExpect(jsonPath("$.items[1].urn", equalTo("urn:samm:org.eclipse.tractusx.test_model_list_by_urns_2:1.0.0#Movement")))
        .andExpect(jsonPath("$.items.length()", equalTo(2)));

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/models/lookup")
        .param("pageSize", "2")
        .param("page", "1")
        .content(new JSONArray(urnSearchArrayOddNumbers).toString())
        .contentType(MediaType.APPLICATION_JSON)
        .with(jwtTokenFactory.allRoles()))
        .andDo( MockMvcResultHandlers.print() )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentPage", equalTo(1)))
        .andExpect(jsonPath("$.totalItems", equalTo(6)))
        .andExpect(jsonPath("$.totalPages", equalTo(3)))
        .andExpect(jsonPath("$.items[0].urn", equalTo("urn:samm:org.eclipse.tractusx.test_model_list_by_urns_3:1.0.0#Movement")))
        .andExpect(jsonPath("$.items[1].urn", equalTo("urn:samm:org.eclipse.tractusx.test_model_list_by_urns_5:1.0.0#Movement")))
        .andExpect(jsonPath("$.items.length()", equalTo(2)));

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/models/lookup")
        .content(new JSONArray(urnSearchArrayNonExistingEntry).toString())
        .contentType(MediaType.APPLICATION_JSON)
        .with(jwtTokenFactory.allRoles()))
        .andDo( MockMvcResultHandlers.print() )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()", equalTo(0)));

    }

   private static String toMovementUrn(String urn){
      return urn + "Movement";
   }
}
