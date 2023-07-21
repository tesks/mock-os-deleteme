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

package jpl.gds.telem.common.config;

import jpl.gds.shared.config.AbstractWritableProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

import java.util.List;

/**
 * Abstract class implementing the IDownlinkProperties interface to consolidate common 'downlink property'
 * property lookups
 *
 */
public abstract class AbstractDownlinkProperties extends AbstractWritableProperties implements IDownlinkProperties  {

    /** Default value for whether or not remote mode is enabled */
    public static final boolean DEFAULT_REMOTE_MODE_PROPERTY = false;

    /** Whether or not miscellaneous features are enabled */
    protected boolean enableMiscFeatures = false;

    /** List of miscellaneous feature class names to use if enableMiscFeatures is enabled*/
    protected List<String> miscFeaturesClassNames;

    /** Whether or not 'Ongoing Mode' is enabled */
    protected boolean enableOngoingMode				= DEFAULT_REMOTE_MODE_PROPERTY;

    /** The configured FSW rest port */
    protected int restPortFsw;

    /** The configured SSE rest port */
    protected int restPortSse;

    /**
     * Constructor that loads the default property file, which will be located using the
     * standard configuration search.
     *
     * @param propertyFile the Property file load
     * @param sseFlag
     *            the SSE context flag
     */
    public AbstractDownlinkProperties(final String propertyFile, final SseContextFlag sseFlag) {
        super(propertyFile, sseFlag);
    }



    @Override
    public boolean isEnableOngoingMode() {
        return enableOngoingMode;
    }


    @Override
    public List<String> getMiscFeatures() {
        return miscFeaturesClassNames;
    }


    @Override
    public boolean isEnableMiscFeatures() {
        return enableMiscFeatures;
    }

    @Override
    public int getRestPortFsw() { return restPortFsw; }

    @Override
    public int getRestPortSse() { return restPortSse; }



}
