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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

@TestInstance( TestInstance.Lifecycle.PER_CLASS )
@DirtiesContext( classMode = DirtiesContext.ClassMode.AFTER_CLASS )
public class ModelsApiTest extends AbstractModelsApiTest{

    @BeforeEach
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
         .andExpect( jsonPath( "$.error.message", containsString("Validation failed" ) ) )
            .andExpect( jsonPath( "$.error.details.ERR_NO_TYPE", containsString(
                  "Could not determine type of bamm-c:Boolean." ) ) )
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
         .andExpect(jsonPath( "$.description", containsString( "Aspect for movement information" )))
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
            .andExpect(jsonPath( "$.info.title", containsString( "Movement" )));
   }

  @Test
  public void testAasxEndpointExpectSuccess() throws Exception {
    String urnPrefix = "urn:samm:org.eclipse.tractusx.testaas:1.0.0#";
    mvc.perform(post( TestUtils.createValidModelRequest(urnPrefix),"DRAFT") )
        .andDo( MockMvcResultHandlers.print() )
        .andExpect( status().isOk() );

     mvc.perform( MockMvcRequestBuilders.get( "/api/v1/models/{urn}/aas?aasFormat=XML", toMovementUrn( urnPrefix ) ).with( jwtTokenFactory.allRoles() ) )
           .andDo( MockMvcResultHandlers.print() )
           .andExpect( status().isOk() )
           .andExpect( content().string( containsString( "urn:samm:org.eclipse.tractusx.testaas:1.0.0#Movement" ) ) );


    mvc.perform(MockMvcRequestBuilders.get("/api/v1/models/{urn}/aas?aasFormat=FILE", toMovementUrn(urnPrefix)).with(jwtTokenFactory.allRoles()))
        .andDo( MockMvcResultHandlers.print())
        .andExpect( header().string("Content-Type", "application/octet-stream") )
        .andExpect( status().isOk() );

     mvc.perform( MockMvcRequestBuilders.get( "/api/v1/models/{urn}/aas?aasFormat=JSON", toMovementUrn( urnPrefix ) ).with( jwtTokenFactory.allRoles() ) )
           .andDo( MockMvcResultHandlers.print() )
           .andExpect( status().isOk() )
           .andExpect( content().string( containsString( "urn:samm:org.eclipse.tractusx.testaas:1.0.0#Movement" ) ) );
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
         .andExpect( jsonPath( "$.error.details.ERR_PROCESSING",
               containsString( "definition for urn:samm:org.eclipse.tractusx.traceability:0.1.1#PartDataCharacteristic not found" ) ) );

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
   @DisplayName("State Transition of Models for SAMM")
   public class StateTransitionTestsForSAMM extends AbstractModelsApiTest{

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

   @Test
   public void testGetModelsExpectSuccessForBAMM() throws Exception {
      String urnPrefix = "urn:bamm:org.eclipse.tractusx:1.0.0#";
      mvc.perform(
                  postBAMM( TestUtils.createValidModelRequestBAMM(urnPrefix),"DRAFT")
            )
            .andDo( MockMvcResultHandlers.print() )
            .andExpect( status().isOk() );

      MvcResult res = mvc.perform(
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
            .andExpect( status().isOk() )
            .andReturn();
   }

   @Test
   public void testSaveValidModelExpectSuccessForBAMM() throws Exception {
      String urnPrefix = "urn:bamm:org.eclipse.tractusx.valid.save:2.0.0#";
      mvc.perform(
                  post( TestUtils.createValidModelRequestBAMM(urnPrefix),"RELEASED")
            )
            .andDo( MockMvcResultHandlers.print() )
            .andExpect( jsonPath( "$.urn", is( toMovementUrn(urnPrefix) ) ) )
            .andExpect( jsonPath( "$.version", is( "2.0.0" ) ) )
            .andExpect( jsonPath( "$.name", is( "Movement" ) ) )
            .andExpect( jsonPath( "$.type", is( "BAMM" ) ) )
            .andExpect( jsonPath( "$.status", is( "RELEASED" ) ) )
            .andExpect( status().isOk() );
   }

   @Test
   public void testSaveInvalidModelExpectSuccessForBAMM() throws Exception {
      String insertModelJson = "@prefix bamm: <urn:bamm:io.openmanufacturing:meta-model:1.0.0#> .\n @prefix bamm-c: <urn:bamm:io.openmanufacturing:characteristicX:1.0.0#> .\n @prefix bamm-e: <urn:bamm:io.openmanufacturing:entity:1.0.0#> .\n @prefix unit: <urn:bamm:io.openmanufacturing:unit:1.0.0#> .\n @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n @prefix : <urn:bamm:org.eclipse.tractusx:1.0.0#> .\n \n :Movement a bamm:Aspect;\n bamm:name \"Movement\";\n bamm:preferredName \"Movement\"@en;\n bamm:description \"Aspect for movement information\"@en;\n bamm:propertiesX (:isMoving :speedLimitWarning :position);\n bamm:operations ().\n :isMoving a bamm:Property;\n bamm:name \"isMoving\";\n bamm:preferredName \"Moving\"@en;\n bamm:description \"Flag indicating whether the asset is currently moving\"@en;\n bamm:characteristic bamm-c:Boolean.\n :speedLimitWarning a bamm:Property;\n bamm:name \"speedLimitWarning\";\n bamm:preferredName \"Speed Limit Warning\"@en;\n bamm:description \"Indicates if the speed limit is adhered to.\"@en;\n bamm:characteristic :TrafficLight.\n :position a bamm:Property;\n bamm:name \"position\";\n bamm:preferredName \"Position\"@en;\n bamm:description \"Indicates a position\"@en;\n bamm:characteristic :SpatialPositionCharacteristic.\n :TrafficLight a bamm-c:Enumeration;\n bamm:name \"TrafficLight\";\n bamm:preferredName \"Warning Level\"@en;\n bamm:description \"Represents if speed of position change is within specification (green), within tolerance (yellow), or outside specification (red).\"@en;\n bamm:dataType xsd:string;\n bamm-c:values (\"green\" \"yellow\" \"red\").\n :SpatialPosition a bamm:Entity;\n bamm:name \"SpatialPosition\";\n bamm:preferredName \"Spatial Position\"@en;\n bamm:description \"Position in space, described along three axis, with the third axis optional, if all positions are in a plane.\"@en;\n bamm:properties (:x :y :z).\n :x a bamm:Property;\n bamm:name \"x\";\n bamm:preferredName \"x\"@en;\n bamm:description \"x coordinate in space\"@en;\n bamm:characteristic :Coordinate.\n :y a bamm:Property;\n bamm:name \"y\";\n bamm:preferredName \"y\"@en;\n bamm:description \"y coordinate in space\"@en;\n bamm:characteristic :Coordinate.\n :z a bamm:Property;\n bamm:name \"z\";\n bamm:preferredName \"z\"@en;\n bamm:description \"z coordinate in space\"@en;\n bamm:characteristic :Coordinate;\n bamm:optional \"true\"^^xsd:boolean.\n :Coordinate a bamm-c:Measurement;\n bamm:name \"Coordinate\";\n bamm:preferredName \"Coordinate\"@en;\n bamm:description \"Represents a coordinate along an axis in space.\"@en;\n bamm:dataType xsd:float;\n bamm-c:unit unit:metre.\n :SpatialPositionCharacteristic a bamm-c:SingleEntity;\n bamm:name \"SpatialPositionCharacteristic\";\n bamm:preferredName \"Spatial Position Characteristic\"@en;\n bamm:description \"Represents a single position in space with optional z coordinate.\"@en;\n bamm:dataType :SpatialPosition.";

      mvc.perform(post( insertModelJson ))
            .andDo( MockMvcResultHandlers.print() )
            .andExpect( jsonPath( "$.error.details.ERR_NO_TYPE", containsString("Could not determine type of urn:samm:io.openmanufacturing:characteristicX:1.0.0#Boolean" ) ) )
            .andExpect( status().is4xxClientError() );
   }

   @Test
   public void testGenerateJsonSchemaExpectSuccessForBAMM() throws Exception {
      String urnPrefix = "urn:bamm:org.eclipse.tractusx.model.status.transition:2.0.0#";
      mvc.perform(
                  post( TestUtils.createValidModelRequestBAMM(urnPrefix),"RELEASED")
            )
            .andDo( MockMvcResultHandlers.print() )
            .andExpect( status().isOk() );

      mvc.perform(
                  MockMvcRequestBuilders.get(
                              "/api/v1/models/{urn}/json-schema", toMovementUrn(urnPrefix))
                        .with(jwtTokenFactory.allRoles())
            )
            .andDo( MockMvcResultHandlers.print() )
            .andExpect( jsonPath( "$.description", containsString("Aspect for movement information" ) ) )
            .andExpect( status().isOk() );
   }

   @Test
   public void testDeleteEndpointWithNotExistingModelExpectNotFoundForBAMM() throws Exception {
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
   public void testGenerateOpenApiEndpointSpecExpectSuccessForBAMM() throws Exception {
      String urnPrefix = "urn:bamm:org.eclipse.tractusx.testopenapi:1.0.0#";
      mvc.perform(post( TestUtils.createValidModelRequestBAMM(urnPrefix),"DRAFT") )
            .andDo( MockMvcResultHandlers.print() )
            .andExpect( status().isOk() );

      mvc.perform(
                  MockMvcRequestBuilders.get( "/api/v1/models/{urn}/openapi?baseUrl=example.com",
                        toMovementUrn(urnPrefix) ).with(jwtTokenFactory.allRoles()))
            .andDo( MockMvcResultHandlers.print() )
            .andExpect( status().isOk() )
            .andExpect( jsonPath( "$.info.title", containsString("Movement" ) ) );
      ;
   }

   @Test
   public void testAasxEndpointExpectSuccessForBAMM() throws Exception {
      String urnPrefix = "urn:bamm:org.eclipse.tractusx.testaas:1.0.0#";
      mvc.perform(post( TestUtils.createValidModelRequestBAMM(urnPrefix),"DRAFT") )
            .andDo( MockMvcResultHandlers.print() )
            .andExpect( status().isOk() );

      mvc.perform(MockMvcRequestBuilders.get("/api/v1/models/{urn}/aas?aasFormat=XML", toMovementUrn(urnPrefix)).with(jwtTokenFactory.allRoles()))
            .andDo( MockMvcResultHandlers.print())
            .andExpect( status().isOk() )
            .andExpect( content().string( containsString( "urn:samm:org.eclipse.tractusx.testaas:1.0.0#Movement" ) ) );

      mvc.perform(MockMvcRequestBuilders.get("/api/v1/models/{urn}/aas?aasFormat=FILE", toMovementUrn(urnPrefix)).with(jwtTokenFactory.allRoles()))
            .andDo( MockMvcResultHandlers.print())
            .andExpect( header().string("Content-Type", "application/octet-stream") )
            .andExpect( status().isOk() );
   }

   @Test
   public void testAasxEndpointExpectErrorForBAMM() throws Exception {
      String urnPrefix = "urn:bamm:org.eclipse.tractusx.testaas:2.0.0#";
      mvc.perform(post( TestUtils.createValidModelRequestBAMM(urnPrefix),"DRAFT") )
            .andDo( MockMvcResultHandlers.print() )
            .andExpect( status().isOk() );

      mvc.perform(MockMvcRequestBuilders.get("/api/v1/models/{urn}/aas?aasFormat=WRONG_FORMAT", toMovementUrn(urnPrefix)).with(jwtTokenFactory.allRoles()))
            .andDo( MockMvcResultHandlers.print())
            .andExpect( status().is4xxClientError() );
   }

   @Test
   public void testExampleGenerateExamplePayloadJsonExpectSuccessForBAMM() throws Exception {
      String urnPrefix = "urn:bamm:org.eclipse.tractusx.testjsonschema:2.0.0#";
      mvc.perform(post( TestUtils.createValidModelRequestBAMM(urnPrefix),"DRAFT")
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
   @Disabled("Until we resolve the BAMM dependency handling with ESMF SDK")
   @Test
   public void testSaveModelWithExternalReferencesExpectSuccessForBAMM() throws Exception {
      // save the model with external reference to a traceability characteristic
      // this will fail because traceability does not exist yet
      String modelWithReferenceToTraceability = TestUtils.loadModelFromResources(
            TestUtils.MODEL_WITH_REFERENCE_TO_TRACEABILITY_MODEL_PATH_FOR_BAMM );
      mvc.perform( post( modelWithReferenceToTraceability,"DRAFT" ) )
            .andDo( MockMvcResultHandlers.print() )
            .andExpect( status().isBadRequest() )
            .andExpect( jsonPath( "$.error.message", is( "Validation failed." ) ) )
            .andExpect( jsonPath( "$.error.details.ERR_PROCESSING",
                  containsString( "urn:samm:org.eclipse.tractusx.traceability:0.1.1#PartDataCharacteristic not found" ) ) );

      // save the traceability aspect model
      String traceabilityModel = TestUtils.loadModelFromResources(
            TestUtils.TRACEABILITY_MODEL_PATH_FOR_BAMM );
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

   @Nested
   @DisplayName("State Transition of Models for BAMM")
   public class StateTransitionTestsForBAMM {
      @Test
      public void testModelStatusTransitionForPostForBAMM() throws Exception {
         String urnPrefix = "urn:bamm:org.eclipse.tractusx.model.status.transition.post:2.0.0#";

         // draft state, deletes and modifications are allowed
         mvc.perform( post( TestUtils.createValidModelRequestBAMM( urnPrefix ), "DRAFT" ) )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect( status().isOk() );

         mvc.perform( delete( urnPrefix ) )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect( status().isNoContent() );

         mvc.perform( post( TestUtils.createValidModelRequestBAMM( urnPrefix ), "DRAFT" ) )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect( status().isOk() );

         // Transition from draft to standardized is not allowed
         mvc.perform( post( TestUtils.createValidModelRequestBAMM( urnPrefix ), "STANDARDIZED" ) )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect( status().isBadRequest() );

         // transition from draft to release is allowed, delete is not allowed
         mvc.perform( post( TestUtils.createValidModelRequestBAMM( urnPrefix ), "RELEASED" ) )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect( status().isOk() );

         mvc.perform( delete( urnPrefix ) )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect( status().isBadRequest() )
               .andExpect( jsonPath( "$.error.message", is(
                     "The package urn:bamm:org.eclipse.tractusx.model.status.transition.post:2.0.0# is already in status RELEASED and cannot be deleted." ) ) );

         // transition from released to draft is not allowed
         mvc.perform( post( TestUtils.createValidModelRequestBAMM( urnPrefix ), "DRAFT" ) )
               .andExpect( jsonPath( "$.error.message", is(
                     "The package urn:bamm:org.eclipse.tractusx.model.status.transition.post:2.0.0# is already in status RELEASED and cannot be modified. Only a transition to STANDARDIZED or DEPRECATED is possible." ) ) )
               .andExpect( status().isBadRequest() );

         // transition from released to standardized is allowed
         mvc.perform( post( TestUtils.createValidModelRequestBAMM( urnPrefix ), "STANDARDIZED" ) )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect( status().isOk() );

         // transition from standardized to draft is not allowed
         mvc.perform( post( TestUtils.createValidModelRequestBAMM( urnPrefix ), "DRAFT" ) )
               .andExpect( jsonPath( "$.error.message", is(
                     "The package urn:bamm:org.eclipse.tractusx.model.status.transition.post:2.0.0# is already in status STANDARDIZED and cannot be modified. Only a transition to DEPRECATED is possible." ) ) )
               .andExpect( status().isBadRequest() );

         // transition from standardized to deprecated is allowed
         mvc.perform(
                     post( TestUtils.createValidModelRequestBAMM( urnPrefix ), "DEPRECATED" )
               )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect( status().isOk() );

         // delete deprecated model is allowed
         mvc.perform(
                     delete( urnPrefix )
               )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect( status().isNoContent() );
      }

      @Test
      public void testModelStatusTransitionForPutForBAMM() throws Exception {
         String urnPrefix = "urn:bamm:org.eclipse.tractusx.model.status.transition.put:2.0.0#";

         // draft state, deletes and modifications are allowed
         mvc.perform(post( TestUtils.createValidModelRequestBAMM(urnPrefix),"DRAFT") )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(status().isOk());

         mvc.perform(delete(urnPrefix))
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(status().isNoContent());

         mvc.perform(post( TestUtils.createValidModelRequestBAMM(urnPrefix), "DRAFT") )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(status().isOk());

         // transition from draft to release is allowed, delete is not allowed
         mvc.perform(put( TestUtils.createValidModelRequestBAMM(urnPrefix),"RELEASED"))
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(status().isOk());

         mvc.perform(delete(urnPrefix))
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(status().isBadRequest())
               .andExpect( jsonPath( "$.error.message", is(
                     "The package urn:bamm:org.eclipse.tractusx.model.status.transition.put:2.0.0# is already in status RELEASED and cannot be deleted." ) ) );

         // transition from released to draft is not allowed
         mvc.perform(put( TestUtils.createValidModelRequestBAMM(urnPrefix), "DRAFT") )
               .andExpect( jsonPath( "$.error.message", is(
                     "The package urn:bamm:org.eclipse.tractusx.model.status.transition.put:2.0.0# is already in status RELEASED and cannot be modified. Only a transition to STANDARDIZED or DEPRECATED is possible." ) ) )
               .andExpect( status().isBadRequest() );

         // transition from released to standardized is allowed
         mvc.perform(put( TestUtils.createValidModelRequestBAMM(urnPrefix),"STANDARDIZED") )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(status().isOk());

         // transition from standardized to deprecated is allowed
         mvc.perform(put( TestUtils.createValidModelRequestBAMM(urnPrefix),"DEPRECATED") )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(status().isOk());

         // delete deprecated model is allowed
         mvc.perform(delete( urnPrefix ))
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(status().isNoContent());
      }

      @Disabled("Until we resolve the BAMM dependency handling with ESMF SDK")
      @Test
      public void testDependentModelTransitionForBAMM() throws Exception {
         String urnPrefix = "urn:bamm:org.eclipse.tractusx.model.status.transitionWithDependency:1.0.0#";

         mvc.perform(postBAMM( TestUtils.createModelDependencyForBAMM(), "DRAFT" ))
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(status().isOk());

         mvc.perform(postBAMM( TestUtils.createDependentModelForBAMM(urnPrefix), "DRAFT") )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(status().isOk());

         mvc.perform(put( TestUtils.createDependentModelForBAMM(urnPrefix), "RELEASED") )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(jsonPath( "$.error.message", is(
                     "It is not allowed to release an aspect that has dependencies in DRAFT state." ) ) );
      }
   }

   @Test
   public void testGetModelListByMultipleUrnsForBAMM() throws Exception {
      String urnPrefixPattern = "urn:bamm:org.eclipse.tractusx.test_model_list_by_urns_%s:1.0.0#";

      List<String> urnSearchArrayEvenNumbers = new ArrayList<String>();
      List<String> urnSearchArrayOddNumbers = new ArrayList<String>();
      List<String> urnSearchArrayNonExistingEntry = new ArrayList<String>();

      for(int i = 1; i <= 11; i++) {
         String urnPrefix = String.format(urnPrefixPattern, i);
         mvc.perform(post( TestUtils.createValidModelRequestBAMM(urnPrefix),"DRAFT") )
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

   @Disabled("Until we resolve the BAMM dependency handling with ESMF SDK")
   @Test
   public void testDependentModelBAMM() throws Exception {

      String PCF_FILE = "Pcf-bamm.ttl";
      String SERIAL_PART_FILE = "SerialPart-bamm.ttl";
      String PHYSICAL_DIMENSIONS_FILE = "PhysicalDimensions-bamm.ttl";
      String DIGITAL_PRODUCT_PASSPORT_FILE = "DigitalProductPassport-bamm.ttl";

      //Given
      mvc.perform( post( TestUtils.getTTLFile( PCF_FILE ), "DRAFT" ) )
            .andDo( MockMvcResultHandlers.print() )
            .andExpect( status().isOk() );

      mvc.perform( post( TestUtils.getTTLFile( SERIAL_PART_FILE ), "DRAFT" ) )
            .andDo( MockMvcResultHandlers.print() )
            .andExpect( status().isOk() );

      mvc.perform( post( TestUtils.getTTLFile( PHYSICAL_DIMENSIONS_FILE ), "DRAFT" ) )
            .andDo( MockMvcResultHandlers.print() )
            .andExpect( status().isOk() );

      //When
      mvc.perform( post( TestUtils.getTTLFile( DIGITAL_PRODUCT_PASSPORT_FILE ), "DRAFT" ) )
            .andDo( MockMvcResultHandlers.print() )
            .andExpect( status().isOk() );
   }

   private static String toMovementUrn(String urn){
      return urn + "Movement";
   }
}