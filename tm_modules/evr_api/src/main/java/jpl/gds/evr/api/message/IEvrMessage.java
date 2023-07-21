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
package jpl.gds.evr.api.message;

import jpl.gds.common.types.IRealtimeRecordedSupport;
import jpl.gds.evr.api.IEvr;
import jpl.gds.shared.interfaces.EscapedCsvSupport;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.template.FullyTemplatable;

/**
 * An interface to be implemented by EVR messages.
 * 
 * @since R8
 */
public interface IEvrMessage extends IMessage, FullyTemplatable, EscapedCsvSupport, IRealtimeRecordedSupport {

    /**
     * Retrieves the EVR member.
     * 
     * @return the EVR object.
     */
    public IEvr getEvr();

}