/*
 * Copyright 2006-2021. California Institute of Technology.
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

package jpl.gds.globallad.io;

import jpl.gds.globallad.IGlobalLadJsonable;
import jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm;

import javax.json.JsonObject;
import java.io.Closeable;

/**
 * Interface for GLAD data sources
 */
public interface IGlobalLadDataSource extends Runnable, IGlobalLadJsonable, Closeable {

    enum DataSourceType {
        JMS("JMS"),
        SOCKET("SOCKET");

        private final String type;

        DataSourceType(final String type) {
            this.type = type;
        }

        public static DataSourceType fromString(final String type) {
            switch(type.toUpperCase()) {
                case "JMS":
                    return JMS;
                case "SOCKET":
                    return SOCKET;
                default:
                    return null;
            }
        }
    }

    /**
     * Get data source statistics
     *
     * @return json object
     */
    JsonObject getStats();

    /**
     * Get data source metadata
     *
     * @param matcher unused at data source level
     * @return json object
     */
    JsonObject getMetadata(IGlobalLadContainerSearchAlgorithm matcher);

    /**
     * Get string identifier for data source
     *
     * @return
     */
    String getJsonId();
}
