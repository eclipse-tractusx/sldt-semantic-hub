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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.atlas.json.JSON;
import org.apache.poi.hpsf.Array;
import org.json.JSONArray;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.internal.function.text.Length;

import jakarta.json.JsonArray;

@DirtiesContext( classMode = DirtiesContext.ClassMode.AFTER_CLASS )
public class ModelsApiTest extends AbstractModelsApiTest{

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
      String urnPrefix = "urn:bamm:org.eclipse.tractusx:1.0.0#";
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
         .andExpect( jsonPath( "$.items[*].type", hasItem( "BAMM" ) ) )
         .andExpect( jsonPath( "$.items[*].status", hasItem( "DRAFT" ) ) )
         .andExpect( jsonPath( "$.totalItems", greaterThan( 0 ) ) )
         .andExpect( jsonPath( "$.itemCount", greaterThan( 0 ) ) )
         .andExpect( status().isOk() );
   }

   @Test
   public void testSaveValidModelExpectSuccess() throws Exception {
      String urnPrefix = "urn:bamm:org.eclipse.tractusx.valid.save:2.0.0#";
      mvc.perform(
               post( TestUtils.createValidModelRequest(urnPrefix),"RELEASED")
         )
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( jsonPath( "$.urn", is( toMovementUrn(urnPrefix) ) ) )
         .andExpect( jsonPath( "$.version", is( "2.0.0" ) ) )
         .andExpect( jsonPath( "$.name", is( "Movement" ) ) )
         .andExpect( jsonPath( "$.type", is( "BAMM" ) ) )
         .andExpect( jsonPath( "$.status", is( "RELEASED" ) ) )
         .andExpect( status().isOk() );
   }

   // TODO: Check, if this model is invalid.
   @Test
   public void testSaveInvalidModelExpectSuccess() throws Exception {
      String insertModelJson = "@prefix bamm: <urn:bamm:io.openmanufacturing:meta-model:1.0.0#> .\n @prefix bamm-c: <urn:bamm:io.openmanufacturing:characteristicX:1.0.0#> .\n @prefix bamm-e: <urn:bamm:io.openmanufacturing:entity:1.0.0#> .\n @prefix unit: <urn:bamm:io.openmanufacturing:unit:1.0.0#> .\n @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n @prefix : <urn:bamm:org.eclipse.tractusx:1.0.0#> .\n \n :Movement a bamm:Aspect;\n bamm:name \"Movement\";\n bamm:preferredName \"Movement\"@en;\n bamm:description \"Aspect for movement information\"@en;\n bamm:propertiesX (:isMoving :speedLimitWarning :position);\n bamm:operations ().\n :isMoving a bamm:Property;\n bamm:name \"isMoving\";\n bamm:preferredName \"Moving\"@en;\n bamm:description \"Flag indicating whether the asset is currently moving\"@en;\n bamm:characteristic bamm-c:Boolean.\n :speedLimitWarning a bamm:Property;\n bamm:name \"speedLimitWarning\";\n bamm:preferredName \"Speed Limit Warning\"@en;\n bamm:description \"Indicates if the speed limit is adhered to.\"@en;\n bamm:characteristic :TrafficLight.\n :position a bamm:Property;\n bamm:name \"position\";\n bamm:preferredName \"Position\"@en;\n bamm:description \"Indicates a position\"@en;\n bamm:characteristic :SpatialPositionCharacteristic.\n :TrafficLight a bamm-c:Enumeration;\n bamm:name \"TrafficLight\";\n bamm:preferredName \"Warning Level\"@en;\n bamm:description \"Represents if speed of position change is within specification (green), within tolerance (yellow), or outside specification (red).\"@en;\n bamm:dataType xsd:string;\n bamm-c:values (\"green\" \"yellow\" \"red\").\n :SpatialPosition a bamm:Entity;\n bamm:name \"SpatialPosition\";\n bamm:preferredName \"Spatial Position\"@en;\n bamm:description \"Position in space, described along three axis, with the third axis optional, if all positions are in a plane.\"@en;\n bamm:properties (:x :y :z).\n :x a bamm:Property;\n bamm:name \"x\";\n bamm:preferredName \"x\"@en;\n bamm:description \"x coordinate in space\"@en;\n bamm:characteristic :Coordinate.\n :y a bamm:Property;\n bamm:name \"y\";\n bamm:preferredName \"y\"@en;\n bamm:description \"y coordinate in space\"@en;\n bamm:characteristic :Coordinate.\n :z a bamm:Property;\n bamm:name \"z\";\n bamm:preferredName \"z\"@en;\n bamm:description \"z coordinate in space\"@en;\n bamm:characteristic :Coordinate;\n bamm:optional \"true\"^^xsd:boolean.\n :Coordinate a bamm-c:Measurement;\n bamm:name \"Coordinate\";\n bamm:preferredName \"Coordinate\"@en;\n bamm:description \"Represents a coordinate along an axis in space.\"@en;\n bamm:dataType xsd:float;\n bamm-c:unit unit:metre.\n :SpatialPositionCharacteristic a bamm-c:SingleEntity;\n bamm:name \"SpatialPositionCharacteristic\";\n bamm:preferredName \"Spatial Position Characteristic\"@en;\n bamm:description \"Represents a single position in space with optional z coordinate.\"@en;\n bamm:dataType :SpatialPosition.";

      mvc.perform(post( insertModelJson ))
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( jsonPath( "$.error.details.ERR_PROCESSING", containsString(
               "Validation succeeded, but an error was found while processing the model. This indicates an error in the model validation; please consider reporting this issue including the model at https://github.com/OpenManufacturingPlatform/sds-bamm-aspect-meta-model/issues -- Could not load Aspect model, please make sure the model is valid: Could not load urn:bamm:org.eclipse.tractusx:1.0.0#Coordinate: Unknown type urn:bamm:io.openmanufacturing:characteristicX:1.0.0#Measurement" ) ) )
         .andExpect( status().is4xxClientError() );
   }

   @Test
   public void testGenerateJsonSchemaExpectSuccess() throws Exception {
      String urnPrefix = "urn:bamm:org.eclipse.tractusx.model.status.transition:2.0.0#";
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
               "{\"$schema\":\"http://json-schema.org/draft-04/schema\",\"type\":\"object\",\"components\":{\"schemas\":{\"urn_bamm_io.openmanufacturing_characteristic_2.0.0_Boolean\":{\"type\":\"boolean\"},\"urn_bamm_org.eclipse.tractusx.model.status.transition_2.0.0_WarningLevel\":{\"type\":\"string\",\"enum\":[\"green\",\"yellow\",\"red\"]},\"urn_bamm_org.eclipse.tractusx.model.status.transition_2.0.0_Coordinate\":{\"type\":\"number\"},\"urn_bamm_org.eclipse.tractusx.model.status.transition_2.0.0_SpatialPosition\":{\"type\":\"object\",\"properties\":{\"x\":{\"$ref\":\"#/components/schemas/urn_bamm_org.eclipse.tractusx.model.status.transition_2.0.0_Coordinate\"},\"y\":{\"$ref\":\"#/components/schemas/urn_bamm_org.eclipse.tractusx.model.status.transition_2.0.0_Coordinate\"},\"z\":{\"$ref\":\"#/components/schemas/urn_bamm_org.eclipse.tractusx.model.status.transition_2.0.0_Coordinate\"}},\"required\":[\"x\",\"y\"]}}},\"properties\":{\"moving\":{\"$ref\":\"#/components/schemas/urn_bamm_io.openmanufacturing_characteristic_2.0.0_Boolean\"},\"speedLimitWarning\":{\"$ref\":\"#/components/schemas/urn_bamm_org.eclipse.tractusx.model.status.transition_2.0.0_WarningLevel\"},\"position\":{\"$ref\":\"#/components/schemas/urn_bamm_org.eclipse.tractusx.model.status.transition_2.0.0_SpatialPosition\"}},\"required\":[\"moving\",\"speedLimitWarning\",\"position\"]}" ) )
         .andExpect( status().isOk() );
   }

   @Test
   public void testDeleteEndpointWithNotExistingModelExpectNotFound() throws Exception {
      mvc.perform(
               MockMvcRequestBuilders.delete(
                     "/api/v1/models/{urn}",
                     "urn:bamm:org.eclipse.tractusx.notexistingpackage:2.0.0#" )
                       .with(jwtTokenFactory.allRoles())
         )
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( status().isNotFound() );
   }

   @Test
   public void testGenerateOpenApiEndpointSpecExpectSuccess() throws Exception {
      String urnPrefix = "urn:bamm:org.eclipse.tractusx.testopenapi:1.0.0#";
      mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"DRAFT") )
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( status().isOk() );

      mvc.perform(
               MockMvcRequestBuilders.get( "/api/v1/models/{urn}/openapi?baseUrl=example.com",
                     toMovementUrn(urnPrefix) ).with(jwtTokenFactory.allRoles()))
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( status().isOk() )
         .andExpect( content().json(
               "{\"openapi\":\"3.0.3\",\"info\":{\"title\":\"Movement\",\"version\":\"v1.0.0\"},\"servers\":[{\"url\":\"example.com/api/v1.0.0\",\"variables\":{\"api-version\":{\"default\":\"v1.0.0\"}}}],\"paths\":{\"/{tenant-id}/movement\":{\"get\":{\"tags\":[\"Movement\"],\"operationId\":\"getMovement\",\"parameters\":[{\"name\":\"tenant-id\",\"in\":\"path\",\"description\":\"The ID of the tenant owning the requested Twin.\",\"required\":true,\"schema\":{\"type\":\"string\",\"format\":\"uuid\"}}],\"responses\":{\"200\":{\"$ref\":\"#/components/responses/Movement\"},\"401\":{\"$ref\":\"#/components/responses/ClientError\"},\"402\":{\"$ref\":\"#/components/responses/Unauthorized\"},\"403\":{\"$ref\":\"#/components/responses/Forbidden\"},\"404\":{\"$ref\":\"#/components/responses/NotFoundError\"}}}}},\"components\":{\"schemas\":{\"ErrorResponse\":{\"type\":\"object\",\"required\":[\"error\"],\"properties\":{\"error\":{\"$ref\":\"#/components/schemas/Error\"}}},\"Error\":{\"type\":\"object\",\"required\":[\"details\"],\"properties\":{\"message\":{\"type\":\"string\",\"minLength\":1},\"path\":{\"type\":\"string\",\"minLength\":1},\"details\":{\"type\":\"object\",\"minLength\":1,\"additionalProperties\":{\"type\":\"object\"}},\"code\":{\"type\":\"string\",\"nullable\":true}}},\"urn_bamm_io.openmanufacturing_characteristic_2.0.0_Boolean\":{\"type\":\"boolean\"},\"urn_bamm_org.eclipse.tractusx.testopenapi_1.0.0_WarningLevel\":{\"type\":\"string\",\"enum\":[\"green\",\"yellow\",\"red\"]},\"urn_bamm_org.eclipse.tractusx.testopenapi_1.0.0_Coordinate\":{\"type\":\"number\"},\"urn_bamm_org.eclipse.tractusx.testopenapi_1.0.0_SpatialPosition\":{\"type\":\"object\",\"properties\":{\"x\":{\"$ref\":\"#/components/schemas/urn_bamm_org.eclipse.tractusx.testopenapi_1.0.0_Coordinate\"},\"y\":{\"$ref\":\"#/components/schemas/urn_bamm_org.eclipse.tractusx.testopenapi_1.0.0_Coordinate\"},\"z\":{\"$ref\":\"#/components/schemas/urn_bamm_org.eclipse.tractusx.testopenapi_1.0.0_Coordinate\"}},\"required\":[\"x\",\"y\"]},\"Movement\":{\"type\":\"object\",\"properties\":{\"moving\":{\"$ref\":\"#/components/schemas/urn_bamm_io.openmanufacturing_characteristic_2.0.0_Boolean\"},\"speedLimitWarning\":{\"$ref\":\"#/components/schemas/urn_bamm_org.eclipse.tractusx.testopenapi_1.0.0_WarningLevel\"},\"position\":{\"$ref\":\"#/components/schemas/urn_bamm_org.eclipse.tractusx.testopenapi_1.0.0_SpatialPosition\"}},\"required\":[\"moving\",\"speedLimitWarning\",\"position\"]}},\"responses\":{\"Unauthorized\":{\"description\":\"The requesting user or client is not authenticated.\"},\"Forbidden\":{\"description\":\"The requesting user or client is not authorized to access resources for the given tenant.\"},\"NotFoundError\":{\"description\":\"The requested Twin has not been found.\"},\"ClientError\":{\"description\":\"Payload or user input is invalid. See error details in the payload for more.\",\"content\":{\"application/json\":{\"schema\":{\"$ref\":\"#/components/schemas/ErrorResponse\"}}}},\"Movement\":{\"content\":{\"application/json\":{\"schema\":{\"$ref\":\"#/components/schemas/Movement\"}}},\"description\":\"The request was successful.\"}},\"requestBodies\":{\"Movement\":{\"content\":{\"application/json\":{\"schema\":{\"$ref\":\"#/components/schemas/Movement\"}}}}}}}" ) )
      ;
   }

  @Test
  public void testAasxEndpointExpectSuccess() throws Exception {
    String urnPrefix = "urn:bamm:org.eclipse.tractusx.testaas:1.0.0#";
    mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"DRAFT") )
        .andDo( MockMvcResultHandlers.print() )
        .andExpect( status().isOk() );

    mvc.perform(MockMvcRequestBuilders.get("/api/v1/models/{urn}/aas?aasFormat=XML", toMovementUrn(urnPrefix)).with(jwtTokenFactory.allRoles()))
      .andDo( MockMvcResultHandlers.print())
      .andExpect( status().isOk() )
      .andExpect(content().xml("<?xml version='1.0' encoding='UTF-8'?>\n<aas:aasenv xmlns:aas=\"http://www.admin-shell.io/aas/3/0\" xmlns:IEC61360=\"http://www.admin-shell.io/IEC61360/3/0\" xmlns:abac=\"http://www.admin-shell.io/aas/abac/3/0\" xmlns:aas_common=\"http://www.admin-shell.io/aas_common/3/0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.admin-shell.io/aas/3/0 AAS.xsd http://www.admin-shell.io/IEC61360/3/0 IEC61360.xsd http://www.admin-shell.io/aas/abac/3/0 AAS_ABAC.xsd\">\n  <aas:assetAdministrationShells>\n    <aas:assetAdministrationShell>\n      <aas:idShort>defaultAdminShell</aas:idShort>\n    </aas:assetAdministrationShell>\n  </aas:assetAdministrationShells>\n  <aas:assets>\n    <aas:asset>\n      <aas:idShort>defaultAsset</aas:idShort>\n    </aas:asset>\n  </aas:assets>\n  <aas:conceptDescriptions>\n    <aas:conceptDescription>\n      <aas:idShort>Movement</aas:idShort>\n      <aas:displayName>\n        <aas:langString lang=\"en\">Movement</aas:langString>\n      </aas:displayName>\n      <aas:category>APPLICATION_CLASS</aas:category>\n      <aas:description>\n        <aas:langString lang=\"en\">Aspect for movement information</aas:langString>\n      </aas:description>\n      <aas:identification idType=\"Custom\">urn:bamm:org.eclipse.tractusx.testaas:1.0.0#Movement</aas:identification>\n      <aas:embeddedDataSpecification>\n        <aas:dataSpecificationContent>\n          <aas:dataSpecificationIEC61360>\n            <IEC61360:preferredName>\n              <IEC61360:langString lang=\"en\">Movement</IEC61360:langString>\n            </IEC61360:preferredName>\n            <IEC61360:shortName>\n              <IEC61360:langString lang=\"EN\">Movement</IEC61360:langString>\n            </IEC61360:shortName>\n            <IEC61360:definition>\n              <IEC61360:langString lang=\"en\">Aspect for movement information</IEC61360:langString>\n            </IEC61360:definition>\n          </aas:dataSpecificationIEC61360>\n        </aas:dataSpecificationContent>\n        <aas:dataSpecification>\n          <aas:keys>\n            <aas:key idType=\"Iri\" type=\"GlobalReference\">http://admin-shell.io/DataSpecificationTemplates/DataSpecificationIEC61360/2/0</aas:key>\n          </aas:keys>\n        </aas:dataSpecification>\n      </aas:embeddedDataSpecification>\n    </aas:conceptDescription>\n    <aas:conceptDescription>\n      <aas:idShort>Boolean</aas:idShort>\n      <aas:displayName>\n        <aas:langString lang=\"en\">Boolean</aas:langString>\n      </aas:displayName>\n      <aas:identification idType=\"Custom\">urn:bamm:org.eclipse.tractusx.testaas:1.0.0#moving</aas:identification>\n      <aas:embeddedDataSpecification>\n        <aas:dataSpecificationContent>\n          <aas:dataSpecificationIEC61360>\n            <IEC61360:preferredName>\n              <IEC61360:langString lang=\"en\">Moving</IEC61360:langString>\n            </IEC61360:preferredName>\n            <IEC61360:shortName>\n              <IEC61360:langString lang=\"EN\">moving</IEC61360:langString>\n            </IEC61360:shortName>\n            <IEC61360:dataType>Boolean</IEC61360:dataType>\n            <IEC61360:definition>\n              <IEC61360:langString lang=\"en\">Represents a boolean value (i.e. a \"flag\").</IEC61360:langString>\n            </IEC61360:definition>\n          </aas:dataSpecificationIEC61360>\n        </aas:dataSpecificationContent>\n        <aas:dataSpecification>\n          <aas:keys>\n            <aas:key idType=\"Iri\" type=\"GlobalReference\">http://admin-shell.io/DataSpecificationTemplates/DataSpecificationIEC61360/2/0</aas:key>\n          </aas:keys>\n        </aas:dataSpecification>\n      </aas:embeddedDataSpecification>\n    </aas:conceptDescription>\n    <aas:conceptDescription>\n      <aas:idShort>WarningLevel</aas:idShort>\n      <aas:displayName>\n        <aas:langString lang=\"en\">Warning Level</aas:langString>\n      </aas:displayName>\n      <aas:identification idType=\"Custom\">urn:bamm:org.eclipse.tractusx.testaas:1.0.0#speedLimitWarning</aas:identification>\n      <aas:embeddedDataSpecification>\n        <aas:dataSpecificationContent>\n          <aas:dataSpecificationIEC61360>\n            <IEC61360:preferredName>\n              <IEC61360:langString lang=\"en\">Speed Limit Warning</IEC61360:langString>\n            </IEC61360:preferredName>\n            <IEC61360:shortName>\n              <IEC61360:langString lang=\"EN\">speedLimitWarning</IEC61360:langString>\n            </IEC61360:shortName>\n            <IEC61360:dataType>String</IEC61360:dataType>\n            <IEC61360:definition>\n              <IEC61360:langString lang=\"en\">Represents if speed of position change is within specification (green), within tolerance (yellow), or outside specification (red).</IEC61360:langString>\n            </IEC61360:definition>\n            <IEC61360:valueList>\n              <IEC61360:valueReferencePair>\n                <IEC61360:valueId>\n                  <IEC61360:keys>\n                    <IEC61360:key idType=\"Custom\" type=\"DataElement\">urn:bamm:org.eclipse.tractusx.testaas:1.0.0#WarningLevel:DefaultScalarValue[value=green, typeUri='DefaultScalar[metaModelVersion=BAMM_2_0_0, urn='http://www.w3.org/2001/XMLSchema#string']']</IEC61360:key>\n                  </IEC61360:keys>\n                </IEC61360:valueId>\n                <IEC61360:value>DefaultScalarValue[value=green, typeUri='DefaultScalar[metaModelVersion=BAMM_2_0_0, urn='http://www.w3.org/2001/XMLSchema#string']']</IEC61360:value>\n              </IEC61360:valueReferencePair>\n              <IEC61360:valueReferencePair>\n                <IEC61360:valueId>\n                  <IEC61360:keys>\n                    <IEC61360:key idType=\"Custom\" type=\"DataElement\">urn:bamm:org.eclipse.tractusx.testaas:1.0.0#WarningLevel:DefaultScalarValue[value=yellow, typeUri='DefaultScalar[metaModelVersion=BAMM_2_0_0, urn='http://www.w3.org/2001/XMLSchema#string']']</IEC61360:key>\n                  </IEC61360:keys>\n                </IEC61360:valueId>\n                <IEC61360:value>DefaultScalarValue[value=yellow, typeUri='DefaultScalar[metaModelVersion=BAMM_2_0_0, urn='http://www.w3.org/2001/XMLSchema#string']']</IEC61360:value>\n              </IEC61360:valueReferencePair>\n              <IEC61360:valueReferencePair>\n                <IEC61360:valueId>\n                  <IEC61360:keys>\n                    <IEC61360:key idType=\"Custom\" type=\"DataElement\">urn:bamm:org.eclipse.tractusx.testaas:1.0.0#WarningLevel:DefaultScalarValue[value=red, typeUri='DefaultScalar[metaModelVersion=BAMM_2_0_0, urn='http://www.w3.org/2001/XMLSchema#string']']</IEC61360:key>\n                  </IEC61360:keys>\n                </IEC61360:valueId>\n                <IEC61360:value>DefaultScalarValue[value=red, typeUri='DefaultScalar[metaModelVersion=BAMM_2_0_0, urn='http://www.w3.org/2001/XMLSchema#string']']</IEC61360:value>\n              </IEC61360:valueReferencePair>\n            </IEC61360:valueList>\n          </aas:dataSpecificationIEC61360>\n        </aas:dataSpecificationContent>\n        <aas:dataSpecification>\n          <aas:keys>\n            <aas:key idType=\"Iri\" type=\"GlobalReference\">http://admin-shell.io/DataSpecificationTemplates/DataSpecificationIEC61360/2/0</aas:key>\n          </aas:keys>\n        </aas:dataSpecification>\n      </aas:embeddedDataSpecification>\n    </aas:conceptDescription>\n    <aas:conceptDescription>\n      <aas:idShort>Coordinate</aas:idShort>\n      <aas:displayName>\n        <aas:langString lang=\"en\">Coordinate</aas:langString>\n      </aas:displayName>\n      <aas:identification idType=\"Custom\">urn:bamm:org.eclipse.tractusx.testaas:1.0.0#x</aas:identification>\n      <aas:embeddedDataSpecification>\n        <aas:dataSpecificationContent>\n          <aas:dataSpecificationIEC61360>\n            <IEC61360:preferredName>\n              <IEC61360:langString lang=\"en\">x</IEC61360:langString>\n            </IEC61360:preferredName>\n            <IEC61360:shortName>\n              <IEC61360:langString lang=\"EN\">x</IEC61360:langString>\n            </IEC61360:shortName>\n            <IEC61360:unit>metre</IEC61360:unit>\n            <IEC61360:dataType>RealMeasure</IEC61360:dataType>\n            <IEC61360:definition>\n              <IEC61360:langString lang=\"en\">Represents a coordinate along an axis in space.</IEC61360:langString>\n            </IEC61360:definition>\n          </aas:dataSpecificationIEC61360>\n        </aas:dataSpecificationContent>\n        <aas:dataSpecification>\n          <aas:keys>\n            <aas:key idType=\"Iri\" type=\"GlobalReference\">http://admin-shell.io/DataSpecificationTemplates/DataSpecificationIEC61360/2/0</aas:key>\n          </aas:keys>\n        </aas:dataSpecification>\n      </aas:embeddedDataSpecification>\n    </aas:conceptDescription>\n    <aas:conceptDescription>\n      <aas:idShort>Coordinate</aas:idShort>\n      <aas:displayName>\n        <aas:langString lang=\"en\">Coordinate</aas:langString>\n      </aas:displayName>\n      <aas:identification idType=\"Custom\">urn:bamm:org.eclipse.tractusx.testaas:1.0.0#y</aas:identification>\n      <aas:embeddedDataSpecification>\n        <aas:dataSpecificationContent>\n          <aas:dataSpecificationIEC61360>\n            <IEC61360:preferredName>\n              <IEC61360:langString lang=\"en\">y</IEC61360:langString>\n            </IEC61360:preferredName>\n            <IEC61360:shortName>\n              <IEC61360:langString lang=\"EN\">y</IEC61360:langString>\n            </IEC61360:shortName>\n            <IEC61360:unit>metre</IEC61360:unit>\n            <IEC61360:dataType>RealMeasure</IEC61360:dataType>\n            <IEC61360:definition>\n              <IEC61360:langString lang=\"en\">Represents a coordinate along an axis in space.</IEC61360:langString>\n            </IEC61360:definition>\n          </aas:dataSpecificationIEC61360>\n        </aas:dataSpecificationContent>\n        <aas:dataSpecification>\n          <aas:keys>\n            <aas:key idType=\"Iri\" type=\"GlobalReference\">http://admin-shell.io/DataSpecificationTemplates/DataSpecificationIEC61360/2/0</aas:key>\n          </aas:keys>\n        </aas:dataSpecification>\n      </aas:embeddedDataSpecification>\n    </aas:conceptDescription>\n    <aas:conceptDescription>\n      <aas:idShort>Coordinate</aas:idShort>\n      <aas:displayName>\n        <aas:langString lang=\"en\">Coordinate</aas:langString>\n      </aas:displayName>\n      <aas:identification idType=\"Custom\">urn:bamm:org.eclipse.tractusx.testaas:1.0.0#z</aas:identification>\n      <aas:embeddedDataSpecification>\n        <aas:dataSpecificationContent>\n          <aas:dataSpecificationIEC61360>\n            <IEC61360:preferredName>\n              <IEC61360:langString lang=\"en\">z</IEC61360:langString>\n            </IEC61360:preferredName>\n            <IEC61360:shortName>\n              <IEC61360:langString lang=\"EN\">z</IEC61360:langString>\n            </IEC61360:shortName>\n            <IEC61360:unit>metre</IEC61360:unit>\n            <IEC61360:dataType>RealMeasure</IEC61360:dataType>\n            <IEC61360:definition>\n              <IEC61360:langString lang=\"en\">Represents a coordinate along an axis in space.</IEC61360:langString>\n            </IEC61360:definition>\n          </aas:dataSpecificationIEC61360>\n        </aas:dataSpecificationContent>\n        <aas:dataSpecification>\n          <aas:keys>\n            <aas:key idType=\"Iri\" type=\"GlobalReference\">http://admin-shell.io/DataSpecificationTemplates/DataSpecificationIEC61360/2/0</aas:key>\n          </aas:keys>\n        </aas:dataSpecification>\n      </aas:embeddedDataSpecification>\n    </aas:conceptDescription>\n    <aas:conceptDescription>\n      <aas:idShort>PositionSingleEntity</aas:idShort>\n      <aas:displayName>\n        <aas:langString lang=\"en\">Spatial Position Characteristic</aas:langString>\n      </aas:displayName>\n      <aas:identification idType=\"Custom\">urn:bamm:org.eclipse.tractusx.testaas:1.0.0#position</aas:identification>\n      <aas:embeddedDataSpecification>\n        <aas:dataSpecificationContent>\n          <aas:dataSpecificationIEC61360>\n            <IEC61360:preferredName>\n              <IEC61360:langString lang=\"en\">Position</IEC61360:langString>\n            </IEC61360:preferredName>\n            <IEC61360:shortName>\n              <IEC61360:langString lang=\"EN\">position</IEC61360:langString>\n            </IEC61360:shortName>\n            <IEC61360:dataType>String</IEC61360:dataType>\n            <IEC61360:definition>\n              <IEC61360:langString lang=\"en\">Represents a single location in space.</IEC61360:langString>\n            </IEC61360:definition>\n          </aas:dataSpecificationIEC61360>\n        </aas:dataSpecificationContent>\n        <aas:dataSpecification>\n          <aas:keys>\n            <aas:key idType=\"Iri\" type=\"GlobalReference\">http://admin-shell.io/DataSpecificationTemplates/DataSpecificationIEC61360/2/0</aas:key>\n          </aas:keys>\n        </aas:dataSpecification>\n      </aas:embeddedDataSpecification>\n    </aas:conceptDescription>\n  </aas:conceptDescriptions>\n  <aas:submodels>\n    <aas:submodel>\n      <aas:idShort>Movement</aas:idShort>\n      <aas:description>\n        <aas:langString lang=\"en\">Aspect for movement information</aas:langString>\n      </aas:description>\n      <aas:kind>Template</aas:kind>\n      <aas:semanticId>\n        <aas:keys>\n          <aas:key idType=\"Custom\" type=\"ConceptDescription\">urn:bamm:org.eclipse.tractusx.testaas:1.0.0#Movement</aas:key>\n        </aas:keys>\n      </aas:semanticId>\n      <aas:submodelElements>\n        <aas:submodelElement>\n          <aas:property>\n            <aas:idShort>moving</aas:idShort>\n            <aas:displayName>\n              <aas:langString lang=\"en\">Moving</aas:langString>\n            </aas:displayName>\n            <aas:description>\n              <aas:langString lang=\"en\">Flag indicating if the position is changing</aas:langString>\n            </aas:description>\n            <aas:kind>Template</aas:kind>\n            <aas:semanticId>\n              <aas:keys>\n                <aas:key idType=\"Custom\" type=\"ConceptDescription\">urn:bamm:org.eclipse.tractusx.testaas:1.0.0#moving</aas:key>\n              </aas:keys>\n            </aas:semanticId>\n            <aas:value>Unknown</aas:value>\n            <aas:valueType>http://www.w3.org/2001/XMLSchema#boolean</aas:valueType>\n          </aas:property>\n        </aas:submodelElement>\n        <aas:submodelElement>\n          <aas:property>\n            <aas:idShort>speedLimitWarning</aas:idShort>\n            <aas:displayName>\n              <aas:langString lang=\"en\">Speed Limit Warning</aas:langString>\n            </aas:displayName>\n            <aas:description>\n              <aas:langString lang=\"en\">Indicats if speed limit is adhered to.</aas:langString>\n            </aas:description>\n            <aas:kind>Template</aas:kind>\n            <aas:semanticId>\n              <aas:keys>\n                <aas:key idType=\"Custom\" type=\"ConceptDescription\">urn:bamm:org.eclipse.tractusx.testaas:1.0.0#speedLimitWarning</aas:key>\n              </aas:keys>\n            </aas:semanticId>\n            <aas:value>Unknown</aas:value>\n            <aas:valueType>http://www.w3.org/2001/XMLSchema#string</aas:valueType>\n          </aas:property>\n        </aas:submodelElement>\n        <aas:submodelElement>\n          <aas:submodelElementCollection>\n            <aas:idShort>SpatialPosition</aas:idShort>\n            <aas:displayName>\n              <aas:langString lang=\"en\">Spatial Position</aas:langString>\n            </aas:displayName>\n            <aas:description>\n              <aas:langString lang=\"en\">Position in space, described along three axis, with the third axis optional, if all positions are in a plane.</aas:langString>\n            </aas:description>\n            <aas:allowDuplicates>false</aas:allowDuplicates>\n            <aas:ordered>false</aas:ordered>\n            <aas:value>\n              <aas:submodelElement>\n                <aas:property>\n                  <aas:idShort>x</aas:idShort>\n                  <aas:displayName>\n                    <aas:langString lang=\"en\">x</aas:langString>\n                  </aas:displayName>\n                  <aas:description>\n                    <aas:langString lang=\"en\">x coordinate in space</aas:langString>\n                  </aas:description>\n                  <aas:kind>Template</aas:kind>\n                  <aas:semanticId>\n                    <aas:keys>\n                      <aas:key idType=\"Custom\" type=\"ConceptDescription\">urn:bamm:org.eclipse.tractusx.testaas:1.0.0#x</aas:key>\n                    </aas:keys>\n                  </aas:semanticId>\n                  <aas:value>Unknown</aas:value>\n                  <aas:valueType>http://www.w3.org/2001/XMLSchema#float</aas:valueType>\n                </aas:property>\n              </aas:submodelElement>\n              <aas:submodelElement>\n                <aas:property>\n                  <aas:idShort>y</aas:idShort>\n                  <aas:displayName>\n                    <aas:langString lang=\"en\">y</aas:langString>\n                  </aas:displayName>\n                  <aas:description>\n                    <aas:langString lang=\"en\">y coordinate in space</aas:langString>\n                  </aas:description>\n                  <aas:kind>Template</aas:kind>\n                  <aas:semanticId>\n                    <aas:keys>\n                      <aas:key idType=\"Custom\" type=\"ConceptDescription\">urn:bamm:org.eclipse.tractusx.testaas:1.0.0#y</aas:key>\n                    </aas:keys>\n                  </aas:semanticId>\n                  <aas:value>Unknown</aas:value>\n                  <aas:valueType>http://www.w3.org/2001/XMLSchema#float</aas:valueType>\n                </aas:property>\n              </aas:submodelElement>\n              <aas:submodelElement>\n                <aas:property>\n                  <aas:idShort>z</aas:idShort>\n                  <aas:displayName>\n                    <aas:langString lang=\"en\">z</aas:langString>\n                  </aas:displayName>\n                  <aas:description>\n                    <aas:langString lang=\"en\">z coordinate in space</aas:langString>\n                  </aas:description>\n                  <aas:kind>Template</aas:kind>\n                  <aas:semanticId>\n                    <aas:keys>\n                      <aas:key idType=\"Custom\" type=\"ConceptDescription\">urn:bamm:org.eclipse.tractusx.testaas:1.0.0#z</aas:key>\n                    </aas:keys>\n                  </aas:semanticId>\n                  <aas:value>Unknown</aas:value>\n                  <aas:valueType>http://www.w3.org/2001/XMLSchema#float</aas:valueType>\n                </aas:property>\n              </aas:submodelElement>\n            </aas:value>\n          </aas:submodelElementCollection>\n        </aas:submodelElement>\n      </aas:submodelElements>\n    </aas:submodel>\n  </aas:submodels>\n</aas:aasenv>"));

    mvc.perform(MockMvcRequestBuilders.get("/api/v1/models/{urn}/aas?aasFormat=FILE", toMovementUrn(urnPrefix)).with(jwtTokenFactory.allRoles()))
        .andDo( MockMvcResultHandlers.print())
        .andExpect( header().string("Content-Type", "application/octet-stream") )
        .andExpect( status().isOk() );
  }

    @Test
    public void testAasxEndpointExpectError() throws Exception {
        String urnPrefix = "urn:bamm:org.eclipse.tractusx.testaas:2.0.0#";
        mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"DRAFT") )
            .andDo( MockMvcResultHandlers.print() )
            .andExpect( status().isOk() );

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/models/{urn}/aas?aasFormat=WRONG_FORMAT", toMovementUrn(urnPrefix)).with(jwtTokenFactory.allRoles()))
        .andDo( MockMvcResultHandlers.print())
        .andExpect( status().is4xxClientError() );
    }

   @Test
   public void testExampleGenerateExamplePayloadJsonExpectSuccess() throws Exception {
      String urnPrefix = "urn:bamm:org.eclipse.tractusx.testjsonschema:2.0.0#";
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
               containsString( "urn:bamm:org.eclipse.tractusx.traceability:0.1.1#PartDataCharacteristic" ) ) );

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
                     "urn:bamm:org.eclipse.tractusx.modelwithreferencetotraceability:0.1.1#ModelWithReferenceToTraceability" )
                       .with(jwtTokenFactory.allRoles())
         )
         .andDo( MockMvcResultHandlers.print() )
         .andExpect( jsonPath( "$.staticData" ).exists() )
         .andExpect( jsonPath( "$.staticData.customerId" ).exists() )
         .andExpect( status().isOk() );

      // verify that the turtle file contains a complete resolved model
      String traceabilityBaseUrn = "urn:bamm:org.eclipse.tractusx.traceability:0.1.1";
      String modelExtBaseUrn = "urn:bamm:org.eclipse.tractusx.modelwithreferencetotraceability:0.1.1";
      mvc.perform(
               MockMvcRequestBuilders.get( "/api/v1/models/{urn}/file",
                     "urn:bamm:org.eclipse.tractusx.modelwithreferencetotraceability:0.1.1#ModelWithReferenceToTraceability" )
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
         String urnPrefix = "urn:bamm:org.eclipse.tractusx.model.status.transition.post:2.0.0#";

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

         // transition from draft to release is allowed, delete is not allowed
         mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"RELEASED") )
                 .andDo( MockMvcResultHandlers.print() )
                 .andExpect(status().isOk());

         mvc.perform(delete(urnPrefix))
                 .andDo( MockMvcResultHandlers.print() )
                 .andExpect(status().isBadRequest())
                 .andExpect( jsonPath( "$.error.message", is(
                         "The package urn:bamm:org.eclipse.tractusx.model.status.transition.post:2.0.0# is already in status RELEASED and cannot be deleted." ) ) );

         // transition from released to draft is not allowed
         mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"DRAFT") )
                 .andExpect( jsonPath( "$.error.message", is(
                         "The package urn:bamm:org.eclipse.tractusx.model.status.transition.post:2.0.0# is already in status RELEASED and cannot be modified. Only a transition to DEPRECATED is possible." ) ) )
                 .andExpect( status().isBadRequest() );

         // transition from released to deprecated is allowed
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
         String urnPrefix = "urn:bamm:org.eclipse.tractusx.model.status.transition.put:2.0.0#";

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
                         "The package urn:bamm:org.eclipse.tractusx.model.status.transition.put:2.0.0# is already in status RELEASED and cannot be deleted." ) ) );

         // transition from released to draft is not allowed
         mvc.perform(put( TestUtils.createValidModelRequest(urnPrefix), "DRAFT") )
                 .andExpect( jsonPath( "$.error.message", is(
                         "The package urn:bamm:org.eclipse.tractusx.model.status.transition.put:2.0.0# is already in status RELEASED and cannot be modified. Only a transition to DEPRECATED is possible." ) ) )
                 .andExpect( status().isBadRequest() );

         // transition from released to deprecated is allowed
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
        String urnPrefix = "urn:bamm:org.eclipse.tractusx.model.status.transitionWithDependency:1.0.0#";

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
        String urnPrefixPattern = "urn:bamm:org.eclipse.tractusx.test_model_list_by_urns_%s:1.0.0#";

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

        urnSearchArrayNonExistingEntry.add("urn:bamm:org.eclipse.tractusx.test_model_list_by_urns_50:1.0.0#Movement");

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
        .andExpect(jsonPath("$.items[0].urn", equalTo("urn:bamm:org.eclipse.tractusx.test_model_list_by_urns_10:1.0.0#Movement")))
        .andExpect(jsonPath("$.items[1].urn", equalTo("urn:bamm:org.eclipse.tractusx.test_model_list_by_urns_2:1.0.0#Movement")))
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
        .andExpect(jsonPath("$.items[0].urn", equalTo("urn:bamm:org.eclipse.tractusx.test_model_list_by_urns_3:1.0.0#Movement")))
        .andExpect(jsonPath("$.items[1].urn", equalTo("urn:bamm:org.eclipse.tractusx.test_model_list_by_urns_5:1.0.0#Movement")))
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
