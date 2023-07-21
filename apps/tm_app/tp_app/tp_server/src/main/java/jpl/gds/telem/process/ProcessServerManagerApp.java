/*
 * Copyright 2006-2018. California Institute of Technology.
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

package jpl.gds.telem.process;

import jpl.gds.common.config.TimeComparisonStrategy;
import jpl.gds.common.service.telem.ITelemetryProcessorSummary;
import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDictionary;
import jpl.gds.dictionary.api.client.cache.IDictionaryCache;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.exceptions.ApplicationException;
import jpl.gds.shared.exceptions.RestfulTelemetryException;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.metadata.context.ContextKey;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.telem.common.ITelemetryWorker;
import jpl.gds.telem.common.app.mc.AbstractTelemetryServerApp;
import jpl.gds.telem.process.app.TelemetryProcessWorker;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;

import static jpl.gds.shared.metadata.context.ContextConfigurationType.TELEM_PROCESS_SERVER;

/**
 * Manager class for IProcessWorker processes
 *
 */
@Service
public class ProcessServerManagerApp extends AbstractTelemetryServerApp {

    private final IDictionaryCache dictionaryCache;

    private final    ProcessConfiguration              downConfig;
    private final TimeComparisonStrategyContextFlag serverTcStratCtxFlag;

    /**
     * Constructor
     *
     * @param ctx            ApplicationContext
     * @param t              Tracer
     * @param taskExecutor   Task Executor
     * @param dictProperties DictionaryProperties
     * @param cache          Dictionary Cache
     * @param downConfig     Process Configuration
     */
    public ProcessServerManagerApp(final ApplicationContext ctx, final Tracer t, final ThreadPoolTaskExecutor taskExecutor,
                                   final DictionaryProperties dictProperties,
                                   final IDictionaryCache cache, final ProcessConfiguration downConfig) {
        super(ctx, t, taskExecutor);

        this.dictionaryCache = cache;

        this.downConfig = downConfig;

        this.writeableProperties = downConfig.getFeatureSet();
        this.serverTcStratCtxFlag = parentContext.getBean(TimeComparisonStrategyContextFlag.class);

    }

    @Override
    @PostConstruct
    public void init() {
        super.commonInit();
        loadDictionaries(null, null);
    }

    @Override
    public IContextIdentification createWorker(final SessionConfiguration config) throws ApplicationException{
        tracer.warn("TP processors should be instantiated with a context ID filter. This method should only be used for tests.");
        final IContextKey key = new ContextKey();
        key.setNumber(0L);
        key.setParentNumber(parentContext.getBean(IContextKey.class).getNumber());
        return createWorker(config, key, new String[0]);
    }

    @Override
    public ISimpleContextConfiguration createServerContext(){
        return contextFactory.createContextConfiguration(TELEM_PROCESS_SERVER, false);
    }

    /**
     * Create a process app for downlink
     *
     * @param config
     *            the session configuration to create a worker for
     * @param foreignContextKey
     *            the context key from a TI session start
     * @param args The list arguments used to configure the new worker
     *
     * @return IContextIdentification context ID]
     */
    private IContextIdentification createWorker(final SessionConfiguration config,
                                                final IContextKey foreignContextKey, final String[] args) throws ApplicationException{
        return createWorker(config, foreignContextKey, createWorkerContext(), args);
    }

    @Override
    public IProcessWorker getWorker(final long key, final String host, final int number) {
        final ITelemetryWorker worker = super.getWorker(key, host, number);
        return (worker == null ? null : ((IProcessWorker) worker));
    }

    /**
     * Create a process app for downlink
     *
     * @param config
     *            the session configuration to create a worker for
     * @param foreignContextKey
     *            the context key from a TI session start
     * @param processContext
     *            the target spring application context for the worker
     * @param args The list arguments used to configure the new worker
     *
     * @return the context identification
     */
    IContextIdentification createWorker(final SessionConfiguration config, final IContextKey foreignContextKey,
                                        final ApplicationContext processContext, final String[] args) throws
            ApplicationException {

        final SessionConfiguration sessionConfig = new SessionConfiguration(processContext);
        sessionConfig.copyValuesFrom(config);

        setValuesFromParent(processContext, sessionConfig);

        final TelemetryProcessWorker app = (TelemetryProcessWorker) processContext.getBean(IProcessWorker.class,
                                                                                           sessionConfig, foreignContextKey,
                                                                                           serverTcStratCtxFlag,
                                                                                           downConfig,
                                                                                           secureLoader);

        return launchWorker(app, args);
    }

    /**
     * Create a new application context for a worker
     *
     * @return a worker application context
     */
    private ApplicationContext createWorkerContext() {
        return SpringContextFactory.getSpringContext(
                Collections.singletonList(TelemetryProcessWorker.class.getPackage().getName() + ".bootstrap"),
                true);
    }


    /**
     * Create a TP worker and Attach it to an existing session based on the specified key and host
     *
     * @param key The session key
     * @param host The session host
     * @param args The list arguments used to configure the new worker
     *
     * @return IContextKey
     * @throws Exception when an error occurs
     */
    public IContextKey attachWorkerToSession(final long key, final String host, final String[] args) throws ApplicationException {

        // Don't allow more that one TP worker from attaching to the same TI worker session
        for (final ITelemetryWorker worker : getAllWorkers()) {
            final IContextKey contextKey = worker.getContextConfiguration().getContextId().getContextKey();
            if (contextKey.getNumber() == key && contextKey.getHost().equalsIgnoreCase(host)) {
                throw new RestfulTelemetryException(HttpStatus.PRECONDITION_FAILED,
                        "TP processor with id: '" + contextKey.toString()
                        + "' is already attached to sessionKey: '"
                        + key + "' and sessionHost: '" + host + "'. Attaching more than one TP"
                        + " processor to the same TI session not supported.");
            }
        }

        // Get a SessionConfiguration object based on the TI worker created session
        // that the TP worker will be attaching to.
        SessionConfiguration sessionConfig = getSessionConfigurationFromDatabase(key, host);
        // Need to set the ContextId Type to "chill_telem_process" so that
        // we can distinguish the Session table entries.
        sessionConfig.getContextId().setType(ApplicationConfiguration.getApplicationName());

        final IContextKey foreignContextKey = new ContextKey();
        foreignContextKey.setNumber(key);
        foreignContextKey.setHost(host);

        final IContextIdentification contextConfig = createWorker(sessionConfig, foreignContextKey, args);

        return contextConfig.getContextKey();
    }


    @Override
    public ITelemetryProcessorSummary getSessionSummary(final long key, final String host, final int number) {
        final ITelemetrySummary summary = super.getSessionSummary(key, host, number);
        return summary == null ? null : ((ITelemetryProcessorSummary) summary);
    }


    @Override
    public void run() {
        Thread.currentThread().setName("TP Server Manager");

        // Allow TI/TP to shutdown
    }


    /* -------------- Dictionary functions -------------- */

    /**
     * @param name      dictionary name
     * @param directory dictionary directory
     */
    private void loadDictionaries(final String name, final String directory) {
        final boolean isSse = sseContextFlag.isApplicationSse();

        if (name != null && !name.isEmpty()) {
            parentDictionaryProperties.setFswVersion(name);
        }

        if (directory != null && !directory.isEmpty() && directoryExists(directory)) {
            if (isSse) {
                parentDictionaryProperties.setSseDictionaryDir(directory);
            } else {
                parentDictionaryProperties.setFswDictionaryDir(directory);
            }
        }

        final Callable<Integer> dictLoader = () -> {

            final long start = System.nanoTime();

            IChannelDictionary chanDict = null;
            try {
                if (isSse) {
                    chanDict = dictionaryCache.getSseChannelDictionary(parentDictionaryProperties);
                } else {
                    chanDict = dictionaryCache.getFlightChannelDictionary(parentDictionaryProperties);
                }
            } catch (final DictionaryException e) {
                tracer.error("Error occurred loading ", DictionaryType.CHANNEL ,
                             " dictionary into cache. ",e.getMessage());
            }
            try {
                dictionaryCache.getAlarmDictionary(parentDictionaryProperties, chanDict.getChannelDefinitionMap(),
                        isSse);
            } catch (final DictionaryException e) {
                tracer.info("Error occurred loading ", DictionaryType.ALARM ,
                             " dictionary into cache. ",e.getMessage());
            }
            try {
                dictionaryCache.getChannelDecomDictionary(parentDictionaryProperties,
                        chanDict.getChannelDefinitionMap(),
                        isSse);
            } catch (final DictionaryException e) {
                tracer.error("Error occurred loading ", DictionaryType.DECOM ,
                             " dictionary into cache. ",e.getMessage());
            }
            try {
                dictionaryCache.getApidDictionary(parentDictionaryProperties, isSse);
            } catch (final DictionaryException e) {
                tracer.error("Error occurred loading ", DictionaryType.APID ,
                             " dictionary into cache. ",e.getMessage());
            }
            try {
                dictionaryCache.getEvrDictionary(parentDictionaryProperties, isSse);
            } catch (final DictionaryException e) {
                tracer.error("Error occurred loading ", DictionaryType.EVR ,
                             " dictionary into cache. ",e.getMessage());
            }
            try {
                dictionaryCache.getHeaderChannelDictionary(parentDictionaryProperties);
            } catch (final DictionaryException e) {
                tracer.error("Error occurred loading ", DictionaryType.HEADER,
                             " dictionary into cache. ",e.getMessage());
            }
            try {
                dictionaryCache.getMonitorDictionary(parentDictionaryProperties);
            } catch (final DictionaryException e) {
                tracer.error("Error occurred loading ", DictionaryType.MONITOR ,
                             " dictionary into cache. ",e.getMessage());
            }
            try {
                dictionaryCache.getCommandDictionary(parentDictionaryProperties);
            } catch (final DictionaryException e) {
                tracer.error("Error occurred loading ", DictionaryType.COMMAND ,
                             " dictionary into cache. ",e.getMessage());
            }
            try {
                dictionaryCache.getTransferFrameDictionary(parentDictionaryProperties);
            } catch (final DictionaryException e) {
                tracer.error("Error occurred loading ", DictionaryType.FRAME ,
                             " dictionary into cache. ",e.getMessage());
            }


            final long end = System.nanoTime();

            final double elapsed = (end - start) / 1_000_000_000.0;

            tracer.debug("Dictionary cached in ", elapsed, " seconds.");

            return 1;
        };

        final CompletionService<Integer> cs = new ExecutorCompletionService<>(executor);

        final long start = System.nanoTime();
        cs.submit(dictLoader);

        final int jobs = 1;

        final Runnable watcher = () -> {
            int returned = 0;
            while (returned < jobs) {
                try {
                    cs.take();
                    returned++;
                } catch (final InterruptedException e) {
                    tracer.warn("Dictionary caching has been interrupted: ", e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
            final long   end     = System.nanoTime();
            final double elapsed = (end - start) / 1_000_000_000.0;
            tracer.info("Dictionary has been cached: ",
                    name != null ? name : parentDictionaryProperties.getFswVersion());
            tracer.debug("All dictionaries cache jobs completed in ", elapsed, " seconds.");
        };

        executor.execute(watcher);
    }

    /**
     * Load a dictionary into the cache
     *
     * @param dictionary dictionary name
     * @param directory  dictionary directory (optional)
     */
    public void cacheDictionary(final String dictionary, final String directory) {
        final boolean inDirectory = directoryExists(directory);
        if (inDirectory) {
            loadDictionaries(dictionary, directory);
        } else {
            if (directory != null) {
                tracer.warn("Dictionary directory provided does not exist.");
                throw new RestfulTelemetryException(HttpStatus.BAD_REQUEST, "Directory specified does not exist.");
            }
            loadDictionaries(dictionary, null);
        }
    }

    /* -------------- ICommandLineApp methods -------------- */

    @Override
    public BaseCommandOptions createOptions() {

        if (options != null) {
            return options;
        }

        super.createOptions(baseCommandOptions);

        return options;
    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        // default rest port depends on SSE option; has to happen first
        contextOpts.parseAllOptionsAsOptional(commandLine);

        //set defaults for REST port
        restOptions.REST_PORT_OPTION.setDefaultValue(sseContextFlag.isApplicationSse() ? downConfig.getFeatureSet().getRestPortSse() :
                                                             downConfig.getFeatureSet().getRestPortFsw());

        //will also parse rest port
        super.configure(commandLine);

        downConfig.setUseMessageService(messageSvcConfig.getUseMessaging());

        downConfig.setUseDb(dbOptions.getDatabaseConfiguration().getUseDatabase());

        if (venueConfig.getVenueType() == null || tempSession.getContextId().getName() == null) {
            throw new ParseException("To use the " + BaseCommandOptions.AUTORUN.getLongOpt()
                    + " option you must either supply a session configuration file\n"
                    + "or a venue type and a session name");
        }
    }

    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        if (options == null) {
            createOptions();
        }

        try (final PrintWriter pw = new PrintWriter(System.out)) {
            pw.println(
                    "Usage: " + jpl.gds.shared.cli.app.ApplicationConfiguration.getApplicationName() + " [options]\n");

            options.getOptions().printOptions(pw);

            pw.flush();
        }
    }

    /**
     * Saves the lad for a specified session
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     * 
     * @param filename
     *            file to save to
     * @return true if an attempt was made to save the lad
     */
    public boolean saveLadToFile(final long key, final String host, final int fragment, final String filename) {
        final IProcessWorker worker = getWorker(key, host, fragment);
        if (worker != null) {
            return worker.saveLadToFile(filename);
        }
        else {
            tracer.info("Unable to save LAD for session ", key, " to ", filename,
                        " because no processor exists matching that ID");
        }
        return false;
    }

    /**
     * Clears the lad for a specified session
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     * @return true if an attempt was made to clear the lad
     */
    public boolean clearChannelState(final long key, final String host, final int fragment) {
        final IProcessWorker worker = getWorker(key, host, fragment);
        if (worker != null) {
            worker.clearChannelState();
            return true;
        }
        else {
            tracer.info("Unable to clear LAD state for session ", key,
                        " because no processor exists matching that ID");
        }
        return false;
    }

    /**
     * Gets the lad contents of a session as a String
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     * @return Lad as a string
     */
    public String getLadContentsAsString(final long key, final String host, final int fragment) {
        final IProcessWorker worker = getWorker(key, host, fragment);
        if (worker != null) {
            return worker.getLadContentsAsString();
        }
        else {
            tracer.info("Unable to get LAD contents for session ", key,
                        " because no processor exists matching that ID");
        }
        return "";
    }

    /**
     * Gets the applications time comparison strategy
     * 
     * @return <TimeComparisonStrategy> of the server
     */
    public TimeComparisonStrategy getTimeComparisonStrategy() {
        return serverTcStratCtxFlag.getTimeComparisonStrategy();
    }

    @Override
    public IContextIdentification createWorker(final SessionConfiguration config, final String[] args) {
        throw new UnsupportedOperationException(ApplicationConfiguration.getApplicationName()
                + " Does not support passing arguments to a processor");
    }

}
