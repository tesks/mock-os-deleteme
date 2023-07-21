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

package jpl.gds.cfdp.clt.action;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import cfdp.engine.ampcs.TransactionSequenceNumbersListUtil;
import jpl.gds.cfdp.common.GenericRequest;
import jpl.gds.cfdp.common.action.EActionCommandType;
import jpl.gds.cfdp.common.action.ETransactionIdentificationType;
import jpl.gds.cfdp.common.action.TransactionIdentifyingActionRequest;
import jpl.gds.cfdp.common.action.TransactionsActionAppliedResponse;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * Class ATransactionIdentifyingClt
 */
public abstract class ATransactionIdentifyingClt extends AActionClt {

    private static final String ALL_SHORT = "a";
    private static final String ALL_LONG = "all";
    private static final String REMOTE_ENTITY_SHORT = "r";
    private static final String REMOTE_ENTITY_LONG = "remoteEntity";

    /*
     * MPCS-9750 - 5/15/2018
     *
     * JavaCFDP 1.2.1-crc does not support multiple transaction numbers. In 1.1, I had to manually implemented
     * the feature in JavaCFDP. Do not want to reimplement over and over again. So disable the multiple
     * transaction numbers feature and just accept one at a time.
     */
    private static final String TRANSACTION_SHORT = "t";
    private static final String TRANSACTION_LONG = "transaction";

    private final FlagOption allOption = new FlagOption(ALL_SHORT, ALL_LONG, "all transactions");
    private final StringOption remoteEntityOption = new StringOption(REMOTE_ENTITY_SHORT,
            REMOTE_ENTITY_LONG, "mnemonic/id", "remote CFDP mnemonic or entity ID", false);

    /*
     * MPCS-9750 - 5/15/2018 - See comment above
     * 
     *  private final StringOption transactionsOptions = new StringOption(TRANSACTIONS_SHORT, TRANSACTIONS_LONG,
            "entity-id:tx-seq-num(s)",
            "one or more transactions, comma-separated and/or range specified (e.g. 1:2-3,5,6-10", false);

     */
    private final StringOption transactionsOptions = new StringOption(TRANSACTION_SHORT, TRANSACTION_LONG,
            "entity-id:tx-seq-num",
            "a transaction ID", false);

    protected TransactionIdentifyingActionRequest req = new TransactionIdentifyingActionRequest();
    
    
    public ATransactionIdentifyingClt(final EActionCommandType actionType) {
    	super(actionType);
    }
	
	@Override
	public void run() {
		postAndPrint(actionType.getRelativeUri(), req);
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
        options.addOption(allOption);
        options.addOption(classOption);
        options.addOption(remoteEntityOption);
        options.addOption(transactionsOptions);
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

        req.setRequesterId(GdsSystemProperties.getSystemUserName());

        final Boolean allParsed = allOption.parse(commandLine);
        final UnsignedInteger serviceClassParsed = classOption.parse(commandLine);
        final String remoteEntityParsed = remoteEntityOption.parse(commandLine);
        final String transactionsParsed = transactionsOptions.parse(commandLine);

        int providedOptionsCount = 0;

        if (allParsed != null && allParsed.booleanValue()) {
            providedOptionsCount++;
            req.setTransactionIdentificationType(ETransactionIdentificationType.ALL);
        }

        if (serviceClassParsed != null) {
            providedOptionsCount++;
            req.setTransactionIdentificationType(ETransactionIdentificationType.SERVICE_CLASS);
            req.setServiceClass(Byte.parseByte(serviceClassParsed.toString()));
        }

        if (remoteEntityParsed != null) {
            providedOptionsCount++;
            req.setTransactionIdentificationType(ETransactionIdentificationType.REMOTE_ENTITY_ID);
            req.setRemoteEntityId(translatePossibleMnemonic(remoteEntityParsed));
        }

        if (transactionsParsed != null) {
            providedOptionsCount++;
            req.setTransactionIdentificationType(ETransactionIdentificationType.TRANSACTION_IDS);
            req.setTransactionEntityId(parseTransactionEntity(transactionsParsed));
            req.setTransactionSequenceNumbers(parseTransactionSequenceNumbers(transactionsParsed));
        }

        if (providedOptionsCount < 1) {
            throw new ParseException("One option is required");
        } else if (providedOptionsCount > 1) {
            throw new ParseException("Only one option is allowed");
        }

    }

    long parseTransactionEntity(final String transactionsStr) throws ParseException {

        try {
            return translatePossibleMnemonic(transactionsStr.trim().split(":")[0]);
        } catch (final Exception e) {
            throw new ParseException(ExceptionTools.getMessage(e));
        }

    }

    List<List<Long>> parseTransactionSequenceNumbers(final String transactionsStr) throws ParseException {
        final String[] entitySequenceNumbersSplit = transactionsStr.trim().split(":");

        if (entitySequenceNumbersSplit.length != 2) {
            throw new ParseException(transactionsStr + " is not correctly formatted");
        }

        final String[] items = entitySequenceNumbersSplit[1].trim().split(",");
        final List<List<Long>> parsed = new ArrayList<>(items.length);

        for (final String s : items) {
            final String[] rangeSplit = s.trim().split("-");

            if (rangeSplit.length == 1) {
                final List<Long> single = new ArrayList<>(1);
                single.add(Long.parseUnsignedLong(rangeSplit[0]));
                parsed.add(single);
            } else if (rangeSplit.length == 2) {
                final long a = Long.parseUnsignedLong(rangeSplit[0]);
                final long b = Long.parseUnsignedLong(rangeSplit[1]);

                if (Long.compareUnsigned(a, b) > 0) {
                    throw new ParseException(s + " is not a valid range");
                } else if (Long.compareUnsigned(a, b) == 0) {
                    final List<Long> single = new ArrayList<>(1);
                    single.add(a);
                    parsed.add(single);
                } else {
                    final List<Long> range = new ArrayList<>(2);
                    range.add(a);
                    range.add(b);
                    parsed.add(range);
                }

            } else {
                throw new ParseException(s + " is not a valid range");
            }

        }

        TransactionSequenceNumbersListUtil.INSTANCE.sort(parsed);
        return parsed;
    }

    @Override
    protected void responseSpecificPostAndPrint(final String absoluteUri, final GenericRequest req)
            throws HttpClientErrorException, RestClientException {    	
        final RestTemplate rest = new RestTemplate();
    	final RequestEntity<GenericRequest> requestEntity = new RequestEntity<>(req, headers, HttpMethod.POST, URI.create(absoluteUri));
        final ResponseEntity<TransactionsActionAppliedResponse> resp = rest.exchange(requestEntity, TransactionsActionAppliedResponse.class);
        resp.getBody().printToSystemOut();
    }

}
