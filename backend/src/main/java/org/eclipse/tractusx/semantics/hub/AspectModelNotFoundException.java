/*
 * Copyright (c) 2022 Bosch Software Innovations GmbH. All rights reserved.
 */

package org.eclipse.tractusx.semantics.hub;

import io.openmanufacturing.sds.aspectmodel.urn.AspectModelUrn;

public class AspectModelNotFoundException extends RuntimeException {

   public AspectModelNotFoundException( final AspectModelUrn urn ) {
      super( String.format( "Aspect Model with urn [%s] not found.", urn ) );
   }
}
