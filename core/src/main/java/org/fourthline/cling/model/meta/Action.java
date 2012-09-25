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

package org.teleal.cling.model.meta;

import org.teleal.cling.controlpoint.ActionCallback.ActionCallbackInterceptor;
import org.teleal.cling.model.ModelUtil;
import org.teleal.cling.model.Validatable;
import org.teleal.cling.model.ValidationError;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Describes an action and its input/output arguments.
 *
 * @author Christian Bauer
 */
public class Action<S extends Service> implements Validatable {

    final private static Logger log = Logger.getLogger(Action.class.getName());

    final private String name;
    final private ActionArgument[] arguments;
    final private ActionArgument[] inputArguments;
    final private ActionArgument[] outputArguments;

    // Package mutable state
    private S service;
    
    private ActionCallbackInterceptor actionCallbackInterceptor;


    public void setActionCallbackInterceptor(ActionCallbackInterceptor actionCallbackInterceptor) {
    	this.actionCallbackInterceptor = actionCallbackInterceptor;
    }
    
    public ActionCallbackInterceptor getActionCallbackInterceptor() {
    	return actionCallbackInterceptor;
    }


    public Action(String name, ActionArgument[] arguments) {
        this.name = name;
        if (arguments != null) {

            List<ActionArgument> inputList= new ArrayList();
            List<ActionArgument> outputList = new ArrayList();

            for (ActionArgument argument : arguments) {
                argument.setAction(this);
                if (argument.getDirection().equals(ActionArgument.Direction.IN))
                    inputList.add(argument);
                if (argument.getDirection().equals(ActionArgument.Direction.OUT))
                    outputList.add(argument);
            }

            this.arguments = arguments;
            this.inputArguments = inputList.toArray(new ActionArgument[inputList.size()]);
            this.outputArguments = outputList.toArray(new ActionArgument[outputList.size()]);
        } else {
            this.arguments = new ActionArgument[0];
            this.inputArguments = new ActionArgument[0];
            this.outputArguments = new ActionArgument[0];
        }
    }

    public String getName() {
        return name;
    }

    public boolean hasArguments() {
        return getArguments() != null && getArguments().length > 0;
    }

    public ActionArgument[] getArguments() {
        return arguments;
    }

    public S getService() {
        return service;
    }

    void setService(S service) {
        if (this.service != null)
            throw new IllegalStateException("Final value has been set already, model is immutable");
        this.service = service;
    }

    public ActionArgument<S> getFirstInputArgument() {
        if (!hasInputArguments()) throw new IllegalStateException("No input arguments: " + this);
        return getInputArguments()[0];
    }

    public ActionArgument<S> getFirstOutputArgument() {
        if (!hasOutputArguments()) throw new IllegalStateException("No output arguments: " + this);
        return getOutputArguments()[0];
    }

    public ActionArgument<S>[] getInputArguments() {
        return inputArguments;
    }

    public ActionArgument<S> getInputArgument(String name) {
        for (ActionArgument<S> arg : getInputArguments()) {
            if (arg.isNameOrAlias(name)) return arg;
        }
        return null;
    }

    public ActionArgument<S>[] getOutputArguments() {
        return outputArguments;
    }

    public ActionArgument<S> getOutputArgument(String name) {
        for (ActionArgument<S> arg : getOutputArguments()) {
            if (arg.getName().equals(name)) return arg;
        }
        return null;
    }

    public boolean hasInputArguments() {
        return getInputArguments() != null && getInputArguments().length > 0;
    }

    public boolean hasOutputArguments() {
        return getOutputArguments() != null && getOutputArguments().length > 0;
    }


    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() +
                ", Arguments: " + (getArguments() != null ? getArguments().length : "NO ARGS") +
                ") " + getName();
    }

    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList();

        if (getName() == null || getName().length() == 0) {
            errors.add(new ValidationError(
                    getClass(),
                    "name",
                    "Action without name of: " + getService()
            ));
        } else if (!ModelUtil.isValidUDAName(getName())) {
            log.warning("UPnP specification violation of: " + getService().getDevice());
            log.warning("Invalid action name: " + this);
        }

        for (ActionArgument actionArgument : getArguments()) {
            // Check argument relatedStateVariable in service state table

            if (getService().getStateVariable(actionArgument.getRelatedStateVariableName()) == null) {
                errors.add(new ValidationError(
                        getClass(),
                        "arguments",
                        "Action argument references an unknown state variable: " + actionArgument.getRelatedStateVariableName()
                ));
            }
        }

        ActionArgument retValueArgument = null;
        int retValueArgumentIndex = 0;
        int i = 0;
        for (ActionArgument actionArgument : getArguments()) {
            // Check retval
            if (actionArgument.isReturnValue()) {
                if (retValueArgument != null) {
                    log.warning("UPnP specification violation of: " + getService().getDevice());
                    log.warning("Only one argument of action '" + getName() + "' can be <retval/>");
                }
                retValueArgument = actionArgument;
                retValueArgumentIndex = i;
            }
            i++;
        }
        if (retValueArgument != null) {
            for (int j = 0; j < retValueArgumentIndex; j++) {
                ActionArgument a = getArguments()[j];
                if (a.getDirection() == ActionArgument.Direction.OUT) {
                    log.warning("UPnP specification violation of: " + getService().getDevice());
                    log.warning("Argument '" + retValueArgument.getName() + "' of action '" + getName() + "' is <retval/> but not the first OUT argument");
                }
            }
        }

        for (ActionArgument argument : arguments) {
            errors.addAll(argument.validate());
        }

        return errors;
    }

    public Action<S> deepCopy() {
        ActionArgument<S>[] actionArgumentsDupe = new ActionArgument[getArguments().length];
        for (int i = 0; i < getArguments().length; i++) {
            ActionArgument arg = getArguments()[i];
            actionArgumentsDupe[i] = arg.deepCopy();
        }

        return new Action<S>(
                getName(),
                actionArgumentsDupe
        );
    }

}
