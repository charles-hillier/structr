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
package org.structr.core.function;

import java.util.Arrays;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.PropertyKey;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.SchemaHelper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

public class EnumInfoFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_ENUM_INFO    = "Usage: ${enum_info(type, enumProperty)}. Example ${enum_info('Document', 'documentType')}";
	public static final String ERROR_MESSAGE_ENUM_INFO_JS = "Usage: ${Structr.enum_info(type, enumProperty)}. Example ${Structr.enum_info('Document', 'documentType')}";

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			final ConfigurationProvider config = StructrApp.getConfiguration();
			final String typeName = sources[0].toString();
			final String enumPropertyName = sources[1].toString();
			final Class type = SchemaHelper.getEntityClassForRawType(typeName);

			if (type != null) {

				final PropertyKey key = config.getPropertyKeyForJSONName(type, enumPropertyName, false);
				if (key != null) {

					if (key instanceof EnumProperty) {
						final String formatString = SchemaHelper.getPropertyInfo(ctx.getSecurityContext(), key).get("format").toString();

						return Arrays.asList(formatString.replace(" ", "").split(","));

					} else {

						logger.warn("Error: Not an Enum property \"{}.{}\"", typeName, enumPropertyName);
						return "Not an Enum property " + typeName + "." + enumPropertyName;
					}

				} else {

					logger.warn("Error: Unknown property \"{}.{}\"", typeName, enumPropertyName);
					return "Unknown property " + typeName + "." + enumPropertyName;
				}

			} else {

				logger.warn("Error: Unknown type \"{}\"", typeName);
				return "Unknown type " + typeName;
			}


		}

		return null;

	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_ENUM_INFO_JS : ERROR_MESSAGE_ENUM_INFO);
	}

	@Override
	public String shortDescription() {
		return "Returns the enum values as an array";
	}

	@Override
	public String getName() {
		return "enum_info()";
	}

}