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
package jpl.gds.db.app;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.ParseException;

import jpl.gds.cli.legacy.options.DssVcidOptions;
import jpl.gds.db.api.sql.fetch.ApidRanges;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.order.IProductOrderByType;
import jpl.gds.db.api.sql.store.ldi.IProductLDIStore;
import jpl.gds.db.mysql.impl.sql.order.ProductOrderByType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.time.DataValidityTime;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.SclkFmt.DvtFormatter;
import jpl.gds.shared.time.TimeProperties;

/**
 * This is the command line application used to query products out of the
 * database.
 */
public class ProductFetchApp extends AbstractFetchApp
{
    private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_get_products");
    private static final int NUM_QUERY_PARAMS = 6;


    /**
     * The list of the product requestedIds user wants from database as a CSV
     */
    private String[] requestId;

    /**
     * The APIDs to query by
     */
    private final ApidRanges apid;

    /**
     * Flag to indicate that partial products should be retrieved
     * It is null if both partial and complete products should be retrieved,
     * true if partial products only should be retrieved, and false
     * if complete products should be retrieved.
     */
    protected Boolean retrievePartialProducts;

    /** VCIDs to query for */
    private Set<Integer> vcids = null;


    /**
     * Creates an instance of ProductFetchApp.
     */
    public ProductFetchApp()
    {
        super(IProductLDIStore.DB_PRODUCT_DATA_TABLE_NAME,
              APP_NAME,
              "ProductQuery");

        suppressInfo();

        this.requestId = null;
        this.apid = new ApidRanges();
        this.retrievePartialProducts = null;
    }


    /**
     * Get specified request-ids.
     *
     * @return Request ids.
     */
    public String[] getRequestId()
	{
        return (this.requestId != null)
                   ? Arrays.copyOf(this.requestId, this.requestId.length)
                   : null;
	}


    /**
     * Get specified APID ranges.
     *
     * @return APID ranges
     */
	public ApidRanges getApid()
	{
		return this.apid;
	}


    /**
     * Get retrieve-partial-products state.
     *
     * @return Retrieve-partial-products state
     */
	public Boolean getRetrievePartialProducts()
	{
		return this.retrievePartialProducts;
	}


    /**
     * Set VCIDs.
     *
     * @param vcids VCID collection
     */
    public void setVcid(final Collection<Integer> vcids)
    {
        if (vcids != null)
        {
            this.vcids = new TreeSet<Integer>(vcids);
        }
        else
        {
            this.vcids = null;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void createRequiredOptions() throws ParseException
    {    	
    	super.createRequiredOptions();
        this.requiredOptions.add(BEGIN_TIME_LONG);
        this.requiredOptions.add(END_TIME_LONG);
        this.requiredOptions.add(PRODUCT_PARTIAL_LONG);
        this.requiredOptions.add(PRODUCT_COMPLETE_LONG);
        this.requiredOptions.add(PRODUCT_APID_LONG);
        this.requiredOptions.add(REQUEST_ID_LONG);
    }

    /**
     * {@inheritDoc}
     */
	@Override
    public void configureApp(final CommandLine cmdline) throws ParseException
    {
        super.configureApp(cmdline);

        if(cmdline.hasOption(REQUEST_ID_SHORT))
        {
        	final String requestIdStr = cmdline.getOptionValue(REQUEST_ID_LONG);
        	if(requestIdStr == null)
        	{
        		throw new MissingArgumentException("The option -" + REQUEST_ID_SHORT + " requires a value.");
        	}
        	
        	this.requestId = requestIdStr.trim().split(",");
        }

        if(cmdline.hasOption(PRODUCT_APID_SHORT))
        {
            apid.processOptions(cmdline.getOptionValue(PRODUCT_APID_SHORT),PRODUCT_APID_SHORT);
        }
        
        this.retrievePartialProducts = null;
        if(cmdline.hasOption(PRODUCT_COMPLETE_SHORT) == true && cmdline.hasOption(PRODUCT_PARTIAL_SHORT) == false)
        {
        	this.retrievePartialProducts = Boolean.FALSE;
        }
        else if(cmdline.hasOption(PRODUCT_COMPLETE_SHORT) == false && cmdline.hasOption(PRODUCT_PARTIAL_SHORT) == true)
        {
        	this.retrievePartialProducts = Boolean.TRUE;
        }

        vcids = DssVcidOptions.parseVcid(missionProps, cmdline, (String) null);
        
        /**
         * chill_get_product 'SCLK' time type should be processed as DVT 
         */
        if (this.times.getTimeType().getValueAsInt() == DatabaseTimeType.SCLK_TYPE) { 
        	final DvtFormatter dvtFmt = TimeProperties.getInstance().getDvtFormatter();
        	DataValidityTime fromSclk = null;
            DataValidityTime thruSclk = null;
            
        	try { 
        		if (this.beginTimeString != null) { 
        			fromSclk = dvtFmt.valueOf(beginTimeString.trim());
        			this.times.setStartSclk(new Sclk(fromSclk.getCoarse(), fromSclk.getFine(), fromSclk.getEncoding()));
        		}
        	} catch(final IllegalArgumentException e) { 
        		throw new ParseException("Begin time SCLK range has invalid format: " + 
        				e.getMessage() + 
        				constructExampleDvtMessage(fromSclk));
        	}
        	
        	try { 
        		if (this.endTimeString != null) { 
        			thruSclk = dvtFmt.valueOf(endTimeString.trim());
        			this.times.setStopSclk(new Sclk(thruSclk.getCoarse(), thruSclk.getFine(), thruSclk.getEncoding()));
        		}
        	} catch(final IllegalArgumentException e) { 
        		throw new ParseException("End time SCLK range has invalid format: " + 
        				e.getMessage() + 
        				constructExampleDvtMessage(thruSclk));
        	}
        }
    }
	
    /**
     * Construct a message describing the syntax of a DVT SCLK value.
     * Note that input may be in either subticks or fractional
     * mode, no matter the configured output mode.
     *
     * @param DataValidityTime A DVT SCLK value to use to extract bounds, etc.
     *
     * @return Message
     *
     */
    private static String constructExampleDvtMessage(final DataValidityTime sclk)
    {
        if (sclk == null)
        {
            return "";
        }

        final StringBuilder sb = new StringBuilder();

        sb.append("\n\nShould be COARSE");
        sb.append(TimeProperties.getInstance().getDvtFormatter().getTicksSep());
        sb.append("FINE in subtick format where COARSE is [0,");
        sb.append(sclk.getCoarseUpperLimit());
        sb.append("] and FINE is [0,");
        sb.append(sclk.getFineUpperLimit());
        sb.append("].\n");
        sb.append("COARSE and FINE may be padded with any number of 0s on the left.\n");

        sb.append("\nOr, COARSE");
        sb.append(TimeProperties.getInstance().getDvtFormatter().getFracSep());
        sb.append("FINE in fractional format where COARSE is [0,");
        sb.append(sclk.getCoarseUpperLimit());
        sb.append("] and FINE is one or more digits.\n");
        sb.append("COARSE may be padded ");
        sb.append("with any number of 0s on the left ");
        sb.append("and FINE on the right.\n");

        return sb.toString();
    }




    /**
     * {@inheritDoc}
     */
	@Override
    public IDbSqlFetch getFetch(final boolean sqlStmtOnly)
    {
		fetch = appContext.getBean(IDbSqlFetchFactory.class).getProductFetch(sqlStmtOnly);
		return fetch;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getFetchParameters()
    {
    	final Object[] params = new Object[NUM_QUERY_PARAMS];
    	
    	IProductOrderByType orderType = null;
		if(this.orderByString != null)
		{
			try
			{
				orderType = new ProductOrderByType(this.orderByString.trim());
			}
			catch(final IllegalArgumentException iae)
			{
				throw new IllegalArgumentException("The value \"" + this.orderByString + "\" is not a legal ordering value for this application.",iae);
			}
		}
	 	else if (this.times != null) {
	 		switch(this.times.getTimeType().getValueAsInt()) {
	 		case DatabaseTimeType.ERT_TYPE:
	 			orderType = ProductOrderByType.ERT;
	 			break;
	 		case DatabaseTimeType.SCET_TYPE:
	 			orderType = ProductOrderByType.DVT_SCET;
	 			break;
	 		case DatabaseTimeType.LST_TYPE:
	 			orderType = ProductOrderByType.DVT_LST;
	 			break;
	 		case DatabaseTimeType.SCLK_TYPE:
	 			orderType = ProductOrderByType.DVT_SCLK;
	 			break;
	 		case DatabaseTimeType.CREATION_TIME_TYPE:
	 			orderType = ProductOrderByType.CREATION_TIME;
	 			break;
	 		case DatabaseTimeType.RCT_TYPE:
	 			orderType = ProductOrderByType.RCT;
	 			break;
            default:
                break;
	 		}
	 	}
	 	else
		{
			orderType = ProductOrderByType.DEFAULT;
		}
		
		params[0] = this.apid;
    	params[1] = this.requestId;
    	params[2] = this.retrievePartialProducts;
    	params[3] = orderType;
    	params[4] = vcids;
    	params[5] = null; // Was station

    	return(params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkTimeType(final DatabaseTimeRange range) throws ParseException
    {
    	switch(range.getTimeType().getValueAsInt())
    	{
            case DatabaseTimeType.SCET_TYPE:
            case DatabaseTimeType.CREATION_TIME_TYPE:
            case DatabaseTimeType.SCLK_TYPE:
            case DatabaseTimeType.ERT_TYPE:
            case DatabaseTimeType.LST_TYPE:
            case DatabaseTimeType.RCT_TYPE:

                break;

            default:

                throw new ParseException("TimeType is not one of: ERT, SCET, CREATION_TIME, SCLK, LST, RCT");
    	}
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public DatabaseTimeType getDefaultTimeType()
    {
        return (DatabaseTimeType.SCET);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getOrderByValues()
    {
        return (ProductOrderByType.orderByTypes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsage()
    {
        return APP_NAME +  " [" + "--" + BEGIN_TIME_LONG + " <time> " + "--" + END_TIME_LONG + " <time>\n" +
                "                   " + "--" + TIME_TYPE_LONG + " <" + TIME_TYPE_ARG + "> " + "--" + PRODUCT_APID_LONG + " <apid,...>]\n" + 
                "                   [Session search options - Not  required]\n";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addAppOptions()
    {
        super.addAppOptions();

        addOption(OUTPUT_FORMAT_SHORT,OUTPUT_FORMAT_LONG,"format", OUTPUT_FORMAT_DESC);
    	addOption(SHOW_COLUMNS_SHORT,SHOW_COLUMNS_LONG, null, SHOW_COLUMNS_DESC);
        addOption(PRODUCT_APID_SHORT, PRODUCT_APID_LONG, "int,...","Product Application ID");
        addOption(TIME_TYPE_SHORT,TIME_TYPE_LONG,TIME_TYPE_ARG,"Time Type: (ERT,SCET,CREATION_TIME,SCLK, LST), SCLK,SCET,LST are DVT times, CREATION_TIME for ground creation time"
                        + "  Default time type is " + getDefaultTimeType().getValueAsString());
        addOption(BEGIN_TIME_SHORT, BEGIN_TIME_LONG, "Time","Begin time of product range");
        addOption(END_TIME_SHORT, END_TIME_LONG, "Time","End time of product range");
        addOption(PRODUCT_COMPLETE_SHORT, PRODUCT_COMPLETE_LONG, null,"Flag to indicate that only complete products will be retrieved.");
        addOption(PRODUCT_PARTIAL_SHORT, PRODUCT_PARTIAL_LONG, null,"Flag to indicate that only partial products will be retrieved.");
        this.options.addOption(REQUEST_ID_SHORT,REQUEST_ID_LONG,true,
        		"Comma separated value list of Product requestID to be selected from the Database.");
        addOrderOption();

        DssVcidOptions.addVcidOption(options);
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public void showHelp()
    {
        super.showHelp();
        System.out.println("\nProduct metadata for selected products will be written to standard");
        System.out.println("output.  Format of this output can be specified using the -" + OUTPUT_FORMAT_SHORT + " option.");
        printTemplateStyles();
    }


    /**
     * Main entry point to run application.
     *
     * @param args Arguments from command-line.
     */    
    public static void main(final String[] args)
    {
        final ProductFetchApp app = new ProductFetchApp();
        app.runMain(args);
    }
}
