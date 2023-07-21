/*
 * Copyright 2006-2019. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */

package jpl.gds.sleproxy.server.sleinterface;

import com.lsespace.sle.user.SLEUserServiceFactory;
import com.lsespace.sle.user.proxy.isp1.ISP1PeerConfigMutable;
import com.lsespace.sle.user.proxy.isp1.ISP1SLEUserFcltuProxy;
import com.lsespace.sle.user.proxy.isp1.ISP1SLEUserProxyConfigMutable;
import com.lsespace.sle.user.proxy.isp1.ISP1SLEUserProxyFactory;
import com.lsespace.sle.user.proxy.isp1.ISP1SLEUserRafProxy;
import com.lsespace.sle.user.proxy.isp1.ISP1SLEUserRcfProxy;
import com.lsespace.sle.user.service.AllowedReturnFrameQuality;
import com.lsespace.sle.user.service.GVCID;
import com.lsespace.sle.user.service.SLEUserFcltuConfigMutable;
import com.lsespace.sle.user.service.SLEUserFcltuInstance;
import com.lsespace.sle.user.service.SLEUserRafConfigMutable;
import com.lsespace.sle.user.service.SLEUserRcfConfigMutable;
import com.lsespace.sle.user.service.SLEUserServiceInstance;
import com.lsespace.sle.user.util.ObservableSet;
import jpl.gds.sleproxy.server.sleinterface.internal.config.SLEInterfaceInternalConfigManager;
import jpl.gds.sleproxy.server.sleinterface.profile.ISLEInterfaceProfile;
import jpl.gds.sleproxy.server.sleinterface.profile.ProviderHost;
import jpl.gds.sleproxy.server.sleinterface.profile.ReturnSLEInterfaceProfile;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Static factory for creation of auto bind SLE User Service Instances (FCLTU, RCF, RAF).
 *
 */
public class AutoBindSleInstanceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoBindSleInstanceFactory.class);

    /**
     * Private constructor. Do not instantiate!
     */
    private AutoBindSleInstanceFactory() {
    }

    /**
     * Get an auto-binding instance for forward CLTU SLE service instance.
     *
     * @param proxyFactory   SLE proxy factory
     * @param serviceFactory SLE service factory
     * @param profile        Forward SLE profile
     * @return auto-bind forward SLE service instance
     */
    public static AutoBindSleServiceInstance<SLEUserFcltuInstance> getFcltuInstance(
            final ISP1SLEUserProxyFactory proxyFactory,
            final SLEUserServiceFactory serviceFactory,
            final ISLEInterfaceProfile profile) {
        List<ProviderHost> hosts = profile.getProviderHosts();

        List<SLEUserFcltuInstance> userFcltuInstances = new ArrayList<>(hosts.size());
        for (final ProviderHost host : hosts) {
            userFcltuInstances.add(createFcltuInstance(proxyFactory, serviceFactory, profile, host));
        }

        return new AutoBindSleServiceInstance<>(userFcltuInstances);
    }

    /**
     * Get an auto-binding instance for reverse all/channel frame SLE service instance.
     *
     * @param proxyFactory   SLE proxy factory
     * @param serviceFactory SLE service factory
     * @param profile        Return SLE profile
     * @return auto-bind return SLE service instance
     */
    public static AutoBindSleServiceInstance<SLEUserServiceInstance> getRcfOrRafInstance(
            final ISP1SLEUserProxyFactory proxyFactory,
            final SLEUserServiceFactory serviceFactory,
            final ReturnSLEInterfaceProfile profile) {

        List<ProviderHost> hosts = profile.getProviderHosts();

        List<SLEUserServiceInstance> userRcfOrRafInstances = new ArrayList<>(hosts.size());
        for (final ProviderHost host : hosts) {
            userRcfOrRafInstances.add(createRcfOrRafInstance(proxyFactory, serviceFactory, profile, host));
        }

        return new AutoBindSleServiceInstance<>(userRcfOrRafInstances);
    }

    private static SLEUserServiceInstance createRcfOrRafInstance(ISP1SLEUserProxyFactory proxyFactory,
                                                                 SLEUserServiceFactory serviceFactory,
                                                                 ReturnSLEInterfaceProfile profile, ProviderHost host) {
        // Create and set the Proxy Configuration
        ISP1SLEUserProxyConfigMutable proxyConfig = createProxyConfig(profile, host);

        // Create the Service Instance
        switch (profile.getInterfaceType()) {
            case RETURN_ALL:
                ISP1SLEUserRafProxy rafProxy = proxyFactory.createRafProxy(proxyConfig);
                SLEUserRafConfigMutable rafServiceConfig = new SLEUserRafConfigMutable(profile.getServiceInstanceID());
                rafServiceConfig
                        .setDesiredVersion(SLEInterfaceInternalConfigManager.INSTANCE.getReturnServiceVersion());
                LOGGER.info("This SLE interface RAF service instance will use SLE version {}",
                        SLEInterfaceInternalConfigManager.INSTANCE.getReturnServiceVersion());
                ObservableSet<AllowedReturnFrameQuality> frameQualities = rafServiceConfig
                        .getModifiableAllowedQualities();
                frameQualities.add(profile.getReturnFrameQuality());
                LOGGER.info("This SLE interface RAF service instance will accept the frame quality {}",
                        profile.getReturnFrameQuality());
                return serviceFactory.createRafServiceInstance(rafServiceConfig, rafProxy);
            case RETURN_CHANNEL:
                ISP1SLEUserRcfProxy rcfProxy = proxyFactory.createRcfProxy(proxyConfig);
                SLEUserRcfConfigMutable rcfServiceConfig = new SLEUserRcfConfigMutable(profile.getServiceInstanceID());
                rcfServiceConfig
                        .setDesiredVersion(SLEInterfaceInternalConfigManager.INSTANCE.getReturnServiceVersion());
                LOGGER.info("This SLE interface RCF service instance will use SLE version {}",
                        SLEInterfaceInternalConfigManager.INSTANCE.getReturnServiceVersion());
                ObservableSet<GVCID> permittedGVCIDs = rcfServiceConfig.getModifiablePermittedGVCIDs();
                permittedGVCIDs.add(new GVCID(profile.getSpacecraftID(), profile.getFrameVersion(),
                        profile.getVirtualChannel()));
                LOGGER.info("This SLE interface RCF service instance will accept frames with GVCID {},{},{}",
                        profile.getSpacecraftID(),
                        profile.getFrameVersion(), profile.getVirtualChannel());
                return serviceFactory.createRcfServiceInstance(rcfServiceConfig, rcfProxy);
            default:
                LOGGER.error("SLE interface return service BIND FAILED. UNEXPECTED INTERFACE TYPE: {}",
                        profile.getInterfaceType());
                LOGGER.debug("bind(): Throwing an IllegalArgumentException to abort this method");
                throw new IllegalArgumentException(
                        "SLE interface return service BIND FAILED. UNEXPECTED INTERFACE TYPE: " + profile
                                .getInterfaceType());
        }
    }

    private static SLEUserFcltuInstance createFcltuInstance(ISP1SLEUserProxyFactory proxyFactory,
                                                            SLEUserServiceFactory serviceFactory,
                                                            ISLEInterfaceProfile profile, ProviderHost host) {
        ISP1SLEUserProxyConfigMutable proxyConfig = createProxyConfig(profile, host);

        // Create the Service Instance
        ISP1SLEUserFcltuProxy     fcltuProxy         = proxyFactory.createFcltuProxy(proxyConfig);
        SLEUserFcltuConfigMutable fcltuServiceConfig = new SLEUserFcltuConfigMutable(profile.getServiceInstanceID());
        final int serviceVersion = SLEInterfaceInternalConfigManager.INSTANCE
                .getForwardServiceVersion();
        fcltuServiceConfig.setDesiredVersion(serviceVersion);
        LOGGER.info("This SLE interface forward service instance will use SLE version {}", serviceVersion);
        return serviceFactory.createFcltuServiceInstance(fcltuServiceConfig, fcltuProxy);
    }

    @NotNull
    private static ISP1SLEUserProxyConfigMutable createProxyConfig(ISLEInterfaceProfile profile, ProviderHost host) {
        // Create and set the Proxy Configuration
        ISP1SLEUserProxyConfigMutable proxyConfig = new ISP1SLEUserProxyConfigMutable();
        proxyConfig.setHostname(host.getHostName());
        proxyConfig.setPort(host.getPort());

        proxyConfig.setTmsTimeout(SLEInterfaceInternalConfigManager.INSTANCE.getStartupTimeoutPeriodMillis() / 1000);
        proxyConfig
                .setCpaTimeout(SLEInterfaceInternalConfigManager.INSTANCE.getCloseAfterPeerAbortTimeoutMillis() / 1000);

        proxyConfig.setHeartBeat(SLEInterfaceInternalConfigManager.INSTANCE.getHeartbeatIntervalMillis() / 1000);
        proxyConfig.setDeadFactor(SLEInterfaceInternalConfigManager.INSTANCE.getDeadFactor());
        proxyConfig.setExpirationPeriod(SLEInterfaceInternalConfigManager.INSTANCE.getAcceptableDelayMillis() / 1000);

        // Set the Provider Peer Configuration
        ISP1PeerConfigMutable providerPeer = new ISP1PeerConfigMutable(profile.getProviderName(),
                profile.getProviderPassword().getBytes(),
                profile.getProviderAuthenticationMode());
        proxyConfig.updateProviderPeerConfig(providerPeer);

        // Set the User Peer Configuration
        ISP1PeerConfigMutable userPeer = new ISP1PeerConfigMutable(profile.getUserName(),
                profile.getUserPassword().getBytes(),
                profile.getUserAuthenticationMode());

        proxyConfig.updateUserPeerConfig(userPeer);
        return proxyConfig;
    }
}
