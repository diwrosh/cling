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

package org.teleal.cling.registry;

import org.teleal.cling.UpnpService;
import org.teleal.cling.UpnpServiceConfiguration;
import org.teleal.cling.model.ExpirationDetails;
import org.teleal.cling.model.ServiceReference;
import org.teleal.cling.model.gena.LocalGENASubscription;
import org.teleal.cling.model.gena.RemoteGENASubscription;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.RemoteDeviceIdentity;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.resource.Resource;
import org.teleal.cling.model.resource.ResourceProcessor;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.protocol.ProtocolFactory;

// import javax.enterprise.context.ApplicationScoped;
// import javax.inject.Inject;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of {@link Registry}.
 *
 * @author Christian Bauer
 */
// @ApplicationScoped
public class RegistryImpl implements Registry {

    private static Logger log = Logger.getLogger(Registry.class.getName());

    protected UpnpService upnpService;
    protected RegistryMaintainer registryMaintainer;
    protected ReentrantLock remoteSubscriptionsLock = new ReentrantLock(true);
    protected ResourceProcessor resourceMissingProcessor;	

    public RegistryImpl() {
    }

    /**
     * Starts background maintenance immediately.
     */
    // @Inject
    public RegistryImpl(UpnpService upnpService) {
        log.fine("Creating Registry: " + getClass().getName());

        this.upnpService = upnpService;

        log.fine("Starting registry background maintenance...");
        registryMaintainer = createRegistryMaintainer();
        if (registryMaintainer != null) {
            getConfiguration().getRegistryMaintainerExecutor().execute(registryMaintainer);
        }
    }

    public UpnpService getUpnpService() {
        return upnpService;
    }

    public UpnpServiceConfiguration getConfiguration() {
        return getUpnpService().getConfiguration();
    }

    public ProtocolFactory getProtocolFactory() {
        return getUpnpService().getProtocolFactory();
    }

    protected RegistryMaintainer createRegistryMaintainer() {
        return new RegistryMaintainer(
                this,
                getConfiguration().getRegistryMaintenanceIntervalMillis()
        );
    }

    // #################################################################################################

    protected final Set<RegistryListener> registryListeners = new HashSet();
    protected final Set<RegistryItem<URI, Resource>> resourceItems = new HashSet();
    protected final List<Runnable> pendingExecutions = new ArrayList();

    protected final RemoteItems remoteItems = new RemoteItems(this);
    protected final LocalItems localItems = new LocalItems(this);

    // #################################################################################################

    synchronized public void addListener(RegistryListener listener) {
        registryListeners.add(listener);
    }

    synchronized public void removeListener(RegistryListener listener) {
        registryListeners.remove(listener);
    }

    synchronized public Collection<RegistryListener> getListeners() {
        return Collections.unmodifiableCollection(registryListeners);
    }

    synchronized public boolean notifyDiscoveryStart(final RemoteDevice device) {
        // Exit if we have it already, this is atomic inside this method, finally
        if (getUpnpService().getRegistry().getRemoteDevice(device.getIdentity().getUdn(), true) != null) {
            log.finer("Not notifying listeners, already registered: " + device);
            return false;
        }
        for (final RegistryListener listener : getListeners()) {
            getConfiguration().getRegistryListenerExecutor().execute(
                    new Runnable() {
                        public void run() {
                            listener.remoteDeviceDiscoveryStarted(RegistryImpl.this, device);
                        }
                    }
            );
        }
        return true;
    }

    synchronized public void notifyDiscoveryFailure(final RemoteDevice device, final Exception ex) {
        for (final RegistryListener listener : getListeners()) {
            getConfiguration().getRegistryListenerExecutor().execute(
                    new Runnable() {
                        public void run() {
                            listener.remoteDeviceDiscoveryFailed(RegistryImpl.this, device, ex);
                        }
                    }
            );
        }
    }

    // #################################################################################################
    
    synchronized public void setLocalDeviceAdvertising(LocalDevice localDevice, boolean isAdvertising) {
    	localItems.setDeviceAdvertising(localDevice, isAdvertising);
    }

    synchronized public void addDevice(LocalDevice localDevice) {
        localItems.add(localDevice);
    }

    synchronized public void addDevice(RemoteDevice remoteDevice) {
        remoteItems.add(remoteDevice);
    }

    synchronized public boolean update(RemoteDeviceIdentity rdIdentity) {
        return remoteItems.update(rdIdentity);
    }

    synchronized public boolean removeDevice(LocalDevice localDevice) {
        return localItems.remove(localDevice);
    }

    synchronized public boolean removeDevice(RemoteDevice remoteDevice) {
        return remoteItems.remove(remoteDevice);
    }

    synchronized public void removeAllLocalDevices() {
        localItems.removeAll();
    }

    synchronized public void removeAllRemoteDevices() {
        remoteItems.removeAll();
    }

    synchronized public boolean removeDevice(UDN udn) {
        Device device = getDevice(udn, true);
        if (device != null && device instanceof LocalDevice)
            return removeDevice((LocalDevice) device);
        if (device != null && device instanceof RemoteDevice)
            return removeDevice((RemoteDevice) device);
        return false;
    }

    synchronized public Device getDevice(UDN udn, boolean rootOnly) {
        Device device;
        if ((device = localItems.get(udn, rootOnly)) != null) return device;
        if ((device = remoteItems.get(udn, rootOnly)) != null) return device;
        return null;
    }

    synchronized public LocalDevice getLocalDevice(UDN udn, boolean rootOnly) {
        return localItems.get(udn, rootOnly);
    }

    synchronized public RemoteDevice getRemoteDevice(UDN udn, boolean rootOnly) {
        return remoteItems.get(udn, rootOnly);
    }

    synchronized public Collection<LocalDevice> getLocalDevices() {
        return Collections.unmodifiableCollection(localItems.get());
    }

    synchronized public Collection<RemoteDevice> getRemoteDevices() {
        return Collections.unmodifiableCollection(remoteItems.get());
    }

    synchronized public Collection<Device> getDevices() {
        Set all = new HashSet();
        all.addAll(localItems.get());
        all.addAll(remoteItems.get());
        return Collections.unmodifiableCollection(all);
    }

    synchronized public Collection<Device> getDevices(DeviceType deviceType) {
        Collection<Device> devices = new HashSet();

        devices.addAll(localItems.get(deviceType));
        devices.addAll(remoteItems.get(deviceType));

        return Collections.unmodifiableCollection(devices);
    }

    synchronized public Collection<Device> getDevices(ServiceType serviceType) {
        Collection<Device> devices = new HashSet();

        devices.addAll(localItems.get(serviceType));
        devices.addAll(remoteItems.get(serviceType));

        return Collections.unmodifiableCollection(devices);
    }

    synchronized public Service getService(ServiceReference serviceReference) {
        Device device;
        if ((device = getDevice(serviceReference.getUdn(), false)) != null) {
            return device.findService(serviceReference.getServiceId());
        }
        return null;
    }

    // #################################################################################################

    synchronized public Resource getResource(URI pathQuery) throws IllegalArgumentException {
        if (pathQuery.isAbsolute()) {
            throw new IllegalArgumentException("Resource URI can not be absolute, only path and query:" + pathQuery);
        }

        // Note: Uses field access on resourceItems for performance reasons

		for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
        	Resource resource = resourceItem.getItem();
        	if (resource.matches(pathQuery)) {
                return resource;
            }
        }

        // TODO: UPNP VIOLATION: Fuppes on my ReadyNAS thinks it's a cool idea to add a slash at the end of the callback URI...
        // It also cuts off any query parameters in the callback URL - nice!
        if (pathQuery.getPath().endsWith("/")) {
            URI pathQueryWithoutSlash = URI.create(pathQuery.toString().substring(0, pathQuery.toString().length() - 1));

 			for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
            	Resource resource = resourceItem.getItem();
            	if (resource.matches(pathQueryWithoutSlash)) {
                    return resource;
                }
            }
        }

        return null;
    }

    synchronized public <T extends Resource> T getResource(Class<T> resourceType, URI pathQuery) throws IllegalArgumentException {
        Resource resource = getResource(pathQuery);
        if (resource != null && resourceType.isAssignableFrom(resource.getClass())) {
            return (T) resource;
        }
        return null;
    }

    synchronized public Collection<Resource> getResources() {
        Collection<Resource> s = new HashSet();
        for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
            s.add(resourceItem.getItem());
        }
        return s;
    }

    synchronized public <T extends Resource> Collection<T> getResources(Class<T> resourceType) {
        Collection<T> s = new HashSet();
        for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
            if (resourceType.isAssignableFrom(resourceItem.getItem().getClass()))
                s.add((T) resourceItem.getItem());
        }
        return s;
    }

    synchronized public void addResource(Resource resource) {
        addResource(resource, ExpirationDetails.UNLIMITED_AGE);
    }

    synchronized public void addResource(Resource resource, int maxAgeSeconds) {
        RegistryItem resourceItem = new RegistryItem(resource.getPathQuery(), resource, maxAgeSeconds);
        resourceItems.remove(resourceItem);
        resourceItems.add(resourceItem);
    }

    synchronized public boolean removeResource(Resource resource) {
        return resourceItems.remove(new RegistryItem(resource.getPathQuery()));
    }

    // #################################################################################################

    synchronized public void addLocalSubscription(LocalGENASubscription subscription) {
        localItems.addSubscription(subscription);
    }

    synchronized public LocalGENASubscription getLocalSubscription(String subscriptionId) {
        return localItems.getSubscription(subscriptionId);
    }

    synchronized public boolean updateLocalSubscription(LocalGENASubscription subscription) {
        return localItems.updateSubscription(subscription);
    }

    synchronized public boolean removeLocalSubscription(LocalGENASubscription subscription) {
        return localItems.removeSubscription(subscription);
    }

    synchronized public void addRemoteSubscription(RemoteGENASubscription subscription) {
        remoteItems.addSubscription(subscription);
    }

    synchronized public RemoteGENASubscription getRemoteSubscription(String subscriptionId) {
        return remoteItems.getSubscription(subscriptionId);
    }

    synchronized public void updateRemoteSubscription(RemoteGENASubscription subscription) {
        remoteItems.updateSubscription(subscription);
    }

    synchronized public void removeRemoteSubscription(RemoteGENASubscription subscription) {
        remoteItems.removeSubscription(subscription);
    }

    /* ############################################################################################################ */

    // When you call this, make sure you have the Router lock before this lock is obtained!
    synchronized public void shutdown() {
        log.fine("Shutting down registry...");

        if (registryMaintainer != null)
            registryMaintainer.stop();
        
        // Final cleanup run to flush out pending executions which might
        // not have been caught by the maintainer before it stopped
        log.finest("Executing final pending operations on shutdown: " + pendingExecutions.size());
        runPendingExecutions(false);

        for (RegistryListener listener : registryListeners) {
            listener.beforeShutdown(this);
        }

        RegistryItem<URI, Resource>[] resources = resourceItems.toArray(new RegistryItem[resourceItems.size()]);
        for (RegistryItem<URI, Resource> resourceItem : resources) {
            resourceItem.getItem().shutdown();
        }

        remoteItems.shutdown();
        localItems.shutdown();

        for (RegistryListener listener : registryListeners) {
            listener.afterShutdown();
        }
    }

    synchronized public void pause() {
        if (registryMaintainer != null) {
            log.fine("Pausing registry maintenance");
            runPendingExecutions(true);
            registryMaintainer.stop();
            registryMaintainer = null;
        }
    }

    synchronized public void resume() {
        if (registryMaintainer == null) {
            log.fine("Resuming registry maintenance");
            remoteItems.resume();
            registryMaintainer = createRegistryMaintainer();
            if (registryMaintainer != null) {
                getConfiguration().getRegistryMaintainerExecutor().execute(registryMaintainer);
            }
        }
    }

    synchronized public boolean isPaused() {
        return registryMaintainer == null;
    }

    /* ############################################################################################################ */

    synchronized void maintain() {

        if (log.isLoggable(Level.FINEST))
            log.finest("Maintaining registry...");

        // Remove expired resources
        Iterator<RegistryItem<URI, Resource>> it = resourceItems.iterator();
        while (it.hasNext()) {
            RegistryItem<URI, Resource> item = it.next();
            if (item.getExpirationDetails().hasExpired()) {
                if (log.isLoggable(Level.FINER))
                    log.finer("Removing expired resource: " + item);
                it.remove();
            }
        }

        // Let each resource do its own maintenance
        for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
            resourceItem.getItem().maintain(
                    pendingExecutions,
                    resourceItem.getExpirationDetails()
            );
        }

        // These add all their operations to the pendingExecutions queue
        remoteItems.maintain();
        localItems.maintain();

        // We now run the queue asynchronously so the maintenance thread can continue its loop undisturbed
        runPendingExecutions(true);
    }

    synchronized void executeAsyncProtocol(Runnable runnable) {
        pendingExecutions.add(runnable);
    }

    synchronized void runPendingExecutions(boolean async) {
        if (log.isLoggable(Level.FINEST))
            log.finest("Executing pending operations: " + pendingExecutions.size());
        for (Runnable pendingExecution : pendingExecutions) {
            if (async)
                getConfiguration().getAsyncProtocolExecutor().execute(pendingExecution);
            else
                pendingExecution.run();
        }
        if (pendingExecutions.size() > 0) {
            pendingExecutions.clear();
        }
    }

    /* ############################################################################################################ */

    public void printDebugLog() {
        if (log.isLoggable(Level.FINE)) {
            log.fine("====================================    REMOTE   ================================================");

            for (RemoteDevice remoteDevice : remoteItems.get()) {
                log.fine(remoteDevice.toString());
            }

            log.fine("====================================    LOCAL    ================================================");

            for (LocalDevice localDevice : localItems.get()) {
                log.fine(localDevice.toString());
            }

            log.fine("====================================  RESOURCES  ================================================");

            for (RegistryItem<URI, Resource> resourceItem : resourceItems) {
                log.fine(resourceItem.toString());
            }

            log.fine("=================================================================================================");

        }

    }
    
 	@Override
	public void lockRemoteSubscriptions() {
		remoteSubscriptionsLock.lock();
	}
	
	@Override
	public void unlockRemoteSubscriptions() {
		remoteSubscriptionsLock.unlock();
	}
	
	@Override
	public void setResourceMissingProcessor(ResourceProcessor resourceProcessor) {
		resourceMissingProcessor = resourceProcessor;
	}

	@Override
	public ResourceProcessor getResourceMissingProcessor() {
		return resourceMissingProcessor;
	}
}
