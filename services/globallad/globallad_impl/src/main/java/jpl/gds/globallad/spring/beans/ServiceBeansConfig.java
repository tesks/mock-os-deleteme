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
package jpl.gds.globallad.spring.beans;

import com.google.common.util.concurrent.MoreExecutors;
import jpl.gds.context.api.ContextTopicNameFactory;
import jpl.gds.context.api.TopicNameToken;
import jpl.gds.eha.api.channel.alarm.IAlarmValueSetFactory;
import jpl.gds.globallad.GlobalLadException;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.IGlobalLadReapable;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.container.GlobalLadUtilities;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory;
import jpl.gds.globallad.data.storage.DataInsertionManager;
import jpl.gds.globallad.disruptor.IDisruptorProducer;
import jpl.gds.globallad.io.MessageToGladDataConverter;
import jpl.gds.globallad.io.jms.GlobalLadJmsDataSource;
import jpl.gds.globallad.io.jms.JmsBinaryLoadHandler;
import jpl.gds.globallad.io.socket.GlobalLadSocketServer;
import jpl.gds.globallad.io.socket.SocketBinaryLoadHandler;
import jpl.gds.globallad.memory.GladMemoryThresholdChecker;
import jpl.gds.globallad.memory.IMemoryThresholdChecker;
import jpl.gds.globallad.message.handler.IGlobalLad;
import jpl.gds.globallad.spring.cli.GlobalLadCommandLineParser;
import jpl.gds.globallad.workers.GlobalLadReaper;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.IMessageClientFactory;
import jpl.gds.message.api.external.ITopicSubscriber;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Bean configuration class for global lad services.  Note, this class is not meant to be used directly, it is a
 * configuration class used by Spring and all beans will have the input parameters injected by Spring with Spring bean
 * instances.
 */
@Configuration
public class ServiceBeansConfig {

    /**
     * The socket server port for chill down to connect to.
     */
    @Value("${server.socket.port}")
    public  Integer             socketServerPort;
    @Autowired
    private GlobalLadProperties config;

    /**
     * Creates the socket server which will manage incoming connections from downlink clients.
     *
     * @param dataProducer this will be the data insert manager bean
     * @param factory      data factory bean
     * @param cli          command line override bean
     * @return the socket server instance.
     * @throws IOException
     */
    @Bean(name = BeanNames.GLAD_SOCKET_SERVER)
    @Scope("singleton")
    @Lazy
    public GlobalLadSocketServer globalLadSocketServer(final IDisruptorProducer<IGlobalLADData> dataProducer,
                                                       final IGlobalLadDataFactory factory,
                                                       final GlobalLadCommandLineParser cli) throws IOException {

        // TODO: switch based on config, socket vs jms
        int port = cli.getSocketServerPort();

        /**
         * MPCS-8055 triviski 3/21/2017 - Must do another check for the port.  The cli is used for the command
         * line run of the server, so it can be configured using the MPCS command line option mechanism.  However,
         * this will not work when the LAD is run from within tomcat.  There must be a way to set the socket server
         * port using java options, which spring has.  This check is to see if there is an override set using the
         * Spring method.
         */
        port = socketServerPort == null ? port : socketServerPort;

        return new GlobalLadSocketServer(dataProducer, config, factory, port, config.getTracer());
    }

    /**
     * Creates the data inserter manager which will handle disruptor events from all of the input clients and will
     * insert the data into the lad bean.
     *
     * @param lad                       the data store bean
     * @param binaryLoadHandlerProvider binary load handler provider
     * @return the data inserter manager
     */
    @Bean(name = BeanNames.GLAD_DATA_INSERTER)
    public DataInsertionManager dataInserterManager(final IGlobalLad lad,
                                                    final GlobalLadBinaryLoadHandlerProvider binaryLoadHandlerProvider) {
        return new DataInsertionManager(config, config.getTracer(), lad.getMasterContainer(),
                binaryLoadHandlerProvider);
    }

    /**
     * The reaper is responsible for removing stale data from the data store.
     *
     * @param reapTarget the data store
     * @return reaper
     */
    @Bean(name = BeanNames.GLAD_REAPER)
    public GlobalLadReaper globalLadReaper(final IGlobalLadReapable reapTarget, final GlobalLadProperties gladConfig) {
        final double                  memoryThreshold = gladConfig.getReapingMemoryThreshold();
        final IMemoryThresholdChecker memChecker      = new GladMemoryThresholdChecker(memoryThreshold);

        return new GlobalLadReaper(reapTarget, config.getTracer(), memChecker);
    }

    /**
     * Configures the worker executor that is used to run jobs periodically.  Adds the reaper bean to the worker on a
     * schedule.
     *
     * @param reaper
     * @return executor service that runs scheduled jobs.
     */
    @Bean(name = BeanNames.GLAD_WORKER_EXECUTOR)
    public ScheduledExecutorService globalLadWorkerExecutor(final GlobalLadReaper reaper,
                                                            final GlobalLadProperties gladConfig) {
        // Use guava to get an executor that is basically a daemon.
        final ScheduledExecutorService exec = MoreExecutors.getExitingScheduledExecutorService(
                (ScheduledThreadPoolExecutor) Executors
                        .newScheduledThreadPool(1, GlobalLadUtilities.createThreadFactory("glad-worker")));

        /**
         * MPCS-8221 triviski 5/19/2016 - If reaping is disabled will be null, check it first.
         */
        if (reaper != null) {
            final int reapCycle = gladConfig.getReapingTimeInterval();
            exec.scheduleWithFixedDelay(reaper, reapCycle, reapCycle, TimeUnit.SECONDS);
        }

        return exec;
    }

    /**
     * GLAD executor
     *
     * @return
     */
    @Bean(name = BeanNames.GLAD_EXECUTOR)
    public ExecutorService globalLadExecutor() {
        return Executors.newFixedThreadPool(1);
    }

    /**
     * JMS data source
     *
     * @param appContext                  Spring app context
     * @param sseContextFlag              SSE context
     * @param messageClientFactory        JMS messasge client factory
     * @param dataProducer                GLAD data producer
     * @param msgUtil                     JMS message utility
     * @param alarmValueSetFactory        alarm value set factory
     * @param messageServiceConfiguration JMS message service configuration
     * @return
     * @throws GlobalLadException
     */
    @Bean(name = BeanNames.JMS_DATA_SOURCE)
    @Scope("singleton")
    @Lazy
    public GlobalLadJmsDataSource getJmsDataSource(final ApplicationContext appContext,
                                                   final SseContextFlag sseContextFlag,
                                                   final IMessageClientFactory messageClientFactory,
                                                   final IDisruptorProducer<IGlobalLADData> dataProducer,
                                                   final IExternalMessageUtility msgUtil,
                                                   final IAlarmValueSetFactory alarmValueSetFactory,
                                                   final MessageServiceConfiguration messageServiceConfiguration) throws
                                                                                                                  GlobalLadException {
        GlobalLadProperties gladProperties = GlobalLadProperties.getGlobalInstance();
        List<String>        rootTopics     = gladProperties.getJmsRootTopics();
        TopicNameToken      eha;
        TopicNameToken      evr;
        String              topicFilter    = null;
        if (sseContextFlag.isApplicationSse()) {
            eha = TopicNameToken.APPLICATION_SSE_EHA;
            evr = TopicNameToken.APPLICATION_SSE_EVR;
        } else {
            eha = TopicNameToken.APPLICATION_EHA;
            evr = TopicNameToken.APPLICATION_EVR;
        }

        // If root topics are configured to DEFAULT, set default topics from mission properties etc. Topic parts are
        // overridden via GLAD config or CLI (hostname, venue type, venue name, downlink stream id)
        List<ITopicSubscriber> subscribers = new ArrayList<>();
        if (rootTopics.size() == 1 && rootTopics.get(0).equals(GlobalLadProperties.JMS_DEFAULT_TOPICS)) {
            // Init JMS venue
            JmsVenueHelper.initJmsVenue(appContext, gladProperties);
            String rootTopic = ContextTopicNameFactory.getMissionSessionTopic(appContext);
            rootTopics.clear();
            rootTopics.add(rootTopic);
        }

        // Go through root topics, and set up subscribers for EHA and EVR subtopics
        // set to try and cut out duplicates
        Set<String> topics = new HashSet<>();
        for (String rootTopic : rootTopics) {
            // handle a wildcard that matches to the end of a topic
            if (rootTopic.contains(">")) {
                topics.add(rootTopic.substring(0, rootTopic.indexOf('>') + 1));
            } else {
                topics.add(rootTopic);
            }
        }
        try {
            for (String rootTopic : topics) {
                // if we see a wildcard, don't add application topics
                if (rootTopic.contains(">")) {
                    ITopicSubscriber wildcardSubscriber = messageClientFactory
                            .getTopicSubscriber(rootTopic, topicFilter, false);
                    subscribers.add(wildcardSubscriber);
                } else {
                    ITopicSubscriber ehaSubscriber = messageClientFactory
                            .getTopicSubscriber(eha.getApplicationDataTopic(rootTopic), topicFilter, false);
                    subscribers.add(ehaSubscriber);
                    ITopicSubscriber evrSubscriber = messageClientFactory
                            .getTopicSubscriber(evr.getApplicationDataTopic(rootTopic), topicFilter, false);
                    subscribers.add(evrSubscriber);
                }
            }
        } catch (MessageServiceException e) {
            TraceManager.getDefaultTracer().error("Error connecting to JMS on [",
                    messageServiceConfiguration.getMessageServerHost(), ":",
                    messageServiceConfiguration.getMessageServerPort(), "]: ", e.getMessage());
            throw new GlobalLadException("Encountered a JMS exception. Please ensure the JMS bus is running.", e);
        }

        // Init JMS Data source
        MessageToGladDataConverter converter = new MessageToGladDataConverter(alarmValueSetFactory);
        Tracer                     tracer    = GlobalLadProperties.getTracer();
        GlobalLadJmsDataSource jmsDataSource = new GlobalLadJmsDataSource(subscribers, converter, msgUtil, dataProducer,
                tracer);

        return jmsDataSource;
    }

    /**
     * GLAD Data source provider. Returns currently configured data source (JMS or Socket)
     *
     * @param gladConfig
     * @return
     */
    @Bean
    @Lazy
    public GlobalLadDataSourceProvider getDataSourceProvider(GlobalLadProperties gladConfig) {
        return new GlobalLadDataSourceProvider(gladConfig);
    }

    /**
     * GLAD binary load handler provider. Chooses between Socket and JMS based on config
     *
     * @return
     */
    @Bean
    @Lazy
    public GlobalLadBinaryLoadHandlerProvider getBinaryParseHandlerProvider() {
        return new GlobalLadBinaryLoadHandlerProvider();
    }

    /**
     * JMS binary load handler bean
     *
     * @param inputStream          GLAD data protobuf input stream
     * @param dataFactory          GLAD data factory
     * @param dataInsertionManager GLAD data insertion manager
     * @return
     */
    @Bean
    @Lazy
    @Scope("prototype")
    public JmsBinaryLoadHandler getJmsBinaryParseHandler(final InputStream inputStream,
                                                         final IGlobalLadDataFactory dataFactory,
                                                         final DataInsertionManager dataInsertionManager) {
        return new JmsBinaryLoadHandler(inputStream, dataFactory, dataInsertionManager);
    }

    /**
     * Socket binary load handler bean
     *
     * @param inputStream GLAD data protobuf input stream
     * @return
     */
    @Bean
    @Lazy
    @Scope("prototype")
    public SocketBinaryLoadHandler getSocketBinaryLoadHandler(final InputStream inputStream) {
        return new SocketBinaryLoadHandler(inputStream);
    }
}
