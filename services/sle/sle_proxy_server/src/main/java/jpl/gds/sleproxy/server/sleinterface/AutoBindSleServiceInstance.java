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

import com.lsespace.sle.user.proxy.isp1.ISP1SLEUserProxyConfigMutable;
import com.lsespace.sle.user.service.SLEUserServiceInstance;
import com.lsespace.sle.user.util.concurrent.OperationFuture;
import jpl.gds.sleproxy.server.sleinterface.profile.ProviderHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Automatically bind to a collection of SLE User Service Instances. A call to #bind() will attempt to connect to all
 * given service instances, only retaining a single active service instance. If all instances fail to connect, #bind()
 * will return FALSE, and a list of failure causes (Throwables) is available for inspection.
 *
 * @param <T> SLE User Service Instance type (FCLTU, RAF, RCF)
 */
public class AutoBindSleServiceInstance<T extends SLEUserServiceInstance> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AutoBindSleServiceInstance.class);

    private final List<T>                      sleUserServiceInstances;
    private       T                            successfullyBoundInstance = null;
    // May be modified concurrently
    private final List<Throwable>              failureCauses = new CopyOnWriteArrayList<>();
    private final Map<ProviderHost, Throwable> hostThrowableMap          = new ConcurrentHashMap<>();

    public AutoBindSleServiceInstance(final List<T> sleUserServiceInstances) {
        if (sleUserServiceInstances == null || sleUserServiceInstances.isEmpty()) {
            throw new IllegalArgumentException("List of SLE service instances must not be null or empty.");
        }
        this.sleUserServiceInstances = sleUserServiceInstances;
    }

    /**
     * Initiate BIND on all provided SLE user service instances. If successful, a single SLE user service instance will
     * be retained in this class. If unsuccessful, the SLE user service instance will be null, and a list of failure
     * causes can be retrieved for inspection.
     *
     * @return true if BIND successful, false if not
     */
    public boolean bind(final long timeoutMillis) {
        final ExecutorService executor =
                Executors.newFixedThreadPool(sleUserServiceInstances.size());
        final CompletionService<SLEUserServiceInstance> completionService = new ExecutorCompletionService<>(executor);

        final List<Future<SLEUserServiceInstance>> futures = new ArrayList<>();
        submitBindRequests(timeoutMillis, completionService, futures);

        boolean success = waitForBind(completionService, futures);

        LOGGER.debug("Shutting down BIND executor");
        executor.shutdownNow();

        return success;
    }

    /**
     * Submit bind requests to SLE portal service instances
     *
     * @param timeoutMillis     request timeout
     * @param completionService completion service
     * @param futures           list of bind request futures
     */
    private void submitBindRequests(long timeoutMillis, CompletionService<SLEUserServiceInstance> completionService,
                                    List<Future<SLEUserServiceInstance>> futures) {
        for (final SLEUserServiceInstance instance : sleUserServiceInstances) {
            final Callable<SLEUserServiceInstance> callable = createBindCallable(timeoutMillis,
                    instance);
            futures.add(completionService.submit(callable));
        }
    }

    /**
     * Create BIND request callable jobs. The jobs will attempt to BIND to an SLE provider. If successful, they will
     * return a valid service instance. If the job is canceled, another job was successful.
     *
     * @param timeoutMillis request timeout
     * @param instance      service instance
     * @return callable bind request job
     */
    private Callable<SLEUserServiceInstance> createBindCallable(long timeoutMillis, SLEUserServiceInstance instance) {
        final Callable<SLEUserServiceInstance> callable = () -> {
            final ISP1SLEUserProxyConfigMutable config = (ISP1SLEUserProxyConfigMutable) instance
                    .getProxy().getMutableClone();
            final String       host         = config.getHostname();
            final int          port         = config.getPort();
            final ProviderHost providerHost = new ProviderHost(host, String.valueOf(port));
            LOGGER.debug("Invoking BIND on {}", providerHost);
            final OperationFuture<Void> opFuture = instance.bindRequest();
            try {
                opFuture.await(timeoutMillis);
            } catch (InterruptedException e) {
                LOGGER.debug("Interrupted BIND on {}, sending ABORT", providerHost);
                instance.abort();
                Thread.currentThread().interrupt();
                final Throwable t = new Throwable(String.format("BIND canceled for %s", providerHost));
                failureCauses.add(t);
                hostThrowableMap.put(providerHost, t);
                return null;
            }
            if (opFuture.isSuccess()) {
                LOGGER.debug("BIND successful on {}, returning instance", providerHost);
                return instance;
            } else {
                if (!opFuture.isDone()) {
                    // there's some anomaly here, and the future hasn't completed.
                    // it's likely that the underlying BIND request is still waiting
                    // this condition has been observed by Psyche during DSN compatibility testing, specifically
                    // when
                    // a service instance has been taken down for maintenance.
                    LOGGER.debug("BIND FAILED on {}, task still waiting to BIND after timeout", providerHost);
                    final Throwable t = new Throwable(String.format("Task still waiting to BIND after timeout for %s",
                            providerHost));
                    failureCauses.add(t);
                    hostThrowableMap.put(providerHost, t);
                } else {
                    LOGGER.warn("BIND FAILED on host {} with reason: {}", providerHost,
                            opFuture.cause().getMessage());
                    failureCauses.add(opFuture.cause());
                    hostThrowableMap.put(providerHost, opFuture.cause());
                }
                return null;
            }
        };
        return callable;
    }

    /**
     * Wait for BIND requests to return a valid service instance
     *
     * @param completionService completion service
     * @param futures           list of callable futures
     * @return success indicator
     */
    private boolean waitForBind(CompletionService<SLEUserServiceInstance> completionService,
                                List<Future<SLEUserServiceInstance>> futures) {
        int     received = 0;
        boolean errors   = false;
        boolean success  = false;
        while (received < sleUserServiceInstances.size() && !errors) {
            try {
                Future<SLEUserServiceInstance> resultFuture = completionService.take();
                SLEUserServiceInstance         instance     = resultFuture.get();
                if (instance != null) {
                    success = true;
                    successfullyBoundInstance = (T) instance;
                    for (Future<SLEUserServiceInstance> f : futures) {
                        if (f == resultFuture) {
                            continue;
                        }
                        f.cancel(true);
                    }
                    LOGGER.debug("Retrieved bound instance, outstanding requests have been canceled", instance);
                    break;
                }
                received++;
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("BIND failure: " + e.getMessage(), e);
                errors = true;
                Thread.currentThread().interrupt();
            }
        }
        return success;
    }

    /**
     * Return the BIND status of this instance
     *
     * @return true if BIND success, false if not
     */
    public boolean isSuccessfullyBound() {
        return successfullyBoundInstance != null;
    }

    /**
     * Returns the successfully bound SLE user service instance.
     *
     * @return a SLE user service instance, or null
     */
    public T getSuccessfullyBoundInstance() {
        return successfullyBoundInstance;
    }

    /**
     * Return a list of failure causes
     *
     * @return a list of failure causes
     */
    public Map<ProviderHost, Throwable> getFailureCauses() {
        return hostThrowableMap;
    }

}
