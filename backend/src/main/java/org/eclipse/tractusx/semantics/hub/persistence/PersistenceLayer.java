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

import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.tractusx.semantics.hub.domain.ModelPackageStatus;
import org.eclipse.tractusx.semantics.hub.domain.ModelPackageUrn;
import org.eclipse.tractusx.semantics.hub.model.SemanticModel;
import org.eclipse.tractusx.semantics.hub.model.SemanticModelList;
import org.eclipse.tractusx.semantics.hub.model.SemanticModelStatus;
import org.eclipse.tractusx.semantics.hub.model.SemanticModelType;

import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

/**
 * Interface to any model persistency implementation
 */
public interface PersistenceLayer {
   /**
    * search a list of persisted models based on a set of mandatory and optional parameters
    *
    * @param namespaceFilter substring flag
    * @param nameFilter substring flag
    * @param nameType optional string flag determining the scope of the nameFilter (default: the model name
    *       _NAME_, but maybe refer any RDF object)
    * @param status optional string flag
    * @param page number of the page to deliver
    * @param pageSize size of the pages to batch the results in
    * @return a list of models belonging to the searched page
    */
   SemanticModelList getModels(String namespaceFilter, @Nullable ModelPackageStatus status, Integer page, Integer pageSize );

   SemanticModel getModel(AspectModelUrn urn );

   SemanticModel save(SemanticModelType type, String newModel, SemanticModelStatus status);

   String getModelDefinition( AspectModelUrn urn );

   void deleteModelsPackage( ModelPackageUrn urn );

   boolean echo();

   public SemanticModelList findModelListByUrns(List<AspectModelUrn> urns, int page, int pageSize);
}
