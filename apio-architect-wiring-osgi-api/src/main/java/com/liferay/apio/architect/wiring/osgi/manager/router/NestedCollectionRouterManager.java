/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.apio.architect.wiring.osgi.manager.router;

import aQute.bnd.annotation.ProviderType;

import com.liferay.apio.architect.auth.Auth;
import com.liferay.apio.architect.operation.Operation;
import com.liferay.apio.architect.routes.NestedCollectionRoutes;

import java.util.List;
import java.util.Optional;

/**
 * Provides methods to retrieve the routes information provided by the different
 * {@link com.liferay.apio.architect.router.NestedCollectionRouter} instances.
 *
 * @author Alejandro Hernández
 * @see    com.liferay.apio.architect.router.NestedCollectionRouter
 */
@ProviderType
public interface NestedCollectionRouterManager {

	/**
	 * Returns the nested collection routes for the nested collection resource's
	 * name.
	 *
	 * @param  name the parent resource's name
	 * @param  nestedName the nested collection resource's name
	 * @return the nested collection routes
	 */
	public <T, S> Optional<NestedCollectionRoutes<T, S>>
		getNestedCollectionRoutesOptional(String name, String nestedName);

	/**
	 * Returns the operations for the nested page resource's class.
	 *
	 * @param  name the parent resource's name
	 * @param  nestedName the nested collection resource's name
	 * @param  auth the actual HTTP authentication information
	 * @param  identifier the parent identifier
	 * @return the list of operations
	 * @review
	 */
	public <S> List<Operation> getOperations(
		String name, String nestedName, Auth auth, S identifier);

}