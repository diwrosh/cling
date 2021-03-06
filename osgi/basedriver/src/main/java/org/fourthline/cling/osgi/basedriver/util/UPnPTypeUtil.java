/*
 * Copyright (C) 2011 4th Line GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.fourthline.cling.osgi.basedriver.util;

import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.service.upnp.UPnPLocalStateVariable;
import org.osgi.service.upnp.UPnPStateVariable;

/**
 * @author Bruce Green
 */
public class UPnPTypeUtil {

    private static final Object[][] records = {
		// Integer              ui1, ui2, i1, i2, i4, int
		{	UPnPStateVariable.TYPE_UI1,				Integer.class	},
		{	UPnPStateVariable.TYPE_UI2,				Integer.class	},
		{	UPnPStateVariable.TYPE_I1,				Integer.class	},
		{	UPnPStateVariable.TYPE_I2,				Integer.class	},
		{	UPnPStateVariable.TYPE_I4,				Integer.class	},
		{	UPnPStateVariable.TYPE_INT,				Integer.class	},
		// Long                 ui4, time, time.tz
		{	UPnPStateVariable.TYPE_UI4,				Long.class		},
		{	UPnPStateVariable.TYPE_TIME,			Long.class		},
		{	UPnPStateVariable.TYPE_TIME_TZ,			Long.class		},
		// Float                r4, float
		{	UPnPStateVariable.TYPE_R4,				Float.class		},
		{	UPnPStateVariable.TYPE_FLOAT,			Float.class		},
		// Double               r8, number, fixed.14.4
		{	UPnPStateVariable.TYPE_R8,				Double.class	},
		{	UPnPStateVariable.TYPE_NUMBER,			Double.class	},
		{	UPnPStateVariable.TYPE_FIXED_14_4,		Double.class	},
		// Character            char
		{ UPnPLocalStateVariable.TYPE_CHAR,			Character.class	},
		// String               string, uri, uuid
		{ UPnPLocalStateVariable.TYPE_STRING,		String.class	},
		{ UPnPLocalStateVariable.TYPE_URI,			String.class	},
		{ UPnPLocalStateVariable.TYPE_UUID,			String.class	},
		// Date                 date, dateTime, dateTime.tz
		{ UPnPLocalStateVariable.TYPE_DATE,			Date.class		},
		{ UPnPLocalStateVariable.TYPE_DATETIME,		Date.class		},
		{ UPnPLocalStateVariable.TYPE_DATETIME_TZ,	Date.class		},
		// Boolean
		{ UPnPLocalStateVariable.TYPE_BOOLEAN,		Boolean.class	},
		// byte[]               bin.base64, bin.hex
		{ UPnPLocalStateVariable.TYPE_BIN_BASE64,	byte[].class	},
		{ UPnPLocalStateVariable.TYPE_BIN_HEX,		byte[].class	},
	};

	private static final Map<String, Class<?>> classLookup = new Hashtable<String, Class<?>>();

	static {
		for (Object[] record : records) {
			classLookup.put((String) record[0], (Class<?>) record[1]);
		}
	}

	public static Class<?> getUPnPClass(String type) {
		return classLookup.get(type);
	}

}
