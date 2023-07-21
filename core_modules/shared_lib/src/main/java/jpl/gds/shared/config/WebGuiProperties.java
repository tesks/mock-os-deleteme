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

package jpl.gds.shared.config;

import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * Configuration class for Web GUI properties
 */
public class WebGuiProperties extends GdsHierarchicalProperties {

    /** Web GUI properties file */
    protected static final String                   PROPERTY_FILE       = "web_gui.properties";

    private static final String                     PROPERTY_PREFIX     = "webGui.";

    private static final int DEFAULT_WEBGUI_PORT = 4200;
    private static final String DEFAULT_URL = "http://localhost:%d/%s";

    private static final String WEB_GUI_URL = PROPERTY_PREFIX + "url";

    private static final String WEB_GUI_SERVICES_URL = PROPERTY_PREFIX + "%s" + ".url";

    /**
     * Resource handler used to create the mapping for serving the GUI code
     * Ex: /ampcs
     */
    private static final String RESOURCE_HANDLER = PROPERTY_PREFIX + "resource.handler";

    /**
     * Resource redirect routes. Browser refresh/reload events should redirect the user
     * to index.html
     * Ex: /ampcs/fsw-process -> /ampcs/index.html
     */
    private static final String RESOURCE_REDIRECT_ROUTES = PROPERTY_PREFIX + "resource.redirect.routes";

    /**
     * Resource location used in conjunction with the Resource handler to specify
     * the location of the GUI code on disk
     * Ex: /ammos/ampcs/services/mcgui/
     */
    private static final String RESOURCE_LOCATION = PROPERTY_PREFIX + "resource.location";

    private static final String BROWSER = PROPERTY_PREFIX + "browser";

    /**
     * package protected UNIT TEST Constructor for the Web Gui configuration object
     */
    WebGuiProperties() {
        this(new SseContextFlag());
    }


    /**
     * Constructor for the Web Gui configuration object
     * @param sseFlag The SSE Context flag
     */
    public WebGuiProperties(SseContextFlag sseFlag) {

        super(PROPERTY_FILE, true, sseFlag);
    }


    private String getUrl(WebService service) {
        return getProperty(String.format(WEB_GUI_SERVICES_URL, service.toString()),
                           String.format(DEFAULT_URL, service.getDefaultPort(), service.toString()));
    }

    /**
     * Gets the configured Web Gui URL
     * @return the web gui url
     */
    public String getWebGuiUrl() {
        return getProperty(WEB_GUI_URL, String.format(DEFAULT_URL, DEFAULT_WEBGUI_PORT, "ampcs"));
    }

    /**
     * Gets the configured Web service URL
     * @return the web service url
     */
    public String getFswIngestUrl() {
        return getUrl(WebService.FSW_INGEST);
    }

    /**
     * Gets the configured Web service URL
     * @return the web service url
     */
    public String getSseIngestUrl() {
        return getUrl(WebService.SSE_INGEST);
    }

    /**
     * Gets the configured Web service URL
     * @return the web service url
     */
    public String getFswProcessUrl() {
        return getUrl(WebService.FSW_PROCESS);
    }

    /**
     * Gets the configured Web service URL
     * @return the web service url
     */
    public String getSseProcessUrl() {
        return getUrl(WebService.SSE_PROCESS);
    }

    /**
     *
     * Gets the configured Web service URL
     * @return the web service url
     * @deprecated
     *      Marking deprecated until a plan arises for webgui service interactions outside TI/TP
     *      leaving code in place to use for future use
     */
    @Deprecated
    public String getCfdpUrl() {
        return getUrl(WebService.CFDP);
    }

    /**
     *
     * Gets the configured Web service URL
     * @return the web service url
     * @deprecated
     *      Marking deprecated until a plan arises for webgui service interactions outside TI/TP
     *      leaving code in place to use for future use
     */
    @Deprecated
    public String getAutoUrl() {
        return getUrl(WebService.AUTO);
    }

    /**
     * Gets the resource handler for serving MC GUI code
     * <p>
     * Ex: if the TI server is hosting the GUI code and we want
     * the web GUI loaded when the user accesses http://localhost:8081/ampcs
     * then the resource handler should be configured as "/ampcs"
     *
     * @return the resource handler
     */
    public String getResourceHandler() {
        return getProperty(RESOURCE_HANDLER);
    }

    /**
     * Gets the resource location. This is the full path on disk
     * where the MC GUI code is located.
     * <p>
     * Ex: /ammos/ampcs/services/mcgui/
     *
     * @return the resource location
     */
    public String getResourceLocation() {
        return getProperty(RESOURCE_LOCATION);
    }


    /**
     * Gets the resource redirect route list in String form. The route paths defined
     * in this list will be redirected to index.html during browser refresh/reload events.
     *
     * @return the resource redirect routes string
     */
    public String getResourceRedirectRoutes() { return getProperty(RESOURCE_REDIRECT_ROUTES); }

    /**
     * Get web browser property
     * @return teh web browser
     */
    public String getBrowser(){
        return getProperty(BROWSER);
    }

    /**
     * An enumeration of configurable web services
     */
    public enum WebService {
        /** FSW Ingest */
        FSW_INGEST("fswIngest", 8081),
        /** SSE Ingest */
        SSE_INGEST("sseIngest", 8083),

        /** FSW Process*/
        FSW_PROCESS("fswProcess", 8082),
        /** SSE Process */
        SSE_PROCESS("sseProcess", 8084),

        /** CFDP */
        CFDP("cfdp", 8080),

        /** AUTO */
        AUTO("auto", 8384),

        /** Web GUI */
        GUI("ampcs", 4200);

        private String serviceName;
        private int port;
        WebService(final String name, final int port) {
            this.serviceName = name;
            this.port = port;
        }

        @Override
        public String toString() {
            return serviceName;
        }

        /**
         * Gets the default webservice port
         *
         * @return the default webservice port
         */
        public int getDefaultPort() {
            return port;
        }
    }


    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

}