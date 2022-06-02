/*
 * Copyright (c) 2022 Bosch Software Innovations GmbH. All rights reserved.
 */

package org.eclipse.tractusx.semantics.hub;

import org.eclipse.tractusx.semantics.hub.domain.ModelPackageUrn;

public class ModelPackageNotFoundException extends RuntimeException {
   public ModelPackageNotFoundException( final ModelPackageUrn urn ) {
      super( String.format( "Model package with urn [%s] not found.", urn ) );
   }
}
