/********************************************************************************
 * Copyright (c) 2021-2025 Robert Bosch Manufacturing Solutions GmbH
 * Copyright (c) 2021-2025 Contributors to the Eclipse Foundation
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

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.esmf.metamodel.AspectModel;
import org.eclipse.tractusx.semantics.hub.SDKAccessHelper;
import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

public class BammHelperTest {
    @Test
    public void testBammLoaderWithValidModel() {
        final String modelString = "@prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:1.0.0#> .\n @prefix bamm-c: <urn:samm:org.eclipse.esmf.samm:characteristic:1.0.0#> .\n @prefix bamm-e: <urn:samm:org.eclipse.esmf.samm:entity:1.0.0#> .\n @prefix unit: <urn:samm:org.eclipse.esmf.samm:unit:1.0.0#> .\n @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n @prefix : <urn:samm:org.eclipse.tractusx:1.0.0#> .\n \n :Movement a samm:Aspect;\n samm:name \"Movement\";\n samm:preferredName \"Movement\"@en;\n samm:description \"Aspect for movement information\"@en;\n samm:properties (:isMoving :speedLimitWarning :position);\n samm:operations ().\n :isMoving a samm:Property;\n samm:name \"isMoving\";\n samm:preferredName \"Moving\"@en;\n samm:description \"Flag indicating whether the asset is currently moving\"@en;\n samm:characteristic bamm-c:Boolean.\n :speedLimitWarning a samm:Property;\n samm:name \"speedLimitWarning\";\n samm:preferredName \"Speed Limit Warning\"@en;\n samm:description \"Indicates if the speed limit is adhered to.\"@en;\n samm:characteristic :TrafficLight.\n :position a samm:Property;\n samm:name \"position\";\n samm:preferredName \"Position\"@en;\n samm:description \"Indicates a position\"@en;\n samm:characteristic :SpatialPositionCharacteristic.\n :TrafficLight a bamm-c:Enumeration;\n samm:name \"TrafficLight\";\n samm:preferredName \"Warning Level\"@en;\n samm:description \"Represents if speed of position change is within specification (green), within tolerance (yellow), or outside specification (red).\"@en;\n samm:dataType xsd:string;\n bamm-c:values (\"green\" \"yellow\" \"red\").\n :SpatialPosition a samm:Entity;\n samm:name \"SpatialPosition\";\n samm:preferredName \"Spatial Position\"@en;\n samm:description \"Position in space, described along three axis, with the third axis optional, if all positions are in a plane.\"@en;\n samm:properties (:x :y :z).\n :x a samm:Property;\n samm:name \"x\";\n samm:preferredName \"x\"@en;\n samm:description \"x coordinate in space\"@en;\n samm:characteristic :Coordinate.\n :y a samm:Property;\n samm:name \"y\";\n samm:preferredName \"y\"@en;\n samm:description \"y coordinate in space\"@en;\n samm:characteristic :Coordinate.\n :z a samm:Property;\n samm:name \"z\";\n samm:preferredName \"z\"@en;\n samm:description \"z coordinate in space\"@en;\n samm:characteristic :Coordinate;\n samm:optional \"true\"^^xsd:boolean.\n :Coordinate a bamm-c:Measurement;\n samm:name \"Coordinate\";\n samm:preferredName \"Coordinate\"@en;\n samm:description \"Represents a coordinate along an axis in space.\"@en;\n samm:dataType xsd:float;\n bamm-c:unit unit:metre.\n :SpatialPositionCharacteristic a bamm-c:SingleEntity;\n samm:name \"SpatialPositionCharacteristic\";\n samm:preferredName \"Spatial Position Characteristic\"@en;\n samm:description \"Represents a single position in space with optional z coordinate.\"@en;\n samm:dataType :SpatialPosition.\n";

        SDKAccessHelper bammHelper = new SDKAccessHelper();

        assertTrue(bammHelper.loadAspectModel(modelString).isSuccess());
    }

    @Test
    public void testBammLoaderWithNotSelfContainedModel() {
        final String modelString = "@prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:1.0.0#> .\n @prefix bamm-c: <urn:samm:org.eclipse.esmf.samm:characteristic:1.0.0#> .\n @prefix bamm-e: <urn:samm:org.eclipse.esmf.samm:entity:1.0.0#> .\n @prefix unit: <urn:samm:org.eclipse.esmf.samm:unit:1.0.0#> .\n @prefix blub: <urn:samm:org.eclipse.tractusx.extension:1.0.0#> .\n @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n @prefix : <urn:samm:org.eclipse.tractusx:1.0.0#> .\n \n :Movement a samm:Aspect;\n samm:name \"Movement\";\n samm:preferredName \"Movement\"@en;\n samm:description \"Aspect for movement information\"@en;\n samm:properties (:position);\n samm:operations ().\n :position a samm:Property;\n samm:name \"position\";\n samm:preferredName \"Position\"@en;\n samm:description \"Indicates a position\"@en;\n samm:characteristic :SpatialPositionCharacteristic.\n :SpatialPositionCharacteristic a bamm-c:SingleEntity;\n samm:name \"SpatialPositionCharacteristic\";\n samm:preferredName \"Spatial Position Characteristic\"@en;\n samm:description \"Represents a single position in space with optional z coordinate.\"@en;\n samm:dataType blub:SpatialPosition.\n";

        SDKAccessHelper bammHelper = new SDKAccessHelper();

        assertTrue(bammHelper.loadAspectModel(modelString).isFailure());
    }

    @Test
    public void testBammValidationWithValidModel() {
        final String modelString = "@prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:1.0.0#> .\n @prefix bamm-c: <urn:samm:org.eclipse.esmf.samm:characteristic:1.0.0#> .\n @prefix bamm-e: <urn:samm:org.eclipse.esmf.samm:entity:1.0.0#> .\n @prefix unit: <urn:samm:org.eclipse.esmf.samm:unit:1.0.0#> .\n @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n @prefix : <urn:samm:org.eclipse.tractusx:1.0.0#> .\n \n :Movement a samm:Aspect;\n samm:name \"Movement\";\n samm:preferredName \"Movement\"@en;\n samm:description \"Aspect for movement information\"@en;\n samm:properties (:isMoving :speedLimitWarning :position);\n samm:operations ().\n :isMoving a samm:Property;\n samm:name \"isMoving\";\n samm:preferredName \"Moving\"@en;\n samm:description \"Flag indicating whether the asset is currently moving\"@en;\n samm:characteristic bamm-c:Boolean.\n :speedLimitWarning a samm:Property;\n samm:name \"speedLimitWarning\";\n samm:preferredName \"Speed Limit Warning\"@en;\n samm:description \"Indicates if the speed limit is adhered to.\"@en;\n samm:characteristic :TrafficLight.\n :position a samm:Property;\n samm:name \"position\";\n samm:preferredName \"Position\"@en;\n samm:description \"Indicates a position\"@en;\n samm:characteristic :SpatialPositionCharacteristic.\n :TrafficLight a bamm-c:Enumeration;\n samm:name \"TrafficLight\";\n samm:preferredName \"Warning Level\"@en;\n samm:description \"Represents if speed of position change is within specification (green), within tolerance (yellow), or outside specification (red).\"@en;\n samm:dataType xsd:string;\n bamm-c:values (\"green\" \"yellow\" \"red\").\n :SpatialPosition a samm:Entity;\n samm:name \"SpatialPosition\";\n samm:preferredName \"Spatial Position\"@en;\n samm:description \"Position in space, described along three axis, with the third axis optional, if all positions are in a plane.\"@en;\n samm:properties (:x :y :z).\n :x a samm:Property;\n samm:name \"x\";\n samm:preferredName \"x\"@en;\n samm:description \"x coordinate in space\"@en;\n samm:characteristic :Coordinate.\n :y a samm:Property;\n samm:name \"y\";\n samm:preferredName \"y\"@en;\n samm:description \"y coordinate in space\"@en;\n samm:characteristic :Coordinate.\n :z a samm:Property;\n samm:name \"z\";\n samm:preferredName \"z\"@en;\n samm:description \"z coordinate in space\"@en;\n samm:characteristic :Coordinate;\n samm:optional \"true\"^^xsd:boolean.\n :Coordinate a bamm-c:Measurement;\n samm:name \"Coordinate\";\n samm:preferredName \"Coordinate\"@en;\n samm:description \"Represents a coordinate along an axis in space.\"@en;\n samm:dataType xsd:float;\n bamm-c:unit unit:metre.\n :SpatialPositionCharacteristic a bamm-c:SingleEntity;\n samm:name \"SpatialPositionCharacteristic\";\n samm:preferredName \"Spatial Position Characteristic\"@en;\n samm:description \"Represents a single position in space with optional z coordinate.\"@en;\n samm:dataType :SpatialPosition.\n";

        SDKAccessHelper bammHelper = new SDKAccessHelper();

        Try<AspectModel> model = bammHelper.loadAspectModel(modelString);

        assertTrue(model.isSuccess());
    }
    
    @Test
    public void testBammValidationWithInvalidModel() {
        final String modelString = "@prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:1.0.0#> .\n @prefix bamm-c: <urn:samm:org.eclipse.esmf.samm:characteristicX:1.0.0#> .\n @prefix bamm-e: <urn:samm:org.eclipse.esmf.samm:entity:1.0.0#> .\n @prefix unit: <urn:samm:org.eclipse.esmf.samm:unit:1.0.0#> .\n @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n @prefix : <urn:samm:org.eclipse.tractusx:1.0.0#> .\n \n :Movement a samm:Aspect;\n samm:name \"Movement\";\n samm:preferredName \"Movement\"@en;\n samm:description \"Aspect for movement information\"@en;\n samm:property (:isMoving :speedLimitWarning :position);\n samm:operations ().\n :isMoving a samm:Property;\n samm:name \"isMoving\";\n samm:preferredName \"Moving\"@en;\n samm:description \"Flag indicating whether the asset is currently moving\"@en;\n samm:characteristic bamm-c:Boolean.\n :speedLimitWarning a samm:Property;\n samm:name \"speedLimitWarning\";\n samm:preferredName \"Speed Limit Warning\"@en;\n samm:description \"Indicates if the speed limit is adhered to.\"@en;\n samm:characteristic :TrafficLight.\n :position a samm:Property;\n samm:name \"position\";\n samm:preferredName \"Position\"@en;\n samm:description \"Indicates a position\"@en;\n samm:characteristic :SpatialPositionCharacteristic.\n :TrafficLight a bamm-c:Enumeration;\n samm:name \"TrafficLight\";\n samm:preferredName \"Warning Level\"@en;\n samm:description \"Represents if speed of position change is within specification (green), within tolerance (yellow), or outside specification (red).\"@en;\n samm:dataType xsd:string;\n bamm-c:values (\"green\" \"yellow\" \"red\").\n :SpatialPosition a samm:Entity;\n samm:name \"SpatialPosition\";\n samm:preferredName \"Spatial Position\"@en;\n samm:description \"Position in space, described along three axis, with the third axis optional, if all positions are in a plane.\"@en;\n samm:properties (:x :y :z).\n :x a samm:Property;\n samm:name \"x\";\n samm:preferredName \"x\"@en;\n samm:description \"x coordinate in space\"@en;\n samm:characteristic :Coordinate.\n :y a samm:Property;\n samm:name \"y\";\n samm:preferredName \"y\"@en;\n samm:description \"y coordinate in space\"@en;\n samm:characteristic :Coordinate.\n :z a samm:Property;\n samm:name \"z\";\n samm:preferredName \"z\"@en;\n samm:description \"z coordinate in space\"@en;\n samm:characteristic :Coordinate;\n samm:optional \"true\"^^xsd:boolean.\n :Coordinate a bamm-c:Measurement;\n samm:name \"Coordinate\";\n samm:preferredName \"Coordinate\"@en;\n samm:description \"Represents a coordinate along an axis in space.\"@en;\n samm:dataType xsd:float;\n bamm-c:unit unit:metre.\n :SpatialPositionCharacteristic a bamm-c:SingleEntity;\n samm:name \"SpatialPositionCharacteristic\";\n samm:preferredName \"Spatial Position Characteristic\"@en;\n samm:description \"Represents a single position in space with optional z coordinate.\"@en;\n samm:dataType :SpatialPosition.\n";

        SDKAccessHelper bammHelper = new SDKAccessHelper();

        Try<AspectModel> model = bammHelper.loadAspectModel(modelString);

        assertTrue(model.isFailure());
    }

    @Test
    public void testBammLoaderInVersion2_0_0_WithValidModelExpectSuccess() {
        final String modelString = "@prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:2.0.0#> .\n @prefix bamm-c: <urn:samm:org.eclipse.esmf.samm:characteristic:2.0.0#> .\n @prefix bamm-e: <urn:samm:org.eclipse.esmf.samm:entity:2.0.0#> .\n @prefix unit: <urn:samm:org.eclipse.esmf.samm:unit:2.0.0#> .\n @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n @prefix : <urn:samm:org.eclipse.tractusx:1.0.0#> .\n \n :Movement a samm:Aspect;\n samm:name \"Movement\";\n samm:preferredName \"Movement\"@en;\n samm:description \"Aspect for movement information\"@en;\n samm:properties (:isMoving :speedLimitWarning :position);\n samm:operations ().\n :isMoving a samm:Property;\n samm:name \"isMoving\";\n samm:preferredName \"Moving\"@en;\n samm:description \"Flag indicating whether the asset is currently moving\"@en;\n samm:characteristic bamm-c:Boolean.\n :speedLimitWarning a samm:Property;\n samm:name \"speedLimitWarning\";\n samm:preferredName \"Speed Limit Warning\"@en;\n samm:description \"Indicates if the speed limit is adhered to.\"@en;\n samm:characteristic :TrafficLight.\n :position a samm:Property;\n samm:name \"position\";\n samm:preferredName \"Position\"@en;\n samm:description \"Indicates a position\"@en;\n samm:characteristic :SpatialPositionCharacteristic.\n :TrafficLight a bamm-c:Enumeration;\n samm:name \"TrafficLight\";\n samm:preferredName \"Warning Level\"@en;\n samm:description \"Represents if speed of position change is within specification (green), within tolerance (yellow), or outside specification (red).\"@en;\n samm:dataType xsd:string;\n bamm-c:values (\"green\" \"yellow\" \"red\").\n :SpatialPosition a samm:Entity;\n samm:name \"SpatialPosition\";\n samm:preferredName \"Spatial Position\"@en;\n samm:description \"Position in space, described along three axis, with the third axis optional, if all positions are in a plane.\"@en;\n samm:properties (:x :y :z).\n :x a samm:Property;\n samm:name \"x\";\n samm:preferredName \"x\"@en;\n samm:description \"x coordinate in space\"@en;\n samm:characteristic :Coordinate.\n :y a samm:Property;\n samm:name \"y\";\n samm:preferredName \"y\"@en;\n samm:description \"y coordinate in space\"@en;\n samm:characteristic :Coordinate.\n :z a samm:Property;\n samm:name \"z\";\n samm:preferredName \"z\"@en;\n samm:description \"z coordinate in space\"@en;\n samm:characteristic :Coordinate;\n samm:optional \"true\"^^xsd:boolean.\n :Coordinate a bamm-c:Measurement;\n samm:name \"Coordinate\";\n samm:preferredName \"Coordinate\"@en;\n samm:description \"Represents a coordinate along an axis in space.\"@en;\n samm:dataType xsd:float;\n bamm-c:unit unit:metre.\n :SpatialPositionCharacteristic a bamm-c:SingleEntity;\n samm:name \"SpatialPositionCharacteristic\";\n samm:preferredName \"Spatial Position Characteristic\"@en;\n samm:description \"Represents a single position in space with optional z coordinate.\"@en;\n samm:dataType :SpatialPosition.\n";

        SDKAccessHelper bammHelper = new SDKAccessHelper();

        assertTrue(bammHelper.loadAspectModel(modelString).isSuccess());
    }

    @Test
    public void testBammLoaderInUnknownVersion0_0_0_WithInvalidModelExpectFailure() {
        final String modelString = "@prefix samm: <urn:samm:org.eclipse.esmf.samm:meta-model:0.0.0#> .\n @prefix bamm-c: <urn:samm:org.eclipse.esmf.samm:characteristic:0.0.0#> .\n @prefix bamm-e: <urn:samm:org.eclipse.esmf.samm:entity:0.0.0#> .\n @prefix unit: <urn:samm:org.eclipse.esmf.samm:unit:0.0.0#> .\n @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n @prefix : <urn:samm:org.eclipse.tractusx:1.0.0#> .\n \n :Movement a samm:Aspect;\n samm:name \"Movement\";\n samm:preferredName \"Movement\"@en;\n samm:description \"Aspect for movement information\"@en;\n samm:properties (:isMoving :speedLimitWarning :position);\n samm:operations ().\n :isMoving a samm:Property;\n samm:name \"isMoving\";\n samm:preferredName \"Moving\"@en;\n samm:description \"Flag indicating whether the asset is currently moving\"@en;\n samm:characteristic bamm-c:Boolean.\n :speedLimitWarning a samm:Property;\n samm:name \"speedLimitWarning\";\n samm:preferredName \"Speed Limit Warning\"@en;\n samm:description \"Indicates if the speed limit is adhered to.\"@en;\n samm:characteristic :TrafficLight.\n :position a samm:Property;\n samm:name \"position\";\n samm:preferredName \"Position\"@en;\n samm:description \"Indicates a position\"@en;\n samm:characteristic :SpatialPositionCharacteristic.\n :TrafficLight a bamm-c:Enumeration;\n samm:name \"TrafficLight\";\n samm:preferredName \"Warning Level\"@en;\n samm:description \"Represents if speed of position change is within specification (green), within tolerance (yellow), or outside specification (red).\"@en;\n samm:dataType xsd:string;\n bamm-c:values (\"green\" \"yellow\" \"red\").\n :SpatialPosition a samm:Entity;\n samm:name \"SpatialPosition\";\n samm:preferredName \"Spatial Position\"@en;\n samm:description \"Position in space, described along three axis, with the third axis optional, if all positions are in a plane.\"@en;\n samm:properties (:x :y :z).\n :x a samm:Property;\n samm:name \"x\";\n samm:preferredName \"x\"@en;\n samm:description \"x coordinate in space\"@en;\n samm:characteristic :Coordinate.\n :y a samm:Property;\n samm:name \"y\";\n samm:preferredName \"y\"@en;\n samm:description \"y coordinate in space\"@en;\n samm:characteristic :Coordinate.\n :z a samm:Property;\n samm:name \"z\";\n samm:preferredName \"z\"@en;\n samm:description \"z coordinate in space\"@en;\n samm:characteristic :Coordinate;\n samm:optional \"true\"^^xsd:boolean.\n :Coordinate a bamm-c:Measurement;\n samm:name \"Coordinate\";\n samm:preferredName \"Coordinate\"@en;\n samm:description \"Represents a coordinate along an axis in space.\"@en;\n samm:dataType xsd:float;\n bamm-c:unit unit:metre.\n :SpatialPositionCharacteristic a bamm-c:SingleEntity;\n samm:name \"SpatialPositionCharacteristic\";\n samm:preferredName \"Spatial Position Characteristic\"@en;\n samm:description \"Represents a single position in space with optional z coordinate.\"@en;\n samm:dataType :SpatialPosition.\n";

        SDKAccessHelper bammHelper = new SDKAccessHelper();

        assertTrue(bammHelper.loadAspectModel(modelString).isFailure());

    }
}
