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

/**
 *  implementation based on the <em>Xml Pull Parser</em> XML processing API.
 * <p>
 * This processor is more lenient with parsing, look only for the required XML tags
 * </p>
 * @author Michael Pujos
 */
package org.teleal.cling.transport.impl;

import java.util.logging.Logger;

import org.teleal.cling.model.message.gena.IncomingEventRequestMessage;
import org.teleal.cling.model.meta.StateVariable;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.transport.spi.UnsupportedDataException;
import org.xmlpull.v1.XmlPullParser;

public class PullGENAEventProcessor extends GENAEventProcessorImpl {

	private static Logger log = Logger.getLogger(PullGENAEventProcessor.class.getName());

	public void readBody(IncomingEventRequestMessage requestMessage) throws UnsupportedDataException {

		checkRequestBodyValidity(requestMessage);

		String body = requestMessage.getBodyString().trim();

		try {
			XmlPullParser xpp = XmlPullParserUtils.createParser(body);
			readProperties(xpp, requestMessage);
		} catch (Exception ex) {
			throw new UnsupportedDataException("Can't transform message payload: " + ex.getMessage(), ex, body);	
		}
	}


	private void readProperties(XmlPullParser xpp, IncomingEventRequestMessage message) throws Exception {

		// we're inside the propertyset tag
		StateVariable[] stateVariables = message.getService().getStateVariables();
		
		// ignore all but the property tags
		
		int event;
		while((event = xpp.next()) != XmlPullParser.END_DOCUMENT) {
			if(event != XmlPullParser.START_TAG) continue;
			
			if(xpp.getName().equals("property")) {
				readProperty(xpp, message, stateVariables);
			} 
		}

	}


	private void readProperty(XmlPullParser xpp, IncomingEventRequestMessage message, StateVariable[] stateVariables) throws Exception  {

		// we're inside the property tag
	
		int event ;
		do {
			event = xpp.next();
			if(event == XmlPullParser.START_TAG) {

				String stateVariableName = xpp.getName();
				for (StateVariable stateVariable : stateVariables) {
					if (stateVariable.getName().equals(stateVariableName)) {
						log.fine("Reading state variable value: " + stateVariableName);
						String value = xpp.nextText();
						message.getStateVariableValues().add(new StateVariableValue(stateVariable, value));
						//log.info(String.format("%s => %s", stateVariables, value));
						break;
					}
				} 
			}

		} while(event != XmlPullParser.END_DOCUMENT && (event != XmlPullParser.END_TAG || !xpp.getName().equals("property")));
	}
}
