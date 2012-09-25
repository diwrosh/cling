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

package org.teleal.cling;

import org.teleal.cling.binding.xml.DeviceDescriptorBinder;
import org.teleal.cling.binding.xml.ServiceDescriptorBinder;
import org.teleal.cling.binding.xml.UDA10DeviceDescriptorBinderImpl;
import org.teleal.cling.binding.xml.UDA10ServiceDescriptorBinderImpl;
import org.teleal.cling.model.ModelUtil;
import org.teleal.cling.model.Namespace;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.transport.impl.DatagramIOConfigurationImpl;
import org.teleal.cling.transport.impl.DatagramIOImpl;
import org.teleal.cling.transport.impl.GENAEventProcessorImpl;
import org.teleal.cling.transport.impl.MulticastReceiverConfigurationImpl;
import org.teleal.cling.transport.impl.MulticastReceiverImpl;
import org.teleal.cling.transport.impl.NetworkAddressFactoryImpl;
import org.teleal.cling.transport.impl.SOAPActionProcessorImpl;
import org.teleal.cling.transport.impl.StreamClientConfigurationImpl;
import org.teleal.cling.transport.impl.StreamClientImpl;
import org.teleal.cling.transport.impl.StreamServerConfigurationImpl;
import org.teleal.cling.transport.impl.StreamServerImpl;
import org.teleal.cling.transport.spi.DatagramIO;
import org.teleal.cling.transport.spi.DatagramProcessor;
import org.teleal.cling.transport.spi.GENAEventProcessor;
import org.teleal.cling.transport.spi.MulticastReceiver;
import org.teleal.cling.transport.spi.NetworkAddressFactory;
import org.teleal.cling.transport.spi.SOAPActionProcessor;
import org.teleal.cling.transport.spi.StreamClient;
import org.teleal.cling.transport.spi.StreamServer;

// import javax.annotation.PostConstruct;
// import javax.enterprise.context.ApplicationScoped;
// import javax.inject.Inject;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

/**
 * Adapter for CDI environments.
 *
 * @author Christian Bauer
 */
// @ApplicationScoped
public class ManagedUpnpServiceConfiguration implements UpnpServiceConfiguration {

    private static Logger log = Logger.getLogger(DefaultUpnpServiceConfiguration.class.getName());

    // TODO: All of these fields should be injected so users can provide values through CDI

    private int streamListenPort;

    private Executor defaultExecutor;

    // @Inject
    protected DatagramProcessor datagramProcessor;

    private SOAPActionProcessor soapActionProcessor;
    private GENAEventProcessor genaEventProcessor;

    private DeviceDescriptorBinder deviceDescriptorBinderUDA10;
    private ServiceDescriptorBinder serviceDescriptorBinderUDA10;

    private Namespace namespace;

    // @PostConstruct
    public void init() {

        if (ModelUtil.ANDROID_RUNTIME) {
            throw new Error("Unsupported runtime environment, use org.teleal.cling.android.AndroidUpnpServiceConfiguration");
        }

        this.streamListenPort = NetworkAddressFactoryImpl.DEFAULT_TCP_HTTP_LISTEN_PORT;

        defaultExecutor = createDefaultExecutor();

        soapActionProcessor = createSOAPActionProcessor();
        genaEventProcessor = createGENAEventProcessor();

        deviceDescriptorBinderUDA10 = createDeviceDescriptorBinderUDA10();
        serviceDescriptorBinderUDA10 = createServiceDescriptorBinderUDA10();

        namespace = createNamespace();
    }

    public DatagramProcessor getDatagramProcessor() {
        return datagramProcessor;
    }

    public SOAPActionProcessor getSoapActionProcessor() {
        return soapActionProcessor;
    }

    public GENAEventProcessor getGenaEventProcessor() {
        return genaEventProcessor;
    }

    public StreamClient createStreamClient() {
        return new StreamClientImpl(new StreamClientConfigurationImpl());
    }

    public MulticastReceiver createMulticastReceiver(NetworkAddressFactory networkAddressFactory) {
        return new MulticastReceiverImpl(
                new MulticastReceiverConfigurationImpl(
                        networkAddressFactory.getMulticastGroup(),
                        networkAddressFactory.getMulticastPort()
                )
        );
    }

    public DatagramIO createDatagramIO(NetworkAddressFactory networkAddressFactory) {
        return new DatagramIOImpl(new DatagramIOConfigurationImpl());
    }

    public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
        return new StreamServerImpl(
                new StreamServerConfigurationImpl(
                        networkAddressFactory.getStreamListenPort()
                )
        );
    }

    public Executor getMulticastReceiverExecutor() {
        return getDefaultExecutor();
    }

    public Executor getDatagramIOExecutor() {
        return getDefaultExecutor();
    }

    public Executor getStreamServerExecutor() {
        return getDefaultExecutor();
    }

    public DeviceDescriptorBinder getDeviceDescriptorBinderUDA10() {
        return deviceDescriptorBinderUDA10;
    }

    public ServiceDescriptorBinder getServiceDescriptorBinderUDA10() {
        return serviceDescriptorBinderUDA10;
    }

    public ServiceType[] getExclusiveServiceTypes() {
        return new ServiceType[0];
    }

    public int getRegistryMaintenanceIntervalMillis() {
        return 1000;
    }

    public Integer getRemoteDeviceMaxAgeSeconds() {
        return null;
    }

    public Executor getAsyncProtocolExecutor() {
        return getDefaultExecutor();
    }

    public Executor getSyncProtocolExecutor() {
        return getDefaultExecutor();
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public Executor getRegistryMaintainerExecutor() {
        return getDefaultExecutor();
    }

    public Executor getRegistryListenerExecutor() {
        return getDefaultExecutor();
    }

    public NetworkAddressFactory createNetworkAddressFactory() {
        return createNetworkAddressFactory(streamListenPort);
    }

    public void shutdown() {
        if (getDefaultExecutor() instanceof ThreadPoolExecutor) {
            log.fine("Shutting down thread pool");
            ((ThreadPoolExecutor) getDefaultExecutor()).shutdown();
        }
    }

    protected NetworkAddressFactory createNetworkAddressFactory(int streamListenPort) {
        return new NetworkAddressFactoryImpl(streamListenPort);
    }

    protected SOAPActionProcessor createSOAPActionProcessor() {
        return new SOAPActionProcessorImpl();
    }

    protected GENAEventProcessor createGENAEventProcessor() {
        return new GENAEventProcessorImpl();
    }

    protected DeviceDescriptorBinder createDeviceDescriptorBinderUDA10() {
        return new UDA10DeviceDescriptorBinderImpl();
    }

    protected ServiceDescriptorBinder createServiceDescriptorBinderUDA10() {
        return new UDA10ServiceDescriptorBinderImpl();
    }

    protected Namespace createNamespace() {
        return new Namespace();
    }

    protected Executor getDefaultExecutor() {
        return defaultExecutor;
    }

    protected Executor createDefaultExecutor() {
        return new DefaultUpnpServiceConfiguration.ClingExecutor();
    }
}
