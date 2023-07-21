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
package jpl.gds.automation.auto.cfdp.service;

import jpl.gds.shared.interfaces.IService;
import org.springframework.http.ResponseEntity;

/**
 * Interface for AUTO cfdp services
 */
public interface IAutoCfdpService extends IService {

    /**
     * Whether or not the service is running
     * @return true if the service is active
     */
    public boolean isRunning();

    /**
     * Handles the PDU aggregation for a destination entity ID
     *
     * @param entityId
     *            The destination entity id
     * @param pduData
     *            The PDU Data
     * @param vcid
     *            The vcid destination
     * @return ResponseEntity<Object> PDU aggregation status
     * TODO: Refactor out the ResponseEntity return
     */
    public ResponseEntity<Object> aggregatePdus(final Integer entityId, final byte[] pduData, final Integer vcid);

}
