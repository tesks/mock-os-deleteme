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
package jpl.gds.telem.common.config;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.message.api.config.MessageServiceConfiguration;

/**
 * Abstract class to handle common code and operations for an IDownlinkConfiguration
 */
public abstract class AbstractDownlinkConfiguration implements IDownlinkConfiguration {


    protected      boolean             useJms;
    protected       boolean             useDb;

    protected final IDatabaseProperties         dbProperties;
    protected final MessageServiceConfiguration msgConfig;

    protected IDownlinkProperties featureSet;


    public AbstractDownlinkConfiguration(final IDatabaseProperties dbProps,
                                         final MessageServiceConfiguration msgConfig) {
        this.dbProperties = dbProps;
        this.msgConfig = msgConfig;

        this.useJms = msgConfig.getUseMessaging();

        this.useDb = this.dbProperties.getUseDatabase();
    }



    @Override
    public IDownlinkProperties getFeatureSet() { return featureSet; }

    @Override
    public void setFeatureSet(final IDownlinkProperties featureSet) {
        this.featureSet = featureSet;
    }


    @Override
    public boolean isUseDb() {
        return useDb;
    }

    @Override
    public void setUseDb(final boolean useDb) {
        this.useDb = useDb;
        this.dbProperties.setUseDatabase(this.useDb);
    }

    @Override
    public boolean isUseMessageService() {
        return useJms;
    }

    @Override
    public void setUseMessageService(final boolean useJms) {
        this.useJms = useJms;
        this.msgConfig.setUseMessaging(this.useJms);
    }
}
