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

package org.teleal.cling.support.avtransport.callback;

import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.UnsignedIntegerFourBytes;
import org.teleal.cling.support.model.PositionInfo;

import java.util.logging.Logger;

/**
 *
 * @author Christian Bauer
 */
public abstract class GetPositionInfo extends ActionCallback {

    private static Logger log = Logger.getLogger(GetPositionInfo.class.getName());

    public GetPositionInfo(Service service) {
        this(new UnsignedIntegerFourBytes(0), service);
    }

    public GetPositionInfo(UnsignedIntegerFourBytes instanceId, Service service) {
        super(new ActionInvocation(service.getAction("GetPositionInfo")));
        getActionInvocation().setInput("InstanceID", instanceId);
    }

    public void success(ActionInvocation invocation) {
        PositionInfo positionInfo = new PositionInfo(invocation.getOutputMap());
        received(invocation, positionInfo);
    }

    public abstract void received(ActionInvocation invocation, PositionInfo positionInfo);

}