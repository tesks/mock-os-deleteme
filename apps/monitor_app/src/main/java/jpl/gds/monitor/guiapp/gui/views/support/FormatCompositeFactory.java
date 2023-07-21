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
/**
 * 
 */
package jpl.gds.monitor.guiapp.gui.views.support;

import org.eclipse.swt.widgets.Shell;

import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.AbstractFormatComposite;
import jpl.gds.shared.swt.FloatFormatComposite;
import jpl.gds.shared.swt.SignedIntFormatComposite;
import jpl.gds.shared.swt.StringFormatComposite;
import jpl.gds.shared.swt.UnsignedIntFormatComposite;

/**
 * The FormatCompositeFactory manufactures GUI Composites that allow the user 
 * to enter formatters for channel data fields.

 */
public class FormatCompositeFactory {

    /**
     * Creates the appropriate Format Composite for the given data type.
     * 
     * @param parent
     *            the parent Shell widget for the composite
     * @param def
     *            the channel data type
     * @param isRaw
     *            true if specifying the Raw/DN formatter, false for Value/EU
     * @return a format composite based on the given channel type
     */
    public static AbstractFormatComposite create(final Shell parent,
            final IChannelDefinition def, final boolean isRaw) {
        final Tracer trace = TraceManager.getDefaultTracer();
        switch (def.getChannelType()) {
        case SIGNED_INT:
            if (isRaw) {
                    return new SignedIntFormatComposite(parent, trace);
            } else {
                if (def.hasEu()) {
                        return new FloatFormatComposite(parent, trace);
                } else {
                        return new SignedIntFormatComposite(parent, trace);
                }
            }

        case UNSIGNED_INT:
        case DIGITAL:
        case TIME:
            if (isRaw) {
                    return new UnsignedIntFormatComposite(parent, trace);
            } else {
                if (def.hasEu()) {
                        return new FloatFormatComposite(parent, trace);
                } else {
                        return new UnsignedIntFormatComposite(parent, trace);
                }
            }

        case FLOAT:
                return new FloatFormatComposite(parent, trace);

        case ASCII:
            return new StringFormatComposite(parent);

        case STATUS:
            if (isRaw) {
                    return new SignedIntFormatComposite(parent, trace);
            } else {
                return new StringFormatComposite(parent);
            }

        case BOOLEAN:
            if (isRaw) {
                    return new UnsignedIntFormatComposite(parent, trace);
            } else {
                return new StringFormatComposite(parent);
            }
        }
        return null;
    }
}
