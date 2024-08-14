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

import io.vavr.control.Try;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.apache.jena.update.UpdateRequest;
import org.eclipse.esmf.aspectmodel.resolver.AspectModelResolver;
import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;
import org.eclipse.esmf.aspectmodel.urn.UrnSyntaxException;
import org.eclipse.tractusx.semantics.hub.AspectModelNotFoundException;
import org.eclipse.tractusx.semantics.hub.InvalidStateTransitionException;
import org.eclipse.tractusx.semantics.hub.ModelPackageNotFoundException;
import org.eclipse.tractusx.semantics.hub.domain.ModelPackage;
import org.eclipse.tractusx.semantics.hub.domain.ModelPackageStatus;
import org.eclipse.tractusx.semantics.hub.domain.ModelPackageUrn;
import org.eclipse.tractusx.semantics.hub.model.SemanticModel;
import org.eclipse.tractusx.semantics.hub.model.SemanticModelList;
import org.eclipse.tractusx.semantics.hub.model.SemanticModelStatus;
import org.eclipse.tractusx.semantics.hub.model.SemanticModelType;
import org.eclipse.tractusx.semantics.hub.persistence.PersistenceLayer;

import javax.annotation.Nullable;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TripleStorePersistence implements PersistenceLayer {

	private final RDFConnectionRemoteBuilder rdfConnectionRemoteBuilder;
	private final SdsSdk sdsSdk;

	public TripleStorePersistence(final RDFConnectionRemoteBuilder rdfConnectionRemoteBuilder,
								  final SdsSdk sdsSdk) {
		this.rdfConnectionRemoteBuilder = rdfConnectionRemoteBuilder;
		this.sdsSdk = sdsSdk;
	}

	private static int getTotalPages(int totalItemsCount, int pageSize) {
		if (totalItemsCount == 0 || pageSize == 0) {
			return 0;
		}
		return (int) Math.ceil(((double) totalItemsCount) / (double) pageSize);
	}

	private static List<SemanticModel> aspectModelFrom(
		final List<QuerySolution> querySolutions) {
		return querySolutions
			.stream()
			.map(TripleStorePersistence::aspectModelFrom)
			.collect(Collectors.toList());
	}

	private static SemanticModel aspectModelFrom(final QuerySolution querySolution) {
		final String urn = querySolution.get(SparqlQueries.ASPECT).toString();
		final String status = querySolution.get(SparqlQueries.STATUS_RESULT).toString();
		AspectModelUrn aspectModelUrn = AspectModelUrn.fromUrn(urn);
		SemanticModel model = new SemanticModel();
		model.setUrn(aspectModelUrn.getUrn().toString());
		model.setType(determineModelType(urn));
		model.setVersion(aspectModelUrn.getVersion());
		model.setName(aspectModelUrn.getName());
		model.setStatus(SemanticModelStatus.fromValue(status));
		return model;
	}

	private static SemanticModelType determineModelType(String aspectUrn) {
		if (aspectUrn.contains("bamm")) {
			return SemanticModelType.BAMM;
		} else {
			return SemanticModelType.SAMM;
		}
	}

	@Override
	public SemanticModelList getModels(String namespaceFilter,
									   @Nullable ModelPackageStatus status, Integer page, Integer pageSize) {
		final Query query = SparqlQueries.buildFindAllQuery(namespaceFilter, status, page,
			pageSize);
		final AtomicReference<List<SemanticModel>> aspectModels = new AtomicReference<>();
		try (final RDFConnection rdfConnection = rdfConnectionRemoteBuilder.build()) {
			rdfConnection.queryResultSet(query, resultSet -> {
				final List<QuerySolution> querySolutions = ResultSetFormatter.toList(resultSet);
				aspectModels.set(TripleStorePersistence.aspectModelFrom(querySolutions));
			});
		}
		int totalSemanticModelCount = getTotalItemsCount(namespaceFilter, status);
		int totalPages = getTotalPages(totalSemanticModelCount, pageSize);
		SemanticModelList modelList = new SemanticModelList();
		List<SemanticModel> semanticModels = aspectModels.get();
		modelList.setCurrentPage(page);
		modelList.setItemCount(semanticModels.size());
		modelList.setTotalPages(totalPages);
		modelList.setTotalItems(totalSemanticModelCount);
		modelList.setItems(aspectModels.get());
		return modelList;
	}

	@Override
	public SemanticModel getModel(final AspectModelUrn urn) {
		return findByUrn(urn);
	}

	@Override
	public SemanticModel updateModel(String urn, SemanticModelStatus status) {

		validateStatusParameter(status);
		SemanticModel semanticModel = Optional.ofNullable(findByUrn(
			AspectModelUrn.fromUrn(urn))).orElseThrow(() -> new IllegalArgumentException(
			String.format("Invalid URN %s",
				ModelPackageUrn.fromUrn(urn).getUrn())));
		ModelPackageStatus persistedModelStatus = ModelPackageStatus.valueOf(
			semanticModel.getStatus().name());

		final Model model = findContainingModelByUrn(urn);

		final AspectModelUrn modelUrn = sdsSdk.getAspectUrn(model);

		validateStatus(status, model, modelUrn, persistedModelStatus);

		updateModel(status, modelUrn, model);

		return findByUrn(modelUrn);
	}

	@Override
	public SemanticModel save(SemanticModelType type, String newModel, SemanticModelStatus status) {
		validateStatusParameter(status);
		final Model rdfModel = sdsSdk.load(newModel.getBytes(StandardCharsets.UTF_8));
		final AspectModelUrn modelUrn = sdsSdk.getAspectUrn(rdfModel);
		Optional<ModelPackage> existsByPackage = findByPackageByUrn(ModelPackageUrn.fromUrn(modelUrn));

		if (existsByPackage.isPresent()) {
			validateStatus(status, rdfModel, modelUrn, existsByPackage.get().getStatus());
		}

		sdsSdk.validate(rdfModel, this::findContainingModelByUrn, type);

		Model rdfModelOriginal = sdsSdk.load(newModel.getBytes(StandardCharsets.UTF_8));

		updateModel(status, modelUrn, rdfModelOriginal);

		return findByUrn(modelUrn);
	}

	private void updateModel(SemanticModelStatus status, AspectModelUrn modelUrn, Model rdfModelOriginal) {
		final Resource rootResource = ResourceFactory.createResource(modelUrn.getUrnPrefix());
		rdfModelOriginal.add(rootResource, SparqlQueries.STATUS_PROPERTY,
			ModelPackageStatus.valueOf(status.name()).toString());

		try (final RDFConnection rdfConnection = rdfConnectionRemoteBuilder.build()) {
			rdfConnection.update(new UpdateBuilder().addInsert(rdfModelOriginal).build());
		}
	}

	private void validateStatus(SemanticModelStatus status, Model rdfModel, AspectModelUrn modelUrn, ModelPackageStatus persistedModelStatus) {
		final ModelPackageStatus desiredModelStatus = ModelPackageStatus.valueOf(status.name());
		switch (persistedModelStatus) {
			case DRAFT:
				if (desiredModelStatus.equals(ModelPackageStatus.RELEASED) && !hasReferenceToDraftPackage(modelUrn, rdfModel)) {
					throw new InvalidStateTransitionException("It is not allowed to release an aspect that has dependencies in DRAFT state.");
				} else if (desiredModelStatus.equals(ModelPackageStatus.STANDARDIZED)) {
					throw new IllegalArgumentException(
						String.format("The package %s is in status %s. Only a transition to RELEASED or DEPRECATED is possible.",
							ModelPackageUrn.fromUrn(modelUrn).getUrn(), persistedModelStatus.name()));
				}
				deleteByUrn(ModelPackageUrn.fromUrn(modelUrn));
				break;
			case RELEASED:
				// released models can only be updated when the new state is deprecated or standardized
				if (desiredModelStatus.equals(ModelPackageStatus.DEPRECATED) || desiredModelStatus.equals(ModelPackageStatus.STANDARDIZED)) {
					deleteByUrn(ModelPackageUrn.fromUrn(modelUrn));
				} else {
					throw new IllegalArgumentException(
						String.format("The package %s is already in status %s and cannot be modified. Only a transition to STANDARDIZED or DEPRECATED is possible.",
							ModelPackageUrn.fromUrn(modelUrn).getUrn(), persistedModelStatus.name()));
				}
				break;
			case STANDARDIZED:
				if (desiredModelStatus.equals(ModelPackageStatus.DEPRECATED)) {
					deleteByUrn(ModelPackageUrn.fromUrn(modelUrn));
				} else {
					throw new IllegalArgumentException(
						String.format("The package %s is already in status %s and cannot be modified. Only a transition to DEPRECATED is possible.",
							ModelPackageUrn.fromUrn(modelUrn).getUrn(), persistedModelStatus.name()));
				}
				break;
			case DEPRECATED:
				throw new IllegalArgumentException(
					String.format("The package %s is already in status %s and cannot be modified.",
						ModelPackageUrn.fromUrn(modelUrn).getUrn(), persistedModelStatus.name()));
		}
	}

	@Override
	public String getModelDefinition(final AspectModelUrn urn) {
		Model jenaModelByUrn = findJenaModelByUrn(urn);
		if (jenaModelByUrn == null) {
			throw new AspectModelNotFoundException(urn);
		}
		StringWriter out = new StringWriter();
		jenaModelByUrn.write(out, "TURTLE");
		return out.toString();
	}

	@Override
	public void deleteModelsPackage(final ModelPackageUrn urn) {
		ModelPackage modelsPackage = findByPackageByUrn(urn)
			.orElseThrow(() -> new ModelPackageNotFoundException(urn));

		ModelPackageStatus status = modelsPackage.getStatus();
		if (ModelPackageStatus.RELEASED.equals(status) || ModelPackageStatus.STANDARDIZED.equals(status)) {
			throw new IllegalArgumentException(
				String.format("The package %s is already in status %s and cannot be deleted.",
					urn.getUrn(), status.name()));
		}
		deleteByUrn(urn);
	}

	@Override
	public SemanticModelList findModelListByUrns(List<AspectModelUrn> urns, int page, int pageSize) {
		final Query query = SparqlQueries.buildFindListByUrns(urns, page, pageSize);
		final AtomicReference<List<SemanticModel>> aspectModels = new AtomicReference<>();
		try (final RDFConnection rdfConnection = rdfConnectionRemoteBuilder.build()) {
			rdfConnection.queryResultSet(query, resultSet -> {
				final List<QuerySolution> querySolutions = ResultSetFormatter.toList(resultSet);
				aspectModels.set(TripleStorePersistence.aspectModelFrom(querySolutions));
			});
		}
		int totalSemanticModelCount = getSelectiveItemsCount(null, null, null, null, urns);
		int totalPages = getTotalPages(totalSemanticModelCount, pageSize);
		SemanticModelList modelList = new SemanticModelList();
		List<SemanticModel> semanticModels = aspectModels.get();
		modelList.setCurrentPage(page);
		modelList.setItemCount(semanticModels.size());
		modelList.setTotalPages(totalPages);
		modelList.setTotalItems(totalSemanticModelCount);
		modelList.setItems(aspectModels.get());
		return modelList;
	}

	public boolean echo() {
		final RDFConnection rdfConnection = rdfConnectionRemoteBuilder.build();

		return rdfConnection.queryAsk(SparqlQueries.echoQuery());
	}

	private boolean hasReferenceToDraftPackage(AspectModelUrn modelUrn, Model model) {
		Pattern pattern = Pattern.compile(SparqlQueries.ALL_SAMM_ASPECT_URN_PREFIX);

		List<String> urns = AspectModelResolver.getAllUrnsInModel(model).stream().filter(urn -> getAspectModelUrn(urn).isSuccess())
			.map(urn -> getAspectModelUrn(urn).get().getUrnPrefix())
			.distinct()
			.collect(Collectors.toList());

		for (var entry : urns) {
			Matcher matcher = pattern.matcher(entry);
			if (!matcher.find() && !modelUrn.getUrnPrefix().equals(entry)) {
				if (findByPackageByUrn(ModelPackageUrn.fromUrn(entry)).get().getStatus().equals(ModelPackageStatus.DRAFT)) {
					return false;
				}
			}
		}

		return true;
	}

	private Integer getTotalItemsCount(@Nullable String namespaceFilter,
									   @Nullable ModelPackageStatus status) {
		try (final RDFConnection rdfConnection = rdfConnectionRemoteBuilder.build()) {
			AtomicReference<Integer> count = new AtomicReference<>();
			rdfConnection.querySelect(
				SparqlQueries.buildCountAspectModelsQuery(namespaceFilter, status),
				querySolution -> {
					int countResult = querySolution.getLiteral("aspectModelCount").getInt();
					count.set(countResult);
				});
			return count.get();
		}
	}

	private Integer getSelectiveItemsCount(@Nullable String namespaceFilter, @Nullable String nameFilter,
										   @Nullable String nameType,
										   @Nullable ModelPackageStatus status, List<AspectModelUrn> urns) {
		try (final RDFConnection rdfConnection = rdfConnectionRemoteBuilder.build()) {
			AtomicReference<Integer> count = new AtomicReference<>();
			rdfConnection.querySelect(
				SparqlQueries.buildCountSelectiveAspectModelsQuery(namespaceFilter, nameFilter, nameType, status, urns),
				querySolution -> {
					int countResult = querySolution.getLiteral("aspectModelCount").getInt();
					count.set(countResult);
				});
			return count.get();
		}
	}

	private void deleteByUrn(final ModelPackageUrn modelsPackage) {
		final UpdateRequest deleteByUrn = SparqlQueries.buildDeleteByUrnRequest(modelsPackage);
		try (final RDFConnection rdfConnection = rdfConnectionRemoteBuilder.build()) {
			rdfConnection.update(deleteByUrn);
		}
	}

	private Model findContainingModelByUrn(final String urn) {
		final Query query = SparqlQueries.buildFindModelElementClosureQuery(urn);
		try (final RDFConnection rdfConnection = rdfConnectionRemoteBuilder.build()) {
			return rdfConnection.queryConstruct(query);
		}
	}

	private Optional<ModelPackage> findByPackageByUrn(ModelPackageUrn modelsPackage) {
		final Query query = SparqlQueries.buildFindByPackageQuery(modelsPackage);
		final AtomicReference<String> aspectModel = new AtomicReference<>();
		try (final RDFConnection rdfConnection = rdfConnectionRemoteBuilder.build()) {
			rdfConnection.querySelect(query,
				result -> aspectModel.set(result.get(SparqlQueries.STATUS_RESULT).toString()));
		}
		if (aspectModel.get() != null) {
			return Optional.of(new ModelPackage(ModelPackageStatus.valueOf(aspectModel.get())));
		}
		return Optional.empty();
	}

	private SemanticModel findByUrn(final AspectModelUrn urn) {
		final Query query = SparqlQueries.buildFindByUrnQuery(urn);
		final AtomicReference<SemanticModel> aspectModel = new AtomicReference<>();
		try (final RDFConnection rdfConnection = rdfConnectionRemoteBuilder.build()) {
			rdfConnection.querySelect(query,
				result -> aspectModel.set(TripleStorePersistence.aspectModelFrom(result)));
		}
		return aspectModel.get();
	}

	private Model findJenaModelByUrn(final AspectModelUrn urn) {
		final Query constructQuery = SparqlQueries.buildFindByUrnConstructQuery(urn);
		try (final RDFConnection rdfConnection = rdfConnectionRemoteBuilder.build()) {
			return rdfConnection.queryConstruct(constructQuery);
		}
	}

	private Try<AspectModelUrn> getAspectModelUrn(String urn) {
		try {
			return Try.success(AspectModelUrn.fromUrn(urn));
		} catch (UrnSyntaxException var2) {
			return Try.failure(var2);
		}
	}

	private void validateStatusParameter(SemanticModelStatus status) {
		if (ObjectUtils.allNull(status)) {
			throw new IllegalArgumentException(
				"SemanticModelStatus cannot be null. Valid values are: DRAFT, RELEASED, STANDARDIZED, DEPRECATED.");
		}
	}
}
