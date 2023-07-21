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
package jpl.gds.eha.api.service.channel;

import jpl.gds.shared.interfaces.IService;


/**
 * An interface to be implemented by hybrid generic decom services, which can
 * decom both channels and EVRs.
 * 
 * @since R8
 */
public interface IHybridGenericPacketDecomService extends IService {
    // an empty marker interface
}