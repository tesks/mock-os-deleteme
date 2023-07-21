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
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory;
import jpl.gds.globallad.data.storage.DataInsertionManager;
import jpl.gds.globallad.io.IBinaryLoadHandler;
import jpl.gds.globallad.io.IGlobalLadDataSource;
import jpl.gds.globallad.io.jms.JmsBinaryLoadHandler;
import jpl.gds.globallad.io.socket.SocketBinaryLoadHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;

/**
 * Spring object provider for IBinaryParseHandler classes
 */
public class GlobalLadBinaryLoadHandlerProvider {

    @Autowired
    private ObjectProvider<JmsBinaryLoadHandler> jmsHandlerProvider;

    @Autowired
    private ObjectProvider<SocketBinaryLoadHandler> socketHandlerProvider;

    @Autowired
    private IGlobalLadDataFactory dataFactory;

    /**
     * Get a binary load handler to insert data into the GLAD from a binary input stream
     *
     * @param inputStream          data input stream
     * @param dataInsertionManager data insertion manager
     * @return
     */
    public synchronized IBinaryLoadHandler getBinaryLoadHandler(final InputStream inputStream,
                                                                final DataInsertionManager dataInsertionManager) {
        if (GlobalLadProperties.getGlobalInstance().getDataSource() == IGlobalLadDataSource.DataSourceType.JMS) {
            return jmsHandlerProvider.getObject(inputStream, dataFactory, dataInsertionManager);
        } else {
            return socketHandlerProvider.getObject(inputStream);
        }
    }
}
