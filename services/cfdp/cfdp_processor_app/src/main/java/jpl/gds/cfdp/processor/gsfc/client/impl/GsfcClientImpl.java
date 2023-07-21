/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.cfdp.processor.gsfc.client.impl;

import cfdp.client.DefaultFilestore;
import cfdp.client.MyFilestoreRequestHandler;
import cfdp.engine.*;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.mib.MibManager;
import jpl.gds.cfdp.processor.out.GenericOutboundPduSinkWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GsfcClientImpl implements Client {

    @Autowired
    private ConfigurationManager configurationManager;

    /**
     * The CommLink object
     */
    @Autowired
    private GenericOutboundPduSinkWrapper genericOutboundPduSinkWrapper;

    private final Filestore filestore = new DefaultFilestore();
    private final FilestoreRequestHandler filestoreRequestHandler = new MyFilestoreRequestHandler((DefaultFilestore) filestore);

    @Autowired
    private MIB mib;

    @Autowired
    private User user;

    @Override
    public CommLink getCommLink() {
        return genericOutboundPduSinkWrapper;
    }

    @Override
    public Filestore getFilestore() {
        return filestore;
    }

    @Override
    public MIB getMIB() {
        return mib;
    }

    @Override
    public String getRestoreDir() {
        return configurationManager.getSavedStateDirectory();
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public boolean restore(final MIB mib) {
        if (!(mib instanceof MibManager)) {
            user.error("GSFCClient.restore provided incompatible MIB");
            return false;
        }
        this.mib = mib;
        return true;
    }

    @Override
    public FilestoreRequestHandler getRequestHandler() {
        return filestoreRequestHandler;
    }

    @Override
    public Clock getClock() {
        // Use JavaCFDP's default clock
        return null;
    }

    @Override
    public ChecksumAlgorithm getEofPduChecksumAlgorithm() {
        return configurationManager.getEofPduChecksumAlgorithm();
    }

    @Override
    public boolean getEofPduChecksumValidationEnabled() {
        return configurationManager.isEofPduChecksumValidationEnabled();
    }

}
