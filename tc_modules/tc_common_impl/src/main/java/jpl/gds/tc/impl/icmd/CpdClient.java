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
package jpl.gds.tc.impl.icmd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jpl.gds.tc.api.exception.ScmfWrapUnwrapException;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.MultiPartMediaTypes;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import gov.nasa.jpl.icmd.schema.AggregationMethod;
import gov.nasa.jpl.icmd.schema.BitRateAndModIndexType;
import gov.nasa.jpl.icmd.schema.BitRateRangeType;
import gov.nasa.jpl.icmd.schema.CMDMessage;
import gov.nasa.jpl.icmd.schema.CMDMessageBody;
import gov.nasa.jpl.icmd.schema.CMDMessageTypes;
import gov.nasa.jpl.icmd.schema.DSMSHeaderType;
import gov.nasa.jpl.icmd.schema.ExecutionMethod;
import gov.nasa.jpl.icmd.schema.ExecutionMode;
import gov.nasa.jpl.icmd.schema.ExecutionStateRequest;
import gov.nasa.jpl.icmd.schema.InsertResponseType;
import gov.nasa.jpl.icmd.schema.ListPreparationStateEnum;
import gov.nasa.jpl.icmd.schema.RequestResultStatusType;
import gov.nasa.jpl.icmd.schema.SenderSourceType;
import gov.nasa.jpl.icmd.schema.StatusMessageListType;
import gov.nasa.jpl.icmd.schema.UplinkFileInfo;
import gov.nasa.jpl.icmd.schema.UplinkRequest;
import gov.nasa.jpl.icmd.schema.UplinkRequestInfo;
import gov.nasa.jpl.icmd.schema.UplinkRequestList;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.security.AccessControlParameters;
import jpl.gds.common.config.types.CommandUserRole;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.security.cam.AccessControl;
import jpl.gds.security.cam.AccessControlException;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.UplinkLogger;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.icmd.CpdDirective;
import jpl.gds.tc.api.icmd.CpdDirectiveArgument;
import jpl.gds.tc.api.icmd.CpdResponse;
import jpl.gds.tc.api.icmd.CpdServiceUriUtil;
import jpl.gds.tc.api.icmd.CpdTriggerEvent;
import jpl.gds.tc.api.icmd.ICpdClient;
import jpl.gds.tc.api.icmd.config.IntegratedCommandProperties;
import jpl.gds.tc.api.icmd.datastructures.CpdConfiguration;
import jpl.gds.tc.api.icmd.datastructures.CpdConnectionStatus;
import jpl.gds.tc.api.icmd.datastructures.CpdDmsBroadcastStatusMessages;
import jpl.gds.tc.api.icmd.datastructures.CpdIncrementalRequestStatus;
import jpl.gds.tc.api.icmd.datastructures.CpdIncrementalRequestStatus.StatusType;
import jpl.gds.tc.api.icmd.exception.AuthenticationException;
import jpl.gds.tc.api.icmd.exception.AuthorizationException;
import jpl.gds.tc.api.icmd.exception.CpdConnectionException;
import jpl.gds.tc.api.icmd.exception.ICmdException;

/**
 * This class is a client class used to interface with CPD server
 *
 * We no longer validate the cookie before use. We assume it is good and, on
 * failure, we get a new token and try again.
 *
 * If the token is invalidated (not the same as timing out) we get an OK status
 * with the wrong result type (HTML vs. XML). Refactored to treat that like a
 * timeout and re-authenticate.
 *
 * Look for uses of issueAndParse for invalidation changes.
 *
 * @since AMPCS R3
 * MPCS-5431 05/27/14 Refactored, methods made static
 * MPCS-5431 06/02/15 Refactored to support invalidation
 * MPCS-7723  - 10/24/16 - Updated Jersey 1 to Jersey 2. Lots of
 *          minor changes to Jersey class, method names, and REST call
 *          construction/execution. ClientHandlerException and UniformInterfaceException
 *          no longer exist in Jersey 2, changed all instances to ProcessingException.
 * MPCS-10149 - 01/03/19 - Cleaned up code, updated error reporting
 */
public final class CpdClient implements ICpdClient
{
    private enum CommandEnum
    {
        GET,
        PUT,
        DELETE
    }

    /*
     * MPCS-5734  2/6/14: Log discarded CPD responses due to internal
     * buffer overflow
     */
    private static final String JAVA_TEMP_DIR_PROPERTY = "java.io.tmpdir";
    private static final String DEFAULT_TEMP_DIR = "/tmp";
    private static final String CPD_DISCARDED_RESPONSE_LOG_FILE_NAME =
            "ampcs_discarded_cpd_responses.log";
    private static final String CMD_JAXB_SCHEMA_LOCATION = 
    		"http://dsms.jpl.nasa.gov/cmd/schema /local/cmd/acmd/tables/schema/cmd/CMDMessage.xsd";

    /** The source of the SCMF */
    private static final String SOURCE_NAME = "AMPCS";

    /** Logger */
    private UplinkLogger                      logger;

    /** Access control for DISA SS */
    private static AccessControl accessControl = null;

    /*
     * MPCS-5934 - 3/27/2015: Introduced a second Jersey client
     * because we want the first client to be always available. The second
     * client will be solely dedicated to long-polls for DMS broadcast status
     * messages.
     */
    /** Jersey Client for regular REST calls */
    private final Client client;

    /**
     * Jersey Client for polling DMS broadcast status messages (which are long
     * polls)
     */
    private final Client dmsBroadcastStatusMessagesPollingClient;

    /** The encoding used to generate XML */
    private final String xmlEncoding;

    /** The JAXB marshaller */
    private final Marshaller marshaller;

    /** The JAXB unmarshaller */
    private final Unmarshaller unmarshaller;

    /*
     * MPCS-5934 - 4/20/2015: Because the usual JAXB unmarshalling
     * doesn't currently work on STATUS_MESSAGE_LIST, DocumentBuilder is needed
     * to manually parse them.
     */
    /**
     * DocumentBuilder for manually unmarshalling STATUS_MESSAGE_LIST CPD
     * responses
     */
    private final DocumentBuilder db;

    /** The CpdConfiguration */
    private final IntegratedCommandProperties config;

    /**
     * An internal, monotonically increasing counter that is used to
     * differentiate outgoing messages to CPD
     */
    private int messageCount;

    /*
     * MPCS-5934 - 3/27/2015: New variables to keep track of needed
     * parameters for the next DMS broadcast status messages poll request.
     *
     * "Last received time" (see 0231-Telecomm-CMDSCH document under
     * "Poll for DMS Broadcast Status Messages" section) in
     * YYYY-DDDTHH:MM:SS.FFF format
     */
    private String lastReceivedTime;

    /**
     * "Last message counter" (see 0231-Telecomm-CMDSCH document under
     * "Poll for DMS Broadcast Status Messages" section) as long integer
     */
    private long lastReceivedCounter;
	private final ApplicationContext appContext;

    /** Set to nonzero to test authentication recovery */
    private static int forceError = 0;

    /** Set to nonzero to test invalidation recovery MPCS-5431 07/02/15 */
    private static int forceInvalidate = 0;
    
    private final CpdServiceUriUtil cpdUriUtil;

    //list of supported message types
    private final String[] supportedTypes = {"CMDMessage"};

    /**
     * Constructor
     *
     * @param appContext the ApplicationContext in which this object is being used
     *
     * @throws JAXBException if an error is encountered while trying to
     *             configure JAXB
     * @throws ParserConfigurationException
     */
    public CpdClient(final ApplicationContext appContext) throws JAXBException, ParserConfigurationException {
    	this.appContext = appContext;
    	
    	this.cpdUriUtil = new CpdServiceUriUtil(appContext);
    	
        this.logger =
                new UplinkLogger(appContext, TraceManager.getTracer(appContext, Loggers.CPD_UPLINK), true);
        this.config = appContext.getBean(IntegratedCommandProperties.class);
        final JAXBContext jaxbCtx =
                JAXBContext.newInstance(this.config.getJaxbPackageName());

        final ClientConfig cc = new ClientConfig();

        this.client = ClientBuilder.newClient(cc).register(MultiPartFeature.class);
        this.client.property(ClientProperties.CONNECT_TIMEOUT, this.config.getRestConnectTimeout());
        this.client.property(ClientProperties.READ_TIMEOUT, this.config.getRegularRestCallTimeout());
        /*
         * MPCS-5934 - 3/27/2015: Initializing the new, second
         * Jersey client, for long-polling of DMS broadcast status messages.
         */
        this.dmsBroadcastStatusMessagesPollingClient = ClientBuilder.newClient(cc);
        this.dmsBroadcastStatusMessagesPollingClient
            .property(ClientProperties.CONNECT_TIMEOUT, this.config.getRestConnectTimeout());
        this.dmsBroadcastStatusMessagesPollingClient
            .property(ClientProperties.READ_TIMEOUT, this.config.getLongRestPollTimeout());

        this.xmlEncoding = this.config.getXmlEncoding();
        this.marshaller = jaxbCtx.createMarshaller();
        this.marshaller.setProperty(Marshaller.JAXB_ENCODING, this.xmlEncoding);
        this.marshaller
            .setProperty(Marshaller.JAXB_SCHEMA_LOCATION, CMD_JAXB_SCHEMA_LOCATION);
        this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        this.marshaller.setProperty(this.config.getJaxbPrefixMapperProperty(),
        		this.config.getPrefixMapper());

        this.unmarshaller = jaxbCtx.createUnmarshaller();
        this.messageCount = 0;
        /*
         * MPCS-5934 - 3/27/2015: 0231-Telecomm-CMDSCH document
         * under "Poll for DMS Broadcast Status Messages" section says these are
         * the recommended "first poll" values.
         *
         * MPCS-5934 - 4/20/2015: To manually unmarshall
         * STATUS_MESSAGE_LIST, set up DocumentBuilder.
         */
        this.lastReceivedTime = "2000-001T00:00:00.000";
        this.lastReceivedCounter = 0;
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        this.db = dbf.newDocumentBuilder();

        // Get security stuff
        getAcInstance();
    }

    /*
     * MPCS-5934 - 3/27/2015: parseClientResponse will now return
     * Object instead of CMDMessage because CPD long-poll responses are not of
     * CMDMessage types. Instead of creating a separate method that mostly does
     * the same thing as this method, only that it returns a non-CMDMessage
     * response type will only lead to code redundancy or deeper levels of
     * method calls. So we have the callers of this method cast it to the
     * expected message type instead.
     *
     * If the token has been invalidated we get an OK status with an unexpected
     * HTML return type. That is treated as an authentication error so that we can
     * reauthorize and continue; but that is just a workaround since we should be
     * getting a specific error.
     *
     * @param response Client response to be parsed
     *
     * @return Parsed object
     *
     * @throws ICmdException On general error
     *
     * MPCS-5431 06/30/15 Refactor to catch invalidated token
     */
    private synchronized Object parseClientResponse(final Response response) throws ICmdException
    {
        InputStream is = null;

        if (response != null) {
        	
            final MediaType responseType = response.getMediaType();

            /*
             * MPCS-5934 - 4/20/2015: CPD long poll response returns
             * "application/xml" type.
             */
            if ((responseType != null) &&
                (responseType.isCompatible(MediaType.TEXT_XML_TYPE) ||
                 responseType.isCompatible(MediaType.APPLICATION_XML_TYPE))) {
            	
                is = response.readEntity(InputStream.class);
            }
            else {
            	
                logger.trace(response.readEntity(String.class));

                final String errorMessage = "Received an unexpected response type (" + responseType +
                                            "). This is most likely due to a failure to " +
                                            "authenticate the user.";
                logger.warn(errorMessage);

                final String user = appContext.getBean(AccessControlParameters.class).getUserId();
                final CommandUserRole role = appContext.getBean(AccessControlParameters.class).getUserRole();
                
                final Status s = Status.fromStatusCode(response.getStatus());

				/*
				 * MPCS-7723 10/20/16 - Reorganized the switch statement.
				 * All used Status codes are in numerical order. All unused
				 * status codes are in numeric order after the used ones.
				 */
                switch (s) {
				case OK:
					if (responseType.isCompatible(MediaType.TEXT_HTML_TYPE)) {
                        // Assume authentication error
                        throw new AuthenticationException("Error authenticating user: '" +
                                                           user + "'");
                    }
                    break;
				case FORBIDDEN:
					throw new AuthorizationException("User '" + user +
                                                     "' not authorized to perform action as '" +
							                         role + "'");
				case UNAUTHORIZED:
					throw new AuthenticationException("Error authenticating user: '" + user + "'");
				case ACCEPTED:
				case BAD_GATEWAY:
				case BAD_REQUEST:
				case CONFLICT:
				case CREATED:
				case EXPECTATION_FAILED:
				case FOUND:
				case GATEWAY_TIMEOUT:
				case GONE:
				case HTTP_VERSION_NOT_SUPPORTED:
				case INTERNAL_SERVER_ERROR:
				case LENGTH_REQUIRED:
				case METHOD_NOT_ALLOWED:
				case MOVED_PERMANENTLY:
				case NOT_ACCEPTABLE:
				case NOT_FOUND:
				case NOT_IMPLEMENTED:
				case NOT_MODIFIED:
				case NO_CONTENT:
				case PARTIAL_CONTENT:
				case PAYMENT_REQUIRED:
				case PRECONDITION_FAILED:
				case PROXY_AUTHENTICATION_REQUIRED:
				case REQUESTED_RANGE_NOT_SATISFIABLE:
				case REQUEST_ENTITY_TOO_LARGE:
				case REQUEST_TIMEOUT:
				case REQUEST_URI_TOO_LONG:
				case RESET_CONTENT:
				case SEE_OTHER:
				case SERVICE_UNAVAILABLE:
				case TEMPORARY_REDIRECT:
				default:
					break;
                }
                throw new ICmdException(errorMessage);
            }
        }
        
        if(is == null) {
        	throw new ICmdException("null Response from CPD");
        }

        Object unmarshalledResponse = null;
        try {
        	
        	if(this.appContext.getBean(IntegratedCommandProperties.class).bufferCpdResponse()) {

                final String rawXml = isToString(is);
                this.logger.debug(rawXml);

                /*
                 * MPCS-5934 - 4/20/2015: If this is a CPD long
                 * polling response (STATUS_MESSAGE_LIST is the root
                 * element), parse it manually. This is a temporary measure
                 * until CPD produces a schema set with multiple root
                 * elements, and when CPD server responds include full
                 * contextual information (e.g. namespace).
                 */
                if (rawXml.indexOf("<STATUS_MESSAGE_LIST") < 0) {
                    synchronized (this.unmarshaller) {
                        unmarshalledResponse = this.unmarshaller
                                .unmarshal(new StringReader(rawXml));
                    }
                } else {
                    unmarshalledResponse = manuallyUnmarshalStatusMessageList(rawXml);
                }

            } else {

                /*
                 * MPCS-5934- 4/20/2015: Since we don't expect
                 * to run this CpdClient in non-buffered mode, we don't
                 * handle STATUS_MESSAGE_LIST here. The STATUS_MESSAGE_LIST
                 * handling in buffered mode is a temporary measure anyway.
                 */
                synchronized (this.unmarshaller) {
                    unmarshalledResponse = this.unmarshaller.unmarshal(is);
                }
            }
        } catch (final JAXBException | SAXException | IOException e) {
            try {
                logger.warn("An error occured while parsing CPD response", e);

                // try to log the response
                if (response != null) {
                    response.readEntity(InputStream.class).reset();
                    this.logger.debug(response.readEntity(String.class));
                }
            } catch (final IOException e1) {
                this.logger.warn("Unable to log the response. " + e1);
            }

            throw new ICmdException("Invalid Response", e);
        }

       return unmarshalledResponse;
    }


    private String isToString(final InputStream is) throws ICmdException {
    	
    	final int bufferSize = this.appContext.getBean(IntegratedCommandProperties.class).getCpdResponseBufferSize();
        // MPCS-5734 1/29/14: Buffer CPD's response
        final StringBuilder sb = new StringBuilder();
        
        String inputLine;
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
            while ((inputLine = in.readLine()) != null) {

                if ((sb.length() + inputLine.length()) > bufferSize) {
                	logOverflowToFile(sb, inputLine, in);
                }
                sb.append(inputLine);
            }
        }
        catch (final IOException e) {
            logger.warn("IOException occurred while parsing CPD response.", e);
        }
        return sb.toString();
	}

    private void logOverflowToFile(final StringBuilder sb, String inputLine, final BufferedReader in) throws ICmdException{

        // log the CPD response the overflowed our
        // buffer
        final File responseFile = new File(System.getProperty(JAVA_TEMP_DIR_PROPERTY, DEFAULT_TEMP_DIR) +
        		                           File.separator + CPD_DISCARDED_RESPONSE_LOG_FILE_NAME);

        try(final FileWriter fw = new FileWriter(responseFile.getAbsoluteFile(), true);
            final BufferedWriter bw = new BufferedWriter(fw)) {

            // write timestamp
            final String timestamp = new AccurateDateTime().getFormattedErt(true);
            bw.write("===== BEGIN RESPONSE [" + timestamp + "] =====\n\n");

            // write contents of StringBuilder
            bw.write(sb.toString());
            // write current line
            bw.write(inputLine);
            // write remaining items

            while ((inputLine = in.readLine()) != null)
            {
                bw.write(inputLine);
            }

            bw.write("===== END RESPONSE [" + timestamp
                    + "] =====\n\n");

        }
        catch(final IOException e) {
            logger.warn("IOException occurred while writing CPD response buffer overflow to" +
            		    " logfile. Data may not be complete", e);
        }

        // throw exception
        throw new ICmdException("CPD response exceeded configured buffer size of " +
                                appContext.getBean(IntegratedCommandProperties.class)
                                                           .getCpdResponseBufferSize() +
                                " characters. Increase configured buffer size or disable " +
                                "buffering. Response discarded and logged to: " + 
                                responseFile.getAbsolutePath());    
    }

	/*
     * MPCS-5934 - 4/20/2015: Before CPD long polling, the messages
     * from CPD had only one common root element: CMDMessage. Now with CPD long
     * polling, the responses can either have CMDMessage or STATUS_MESSAGE_LIST
     * as root elements. This presents a problem because the XML schema provided
     * by CPD defines only one root element, which is CMDMessage. This may be
     * due to the limitation in the XML 1.0 standard. Because of this
     * limitation, however, we're not able to use the easy JAXB "unmarshal" call
     * to obtain the STATUS_MESSAGE_LIST content. We have to resort to
     * populating the content manually, for now.
     */
    private Object manuallyUnmarshalStatusMessageList(final String text)
            throws SAXException, IOException, ICmdException {
        this.logger
            .debug("Manually unmarshalling STATUS_MESSAGE_LIST. NOTE: If the schema changes, the"
            		+ " code needs to change!");

        final Document doc = db.parse(new ByteArrayInputStream(
        		                              text.getBytes(StandardCharsets.UTF_8)));
        final Element root = doc.getDocumentElement();

        if (!"STATUS_MESSAGE_LIST".equals(root.getLocalName())) {
            throw new ICmdException("manuallyUnmarshalStatusMessageList tried to process root "
            		+ "element that is not STATUS_MESSAGE_LIST");
        }

        final StatusMessageListType smlt = new StatusMessageListType();

        // STATUS_MESSAGE_LIST -> STATUS
        final String errMsg = "manuallyUnmarshalStatusMessageList encountered unexpected element: ";
        Node currentElement = root.getChildNodes().item(0);
        if (!"STATUS".equals(currentElement.getLocalName())) {
            throw new ICmdException(errMsg + currentElement.getLocalName());
        }
        smlt.setSTATUS(RequestResultStatusType.fromValue(currentElement.getTextContent()));

        // STATUS_MESSAGE_LIST -> DIAGNOSTIC
        currentElement = root.getChildNodes().item(1);
        if (!"DIAGNOSTIC".equals(currentElement.getLocalName())) {
            throw new ICmdException(errMsg + currentElement.getLocalName());
        }
        smlt.setDIAGNOSTIC(currentElement.getTextContent());

        // STATUS_MESSAGE_LIST -> LAST_MESSAGE_COUNTER
        currentElement = root.getChildNodes().item(2);
        if (!"LAST_MESSAGE_COUNTER".equals(currentElement.getLocalName())) {
            throw new ICmdException(errMsg + currentElement.getLocalName());
        }
        smlt.setLASTMESSAGECOUNTER(Long.valueOf(currentElement.getTextContent()));

        // STATUS_MESSAGE_LIST -> LAST_MESSAGE_TIME
        currentElement = root.getChildNodes().item(3);
        if (!"LAST_MESSAGE_TIME".equals(currentElement.getLocalName())) {
            throw new ICmdException(errMsg + currentElement.getLocalName());
        }
        smlt.setLASTMESSAGETIME(currentElement.getTextContent());

        // STATUS_MESSAGE_LIST -> MESSAGE (zero to many)
        for (int i = 4; i < root.getChildNodes().getLength(); i++) {
            smlt.getMESSAGE().add(root.getChildNodes().item(i).getTextContent());
        }

        return smlt;
    }


    private DSMSHeaderType getDsmsHeader() {
        final DSMSHeaderType header = new DSMSHeaderType();
        header.setDSMSCREATIONTIME(TimeUtility.formatDOY(new AccurateDateTime()));
        header.setDSMSMESSAGETYPE(this.config.getRootMessageType());
        header.setDSMSSCHEMANAME(this.config.getRootMessageSchema());
        header.setDSMSSCHEMAVERSION(this.config.getRootMessageSchemaVersion());
        header.setDSMSSOURCENAME(SOURCE_NAME);
        header.setDSMSSPACECRAFTNUMBER(appContext.getBean(IContextIdentification.class).getSpacecraftId());
        header.getDSMSMISSIONID().add(String.valueOf(appContext.getBean(MissionProperties.class)
        		                                               .getMissionId()));

        return header;
    }


    /**
     * Extract exception cause if it is due to authentication.
     *
     * @param pe Exception received
     *
     * @return AuthenticationException or null
     *
     * MPCS-5431 05/27/14 Extracted from checkExceptionCause
     */
    private static AuthenticationException getAuthenticationException(final ProcessingException pe) {
    	
        final Throwable         cause  = pe.getCause();
        AuthenticationException result = null;

        if (cause instanceof SSLHandshakeException) {
            result = new AuthenticationException(cause.getMessage(), cause);
        }

        return result;
    }


    /**
     * Extract exception cause and decide what to really throw.
     *
     * @param che Exception received
     *
     * @throws CpdConnectionException  On connection failure
     * @throws AuthenticationException On authentication failure
     * @throws ProcessingException     On any other failure
     *

     */
    private static void checkExceptionCause(final ProcessingException che)
            throws CpdConnectionException, AuthenticationException
    {
        final Throwable cause = che.getCause();

        if (cause instanceof ConnectException) {
            throw new CpdConnectionException("Unable to connect to CPD server", cause);
        }

        if (cause instanceof SSLHandshakeException) {
            throw new AuthenticationException(cause.getMessage(), cause);
        }

        throw che;
    }


    /**
     * Issue get, put or delete and check the response. If the error is with
     * authentication, try just one more time. That takes care of the
     * situation where we validate the token just before it expires.
     *
     * @param r      Web resource
     * @param ce     Command type to be issued
     * @param retry  True if retry is needed
     * @param logger Tracer
     *
     * @return Response object
     *
     * @throws ICmdException       On error
     * @throws ProcessingException On error
     *
     */
    private static Response issue(final ApplicationContext appContext, final WebTarget  r,
                                  final CommandEnum  ce, final boolean retry,
                                  final UplinkLogger logger)
                   throws ICmdException {
    
        Response response   = null;
        boolean        keepTrying = true;
        boolean        retried    = retry;

        while (keepTrying) {
        	
            keepTrying = false;

            try {
                // ICmdException thrown if cannot set cookie

                if (forceError > 0) {
                	
                    --forceError;

                    if (forceError == 0) {
                        throw new ProcessingException("Forced error",
                        		                      new SSLHandshakeException("Forced error"));
                    }
                }

                response = issueOnce(appContext, r, ce, retried);
            } catch (final ProcessingException pe) {
                // See if the cause was authentication

                final AuthenticationException ae = getAuthenticationException(pe);

                if (ae == null) {
                    // Not authentication; decide what to throw and throw it
                    checkExceptionCause(pe);
                    break;
                }

                // Try one more time, and only one more time
                if (retried) {
                    throw ae;
                }

                logger.warn("Authentication failure, trying again");

                retried    = true;
                keepTrying = true;
            }
        }


        if ((forceInvalidate > 0) && (accessControl != null))
        {
            --forceInvalidate;

            if (forceInvalidate == 0) {
            	
                // Kill token for next time
                try {
                    accessControl.invalidateSsoToken();
                } catch (final AccessControlException ace) {
                    logger.error("Exception encountered while attempting to invalidate SSO token: " 
                                 + ExceptionTools.getMessage(ace), ace);
                }
            }
        }
        return response;
    }


    /**
     * Issue get, put or delete and check the response.
     *
     * @param r          Web resource
     * @param ce         Command type to be issued
     * @param revalidate If true, revalidate token
     *
     * @return Response object
     *
     * @throws ICmdException       On error
     * @throws ProcessingException On error
     *
     */
    private static Response issueOnce(final ApplicationContext appContext, final WebTarget r,
                                            final CommandEnum ce, final boolean revalidate)
                   throws ICmdException {
    	
        Response response = null;

        // ICmdException thrown if cannot set cookie
        final Builder ib = setCookie(appContext, r, revalidate);

        switch (ce) {
            case GET:
                response = ib.get(Response.class);
                break;

            /*
             * PUT and POST require an entity. PUT REST calls are going directly to
             * the CPD server and responses are in text/xml.
             */
            case PUT:
                response = ib.put(Entity.entity(MediaType.TEXT_XML, MediaType.TEXT_XML_TYPE));
                break;

            case DELETE:
                response = ib.delete(Response.class);
                break;

            default:
                throw new ICmdException("Unknown command type " + ce);
        }

        return response;
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public boolean pingCommandService() throws ICmdException {
        final String cpdServerUrl = cpdUriUtil.getCpdServerUrl();
        this.logger.debug("Pinging " + cpdServerUrl + "...");

        final String serviceUri = cpdUriUtil.getConnectionStateServiceUri();

        final WebTarget r = this.client.target(serviceUri);

        final Response response = issue(appContext, r, CommandEnum.GET, false, logger);

        // HTTP codes >= 400 represent errors
        return response.getStatus() < 400;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public InsertResponseType sendScmf(final IScmf scmf) throws ICmdException,
                                                                ScmfWrapUnwrapException {
    	
        if (scmf == null) {
            throw new ICmdException("null SCMF");
        }

        /* File Info */
        final UplinkFileInfo fileInfo = new UplinkFileInfo();
        fileInfo.setFILENAME(scmf.getSfduHeader().getFileName());

        // checksum needs to be hex, 4 characters in length
        String checksum = Integer.toHexString(scmf.getFileChecksum());
        checksum = String.format("%1$4s", checksum).replace(" ", "0");

        fileInfo.setCHECKSUM(checksum);
        fileInfo.setFILETYPE("SCMF");
        fileInfo.setCREATIONTIME(scmf.getSfduHeader().getProductCreationTime()
                .trim());

        final BitRateRangeType bitRates = new BitRateRangeType();

        final List<Double> selectedBitRates =
        		appContext.getBean(CommandProperties.class)
                        .getSelectedBitRates();

        for (final double br : selectedBitRates) {
            bitRates.getBITRATE().add((float) br);
        }

        
        final long sessionId = appContext.getBean(IContextIdentification.class).getNumber();
        final int hostId = appContext.getBean(IContextIdentification.class).getHostId();
        final String jmsTopicName =  appContext.getBean(IGeneralContextInformation.class)
        		                               .getRootPublicationTopic();
        final int scid = appContext.getBean(IContextIdentification.class).getSpacecraftId();

        final UplinkMetadata metadata = new UplinkMetadata(sessionId, hostId, jmsTopicName, scid);

        final SenderSourceType senderSource = new SenderSourceType();
        senderSource.setSENDERID(metadata.toMetadataString());

        final UplinkRequest uplinkReq = new UplinkRequest();
        uplinkReq.setFILEINFO(fileInfo);
        uplinkReq.setXLATEDICTIONARYVERSION(appContext.getBean(DictionaryProperties.class).getFswVersion());
        uplinkReq.setBITRATERANGE(bitRates);

        final String userId = appContext.getBean(AccessControlParameters.class).getUserId();
        uplinkReq.setUSERID(userId);

        final CommandUserRole userRole = appContext.getBean(AccessControlParameters.class).getUserRole();

        if (userRole == null) {
            throw new ICmdException("Invalid user role");
        }

        uplinkReq.setROLEID(userRole.toString());
        uplinkReq.setSENDERSOURCE(senderSource);

        final UplinkRequestList uplinkReqList = new UplinkRequestList();
        uplinkReqList.getREQUESTINFO().add(uplinkReq);

        final UplinkRequestInfo uplinkReqInfo = new UplinkRequestInfo();
        uplinkReqInfo.setREQUESTINFO(uplinkReq);

        final CMDMessageBody body = new CMDMessageBody();
        body.setINSERTSCMF(uplinkReqInfo);
        body.setMESSAGEID("AMPCS-Session " + appContext.getBean(IContextIdentification.class)
                                                       .getNumber() + "-" + this.messageCount++);
        final CMDMessageTypes msgType = new CMDMessageTypes();
        msgType.setCMDMESSAGE(body);

        final CMDMessage reqCmdMsg = new CMDMessage();
        reqCmdMsg.setDSMSHeader(getDsmsHeader());
        reqCmdMsg.setDSMSBody(msgType);

        String uplinkReqXml = null;
        try(final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            synchronized (this.marshaller) {
                this.marshaller.marshal(reqCmdMsg, baos);
            }
            uplinkReqXml = baos.toString(this.xmlEncoding);
        } catch (final Exception e) {
            throw new ICmdException("Error converting Java objects to XML", e);
        }

        this.logger.debug("Request Message: \n" + uplinkReqXml);
        final byte[] scmfByteContents = scmf.getBytes();
        
        InsertResponseType retVal = null;
        try(final MultiPart multiPartContent = new MultiPart()) {
        
			multiPartContent.bodyPart(new BodyPart(MediaType.APPLICATION_XML_TYPE).entity(uplinkReqXml))
					        .bodyPart(new BodyPart(MediaType.APPLICATION_OCTET_STREAM_TYPE)
					        		      .entity(scmfByteContents));

			// a POST method on the Radiation Request service is an SCMF insertion
			// request
			final String serviceUri = cpdUriUtil.getInsertScmfServiceUri(userRole);

			final WebTarget r = this.client.target(serviceUri);


			this.logger.debug("Sending a radiation request to CPD: " + scmf.getFileName());

			final CMDMessage respCmdMsg = issueAndParse(new GetResponseFunctor(r, multiPartContent, logger));
			if(null != respCmdMsg) {
				retVal = respCmdMsg.getDSMSBody().getCMDMESSAGE().getINSERTRESPONSE();
			}
			
			debugLogCmdMessage(respCmdMsg);
			
        } catch(final IOException e) {
        	logger.debug("Problem closing multiPartContent", e);
        }

        return retVal;
    }


    /**
     * Get client response from web target.
     *
     * @param wt         Web target
     * @param mp         Multi-part content
     * @param revalidate If true, revalidate token
     *
     * @return Response
     *
     * @throws ICmdException           On general error
     * @throws ProcessingException     On post failure
     *
     */
    private static Response getResponse(final ApplicationContext appContext, final WebTarget wt,
                                              final MultiPart mp, final boolean revalidate)
                  throws ICmdException, ProcessingException {
    	
        final Class<Response> clss = Response.class;
        Response cr   = null;

        if (accessControl != null) {
        	
            final Builder ib = setCookie(appContext, wt, revalidate);

            if (!(ib instanceof Builder)) {
            	
                throw new ICmdException("setCookie returned a " + ib.getClass().getName() +
                                        " instead of a " + Builder.class.getName());
            }

            final Builder b = SystemUtilities.castNoWarning(ib);

            cr = b.post(Entity.entity(mp, MultiPartMediaTypes.MULTIPART_MIXED), clss);
            
        } else {
            cr = wt.request().post(Entity.entity(mp, MultiPartMediaTypes.MULTIPART_MIXED), clss);
        }

        return cr;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CpdDmsBroadcastStatusMessages getDmsBroadcastStatusMessages() throws ICmdException {

        final String serviceUri = cpdUriUtil
        		                  .getDmsBroadcastStatusMessagesServiceUri(this.lastReceivedTime,
                                           this.lastReceivedCounter);

        this.logger.debug("Polling DMS broadcast status messages: " + serviceUri);

        final WebTarget wt = this.dmsBroadcastStatusMessagesPollingClient.target(serviceUri);

        // CPD escapes all the HTML code inside MESSAGE elements, so unescape
        // them before trying to parse.

        final StatusMessageListType statusMessageList = issueAndParse(new GetIssueFunctor(wt,
                                                            CommandEnum.GET,
                                                            "Error retrieving radiation list",
                                                            logger));

        final String newLastMessageTime = statusMessageList.getLASTMESSAGETIME();
        final long newLastMessageCounter = statusMessageList.getLASTMESSAGECOUNTER();

        logger.debug("DMS broadcast status message response LAST_MESSAGE_TIME=" + newLastMessageTime
                + ", LAST_MESSAGE_TIME=" + newLastMessageCounter + ", "
                + (statusMessageList.getMESSAGE() != null 
                        ? ("MESSAGE count of " + statusMessageList.getMESSAGE().size()) 
                		: "null MESSAGE"));
        this.lastReceivedTime = newLastMessageTime;
        this.lastReceivedCounter = newLastMessageCounter;

        // MPCS-10862 - Set null as initial state as this is checked later in CpdRadiationListModel and CpdRequestPoolModel
        List<ICpdUplinkStatus> radList = null;
        List<UplinkRequest> reqList = null;
        final List<CpdIncrementalRequestStatus> incReqStatusList = new LinkedList<>();
        CpdConfiguration cpdConfig = null;
        CpdConnectionStatus connState = null;
        BitRateAndModIndexType bitRateModIndex = null;
        
        if(null == statusMessageList.getMESSAGE()) {
        	return new CpdDmsBroadcastStatusMessages(radList, reqList, incReqStatusList, cpdConfig,
                           connState, bitRateModIndex);
        }

		logger.debug("Start parsing MESSAGE elements in DMS broadcast status messages object");

		// Unmarshaller is not thread-safe
        for (int count = 0;count < statusMessageList.getMESSAGE().size();count++) {

            final String msg = statusMessageList.getMESSAGE().get(count);

            // MPCS-11869 - Skip unsupported messages
            if(!isSupportedType(msg)){
                continue;
            }

            logger.trace("Parsing DMS broadcast status message # " + count);

            CMDMessage cmdMessage = null;

            try {
                synchronized (this.unmarshaller) {
                    cmdMessage = (CMDMessage) this.unmarshaller.unmarshal(new StringReader(msg));
                }
            } catch (final JAXBException e) {
                e.printStackTrace();
                this.logger.error(
                        "Message #: " + count
                        + " Error encountered while unmarshalling"
                        + " a STATUS_MESSAGE_LIST MESSAGE value: " + msg,
                        e);
                continue;
            }

            this.debugLogCmdMessage(cmdMessage);

            try {

                // Determine which message type we are looking at
                if (cmdMessage.getDSMSBody().getCMDMESSAGE().getRADIATIONLIST() != null) {
                    final List<UplinkRequest> req = cmdMessage.getDSMSBody().getCMDMESSAGE().getRADIATIONLIST().getREQUESTLIST().getREQUESTINFO();
                    radList = new LinkedList<>();

                    for (final UplinkRequest ur : req) {
                        radList.add(new CpdUplinkStatus(appContext.getBean(MissionProperties.class).getStationMapper(), ur));
                    }

                    // MPCS-10862 - Populate request list from radiation list
                    reqList = new LinkedList<>(req);
                    /*
                     * If we received a RADIATION_LIST, then all the previous UPLINK_REQUEST_*
                     * incremental updates should be reflected in this new list. So we can discard
                     * the previously parsed incremental messages and start the incremental list
                     * from scratch.
                     */
                    incReqStatusList.clear();

                    logger.trace("-- Found RADIATION_LIST");

                } else if (cmdMessage.getDSMSBody().getCMDMESSAGE().getUPLINKREQADDED() != null) {
                    incReqStatusList.add(new CpdIncrementalRequestStatus(StatusType.ADDED,
                            cmdMessage.getDSMSBody().getCMDMESSAGE().getUPLINKREQADDED()));
                    logger.trace("-- Found UPLINK_REQ_ADDED");

                } else if (cmdMessage.getDSMSBody().getCMDMESSAGE().getUPLINKREQUPDATED() != null) {
                    incReqStatusList.add(new CpdIncrementalRequestStatus(StatusType.UPDATED,
                            cmdMessage.getDSMSBody().getCMDMESSAGE().getUPLINKREQUPDATED()));
                    logger.trace("-- Found UPLINK_REQ_UPDATED");

                } else if (cmdMessage.getDSMSBody().getCMDMESSAGE().getUPLINKREQDELETED() != null) {
                    final CpdIncrementalRequestStatus newIncReq = new CpdIncrementalRequestStatus(
                            StatusType.DELETED, cmdMessage.getDSMSBody().getCMDMESSAGE().getUPLINKREQDELETED());
                    incReqStatusList.add(newIncReq);
                    /*
                     * MPCS-7355  - 6/1/2015: Because UPLINK_REQ_DELETED messages don't
                     * actually contain the time that the request was deleted, we have to use the
                     * next best thing, the DSMS_CREATION_TIME in the DSMS header to get as close to
                     * it.
                     */
                    final String dsmsTime = cmdMessage.getDSMSHeader().getDSMSCREATIONTIME();
                    newIncReq.setTimestamp(new AccurateDateTime(dsmsTime));

                    logger.trace("-- Found UPLINK_REQ_DELETED");

                } else if (cmdMessage.getDSMSBody().getCMDMESSAGE().getLISTCONFIGURATION() != null) {
                    cpdConfig = new CpdConfiguration(
                            cmdMessage.getDSMSBody().getCMDMESSAGE().getLISTCONFIGURATION());
                    logger.trace("-- Found LIST_CONFIGURATION: " + cpdConfig);

                } else if (cmdMessage.getDSMSBody().getCMDMESSAGE().getLISTCONFIGURATIONINFO() != null) {
                    cpdConfig = new CpdConfiguration(
                            cmdMessage.getDSMSBody().getCMDMESSAGE().getLISTCONFIGURATIONINFO());
                    logger.trace("-- Found LIST_CONFIGURATION_INFO: " + cpdConfig);

                } else if (cmdMessage.getDSMSBody().getCMDMESSAGE().getSTATUS() != null
                        && cmdMessage.getDSMSBody().getCMDMESSAGE().getSTATUS()
                                .getSTATUSTYPEANDSTATUSVALUE() != null
                        && cmdMessage.getDSMSBody().getCMDMESSAGE()
                                                   .getSTATUS()
                                                   .getSTATUSTYPEANDSTATUSVALUE()
                                                   .getCONNECTIONSTATUS() != null) {
                    /*
                     * MPCS-7327  - 5/12/2015: Re: above if-clause, discovered that
                     * sometimes the STATUS_TYPE_AND_STATUS_VALUE messages contain
                     * PRODUCTION_STATUS, not CONNECTION_STATUS. Best thing to do is avoid
                     * PRODUCTION_STATUS.
                     */

                    /*
                     * MPCS-5934 - 4/28/2015: Confirmed that connection status is
                     * encapsulated in a different format than the previous busy-poll's response
                     * format for the same value.
                     *
                     * Previous: <CMD_MESSAGE> <MESSAGE_ID>00603267</MESSAGE_ID>
                     * <BC_DSS_ID>unknown-dssId</BC_DSS_ID> <CONNECTION_STATUS_RESPONSE> <RESPONSE>
                     * <STATUS>OK</STATUS> </RESPONSE>
                     * <CONNECTION_STATUS>PENDING</CONNECTION_STATUS> </CONNECTION_STATUS_RESPONSE>
                     * </CMD_MESSAGE>
                     *
                     * New: <CMD_MESSAGE> <BC_DSS_ID>unknown-dssId</BC_DSS_ID> <STATUS>
                     * <STATUS_TYPE_AND_STATUS_VALUE> <CONNECTION_STATUS>PENDING</CONNECTION_STATUS>
                     * <DSS_ID>sn_sim</DSS_ID> </STATUS_TYPE_AND_STATUS_VALUE> </STATUS>
                     * </CMD_MESSAGE>
                     */
                    connState = new CpdConnectionStatus(
                            cmdMessage.getDSMSBody().getCMDMESSAGE()
                                                    .getSTATUS()
                                                    .getSTATUSTYPEANDSTATUSVALUE());
                    logger.trace("-- Found CONNECTION_STATUS: " + connState);

                } else if (cmdMessage.getDSMSBody().getCMDMESSAGE().getPARAMS() != null
                        && cmdMessage.getDSMSBody().getCMDMESSAGE()
                                                   .getPARAMS()
                                                   .getBITRATEANDMODINDEX() != null) {
                    /*
                     * MPCS-5934 - 4/28/2015: Son Ho confirmed that bit-rate/mod-index
                     * is encapsulated in a different format than the previous busy-poll's response
                     * format for the same value.
                     *
                     * Previous: <CMD_MESSAGE><MESSAGE_ID>00000125</MESSAGE_ID
                     * ><BC_DSS_ID>unknown-dssId</BC_DSS_ID> <PARAMETER_RESPONSE>
                     * <RESPONSE><STATUS>OK</STATUS></RESPONSE>
                     * <PARAM_VALUE><BIT_RATE_AND_MOD_INDEX> <BIT_RATE>2000.0
                     * </BIT_RATE><MOD_INDEX>0.18</MOD_INDEX ></BIT_RATE_AND_MOD_INDEX>
                     * </PARAM_VALUE> </PARAMETER_RESPONSE> </CMD_MESSAGE>
                     *
                     * New: <CMD_MESSAGE><BC_DSS_ID>unknown-dssId</BC_DSS_ID ><
                     * PARAMS><BIT_RATE_AND_MOD_INDEX><BIT_RATE>2000.0< /BIT_RATE
                     * ><MOD_INDEX>0.18</MOD_INDEX></BIT_RATE_AND_MOD_INDEX ></PARAMS></CMD_MESSAGE>
                     */
                    bitRateModIndex = cmdMessage.getDSMSBody().getCMDMESSAGE()
                                                              .getPARAMS()
                                                              .getBITRATEANDMODINDEX();
                    logger.trace("-- Found BIT_RATE_MOD_INDEX: " + bitRateModIndex);

                }

            } catch (final Exception e) {
                /*
                 * MPCS-7327 - 5/12/2015: Previously, encountering a parsing error
                 * in one of the MESSAGE elements would cause the entire DMS Broadcast Status
                 * Messages poll to fail. We now isolate the errors to specific MESSAGE
                 * instances, so that rest of the working MESSAGE list can go ahead and be
                 * processed.
                 */
                logger.error("Could not process a MESSAGE from DMS Broadcast Status Messages poll. "
                        + "Message #: " + count + ", Error: " + e.getMessage()
                        + ", Message:\n" + cmdMessageToString(cmdMessage));
            }
        }

		logger.debug("End parsing MESSAGE elements in DMS broadcast status messages object");

        return new CpdDmsBroadcastStatusMessages(radList, reqList,
                incReqStatusList, cpdConfig, connState, bitRateModIndex);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<UplinkRequest> getAllRadiationRequests() throws ICmdException {

        final String serviceUri = cpdUriUtil.getRadiationRequestsServiceUri();
        this.logger.debug("Getting request pool: " + serviceUri);

        final WebTarget wt = this.client.target(serviceUri);

        final CMDMessage radList = issueAndParse(new GetIssueFunctor(wt, CommandEnum.GET,
                                                   "Error retrieving radiation requests", logger));
        return radList.getDSMSBody().getCMDMESSAGE()
        		                                                  .getRADIATIONLIST()
                                                                  .getREQUESTLIST()
                                                                  .getREQUESTINFO();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public UplinkRequest getRequest(final String requestId,
            final CommandUserRole requestRole) throws ICmdException {

        if (requestRole == null) {
            throw new ICmdException("Invalid user role");
        }

        final String serviceUri = cpdUriUtil.getRequestStateServiceUri(requestId, requestRole);

        this.logger.debug("Getting request state: " + serviceUri);

        final WebTarget wt = this.client.target(serviceUri);

		final CMDMessage radList = issueAndParse(new GetIssueFunctor(wt, CommandEnum.GET,
				"Error retrieving request state for request with ID: " + requestId, logger));

		return radList.getDSMSBody()
				      .getCMDMESSAGE()
				      .getRADIATIONLIST()
				      .getREQUESTLIST()
				      .getREQUESTINFO()
				      .stream()
				      .findFirst()
				      .orElse(null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
	public CpdResponse setExecutionMode(final ExecutionMode execMode, final CpdTriggerEvent when)
			throws ICmdException {
		if (execMode == null) {
			throw new ICmdException("Invalid Execution Mode");
		}

		if (when == null) {
			throw new ICmdException("Invalid CPD Trigger Event");
		}

		final String serviceUri = cpdUriUtil.getSetExecutionModeServiceUri(execMode, when);

		final WebTarget wt = this.client.target(serviceUri);

		final CMDMessage serverResponse = issueAndParse(
				new GetIssueFunctor(wt, CommandEnum.PUT, 
						"Error setting execution mode to " + execMode, logger));

		this.debugLogCmdMessage(serverResponse);

		return new CpdResponse(serverResponse.getDSMSBody().getCMDMESSAGE()
				                                           .getMDCRESPONSE().getRESPONSE());
	}


    /**
     * {@inheritDoc}
     */
    @Override
    public CpdResponse setExecutionState(final ExecutionStateRequest execState,
            final CpdTriggerEvent when) throws ICmdException {
        if (execState == null) {
            throw new ICmdException("Invalid Execution State");
        }

        if (when == null) {
            throw new ICmdException("Invalid CPD Trigger Event");
        }

		final String serviceUri = cpdUriUtil.getSetExecutionStateServiceUri(execState, when);

		final WebTarget wt = this.client.target(serviceUri);

		final CMDMessage serverResponse = issueAndParse(
				new GetIssueFunctor(wt, CommandEnum.PUT, 
						"Error setting execution state to " + execState, logger));

        this.debugLogCmdMessage(serverResponse);

        return new CpdResponse(serverResponse.getDSMSBody().getCMDMESSAGE()
                .getMDCRESPONSE().getRESPONSE());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CpdResponse setExecutionMethod(final ExecutionMethod execMethod,
            final CpdTriggerEvent when) throws ICmdException {
        if (execMethod == null) {
            throw new ICmdException("Invalid Execution Method");
        }

        if (when == null) {
            throw new ICmdException("Invalid CPD Trigger Event");
        }

        final String serviceUri = cpdUriUtil.getSetExecutionMethodServiceUri(execMethod, when);

        final WebTarget wt = this.client.target(serviceUri);

		final CMDMessage serverResponse = issueAndParse(
				new GetIssueFunctor(wt, CommandEnum.PUT, 
						"Error setting execution method to " + execMethod, logger));

		this.debugLogCmdMessage(serverResponse);

		return new CpdResponse(serverResponse.getDSMSBody().getCMDMESSAGE().getMDCRESPONSE()
				                                                           .getRESPONSE());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CpdResponse setAggregationMethod(final AggregationMethod aggMethod)
            throws ICmdException {
        if (aggMethod == null) {
            throw new ICmdException("Invalid Aggregation Method");
        }

        final String serviceUri = cpdUriUtil.getSetAggregationMethodServiceUri(aggMethod);

        final WebTarget wt = this.client.target(serviceUri);

		final CMDMessage serverResponse = issueAndParse(
				new GetIssueFunctor(wt, CommandEnum.PUT, 
						"Error setting aggregation method to " + aggMethod, logger));

		this.debugLogCmdMessage(serverResponse);

		return new CpdResponse(serverResponse.getDSMSBody().getCMDMESSAGE().getMDCRESPONSE()
				                                                           .getRESPONSE());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CpdResponse setPreparationState(
            final ListPreparationStateEnum prepState) throws ICmdException {
        if (prepState == null) {
            throw new ICmdException("Invalid Preparation State");
        }

        final String serviceUri = cpdUriUtil.getSetPreparationStateServiceUri(prepState);

        final WebTarget wt = this.client.target(serviceUri);

		final CMDMessage serverResponse = issueAndParse(
				new GetIssueFunctor(wt, CommandEnum.PUT, 
						"Error setting preparation state to " + prepState, logger));

		this.debugLogCmdMessage(serverResponse);

		return new CpdResponse(serverResponse.getDSMSBody().getCMDMESSAGE().getMDCRESPONSE()
				                                                           .getRESPONSE());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CpdConfiguration getCpdConfiguration() throws ICmdException {
        final String serviceUri = cpdUriUtil.getCpdConfigurationServiceUrl();

        this.logger.debug("Getting configuration: " + serviceUri);

        final WebTarget wt = this.client.target(serviceUri);

		final CMDMessage serverResponse = issueAndParse(
				new GetIssueFunctor(wt, CommandEnum.GET, "Error retrieving CPD configuration", logger));

		this.debugLogCmdMessage(serverResponse);

		return new CpdConfiguration(serverResponse.getDSMSBody().getCMDMESSAGE()
                                                                .getLISTCONFIGURATION());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CpdResponse connectToStation(final String stationId)
            throws ICmdException {
        if (stationId == null) {
            throw new ICmdException("Invalid Station ID");
        }

        final String serviceUri = cpdUriUtil.getConnectToStationServiceUri(
                        stationId);

        final WebTarget wt = this.client.target(serviceUri);

        logger.info("Requesting CPD to connect to station: " + stationId);

		final CMDMessage serverResponse = issueAndParse(
				new GetIssueFunctor(wt, CommandEnum.PUT, 
						"Error connecting to station: " + stationId, logger));

		this.debugLogCmdMessage(serverResponse);

		return new CpdResponse(serverResponse.getDSMSBody().getCMDMESSAGE().getMDCRESPONSE()
				                                                           .getRESPONSE());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CpdConnectionStatus getConnectionStatus() throws ICmdException {
        final String serviceUri = cpdUriUtil.getConnectionStateServiceUri();

        this.logger.debug("Getting connection status: " + serviceUri);

        final WebTarget wt = this.client.target(serviceUri);

		final CMDMessage serverResponse = issueAndParse(
				new GetIssueFunctor(wt, CommandEnum.GET, "Error retrieving connection status", logger));

		this.debugLogCmdMessage(serverResponse);

		return new CpdConnectionStatus(serverResponse.getDSMSBody().getCMDMESSAGE()
				                                                   .getCONNECTIONSTATUSRESPONSE());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CpdResponse disconnectFromStation() throws ICmdException {
        final String serviceUri = cpdUriUtil.getDisconnectFromStationServiceUri();

        final WebTarget wt = this.client.target(serviceUri);

		final CMDMessage serverResponse = issueAndParse(
				new GetIssueFunctor(wt, CommandEnum.PUT, 
						"Error disconnecting from station", logger));

		this.debugLogCmdMessage(serverResponse);

		return new CpdResponse(serverResponse.getDSMSBody().getCMDMESSAGE().getMDCRESPONSE()
				                                                           .getRESPONSE());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CpdResponse deleteRadiationRequest(final String requestId,
            final CommandUserRole requestRole, final boolean purgeDb)
            throws ICmdException {
        if (requestId == null) {
            throw new ICmdException("Invalid request ID");
        }

        if (requestRole == null) {
            throw new ICmdException("Invalid user role");
        }

        final String serviceUri = cpdUriUtil.getDeleteRadiationRequestServiceUri(requestId,
                                requestRole, purgeDb);

        final WebTarget wt = this.client.target(serviceUri);

		logger.info("Requesting CPD to delete radiation request (ID: " + requestId + ")");

		logger.trace(String.format("Delete radiation request: %s, %s, purge: %s", requestId,
				requestRole.toString(), Boolean.toString(purgeDb)));

		final CMDMessage serverResponse = issueAndParse(new GetIssueFunctor(wt, CommandEnum.DELETE,
				"Error deleting a radiation request of ID: " + requestId, logger));

		this.debugLogCmdMessage(serverResponse);

		return new CpdResponse(serverResponse.getDSMSBody().getCMDMESSAGE().getMDCRESPONSE()
				                                                           .getRESPONSE());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CpdResponse flushRequests(final CommandUserRole rolePool,
            final boolean purgeDb) throws ICmdException {
        // no null check for rolePool because a null rolePool signifies
        // "Flush All"

		final String serviceUri = cpdUriUtil.getFlushRequestsServiceUri(rolePool, purgeDb);

		final WebTarget wt = this.client.target(serviceUri);

		if (rolePool != null) {
			logger.info("Requesting CPD to flush all radiation request " 
		                + "from role pool: " + rolePool);

			logger.trace("Flush: " + rolePool + ", purge: " + purgeDb);
		} else {
			logger.info("Requesting CPD to flush all radiation request");

			logger.trace("Flush: ALL, purge: " + purgeDb);
		}

		final CMDMessage serverResponse = issueAndParse(
				new GetIssueFunctor(wt, CommandEnum.DELETE, "Error flushing radiation requests",
						            logger));

		this.debugLogCmdMessage(serverResponse);

		return new CpdResponse(serverResponse.getDSMSBody().getCMDMESSAGE().getMDCRESPONSE()
				                                                           .getRESPONSE());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CpdResponse setBitRate(final double rate) throws ICmdException {
		final String serviceUri = cpdUriUtil.getSetBitRateServiceUri(rate);

		final WebTarget wt = this.client.target(serviceUri);

		logger.info("Requesting CPD to set bit rate to " + rate + " bps");

		final CMDMessage serverResponse = issueAndParse(
				new GetIssueFunctor(wt, CommandEnum.PUT, "Error setting bit-rate", logger));

		this.debugLogCmdMessage(serverResponse);

		return new CpdResponse(serverResponse.getDSMSBody().getCMDMESSAGE().getMDCRESPONSE()
				                                                           .getRESPONSE());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CpdResponse issueDirective(final CpdDirective directive)
            throws
            ICmdException {
        if (directive == null) {
            throw new ICmdException("Invalid directive");
        }

        this.logger.trace(CpdClient.class.toString()
                + ": received request to issue directive: "
                + directive.toString() + ", args: "
                + directive.getArgumentsString());

        if (directive.equals(CpdDirective.CONNECT_TO_STATION)) {
            final String stationId =
                    directive.getArguments().get(
                            CpdDirectiveArgument.STATION_ID);
            return this.connectToStation(stationId);
        } else if (directive.equals(CpdDirective.DISCONNECT_FROM_STATION)) {
            return this.disconnectFromStation();
        } else if (directive.equals(CpdDirective.QUERY_CONFIGURATION)) {
            return this.getCpdConfiguration();
        } else if (directive.equals(CpdDirective.QUERY_CONNECTION_STATUS)) {
            return this.getConnectionStatus();
        } else if (directive.equals(CpdDirective.SET_EXECUTION_STATE)) {
            final String execState =
                    directive.getArguments().get(
                            CpdDirectiveArgument.EXECUTION_STATE);
            ExecutionStateRequest cpdExecState = null;

            try {
                cpdExecState = ExecutionStateRequest.valueOf(execState.toUpperCase().trim());
            } catch (final Exception e) {
                throw new ICmdException("Unrecognized execution state: " + execState);
            }

            return this.setExecutionState(cpdExecState,
                    CpdTriggerEvent.IMMEDIATELY);
        }

        return null;
    }


    /*
     * MPCS-7327 - 5/12/2015: Refactored out the CMDMessage object
     * to String conversion, so it can be reused elesewhere.
     */
    private void debugLogCmdMessage(final CMDMessage message) {
    	if (message == null) {
    		this.logger.warn("Unable to log response message because message is null");	
    	} else { 
            this.logger.debug("CMDMessage: \n" + cmdMessageToString(message));
    	}
    }


    private String cmdMessageToString(final CMDMessage message) {

    	if (message == null)
            return null;

        String cmdMessageXmlText = null;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            synchronized (this.marshaller) {
                this.marshaller.marshal(message, baos);
            }

            cmdMessageXmlText = baos.toString(this.xmlEncoding);
        } catch (final Exception e) {
            this.logger.warn("Unable to parse CMDMessage XML to string", e);
        }

        return cmdMessageXmlText;
    }


    /**
     * Retrieve access control which must have been built already.
     */
    private synchronized static void getAcInstance() {
        if (accessControl == null) {
            accessControl = AccessControl.getInstance();
        }
    }


    /**
     * Set cookie on resource. Cookie comes from SSO token, which is prompted
     * for as necessary.
     *
     * @param wt         Web target
     * @param revalidate If true, revalidate cookie
     *
     * @return Web target builder that has any required cookie attached
     *
     * @throws ICmdException If error setting cookie
     */
    private synchronized static Builder setCookie(final ApplicationContext appContext, final WebTarget wt,
                                                           final boolean     revalidate)
        throws ICmdException
    {

        // Get AccessControl if we do not have it
        getAcInstance();

        if ((accessControl == null) || accessControl.skipAuthorization()) {
            // Security not enabled
            return wt.request();
        }

        if (! revalidate) {
            // Use current cookie whether valid or not

            return wt.request().cookie(accessControl.getSsoCookie());
        }

        // Get new token (assume the one we have is invalid)


        final String oldUser = appContext.getBean(AccessControlParameters.class).getUserId();
        String       newUser = null;

        try {
            newUser = accessControl.revalidate();
        } catch (final AccessControlException ace) {
            throw new ICmdException("Error getting cookie", ace);
        }

        if (! oldUser.equals(newUser)) {
            // User changed

        	appContext.getBean(AccessControlParameters.class).setUserId(newUser);
        }

        return wt.request().cookie(accessControl.getSsoCookie());
    }


    /**
     * Issue command and parse the result. Retry once on failure to
     * authenticate. The functor is used to make the request and get
     * the response which may be done in various ways.
     *
     * @param igrf Functor to make request
     *
     * @return Result of parse
     *
     * @throws ICmdException On any command error
     *
     * @param <T> Result type
     *
     */
    private <T> T issueAndParse(final IGetResponseFunctor igrf)
        throws ICmdException {
    	
        boolean keepTrying = true;
        boolean retried    = false;
        T       respCmdMsg = null;

        while (keepTrying) {
            keepTrying = false;

            try {
                final Response response = igrf.getResponse(appContext, retried);

                logger.debug(response);

                respCmdMsg = (T) parseClientResponse(response);
            } catch (final AuthenticationException ae) {
                // Try one more time, and only one more time

                if (retried) {
                    throw ae;
                }

                logger.warn("Authentication failure, trying again");

                retried    = true;
                keepTrying = true;
            } catch (final ProcessingException pe) {
                // See if the cause was authentication

                final AuthenticationException ae =
                    getAuthenticationException(pe);

                if (ae == null) {
                    // Not authentication; decide what to throw and throw it

                    checkExceptionCause(pe);
                    break;
                }

                // Try one more time, and only one more time

                if (retried) {
                    throw ae;
                }

                logger.warn("Authentication failure, trying again");

                retried    = true;
                keepTrying = true;
            }
        }

        return respCmdMsg;
    }


    /**
     * Functor interface for use with issueAndParse.
     *
     */
    private interface IGetResponseFunctor {
        /**
         * Get response as configured, retrying as needed.
         *
         * @param appContext the ApplicationContext that in which this object is being used
         *
         * @param retry Retry authentication if true
         *
         * @return Response
         *
         * @throws ICmdException On any command error
         */
        Response getResponse(ApplicationContext appContext, final boolean retry)
            throws ICmdException;
    }


    /**
     * Functor class for use with issueAndParse that uses getResponse.
     *
     */
    private static final class GetResponseFunctor extends Object
        implements IGetResponseFunctor {
    	
        private final WebTarget  _wt;
        private final MultiPart    _mp;
        private final UplinkLogger _logger;


        /**
         * Constructor.
         *
         * @param wt     Web target
         * @param mp     Multi-part
         * @param logger Logger
         */
        public GetResponseFunctor(final WebTarget  wt,
                                  final MultiPart    mp,
                                  final UplinkLogger logger) {
        	
            super();

            _wt     = wt;
            _mp     = mp;
            _logger = logger;
        }


        /**
         * Get response as configured, retrying as needed.
         *
         * @param retry Retry authentication if true
         *
         * @return Client Response
         *
         * @throws ICmdException On any command error
         */
        @Override
        public Response getResponse(final ApplicationContext appContext, final boolean retry)
            throws ICmdException {
        	
            final Response response =
                CpdClient.getResponse(appContext, _wt, _mp, retry);

            _logger.debug(response);

            return response;
        }
    }


    /**
     * Functor class for use with issueAndParse that uses issue.
     *
     */
    private static final class GetIssueFunctor extends Object
        implements IGetResponseFunctor {
    	
        private final WebTarget  _wt;
        private final CommandEnum  _ce;
        private final String       _text;
        private final UplinkLogger _logger;


        /**
         * Constructor.
         *
         * @param wt     Web target
         * @param ce     Type of command
         * @param text   Error message text
         * @param logger Logger
         */
        public GetIssueFunctor(final WebTarget    wt,
                               final CommandEnum  ce,
                               final String       text,
                               final UplinkLogger logger) {
            super();

            _wt     = wt;
            _ce     = ce;
            _text   = text;
            _logger = logger;
        }


        /**
         * Get response as configured, retrying as needed.
         *
         * @param retry Retry authentication if true
         *
         * @return Client response
         *
         * @throws ICmdException On any command error
         */
        @Override
        public Response getResponse(final ApplicationContext appContext, final boolean retry)
            throws ICmdException {
            Response response = null;

            try {
                response = CpdClient.issue(appContext, _wt, _ce, retry, _logger);
            } catch (final ProcessingException che) {
                throw new ICmdException(_text, che);
            }

            _logger.debug(response);

            return response;
        }
    }

    @Override
    public void setLogger(final UplinkLogger logger) {
        this.logger = logger;
    }

    private boolean isSupportedType(final String msg){
        for(String supportedType: supportedTypes){
            if(msg.contains(supportedType)){
                return true;
            }
        }
        return false;
    }
}
