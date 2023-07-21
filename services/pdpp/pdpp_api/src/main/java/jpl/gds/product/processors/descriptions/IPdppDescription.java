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
package jpl.gds.product.processors.descriptions;

/**
 * Interface for Description helper classes, which contain string metadata describing a given Processor's operations
 * and allow for accurate naming of child products and sessions
 */
public interface IPdppDescription {

    /**
     * @return string that will be the standard session suffix for a processor
     */
    String getSessionSuffix();

    /**
     * @return string that will be the standard backlink explanation for a processor
     */
    String getBacklinkExplanation();

    /**
     * Generates the name of the new PDPP session off of the parent session name.
     * @param parentSessionName
     * @return
     */
    String generateName(String parentSessionName);

    /**
     * Builds the description from the parent description.
     * @param description
     * @return
     */
    String generateDescription(String description);

    /**
     * Builds the type from the parent session and the backlink Explanation.
     * @param sessionNumber
     * @return
     */
    String generateType(Long sessionNumber);
}
