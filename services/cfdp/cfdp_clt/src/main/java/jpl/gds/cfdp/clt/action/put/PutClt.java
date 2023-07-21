/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */
package jpl.gds.cfdp.clt.action.put;

import jpl.gds.cfdp.clt.action.AActionClt;
import jpl.gds.cfdp.common.GenericRequest;
import jpl.gds.cfdp.common.action.put.PutActionRequest;
import jpl.gds.cfdp.common.action.put.PutActionResponse;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoUpdater;
import jpl.gds.session.config.options.SessionCommandOptions;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.cli.options.CsvStringOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.cli.options.numeric.UnsignedLongOption;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.types.UnsignedLong;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import static jpl.gds.cfdp.common.action.EActionCommandType.PUT;

public class PutClt extends AActionClt {

    static final String DESTINATION_ENTITY_SHORT = "d";
    static final String DESTINATION_ENTITY_LONG = "destinationEntity";
    static final String SOURCE_FILE_NAME_SHORT = "sf";
    static final String SOURCE_FILE_NAME_LONG = "sourceFileName";
    static final String DESTINATION_FILE_NAME_SHORT = "df";
    static final String DESTINATION_FILE_NAME_LONG = "destinationFileName";
    static final String LOCAL_UPLOAD_FILE_SHORT = "uf";
    static final String LOCAL_UPLOAD_FILE_LONG = "localUploadFile";

    // MPCS-10093 1/6/2019 Match the short and long defined in
    // jpl.gds.session.config.options.SessionCommandOptions.SessionCommandOptions
    static final String SESSION_KEY_SHORT = "K";

    // MPCS-10886 5/9/2019
    static final String MESSAGES_TO_USER_SHORT = "mtu";
    static final String MESSAGES_TO_USER_LONG = "messagesToUser";

    private final StringOption destinationEntityOption = new StringOption(DESTINATION_ENTITY_SHORT,
            DESTINATION_ENTITY_LONG, "mnemonic/id", "destination CFDP mnemonic or entity ID", true);
    private final FileOption sourceFileNameOption = new FileOption(SOURCE_FILE_NAME_SHORT, SOURCE_FILE_NAME_LONG,
            "filename", "source file name (file to send)", true, false);
    private final FileOption destinationFileNameOption = new FileOption(DESTINATION_FILE_NAME_SHORT,
            DESTINATION_FILE_NAME_LONG, "filename", "destination file name (default: source file name)", false, false);
    private final FileOption localUploadFileOption = new FileOption(LOCAL_UPLOAD_FILE_SHORT,
            LOCAL_UPLOAD_FILE_LONG, "filename", "local file to upload to CFDP Processor and have saved as " +
            "\"source file\", prior to starting transaction", false, true);

    // MPCS-10093 1/6/2019 Match the data type and description in
    // jpl.gds.session.config.options.SessionCommandOptions.SessionCommandOptions
    private final UnsignedLongOption sessionKeyOption = new UnsignedLongOption(SESSION_KEY_SHORT, SessionCommandOptions.SESSION_KEY_LONG, "sessionId",
            "the unique numeric identifier for a session", false);

    // MPCS-10886 5/9/2019
    private final CsvStringOption messagesToUserOption = new CsvStringOption(MESSAGES_TO_USER_SHORT, MESSAGES_TO_USER_LONG,
            "message(s)", "comma-separated list of CFDP Messages to User)", false, false, false);

    private long destinationEntity;
    private String sourceFileName;
    private byte serviceClass;
    private String destinationFileName;
    private String localUploadFile;
    private UnsignedLong sessionKey;
    private Collection<String> messagesToUser;

    private ApplicationContext appContext;

    public PutClt() {
        super(PUT);
        // MPCS-10093 1/6/2019 Need Spring application context to check the session database
        appContext = SpringContextFactory.getSpringContext(true);
    }

    /*
     * (non-Javadoc)
     *
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#createOptions()
     */
    @Override
    public BaseCommandOptions createOptions() {

        if (optionsCreated.get()) {
            return options;
        }

        final BaseCommandOptions options = super.createOptions();
        options.addOption(destinationEntityOption);
        options.addOption(sourceFileNameOption);
        options.addOption(classOption);
        options.addOption(destinationFileNameOption);
        options.addOption(localUploadFileOption);

        // MPCS-10093 Accept session key
        options.addOption(sessionKeyOption);

        // MPCS-10886 5/9/2019
        options.addOption(messagesToUserOption);

        return options;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * jpl.gds.shared.cli.app.AbstractCommandLineApp#configure(jpl.gds.shared.cli.
     * cmdline.ICommandLine)
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);
        destinationEntity = translatePossibleMnemonic(destinationEntityOption.parse(commandLine));
        sourceFileName = sourceFileNameOption.parse(commandLine);

        if (serviceClassUnsignedInteger != null) {
            serviceClass = Byte.parseByte(serviceClassUnsignedInteger.toString());
        }

        destinationFileName = destinationFileNameOption.parse(commandLine);

        if (sourceFileName != null && destinationFileName == null) {
            destinationFileName = sourceFileName;
        }

        localUploadFile = localUploadFileOption.parse(commandLine);

        // MPCS-10093 1/6/2019 Grab session option and check if it exists in database
        sessionKey = sessionKeyOption.parse(commandLine);

        if (sessionKey != null) {
            final IDbSqlFetch tsf = appContext.getBean(IDbSqlFetchFactory.class).getSessionFetch(false);
            try {
                final IDbSessionInfoFactory dbSessionInfoFactory = appContext.getBean(IDbSessionInfoFactory.class);
                final IDbSessionInfoUpdater tsi = dbSessionInfoFactory.createQueryableUpdater();
                tsi.addSessionKey(sessionKey.longValue());

                final List<? extends IDbRecord> testSessions = tsf.get(tsi, null, 1, (IDbOrderByType) null);
                if (testSessions.isEmpty()) {
                    tsf.close();
                    throw new ParseException("Value of --" + SessionCommandOptions.SESSION_KEY_LONG +
                            " option must be a valid pre-existing session key. No session with the key '" +
                            sessionKey.longValue() +
                            "' was found.");
                }

            } catch (final DatabaseException s) {
                tsf.close();
                throw new ParseException("Error Connecting to the database while looking up the specified session key: " + s.getMessage());
            } finally {
                tsf.close();
            }
        }

        messagesToUser = messagesToUserOption.parse(commandLine);
    }

    /*
     * (non-Javadoc)
     *
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {

        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final OptionSet options = createOptions().getOptions();
        final PrintWriter pw = new PrintWriter(System.out);
        pw.println("Usage: " + ApplicationConfiguration.getApplicationName() + " " + actionType.getCltCommandStr() + " --"
                + destinationEntityOption.getLongOpt() + " <" + destinationEntityOption.getArgName() + "> --"
                + sourceFileNameOption.getLongOpt() + " <" + sourceFileNameOption.getArgName() + "> [options]");
        pw.println();
        options.printOptions(pw);
        printTemplateStylesAndDirectories(pw);
        pw.flush();
    }

    @Override
    public void run() {
        final PutActionRequest req = new PutActionRequest();
        req.setRequesterId(GdsSystemProperties.getSystemUserName());
        req.setDestinationEntity(destinationEntity);
        req.setSourceFileName(sourceFileName);
        req.setServiceClass(serviceClass);
        req.setDestinationFileName(destinationFileName);
        req.setSessionKey(sessionKey);
        req.setMessagesToUser(messagesToUser);

        try {

            if (localUploadFile != null) {
                req.setUploadFile(Files.readAllBytes(Paths.get(localUploadFile)));
            }

            postAndPrint(actionType.getRelativeUri(), req);

        } catch (IOException e) {
            System.err.println("Could not send request due to IOException: " + ExceptionTools.getMessage(e));
            System.exit(1);
        }

    }

    @Override
    protected void responseSpecificPostAndPrint(final String absoluteUri, final GenericRequest req)
            throws HttpClientErrorException, RestClientException {
        final RequestEntity<GenericRequest> requestEntity = new RequestEntity<>(req, headers, HttpMethod.POST, URI.create(absoluteUri));
        final ResponseEntity<PutActionResponse> resp = new RestTemplate().exchange(requestEntity, PutActionResponse.class);
        resp.getBody().printToSystemOut();
    }

}
