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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.eclipse.tractusx.semantics.FusekiTestContainer;
import org.eclipse.tractusx.semantics.GeneralProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@EnableConfigurationProperties(GeneralProperties.class)
public abstract class AbstractModelsApiTest extends FusekiTestContainer {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper mapper;

    @Autowired
    protected JwtTokenFactory jwtTokenFactory;

    @Autowired
    private RDFConnectionRemoteBuilder rdfConnectionRemoteBuilder;

    public MockHttpServletRequestBuilder post(String payload) {
        return post(payload, "DRAFT");
    }

    public MockHttpServletRequestBuilder post( String payload, String status ) {
        String type = "BAMM";
        return MockMvcRequestBuilders.post( "/api/v1/models")
                .queryParam("type", type)
                .queryParam( "status", status)
                .accept( MediaType.APPLICATION_JSON )
                .contentType( MediaType.TEXT_PLAIN)
                .content( payload )
                .with(jwtTokenFactory.allRoles());
    }

    public MockHttpServletRequestBuilder put( String payload, String status ) {
        String type = "BAMM";
        return MockMvcRequestBuilders.put( "/api/v1/models")
                .queryParam("type", type)
                .queryParam( "status", status )
                .accept( MediaType.APPLICATION_JSON )
                .contentType( MediaType.TEXT_PLAIN )
                .content( payload )
                .with(jwtTokenFactory.allRoles());
    }

    public MockHttpServletRequestBuilder delete(String urnPrefix){
        return MockMvcRequestBuilders.delete(
                        "/api/v1/models/{urn}",
                        urnPrefix )
                .with(jwtTokenFactory.allRoles());
    }

    public void deleteAllData(){
        try ( final RDFConnection rdfConnection = rdfConnectionRemoteBuilder.build() ) {
            rdfConnection.update( "CLEAR DEFAULT" );
        }
    }

}
