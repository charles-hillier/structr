/**
 * Copyright (C) 2010-2017 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.resource;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PagingHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.ViewTransformation;
import org.structr.core.graph.NodeFactory;
import org.structr.core.property.PropertyKey;

/**
 *
 *
 */
public class TransformationResource extends WrappingResource {

	private static final Logger logger = LoggerFactory.getLogger(TransformationResource.class.getName());

	private ViewTransformation transformation = null;

	public TransformationResource(SecurityContext securityContext, ViewTransformation transformation) {
		this.securityContext = securityContext;
		this.transformation  = transformation;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {
		return false;	// no direct instantiation
	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page) throws FrameworkException {

		if(wrappedResource != null && transformation != null) {

			// allow view transformation to avoid evaluation of wrapped resource
			if (transformation.evaluateWrappedResource()) {

				Result result = wrappedResource.doGet(sortKey, sortDescending, NodeFactory.DEFAULT_PAGE_SIZE, NodeFactory.DEFAULT_PAGE);

				try {

					transformation.apply(securityContext, result.getResults());
					result.setRawResultCount(result.size());

				} catch(Throwable t) {
					logger.warn("", t);
				}

				// apply paging later
				return PagingHelper.subResult(result, pageSize, page);

			} else {

				List<? extends GraphObject> listToTransform = new LinkedList<GraphObject>();
				transformation.apply(securityContext, listToTransform);

				Result result = new Result(listToTransform, listToTransform.size(), wrappedResource.isCollectionResource(), wrappedResource.isPrimitiveArray());

				// apply paging later
				return PagingHelper.subResult(result, pageSize, page);

			}
		}

		List emptyList = Collections.emptyList();
		return new Result(emptyList, null, isCollectionResource(), isPrimitiveArray());
	}

	@Override
	public String getResourceSignature() {
		if(wrappedResource != null) {
			return wrappedResource.getResourceSignature();
		}

		return "";
	}
}
