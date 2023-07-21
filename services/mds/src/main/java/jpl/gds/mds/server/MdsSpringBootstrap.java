/*
 * Copyright 2006-2020. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.mds.server;

import jpl.gds.mds.server.config.MdsProperties;
import jpl.gds.mds.server.disruptor.IMessageEventConsumer;
import jpl.gds.mds.server.disruptor.IMessageEventProducer;
import jpl.gds.mds.server.disruptor.RingBufferController;
import jpl.gds.mds.server.disruptor.RingBufferPublisher;
import jpl.gds.mds.server.sfdu.ChdoSfduValidator;
import jpl.gds.mds.server.sfdu.ISfduValidator;
import jpl.gds.mds.server.sfdu.Mon0158Validator;
import jpl.gds.mds.server.spring.ClientConnectionManagerProvider;
import jpl.gds.mds.server.spring.IpFilter;
import jpl.gds.mds.server.spring.Monitor0158Filter;
import jpl.gds.mds.server.spring.SpillProcessorProvider;
import jpl.gds.mds.server.tcp.ClientConnectionManager;
import jpl.gds.mds.server.tcp.IClientConnectionManager;
import jpl.gds.mds.server.tcp.MessageDistributor;
import jpl.gds.mds.server.tcp.SocketFactoryHelper;
import jpl.gds.mds.server.tcp.TcpSocketServer;
import jpl.gds.mds.server.udp.IUdpClient;
import jpl.gds.security.ssl.ISslConfiguration;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.station.api.IStationHeaderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.integration.ip.udp.UnicastSendingMessageHandler;
import org.springframework.messaging.Message;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * Spring bootrstap configuration for Monitor Data Service Note: don't include in the bootstrap package so other apps
 * don't run into issues loading it
 */
@Configuration
public class MdsSpringBootstrap {

    protected static final String UDP_DATA_CONSUMER   = "UDP_DATA_CONSUMER";
    protected static final String MESSAGE_DISTRIBUTOR = "MESSAGE_DISTRIBUTOR";
    protected static final String CHDO_SFDU_VALIDATOR = "CHDO_SFDU_VALIDATOR";
    protected static final String MON0158_VALIDATOR   = "MON0158_VALIDATOR";

    @Autowired
    private ApplicationContext appContext;

    /**
     * Create MDS properties
     *
     * @return MdsProperties
     */
    @Bean
    @Scope("singleton")
    public MdsProperties mdsProperties() {
        return new MdsProperties();
    }

    /**
     * Integration Flow bootstrap bean definition
     *
     * @param mdsProperties MDS properties
     * @param validator     SFDU Validator
     * @return IntegrationFlow
     */
    @Bean
    @Autowired
    public IntegrationFlow processUniCastUdpMessage(final MdsProperties mdsProperties,
                                                    @Qualifier(MON0158_VALIDATOR) ISfduValidator validator) {
        IntegrationFlowBuilder flowBuilder = IntegrationFlows
                .from(new UnicastReceivingChannelAdapter(mdsProperties.getClientPort()));
        if (mdsProperties.isSourceIpFiltering()) {
            IpFilter filter = new IpFilter(mdsProperties.getAllowedSourceIps());
            flowBuilder = flowBuilder.filter(Message.class, filter);
        }
        if (mdsProperties.isValidatePackets()) {
            Monitor0158Filter filter = new Monitor0158Filter(validator);
            flowBuilder = flowBuilder.filter(Message.class, filter);
        }
        flowBuilder = flowBuilder.handle("UDP_DATA_CONSUMER", "handleMessage");
        return flowBuilder.get();
    }


    /**
     * Unicast Sending Message Handler bootstrap bean definition
     *
     * @param mdsProperties MDS properties
     * @return UnicastSendingMessageHandler
     */
    @Bean
    @Autowired
    public UnicastSendingMessageHandler udpSendingAdapter(final MdsProperties mdsProperties) {
        return new UnicastSendingMessageHandler(mdsProperties.getUdpForwardHost(), mdsProperties.getClientPort());
    }

    /**
     * Tcp Socket Server bootstrap bean definition
     *
     * @param messageEventConsumer            message event consumer
     * @param ringBufferController            ring buffer controller
     * @param clientConnectionManagerProvider client connection manager provider
     * @param mdsProperties                   MDS properties
     * @param sslConfiguration                SSL/TLS configuration
     * @param appContext                      spring app context
     * @return TcpSocketServer
     */
    @Bean
    public TcpSocketServer getTcpSocketServer(final IMessageEventConsumer messageEventConsumer,
                                              final RingBufferController ringBufferController,
                                              final ClientConnectionManagerProvider clientConnectionManagerProvider,
                                              final MdsProperties mdsProperties,
                                              final ISslConfiguration sslConfiguration,
                                              final ApplicationContext appContext) {

        ServerSocketFactory socketFactory = null;

        try {
            socketFactory = SocketFactoryHelper
                    .createServerSocketFactory(mdsProperties.isSecureTcp(), sslConfiguration);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException |
                KeyManagementException | IOException e) {
            TraceManager.getTracer(Loggers.MDS).error("Could not initialize TCP socket factory. ", e.getMessage());
            SpringApplication.exit(appContext);
        }

        return new TcpSocketServer(messageEventConsumer, ringBufferController, clientConnectionManagerProvider,
                socketFactory);
    }

    /**
     * Message Event Consumer bootstrap bean definition
     *
     * @return IMessageEventConsumer
     */
    @Bean(MESSAGE_DISTRIBUTOR)
    @Scope("singleton")
    public IMessageEventConsumer messageDistributor() {
        return new MessageDistributor();
    }

    /**
     * Message Event Producer bootstrap bean definition
     *
     * @return IMessageEventProducer
     */
    @Bean(name = UDP_DATA_CONSUMER)
    public IMessageEventProducer messageEventProducer(final IUdpClient udpClient) {
        return new RingBufferPublisher(udpClient);
    }

    /**
     * SFDU Validator bean, using module-local classes. Logs information about why a header is invalid to debug.
     *
     * @param properties MDS properties
     * @return SFDU Validator
     */
    @Bean(MON0158_VALIDATOR)
    public ISfduValidator getSfduValidator(final MdsProperties properties) {
        return new Mon0158Validator(properties);
    }

    /**
     * SFDU Validator bean, using station_api and station_impl classes. Has no output about why the SFDU is invalid.
     *
     * @param factory
     * @return
     */
    @Bean(CHDO_SFDU_VALIDATOR)
    public ISfduValidator getSfduValidator(final IStationHeaderFactory factory) {
        return new ChdoSfduValidator(factory);
    }

    /**
     * Ring Buffer Controller
     *
     * @return RingBufferController
     */
    @Bean
    public RingBufferController ringBufferController() {
        return new RingBufferController();
    }

    /**
     * Client connection manager bean
     *
     * @param messageDistributor     IMessageEventConsumer
     * @param socket                 TCP Socket
     * @param spillProcessorProvider spill processor factory
     * @return IClientConnectionManager
     */
    @Bean
    @Scope("prototype")
    public IClientConnectionManager getClientConnectionManager(final IMessageEventConsumer messageDistributor,
                                                               @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") final Socket socket,
                                                               SpillProcessorProvider spillProcessorProvider) {
        return new ClientConnectionManager(messageDistributor, socket, spillProcessorProvider);
    }

    /**
     * Client connection manager provider
     *
     * @return
     */
    @Bean
    @Scope("singleton")
    @Lazy
    public ClientConnectionManagerProvider clientConnectionManagerProvider() {
        return new ClientConnectionManagerProvider();
    }

    /**
     * Spill processor provider
     *
     * @param mdsProperties
     * @return
     */
    @Bean
    @Scope("singleton")
    @Lazy
    public SpillProcessorProvider getSpillProcessorProvider(final MdsProperties mdsProperties) {
        return new SpillProcessorProvider(mdsProperties);
    }

    /**
     * Creates and returns the Monitor Data Service bean
     *
     * @return Monitor Data Service bean
     */
    @Bean
    @Scope("singleton")
    public MonitorDataService getMonitorDataService() {
        return new MonitorDataService(appContext);
    }
}
