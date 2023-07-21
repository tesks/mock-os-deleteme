/*
 * Copyright 2006-2021. California Institute of Technology.
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

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.io.jms.GlobalLadJmsDataSource;
import jpl.gds.globallad.io.socket.GlobalLadSocketServer;
import jpl.gds.globallad.io.IGlobalLadDataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provides the configured data source to the GLAD.
 */
public class GlobalLadDataSourceProvider {
    private final GlobalLadProperties gladProperties;

    @Autowired
    private ObjectProvider<GlobalLadJmsDataSource> jmsDataSourceObjectProvider;
    private GlobalLadJmsDataSource                 jmsDataSource;

    @Autowired
    private ObjectProvider<GlobalLadSocketServer> socketServerProvider;
    private GlobalLadSocketServer                 socketServer;

    /**
     * Constructor
     *
     * @param gladProperties global LAD properties
     */
    public GlobalLadDataSourceProvider(final GlobalLadProperties gladProperties) {
        this.gladProperties = gladProperties;
    }

    /**
     * Return the data source configured in properties
     *
     * @return global LAD data source, either socket server or JMS
     */
    public synchronized IGlobalLadDataSource getDataSource() {
        if (gladProperties.getDataSource().equals(IGlobalLadDataSource.DataSourceType.JMS)) {
            if (jmsDataSource == null) {
                jmsDataSource = jmsDataSourceObjectProvider.getObject();
            }
            return jmsDataSource;
        } else if (gladProperties.getDataSource().equals(IGlobalLadDataSource.DataSourceType.SOCKET)) {
            if (socketServer == null) {
                socketServer = socketServerProvider.getObject();
            }
            return socketServer;
        }
        throw new IllegalArgumentException("Invalid data source in configuration: " + gladProperties.getDataSource());
    }
}
