/*
 * Copyright 2006-2020. California Institute of Technology.
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

package jpl.gds.tc.legacy.impl.cltu;

import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.TcApiBeans;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.frame.ITcTransferFrameSerializer;
import jpl.gds.tc.impl.cltu.CltuFactory;
import org.springframework.context.ApplicationContext;

/**
 * Legacy CLTU factory. Overrides default behavior for retrieving frame bytes, which must be randomized manually
 * if randomization is enabled in config.
 *
 */
public class LegacyCltuFactory extends CltuFactory {

    private final CommandFrameProperties commandFrameProperties;

    /**
     * @param appContext The current application context
     */
    public LegacyCltuFactory(ApplicationContext appContext) {
        super(appContext);
        this.commandFrameProperties = appContext.getBean(CommandFrameProperties.class);
        //MPCS-11509 - use legacy frame serializer
        this.frameSerializer = appContext.getBean(TcApiBeans.LEGACY_COMMAND_FRAME_SERIALIZER, ITcTransferFrameSerializer.class);
    }

    /**
     * Get bytes from frame
     * @param frame ITcTransferFrame
     * @return byte array
     */
    protected byte[] getBytesFromFrame(ITcTransferFrame frame) {
        byte[] frameBytes = frameSerializer.getBytes(frame);
        if (commandFrameProperties.isDoRandomization()) {
            frameBytes = commandFrameProperties.getRandomizer().randomize(frameBytes);
        }
        return frameBytes;
    }
}
