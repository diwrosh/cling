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

package org.fourthline.cling.osgi.device.light.actions;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPStateVariable;
import org.fourthline.cling.osgi.device.light.variables.LoadLevelStatusStateVariable;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * @author Bruce Green
 */
public class GetLoadLevelStatusAction implements UPnPAction {

    final private static String RET_LOAD_LEVEL_STATUS = "retLoadLevelStatus";
    final private static String[] OUT_ARG_NAMES = {RET_LOAD_LEVEL_STATUS};
    private LoadLevelStatusStateVariable loadLevelStatusStateVariable;

    public GetLoadLevelStatusAction(LoadLevelStatusStateVariable loadLevelStatusStateVariable) {
        this.loadLevelStatusStateVariable = loadLevelStatusStateVariable;
    }

    @Override
    public String getName() {
        return "GetLoadLevelStatus";
    }

    @Override
    public String getReturnArgumentName() {
        return null;
    }

    @Override
    public String[] getInputArgumentNames() {
        return null;
    }

    @Override
    public String[] getOutputArgumentNames() {
        return OUT_ARG_NAMES;
    }

    @Override
    public UPnPStateVariable getStateVariable(String argumentName) {
        return argumentName.equals(RET_LOAD_LEVEL_STATUS) ? loadLevelStatusStateVariable : null;
    }

    @Override
    public Dictionary invoke(Dictionary args) throws Exception {
        Boolean state = (Boolean) loadLevelStatusStateVariable.getCurrentValue();

        args = new Hashtable();
        args.put(RET_LOAD_LEVEL_STATUS, state);

        return args;
    }

}
