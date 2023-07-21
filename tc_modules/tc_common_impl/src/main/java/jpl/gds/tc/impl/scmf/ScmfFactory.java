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
package jpl.gds.tc.impl.scmf;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.impl.ContextIdentification;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.IScmfFactory;
import jpl.gds.tc.api.ITcScmfWriter;
import jpl.gds.tc.api.ITewUtility;
import jpl.gds.tc.api.TcApiBeans;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.cltu.ITcCltuBuilder;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.ScmfParseException;
import jpl.gds.tc.api.exception.ScmfWrapUnwrapException;
import jpl.gds.tc.api.scmf.IScmfBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The ScmfFactory class creates IScmf objects from a file, CLTUs, or arbitrary byte data
 *
 */
public class ScmfFactory implements IScmfFactory {

    private final ApplicationContext appContext;
    private final ContextIdentification contextId;
    private final MissionProperties missionProps;
    private final ITewUtility tewUtility;

    private final ITewUtility legacyTewUtility;

    private final Tracer log;

    public ScmfFactory(final ApplicationContext appContext) {
        this.appContext = appContext;
        this.contextId = appContext.getBean(ContextIdentification.class);
        this.missionProps = appContext.getBean(MissionProperties.class);
        this.tewUtility = appContext.getBean(ITewUtility.class);

        this.legacyTewUtility = appContext.getBean(TcApiBeans.LEGACY_TEW_UTILITY, ITewUtility.class);

        this.log = TraceManager.getTracer(appContext, Loggers.UPLINK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IScmf parse(final String filename) throws ScmfWrapUnwrapException {
        try {
            return tewUtility.reverseScmf(filename);
        } catch (final ScmfWrapUnwrapException | ScmfParseException | IOException e) {

            try {
                log.info("Unable to parse SCMF file " + filename + " with the standard TEW utility. Attempting to parse with legacy...");
                final IScmf tmp = legacyTewUtility.reverseScmf(filename);
                log.info("Legacy parsing successful!");

                return tmp;
            } catch (final Exception ex) {
                log.warn("Unable to parse SCMF file " + filename + " with legacy TEW utility. Reason:" + ExceptionTools.getMessage(ex));
            }

            throw new ScmfWrapUnwrapException(ExceptionTools.getMessage(e));
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public IScmf parse(final File scmfFile) throws ScmfWrapUnwrapException {
        return parse(scmfFile.getAbsolutePath());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public IScmf toScmf(final List<ICltu> flightCltus,
            final boolean writeToDisk) throws CltuEndecException, ScmfWrapUnwrapException {
        if (flightCltus == null) {
            throw new IllegalArgumentException("Null input CLTU list");
        }
        else if (flightCltus.isEmpty()) {
            throw new IllegalArgumentException(
                    "Input CLTU list is empty.  No SCMF file can be written");
        }

        // get the name of the SCMF
        final ScmfProperties scmfConfig = appContext.getBean(ScmfProperties.class);
        final String scmfName = scmfConfig.getScmfName();
        scmfConfig.setScmfName(null);
        if (scmfName == null) {
            TraceManager.getDefaultTracer().error("There is no valid name specified for the output SCMF. No SCMF can be written.");
            return (null);
        }

        final ITcScmfWriter scmfWriter = appContext.getBean(ITcScmfWriter.class);
        configureScmfHeader(scmfConfig, scmfName, scmfWriter);


        for(final ICltu cltu : flightCltus) {
            final ITcCltuBuilder bldr = appContext.getBean(ITcCltuBuilder.class);
            bldr.setCodeblocks(cltu.getCodeblocks());

            if(cltu.getAcquisitionSequence().length > 0) {
                bldr.setAcquisitionSequence(cltu.getAcquisitionSequence());
            }

            if(cltu.getStartSequence().length > 0) {
                bldr.setStartSequence(cltu.getStartSequence());
            }

            if(cltu.getIdleSequence().length > 0) { //The TC CLTUs don't have an idle sequence for some reason
                bldr.setTailSequence(ArrayUtils.addAll(cltu.getTailSequence(), cltu.getIdleSequence()));
            } else if(cltu.getTailSequence().length > 0) {
                bldr.setTailSequence(cltu.getTailSequence());
            }

            scmfWriter.addDataRecord(BinOctHexUtility.toHexFromBytes(bldr.build()));
        }

        scmfWriter.writeScmf();

        try {
            final IScmf returnScmf = tewUtility.reverseScmf(scmfName);

            handleScmfFile(scmfConfig, scmfName);

            return returnScmf;
        } catch (final ScmfParseException | IOException e) {
            throw new ScmfWrapUnwrapException(ExceptionTools.getMessage(e));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IScmf toScmf(final byte[] data, final boolean writeToDisk)
            throws  ScmfWrapUnwrapException {
    	/*
    	 * MPCS-10813 - 04/09/19 - Because we now have a toScmf function that
    	 * does the same thing, but will make multiple messages, this function now
    	 * implements that one.
    	 */
        if (data == null) {
            throw new IllegalArgumentException("Null input byte[] data");
        } else if (data.length == 0) {
            throw new IllegalArgumentException("Empty input byte[] data");
        }
    	
    	final List<Byte[]> tmp = new ArrayList<>();

        /*
         * MPCS-11216 - 09/10/19 - With the legacy SCMF builder and parser we could put all of the data into one
         * command message. However, the CTS library doesn't like that. We've also updated our parsing in LegacyScmfBuilder
         * to also conform to that. This is now reflected in the construction.
         */

        if(data.length > IScmfBuilder.MESSAGE_BYTE_LENGTH_MAX_VALUE) {
            final ByteArrayInputStream bais = new ByteArrayInputStream(data);

            byte[] part;
            int partLen;
            int prtLen;
            while (bais.available() > 0) {
                partLen = Math.min(bais.available(), IScmfBuilder.MESSAGE_BYTE_LENGTH_MAX_VALUE);
                part = new byte[partLen];

                try {
                    prtLen = bais.read(part);
                } catch (final IOException e) {
                    throw new ScmfWrapUnwrapException("Unknown error encountered while trying to split data to multiple command messages - " + e.getMessage());
                }

                // while these two cases shouldn't be hit, let's handle them just in case
                if(prtLen == -1) {
                    break;
                }
                if(prtLen < partLen) {
                    part = Arrays.copyOf(part, prtLen);
                }

                tmp.add(ArrayUtils.toObject(part));
            }
        } else {
            tmp.add(ArrayUtils.toObject(data));
        }
        return toScmf(writeToDisk, tmp);
    }
    
    /**
     * MPCS-10813 - 04/09/19 - New function. An SCMF can hold many ScmfCommandMessages.
     * Here we do not know, nor should we care, which data belongs in which message. Take each
     * element in the List and create a new message within the SCMF that will be returned.
     * 
     * Except for the for loop creating and adding all of the messages, this code is identical to
     * the old toScmf(byte[], boolean)
     */
    @Override
	public IScmf toScmf(final boolean writeToDisk, final List<Byte[]> data)
    		throws ScmfWrapUnwrapException {
    	if (data == null) {
            throw new IllegalArgumentException("Null input byte[] data");
        }

        // get the SCMF name
        final ScmfProperties scmfConfig = appContext.getBean(ScmfProperties.class);
        final String scmfName = scmfConfig.getScmfName();
        scmfConfig.setScmfName(null);

        final ITcScmfWriter scmfWriter = appContext.getBean(ITcScmfWriter.class);
        configureScmfHeader(scmfConfig, scmfName, scmfWriter);


        for(final Byte[] datum : data) {
            // MPCS-11216  - 09/10/19 - added length check to verify we don't create a bad message
            if(datum.length > IScmfBuilder.MESSAGE_BYTE_LENGTH_MAX_VALUE) {
                throw new IllegalArgumentException("A data record entry of " + datum.length + " bytes was supplied. Maximum data record length is " + IScmfBuilder.MESSAGE_BYTE_LENGTH_MAX_VALUE + " bytes");
            }

            scmfWriter.addDataRecord(BinOctHexUtility.toHexFromBytes(ArrayUtils.toPrimitive(datum)));
        }


        scmfWriter.writeScmf();

        final IScmf returnScmf = parse(scmfName);

        handleScmfFile(scmfConfig, scmfName);

        return returnScmf;
    }

    /**
     * Handle the SCMF file according to the SCMF properties
     *
     * @param scmfProperties SCMF properties
     * @param scmfName       file path to the SCMF
     */
    private void handleScmfFile(final ScmfProperties scmfProperties, final String scmfName) {
        if (scmfProperties.getWriteScmf()) {
            log.info("Wrote SCMF to " + scmfName);
        } else {
            final File scmfFile = new File(scmfName);
            if (scmfFile.exists()) {
                scmfFile.delete();
            }
        }
    }

    private void configureScmfHeader(final ScmfProperties scmfConfig, final String scmfName, final ITcScmfWriter scmfWriter) {
        scmfWriter.setScid(contextId.getSpacecraftId());
        scmfWriter.setMissionId(missionProps.getMissionId());
        scmfWriter.setOutScmfFile(scmfName);
        // Set SCMF's header
        scmfWriter.setScmfHeaderPreparerName(scmfConfig.getPreparer());
        scmfWriter.setScmfHeaderBitOneRadiationTime(scmfConfig.getBitOneRadiationTime());
        scmfWriter.setScmfHeaderBitRateIndex(scmfConfig.getBitRateIndex());
        scmfWriter.setScmfHeaderComment(scmfConfig.getComment());
        scmfWriter.setScmfHeaderTitle(scmfConfig.getTitle());
        scmfWriter.setScmfHeaderUntimed(scmfConfig.getUntimed());

        scmfWriter.setScmfMessageRadiationStartTime(ScmfDateUtils.getTransmissionStartTime(scmfConfig));
        scmfWriter.setScmfMessageRadiationWindowOpenTime(ScmfDateUtils.getTransmissionWindowOpenTime(scmfConfig));
        scmfWriter.setScmfMessageRadiationWindowCloseTime(ScmfDateUtils.getTransmissionWindowCloseTime(scmfConfig));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IScmf toScmf(final File dataFile, final boolean isHexFile,
            final boolean writeToDisk) throws IOException, ScmfWrapUnwrapException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(65536);
        FileUtility.writeFileToOutputStream(baos, dataFile, isHexFile);

        return toScmf(baos.toByteArray(), writeToDisk);
    }
}
