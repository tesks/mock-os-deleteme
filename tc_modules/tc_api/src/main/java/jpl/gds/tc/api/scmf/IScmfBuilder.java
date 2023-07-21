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
package jpl.gds.tc.api.scmf;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.IScmfInternalMessageFactory;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.exception.ScmfParseException;

/**
 * IScmfBuilder is the interface to be utilized by a builder that constructs SCMFs
 *
 *
 * MPCS-11216  - 09/10/19 - Moved constants MESSAGE_BIT_LENGTH_MAX_VALUE and MESSAGE_BIT_LENGTH_MIN_VALUE
 *   here from LegacyScmfBuilder. Added BYTE MIN and MAX.
 */
public interface IScmfBuilder {

    /** The max allowable value for the message bit length field */
    int MESSAGE_BIT_LENGTH_MAX_VALUE = 32752;
    int MESSAGE_BYTE_LENGTH_MAX_VALUE = MESSAGE_BIT_LENGTH_MAX_VALUE / Byte.SIZE;
    /** The min allowable value for the message bit length field */
    int MESSAGE_BIT_LENGTH_MIN_VALUE = 16;
    int MESSAGE_BYTE_LENGTH_MIN_VALUE = MESSAGE_BIT_LENGTH_MAX_VALUE / Byte.SIZE;

    /**
     * Set the file path of an SCMF
     *
     * @param filePath the SCMF file path
     * @return this builder
     */
    IScmfBuilder setFilePath(final String filePath);

    /**
     * Set the internal message factory for SCMFs
     *
     * @param scmfInternalMessageFactory SCMF internal message factory
     * @return this builder
     */
    IScmfBuilder setInternalMessageFactory(final IScmfInternalMessageFactory scmfInternalMessageFactory);

    /**
     * Set the Scmf Properties for this builder
     *
     * @param scmfProperties the SCMF properties
     * @return this builder
     */
    IScmfBuilder setScmfProperties(final ScmfProperties scmfProperties);

    /**
     * Set the Mission Properties for this builder
     *
     * @param missionProperties mission properties
     * @return this builder
     */
    IScmfBuilder setMissionProperties(final MissionProperties missionProperties);

    /**
     * Return an IScmf from the builder
     *
     * @return the IScmf that was built from the supplied file
     * @throws ScmfParseException an error occured while attempting to parse the data into an SCMF
     */
    IScmf build() throws ScmfParseException;

}
