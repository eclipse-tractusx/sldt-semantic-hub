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

import org.eclipse.tractusx.semantics.hub.domain.ModelPackageStatus;
import org.eclipse.tractusx.semantics.hub.domain.ModelPackageUrn;

import com.google.common.collect.Lists;

import ch.qos.logback.core.pattern.LiteralConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.impl.XSDPlainType;
import org.apache.jena.graph.impl.AdhocDatatype;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.impl.RDFListImpl;
import org.apache.jena.reasoner.rulesys.FunctorDatatype;
import org.apache.jena.update.UpdateRequest;

import org.eclipse.esmf.aspectmodel.urn.AspectModelUrn;

public class SparqlQueries {
   private static final String AUXILIARY_NAMESPACE = "urn:samm:org.eclipse.esmf.samm:aspect-model:aux#";

   private static final String NAME_TYPE_NAME = "_NAME_";
   private static final String NAME_TYPE_DESCRIPTION = "_DESCRIPTION_";

   public static final String ASPECT = "aspect";
   public static final String STATUS_RESULT = "statusResult";
   public static final String SAMM_ASPECT_URN_REGEX = "(urn:samm:org.eclipse.esmf.samm:meta-model:\\d\\.\\d\\.\\d#Aspect)|(urn:bamm:io.openmanufacturing:meta-model:\\d\\.\\d\\.\\d#Aspect)";
   public static final String BAMM_ASPECT_URN_REGEX = "(urn:samm:org.eclipse.esmf.samm:meta-model:\\d\\.\\d\\.\\d#Aspect)|(urn:bamm:io.openmanufacturing:meta-model:\\d\\.\\d\\.\\d#Aspect)";
   public static final String ALL_SAMM_ASPECT_URN_PREFIX = "(urn:samm:org.eclipse.esmf.samm:([a-z]|-)+:\\d\\.\\d\\.\\d#)|(urn:bamm:io.openmanufacturing:([a-z]|-)+:\\d\\.\\d\\.\\d#)";
   public static final String SAMM_ASPECT_URN_PREFIX = "(urn:samm:org.eclipse.esmf.samm:meta-model:\\d\\.\\d\\.\\d#)|(urn:bamm:io.openmanufacturing:meta-model:\\d\\.\\d\\.\\d#)";
   public static final String SAMM_PREFERRED_NAME = "urn:samm:org.eclipse.esmf.samm:meta-model:1.0.0#preferredName";
   public static final String SAMM_DESCRIPTION = "urn:samm:org.eclipse.esmf.samm:meta-model:1.0.0#description";

   public static final Property STATUS_PROPERTY = ResourceFactory.createProperty( AUXILIARY_NAMESPACE, "status" );

   private static final String DELETE_BY_URN_QUERY =
         "DELETE { ?s ?p ?o . } \n"
               + "WHERE \n"
               + "{ \n"
               + "    ?s ?p ?o ;\n"
               + "    bind( $urnParam as ?urn )\n"
               + "    FILTER ( strstarts(str(?s), ?urn) )\n"
               + "    ?s ?p ?o .\n"
               + "}\n";

   private static final String CONSTRUCT_BY_URN_QUERY =
         "CONSTRUCT {\n"
               + " ?s ?p ?o .\n"
               + "} WHERE {\n"
               + "    bind( ns: as ?aspect )\n"
               + "    ?aspect (<>|!<>)* ?s .\n"
               + "    ?otherAspect (<>|!<>)* ?s .\n"
               + "    ?s ?p ?o .\n"
               + "}";

   /**
    * This query finds all elements belonging to the provided urn
    */
   private static final String FIND_MODEL_ELEMENT_CLOSURE =
         "CONSTRUCT {\n"
               + " ?s ?p ?o .\n"
               + "} WHERE {\n"
               + "    bind( ns: as ?urn )\n"
               + "    ?urn (<>|!<>)* ?s .\n"
               + "    ?s ?p ?o .\n"
               + "}";

   private static final String FIND_BY_URN_QUERY =
         "SELECT  ?aspect (?status as ?statusResult)\n"
               + "WHERE\n"
               + "  {\n"
               + "      bind( $urnParam as ?urn ) \n"
               + "      bind( $packageUrnParam as ?packageUrn ) \n"
               + "      bind( $bammAspectUrnParam as ?bammAspectUrn )\n"
               + "      ?aspect a ?bammAspect .\n"
               + "      ?s aux:status ?status .\n"
               + "      FILTER ( regex(str(?bammAspect), ?bammAspectUrn, \"\") && ( str(?aspect) = ?urn )\n"
               + "            && (str(?s) = ?packageUrn) )\n"
               + "  }";

   private static final String FIND_BY_MULTIPLE_URNS_QUERY =
         "SELECT DISTINCT ?aspect (?status as ?statusResult)\n"
               + "WHERE\n"
               + "  {\n"
               + "      VALUES (?urns) { ?urnParamList } \n"
               + "      VALUES (?packageUrns) { ?packageUrnParamList } \n"
               + "      bind( $bammAspectUrnParam as ?bammAspectUrn )\n"
               + "      ?aspect a ?bammAspect .\n"
               + "      ?s aux:status ?status .\n"
               + "      FILTER ( regex(str(?bammAspect), ?bammAspectUrn, \"\") \n"
               + "      && ( str(?aspect) IN (?urns) )  \n"
               + "      && ( str(?s) IN (?packageUrns)) ) \n"
               + "  }"
               + "ORDER BY lcase(str(?aspect))\n"
               + "OFFSET  $offsetParam\n"
               + "LIMIT   $limitParam";

   private static final String FIND_BY_PACKAGE_URN_QUERY =
         "SELECT (?status as ?statusResult)\n"
               + "WHERE\n"
               + "  {\n"
               + "      bind( $urnParam as ?urn ) \n"
               + "      ?s aux:status ?status .\n"
               + "      FILTER ( ( str(?s) = ?urn ) )\n"
               + "      ?s aux:status ?status .\n"
               + "  }";

   private static final String FILTER_QUERY_MINIMAL_WHERE_CLAUSE = "WHERE {\n"
           + "BIND($bammAspectUrnRegexParam AS ?bammAspectUrnRegex)\n"
           + "BIND(iri($bammFieldToSearchInParam) AS ?bammFieldToSearchIn)\n"
           + "BIND($bammFieldSearchValueParam AS ?bammFieldSearchValue)\n"
           + "BIND($statusFilterParam AS ?statusFilter)\n"
           + "BIND($namespaceFilterParam AS ?namespaceFilter)\n"
           + "?aspect  a ?bammAspect .\n"
           + "FILTER regex(str(?bammAspect), ?bammAspectUrnRegex, \"\")\n"
           + "BIND(iri(concat(strbefore(str(?aspect ), \"#\"), \"#\")) AS ?package)\n"
           + "?package  aux:status  ?status\n"
           + "FILTER ( !bound(?statusFilter) || contains(str(?status), ?statusFilter) )\n"
           + "FILTER ( !bound(?namespaceFilter) || contains(lcase(str(?aspect)), lcase(?namespaceFilter) ) )\n"
           + "}\n";

   private static final String FILTER_QUERY_MINIMAL_WHERE_CLAUSE_SELECTIVE = "WHERE {\n"
           + "BIND($bammAspectUrnRegexParam AS ?bammAspectUrnRegex)\n"
           + "BIND(iri($bammFieldToSearchInParam) AS ?bammFieldToSearchIn)\n"
           + "BIND($bammFieldSearchValueParam AS ?bammFieldSearchValue)\n"
           + "BIND($statusFilterParam AS ?statusFilter)\n"
           + "BIND($namespaceFilterParam AS ?namespaceFilter)\n"
           + "VALUES (?urns) { ?urnParamList } \n"
           + "?aspect  a ?bammAspect .\n"
           + "FILTER regex(str(?bammAspect), ?bammAspectUrnRegex, \"\")\n"
           + "BIND(iri(concat(strbefore(str(?aspect ), \"#\"), \"#\")) AS ?package)\n"
           + "?package  aux:status  ?status\n"
           + "FILTER ( !bound(?statusFilter) || contains(str(?status), ?statusFilter) )\n"
           + "FILTER ( !bound(?namespaceFilter) || contains(lcase(str(?aspect)), lcase(?namespaceFilter) ) )\n"
           + "FILTER ( str(?aspect) IN (?urns) ) "
           + "}\n";

   private static final String FIND_ALL_MINIMAL_QUERY =
           "SELECT DISTINCT ?aspect (?status as ?statusResult)\n"
                   + FILTER_QUERY_MINIMAL_WHERE_CLAUSE
                   + "ORDER BY lcase(str(?aspect))\n"
                   + "OFFSET  $offsetParam\n"
                   + "LIMIT   $limitParam";

   private static final String COUNT_ASPECT_MODELS_MINIMAL_QUERY =
           "SELECT (count(DISTINCT ?aspect) as ?aspectModelCount)\n"
                   + FILTER_QUERY_MINIMAL_WHERE_CLAUSE;

   private static final String COUNT_ASPECT_MODELS_MINIMAL_QUERY_SELECTIVE =
            "SELECT (count(DISTINCT ?aspect) as ?aspectModelCount)\n"
                  + FILTER_QUERY_MINIMAL_WHERE_CLAUSE_SELECTIVE;

   private SparqlQueries() {
   }

   public static Query buildFindByUrnQuery( final AspectModelUrn urn ) {
      final ParameterizedSparqlString pss = create( FIND_BY_URN_QUERY );
      pss.setLiteral( "$urnParam", urn.toString() );
      pss.setLiteral( "$bammAspectUrnParam", SAMM_ASPECT_URN_REGEX );
      pss.setLiteral( "$packageUrnParam", ModelPackageUrn.fromUrn( urn ).getUrn() );
      return pss.asQuery();
   }
   
   public static Query buildFindListByUrns( final List<AspectModelUrn> urns, int page, int pageSize ) {
      final ParameterizedSparqlString pss = create( FIND_BY_MULTIPLE_URNS_QUERY );

      List<RDFNode> urnList = new ArrayList<>();
      List<RDFNode> modelPackageUrnList = new ArrayList<>();

      urns.forEach((AspectModelUrn urn) -> {
         urnList.add(ResourceFactory.createStringLiteral(urn.toString()));
         modelPackageUrnList.add(ResourceFactory.createStringLiteral(ModelPackageUrn.fromUrn(urn).getUrn()));
      });

      pss.setValues("urnParamList", urnList);
      pss.setLiteral( "$bammAspectUrnParam", SAMM_ASPECT_URN_REGEX );
      pss.setValues( "packageUrnParamList", modelPackageUrnList );
      pss.setLiteral("offsetParam", getOffset(page, pageSize));
      pss.setLiteral("limitParam", pageSize);
      
      return pss.asQuery();
   }

   public static Query buildCountAspectModelsQuery( String namespaceFilter,
         ModelPackageStatus status ) {
      return buildMinimalQuery(COUNT_ASPECT_MODELS_MINIMAL_QUERY, namespaceFilter, status).asQuery();
   }

   public static Query buildCountSelectiveAspectModelsQuery( String namespaceFilter, String nameFilter, String nameType,
         ModelPackageStatus status, List<AspectModelUrn> urns ) {
      ParameterizedSparqlString pss = buildMinimalQuery(COUNT_ASPECT_MODELS_MINIMAL_QUERY_SELECTIVE, namespaceFilter, status);
      
      List<RDFNode> urnList = new ArrayList<>();
      List<RDFNode> modelPackageUrnList = new ArrayList<>();

      urns.forEach((AspectModelUrn urn) -> {
         urnList.add(ResourceFactory.createStringLiteral(urn.toString()));
         modelPackageUrnList.add(ResourceFactory.createStringLiteral(ModelPackageUrn.fromUrn(urn).getUrn()));
      });

      pss.setValues("urnParamList", urnList);
      pss.setValues( "packageUrnParamList", modelPackageUrnList );

      return pss.asQuery();
   }

   public static Query buildFindByPackageQuery( final ModelPackageUrn modelsPackage ) {
      final ParameterizedSparqlString pss = create( FIND_BY_PACKAGE_URN_QUERY );
      pss.setLiteral( "$urnParam", modelsPackage.getUrn() );
      return pss.asQuery();
   }

   public static UpdateRequest buildDeleteByUrnRequest( final ModelPackageUrn modelsPackage ) {
      final ParameterizedSparqlString pss = create( DELETE_BY_URN_QUERY );
      pss.setLiteral( "$urnParam", modelsPackage.getUrn() );
      return pss.asUpdate();
   }

   public static Query echoQuery() {
      return create("ASK {}").asQuery();
   }


   /**
    * Returns a Sparql Query based on the given filter parameters.
    *
    * If nameFilter and nameType is provided an extended query is returned, else a simple query.
    *
    * Simple Query:
    *    Searches all aspect models based on status and namespaceFilter.
    *
    * Extended Query:
    *    Searches all aspect models based on status and namespaceFilter and any model elements that matches the nameType and nameFilter.
    *
    * Because the whole graph database has to be scanned for the property name search,
    * the extended query is very slow. With 10 Aspect Models the query takes 9 seconds to complete.
    *
    * The simple query needs 50 milliseconds for the same dataset.
    *
    * @param namespaceFilter searches for any namespace matching this parameter
    * @param status matches the package status
    * @param page the page to retrieve
    * @param pageSize the page size
    * @return a Sparql query with the provided search filters
    */
   public static Query buildFindAllQuery( String namespaceFilter, ModelPackageStatus status,
         int page, int pageSize ) {

      final ParameterizedSparqlString pss = buildMinimalQuery(FIND_ALL_MINIMAL_QUERY, namespaceFilter, status);
      pss.setLiteral( "$limitParam", pageSize );
      pss.setLiteral( "$offsetParam", getOffset( page, pageSize ) );
      return pss.asQuery();
   }

   private static ParameterizedSparqlString buildMinimalQuery(String query, String namespaceFilter, ModelPackageStatus status){
      final ParameterizedSparqlString pss = create( query );
      pss.setLiteral( "$bammAspectUrnRegexParam", SAMM_ASPECT_URN_REGEX );
      if ( StringUtils.isNotBlank( namespaceFilter ) ) {
         pss.setLiteral( "$namespaceFilterParam", namespaceFilter );
      }

      if ( status != null ) {
         pss.setLiteral( "$statusFilterParam", status.name() );
      }
      return pss;
   }

   private static ParameterizedSparqlString buildExtendedSearchQuery( String query, String namespaceFilter,
         String nameFilter, String nameType,
         ModelPackageStatus status ) {
      final ParameterizedSparqlString pss = create( query );
      pss.setLiteral( "$bammAspectUrnRegexParam", SAMM_ASPECT_URN_REGEX );
      boolean nameFilterExists = StringUtils.isNotBlank( nameFilter );

      if ( NAME_TYPE_NAME.equals( nameType ) && nameFilterExists ) {
         pss.setLiteral( "$bammFieldToSearchInParam", SAMM_PREFERRED_NAME );
         pss.setLiteral( "$bammFieldSearchValueParam", nameFilter );
      } else if ( NAME_TYPE_DESCRIPTION.equals( nameType ) && nameFilterExists ) {
         pss.setLiteral( "$bammFieldToSearchInParam", SAMM_DESCRIPTION );
         pss.setLiteral( "$bammFieldSearchValueParam", nameFilter );
      } else if ( StringUtils.isNotBlank( nameType ) && nameFilterExists ) {
         pss.setLiteral( "$bammTypeUrnRegexParam", SAMM_ASPECT_URN_PREFIX + nameType.replace( "samm:", "" ).strip() );
         pss.setLiteral( "$bammFieldToSearchInParam", SAMM_PREFERRED_NAME );
         pss.setLiteral( "$bammFieldSearchValueParam", nameFilter );
      } else {
         pss.setLiteral( "$bammFieldToSearchInParam", SAMM_PREFERRED_NAME );
         pss.setLiteral( "$bammTypeUrnRegexParam", SAMM_ASPECT_URN_REGEX );
      }
      if ( status != null ) {
         pss.setLiteral( "$statusFilterParam", status.name() );
      }
      if ( StringUtils.isNotBlank( namespaceFilter ) ) {
         pss.setLiteral( "$namespaceFilterParam", namespaceFilter );
      }
      return pss;
   }

   private static Integer getOffset( int page, int pageSize ) {
      if ( page == 0 ) {
         return page;
      }
      if ( page == 1){
         return page * pageSize;
      }
      return (page - 1) * pageSize;
   }

   public static Query buildFindByUrnConstructQuery( final AspectModelUrn urn ) {
      final ParameterizedSparqlString pss = create( CONSTRUCT_BY_URN_QUERY );
      pss.setNsPrefix( "ns", urn.getUrn().toString() );
      return pss.asQuery();
   }

   public static Query buildFindModelElementClosureQuery( final String urn ) {
      final ParameterizedSparqlString pss = create( FIND_MODEL_ELEMENT_CLOSURE );
      pss.setNsPrefix( "ns", urn );
      return pss.asQuery();
   }

   private static ParameterizedSparqlString create( final String query ) {
      final ParameterizedSparqlString pss = new ParameterizedSparqlString();
      pss.setCommandText( query );
      pss.setNsPrefix( "aux", AUXILIARY_NAMESPACE );
      return pss;
   }
}
