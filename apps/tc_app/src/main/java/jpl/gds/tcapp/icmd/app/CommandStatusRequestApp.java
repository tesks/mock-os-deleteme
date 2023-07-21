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
package jpl.gds.tcapp.icmd.app;

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.cli.ParseException;
import org.apache.velocity.Template;
import org.springframework.context.ApplicationContext;

import gov.nasa.jpl.icmd.schema.UplinkRequest;
import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.connection.IUplinkConnection;
import jpl.gds.common.config.connection.options.ConnectionCommandOptions;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.security.AccessControlParameters;
import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.config.security.options.AccessControlCommandOptions;
import jpl.gds.common.config.types.LoginEnum;
import jpl.gds.common.options.SpacecraftIdOption;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.security.cam.AccessControl;
import jpl.gds.security.cam.AccessControlException;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.CsvStringOption;
import jpl.gds.shared.cli.options.EnumOption;
import jpl.gds.shared.cli.options.OutputFormatOption;
import jpl.gds.shared.cli.options.ShowColumnsOption;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.template.ApplicationTemplateManager;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.util.HostPortUtility;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tc.api.icmd.ICpdClient;
import jpl.gds.tc.api.icmd.ICpdObjectFactory;
import jpl.gds.tc.api.icmd.exception.ICmdException;

/**
 * This is the main application class for the command line tool
 * chill_get_cmd_status. It fetches from the CPD server, over REST interface,
 * the statuses of previously submitted uplink requests. It can fetch all of
 * them, or for the one specified. Output is templateable.
 *
 * @since	AMPCS R3
 * 7/31/13 replaced password files with keytab files
 */
public class CommandStatusRequestApp extends AbstractCommandLineApp {

	/** Short parameter for uplink request IDs */
	public static final String UPLINK_REQUEST_IDS_PARAM_SHORT = "r";
	/**  Long parameter for uplink request IDs */
	public static final String UPLINK_REQUEST_IDS_PARAM_LONG = "reqIds";
    /** Short order-by option */
    public static final String ORDER_BY_SHORT = "y";
    /** Long order-by option */
    public static final String ORDER_BY_LONG = "orderBy";
	
	private static final String OUTPUT_TEMPLATE_TYPE = "RequestStatuses";

    private final Tracer               log;  


	private static ICpdClient cpd;

	private List<String> uplinkRequestIds;
	private Template template;
	private boolean showColumnHeaders;

	/* Begin security service portion. */

    private static AccessControl ACCESS_CONTROL = null;
	
	private final ApplicationContext springContext;
	private SpacecraftIdOption scidOption;
	private final ShowColumnsOption showColsOption = new ShowColumnsOption(false);
	private final OutputFormatOption outputOption = new OutputFormatOption(false);
	private final CsvStringOption requestIdOption = new CsvStringOption(
			UPLINK_REQUEST_IDS_PARAM_SHORT,
			UPLINK_REQUEST_IDS_PARAM_LONG,
			"id[,id...]",
			"Request ID(s) of the desired uplink request(s) status (returns statuses for all requests if none provided).",
			false, true, false);
	private final EnumOption<OrderByCategory> orderByOption = new EnumOption<OrderByCategory>(
				OrderByCategory.class,
				ORDER_BY_SHORT,
				ORDER_BY_LONG,
				"orderValue",
				"The field that the output should be ordered by",
				false);
    private final AccessControlCommandOptions accessOptions;
	private ConnectionCommandOptions connectionOptions;
    private LoginEnum loginMethod;
    private String username;
    private String keytabFile;
    private String format;

	/* End security service portion. */

	/**
	 * Enumeration for orderBy categories
	 */
    enum OrderByCategory {
		SUBMIT_TIME,
		USER_ID,
		ROLE_ID,
		STATUS,
		STATUS_UPDATE_TIME,
		INCLUDED_IN_EXE_LIST,
		FILENAME,
		BITRATES
	}

	private OrderByCategory orderBy;
	
    private final SecurityProperties securityProps;

	/**
	 * Constructor.
	 */
	public CommandStatusRequestApp() {
		springContext = SpringContextFactory.getSpringContext(true);
        log = TraceManager.getTracer(springContext, Loggers.CPD_UPLINK);
        this.securityProps = springContext.getBean(SecurityProperties.class);
        accessOptions = new AccessControlCommandOptions(securityProps,
                                                        springContext.getBean(AccessControlParameters.class));
		
	}
	
	public ApplicationContext getApplicationContext() {
		return this.springContext;
	}
	
	/**
	 * Main entry point for execution.
	 *
	 * @param args	command line arguments
	 */
	public static void main(final String[] args) {

		final CommandStatusRequestApp app = new CommandStatusRequestApp();

		try {
			final ICommandLine cl = app.createOptions().parseCommandLine(args, true);
			app.configure(cl);

		} catch (final ParseException e) {
			System.err.println("Exception encountered while interpreting arguments: " + e.getMessage());
			System.exit(1);
		}

		/* Begin security service code. */
		app.verifySecurityAccess();
		/* End security service code. */

        try
        {
            cpd = app.getApplicationContext().getBean(ICpdClient.class);
        }
        catch (final Exception e)
        {
        	System.err.println("Exception encountered while instantiating CPD client: " + e.getMessage());
        	e.printStackTrace();
            System.exit(1);
        }

		app.requestUplinkStatuses();

	}


	/**
	 * Security check.
	 */
	public void verifySecurityAccess() {

		if (securityProps.getEnabled()) {

			try {
				
				final AccessControlParameters acParams = this.accessOptions.getAccessControlParameters();
				
				// use username supplied. If
            	// none, use login user
            	final String user = (acParams.getUserId() == null || acParams.getUserId() == "") 
                        ? GdsSystemProperties.getSystemUserName()
                        : acParams.getUserId();

				ACCESS_CONTROL = AccessControl.createAccessControl(
						springContext.getBean(SecurityProperties.class),
						user,
						securityProps.getDefaultRole(),
						acParams.getLoginMethod(),
						acParams.getKeytabFile(), false, null, log);

			} catch (final AccessControlException ace) {
				throw new IllegalArgumentException("Could not start access "
						+ "control, unable to " + "run", ace);
			}

			try {
				ACCESS_CONTROL.requestSsoToken();

				// Now get the real user

				springContext.getBean(AccessControlParameters.class).setUserId(ACCESS_CONTROL.getUser());

			} catch (final AccessControlException ace) {
				throw new IllegalArgumentException("Could not get initial "
						+ "token, unable to " + "run", ace);
			}

		} else {
			log.info("Access control is disabled");

			ACCESS_CONTROL = null;
		}

	}


	/**
	 * {@inheritDoc}
	 *
	 * @see jpl.gds.cli.legacy.app.AbstractCommandLineApp#createOptions()
	 */
	@Override
	public BaseCommandOptions createOptions() {
	    if (optionsCreated.get()) {
            return options;
        }
		super.createOptions(springContext.getBean(BaseCommandOptions.class, this));

		this.scidOption = new SpacecraftIdOption(springContext.getBean(MissionProperties.class), false);
		options.addOption(this.scidOption);	   
		options.addOption(this.requestIdOption);
		options.addOption(this.outputOption);		
        options.addOption(this.orderByOption);
        options.addOption(this.showColsOption);
        this.accessOptions.USER_ID.setDefaultValue(springContext.getBean(AccessControlParameters.class).getUserId());
        options.addOption(this.accessOptions.KEYTAB_FILE);
        options.addOption(this.accessOptions.LOGIN_METHOD_NON_GUI);
        options.addOption(this.accessOptions.USER_ID);
        this.connectionOptions = new ConnectionCommandOptions(springContext.getBean(IConnectionMap.class));	
        options.addOption(this.connectionOptions.FSW_UPLINK_HOST);
        options.addOption(this.connectionOptions.FSW_UPLINK_PORT);

		return options;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#configure(jpl.gds.shared.cli.cmdline.ICommandLine)
	 */
	@Override
	public void configure(final ICommandLine commandLine) throws ParseException {
		super.configure(commandLine);
		
		final UnsignedInteger scid = this.scidOption.parse(commandLine);
		if (scid != null) {
			springContext.getBean(IContextIdentification.class).setSpacecraftId(scid.intValue());
		}
	
		final Collection<String> idsStr = this.requestIdOption.parse(commandLine);
		if (idsStr != null) {
			uplinkRequestIds = new Vector<String>();
			idsStr.forEach((i)->uplinkRequestIds.add(i));
		}

		/*
		 * This app's output is always templated. Default output is CSV.
		 */
		ApplicationTemplateManager tm = null;

		try {
            tm = MissionConfiguredTemplateManagerFactory.getNewApplicationTemplateManager(this,
                                                                                          springContext.getBean(SseContextFlag.class));
		} catch (final TemplateException e) {
			log.warn("Error generating template manager. Defaulting to no formatting: "
					+ rollUpMessages(e));
		}

		format = this.outputOption.parse(commandLine);
		
		if (format != null) {
			try {
				template = tm.getTemplateForStyle(OUTPUT_TEMPLATE_TYPE, format);

			} catch (final TemplateException e) {
				log.warn("Error retrieving template format "
						+ format
						+ " in "
						+ Arrays.toString(tm.getTemplateDirectories(
								OUTPUT_TEMPLATE_TYPE).toArray())
						+ ". Defaulting to no formatting: " + rollUpMessages(e));

			}

		}

		if (template == null) {
			/*
			 * Either no template was specified or the specified template does
			 * not exist. Default to CSV.
			 */
			format = "csv";

			try {
				template = tm.getTemplateForStyle(OUTPUT_TEMPLATE_TYPE, format);

			} catch (final TemplateException e) {
				log.warn("Error retrieving template format "
						+ format
						+ " in "
						+ Arrays.toString(tm.getTemplateDirectories(
								OUTPUT_TEMPLATE_TYPE).toArray())
						+ ". Cannot display output: " + rollUpMessages(e));
				throw new RuntimeException();
			}

		}

		showColumnHeaders = this.showColsOption.parse(commandLine);
	    
		try {
			orderBy = this.orderByOption.parse(commandLine);
		} catch (final ParseException iae) {
			log.warn("Illegal sort field specified. Defaulting to no sorting: " + rollUpMessages(iae));
		}
		
		this.accessOptions.LOGIN_METHOD_NON_GUI.parseWithDefault(commandLine, false, true);
        this.accessOptions.KEYTAB_FILE.parseWithDefault(commandLine, false, true);
        this.accessOptions.USER_ID.parse(commandLine);
        
        final AccessControlParameters acParams = this.accessOptions.getAccessControlParameters();
        this.loginMethod = acParams.getLoginMethod();
        this.username = acParams.getUserId();
        this.keytabFile = acParams.getKeytabFile();

        if ((acParams.getLoginMethod() == LoginEnum.KEYTAB_FILE) && acParams.getKeytabFile().isEmpty())
        {
            throw new ParseException("No keytab file provided");
        }

        
        if ((acParams.getLoginMethod() == LoginEnum.KEYTAB_FILE) && acParams.getUserId() == "") {
            throw new ParseException("No username specified");
        }

        this.connectionOptions.parseAllOptionsAsOptional(commandLine);

        final IConnectionMap hc = this.connectionOptions.getConnectionConfiguration();
        
        final ConnectionProperties connectionProps = springContext.getBean(ConnectionProperties.class);

        final IUplinkConnection uc = hc.getFswUplinkConnection();
        
        if (uc.getHost() == null)
        {
            // Not specified via command line, get configured

			/*
			 * This was copied from CommandStatusPublisherApp's configure()
			 * method, hence the config item being used is named specifically
			 * for status publisher. Something should be done about this. We
			 * should at least rename the config item to be more generic, for
			 * use by both status publisher and this command status request app.
			 */
            final String host = StringUtil.emptyAsNull(
                                    connectionProps.getDefaultUplinkHost(false));
            if (host == null)
            {
                throw new ParseException("No FSW uplink host provided or configured");
            }

            uc.setHost(host);
        }

        if (uc.getPort() == HostPortUtility.UNDEFINED_PORT)
        {
            // Not specified via command line, get configured

        	/*
        	 * See similar comment above for publisher host.
        	 */
            final int port = connectionProps.getDefaultUplinkPort(false);

            if (port == HostPortUtility.UNDEFINED_PORT)
            {
                throw new ParseException("No FSW uplink port provided or configured");
            }

            uc.setPort(port);
        }

	}


	/**
	 * With the parsed parameters, fetch the statuses from CPD.
	 *
	 */
	public void requestUplinkStatuses() {
		List<UplinkRequest> uReqs = null;

		if (uplinkRequestIds != null) {
			/*
			 * Specific request IDs were provided by the user.
			 */
			uReqs = new ArrayList<UplinkRequest>(uplinkRequestIds.size());

			/*
			 * Because fetching a specific request status based on the request
			 * ID now requires the CPD user's role as well, it is more
			 * convenient to just get the entire request list and filter out its
			 * results.
			 */
			List<UplinkRequest> allRequests = null;

			try {
				allRequests = cpd.getAllRadiationRequests();
			} catch (final ICmdException e) {
				log.error("Exception encountered while fetching uplink request statuses: " + e.getMessage());
				return;
			}

			final Map<String, UplinkRequest> allRequestsMap = new HashMap<String, UplinkRequest>(allRequests.size());

			for (final UplinkRequest r : allRequests) {
				allRequestsMap.put(r.getREQUESTID(), r);
			}

			for (final String queriedReqId : uplinkRequestIds) {

				if (allRequestsMap.containsKey(queriedReqId)) {
					uReqs.add(allRequestsMap.get(queriedReqId));
				}

			}

		} else {
			/*
			 * No request IDs provided--fetch statuses for all requests.
			 */

			try {
				uReqs = cpd.getAllRadiationRequests();

			} catch (final ICmdException e) {
				log.error("Exception encountered while fetching all uplink request statuses: " + e.getMessage());
			}

		}

		final TemplatableUplinkRequestStatus turs = new TemplatableUplinkRequestStatus();

		if (showColumnHeaders) {
			turs.setHeader();
			System.out.println(TemplateManager.createText(template, turs.getTemplateContext()));
		}

		if (uReqs == null || uReqs.size() < 1) {
			/*
			 * No results to output. End here.
			 */
			return;
		}

		/*
		 * Convert CPD-specific UplinkRequest objects to our CpdUplinkStatus
		 * objects for sorting and templating.
		 */
		final List<ICpdUplinkStatus> statuses = convertToCpdUplinkStatuses(uReqs);

		if (orderBy != null) {
			sort(statuses);
		}

		for (final ICpdUplinkStatus cus : statuses) {
			turs.set(cus);
			System.out.println(TemplateManager.createText(template,
					turs.getTemplateContext()));
		}

	}

	private void sort(final List<ICpdUplinkStatus> statuses) {

		switch (orderBy) {
		case SUBMIT_TIME:
			Collections.sort(statuses, new Comparator<ICpdUplinkStatus>() {

				@Override
				public int compare(final ICpdUplinkStatus o1, final ICpdUplinkStatus o2) {
					final DateFormat df = TimeUtility.getDoyFormatterFromPool();
					boolean abort = false;

					Date o1Time = null, o2Time = null;

					try {
						o1Time = df.parse(o1.getSubmitTime());
					} catch (final java.text.ParseException e) {
						log.error("Unable to convert submit time string (" + o1.getSubmitTime() + ") to Date object during sort; aborting comparison");
						abort = true;
					}

					try {
						o2Time = df.parse(o2.getSubmitTime());
					} catch (final java.text.ParseException e) {
						log.error("Unable to convert submit time string (" + o2.getSubmitTime() + ") to Date object during sort; aborting comparison");
						abort = true;
					}

					TimeUtility.releaseDoyFormatterToPool(df);

					if (abort) {
						return 0;
					}

					return o1Time.compareTo(o2Time);
				}

			});
			break;

		case USER_ID:
			Collections.sort(statuses, new Comparator<ICpdUplinkStatus>() {

				@Override
				public int compare(final ICpdUplinkStatus o1, final ICpdUplinkStatus o2) {

					if (o1.getUserId() == null) {
						return -1;
					}

					if (o2.getUserId() == null) {
						return 1;
					}

					return o1.getUserId().compareTo(o2.getUserId());
				}

			});
			break;

		case ROLE_ID:
			Collections.sort(statuses, new Comparator<ICpdUplinkStatus>() {

				@Override
				public int compare(final ICpdUplinkStatus o1, final ICpdUplinkStatus o2) {

					if (o1.getRoleId() == null) {
						return -1;
					}

					if (o2.getRoleId() == null) {
						return 1;
					}

					return o1.getRoleId().compareTo(o2.getRoleId());
				}

			});
			break;

		case STATUS:
			Collections.sort(statuses, new Comparator<ICpdUplinkStatus>() {

				@Override
				public int compare(final ICpdUplinkStatus o1, final ICpdUplinkStatus o2) {
					return o1.getStatus().compareTo(o2.getStatus());
				}

			});
			break;

		case STATUS_UPDATE_TIME:
			Collections.sort(statuses, new Comparator<ICpdUplinkStatus>() {

				@Override
				public int compare(final ICpdUplinkStatus o1, final ICpdUplinkStatus o2) {
					return o1.getTimestamp().compareTo(o2.getTimestamp());
				}

			});
			break;

		case INCLUDED_IN_EXE_LIST:
			Collections.sort(statuses, new Comparator<ICpdUplinkStatus>() {

				@Override
				public int compare(final ICpdUplinkStatus o1, final ICpdUplinkStatus o2) {

					if (o1.getIncludedInExeList() == null) {
						return -1;
					}

					if (o2.getIncludedInExeList() == null) {
						return 1;
					}

					return o1.getIncludedInExeList().compareTo(o2.getIncludedInExeList());
				}

			});
			break;

		case FILENAME:
			Collections.sort(statuses, new Comparator<ICpdUplinkStatus>() {

				@Override
				public int compare(final ICpdUplinkStatus o1, final ICpdUplinkStatus o2) {

					if (o1.getFilename() == null) {
						return -1;
					}

					if (o2.getFilename() == null) {
						return 1;
					}

					return o1.getFilename().compareTo(o2.getFilename());
				}

			});
			break;

		case BITRATES:
			Collections.sort(statuses, new Comparator<ICpdUplinkStatus>() {

				@Override
				public int compare(final ICpdUplinkStatus o1, final ICpdUplinkStatus o2) {

					if (o1.getBitrates() == null) {
						return -1;
					}

					if (o2.getBitrates() == null) {
						return 1;
					}

					if (o1.getBitrates().size() < 1) {
						return -1;
					}

					if (o2.getBitrates().size() < 1) {
						return 1;
					}

					return o1.getBitrates().get(0).compareTo(o2.getBitrates().get(0));
				}

			});
			break;

		}

	}


	private List<ICpdUplinkStatus> convertToCpdUplinkStatuses(final List<UplinkRequest> uReqs) {
		final List<ICpdUplinkStatus> statuses = new ArrayList<ICpdUplinkStatus>(uReqs.size());

		for (final UplinkRequest ur : uReqs) {
			statuses.add(springContext.getBean(ICpdObjectFactory.class).
			        createCpdUplinkStatus(springContext.getBean(MissionProperties.class).getStationMapper(), ur));
		}

		return statuses;
	}

	/**
	 * Inner class for using templates with uplink requests
	 *
	 *
	 */
	private static class TemplatableUplinkRequestStatus implements Templatable {
		private final HashMap<String, Object> context;
		private ICpdUplinkStatus cpdUplinkStatus;


		public TemplatableUplinkRequestStatus() {
			context = new HashMap<String,Object>(10);
		}


		public void set(final ICpdUplinkStatus cus) {
			context.clear();
			cpdUplinkStatus = cus;
			setTemplateContext(context);
		}


		public void setHeader() {
			context.put("header", true);
		}


		/**
		 * {@inheritDoc}
		 *
		 * @see jpl.gds.shared.template.Templatable#setTemplateContext(java.util.Map)
		 */
		@Override
		public void setTemplateContext(final Map<String, Object> map) {
			map.put("requestId", cpdUplinkStatus.getId());
			map.put("status", cpdUplinkStatus.getStatus().toString());
			map.put("filename", cpdUplinkStatus.getFilename());
			map.put("bitrates", cpdUplinkStatus.getBitrates());
			map.put("userId", cpdUplinkStatus.getUserId());
			map.put("roleId", cpdUplinkStatus.getRoleId());
			map.put("submitTime", cpdUplinkStatus.getSubmitTime());
			map.put("statusUpdateTime", cpdUplinkStatus.getTimestampString());
			map.put("includedInExeList", cpdUplinkStatus.getIncludedInExeList());
		}


		public HashMap<String, Object> getTemplateContext() {
			return context;
		}

	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }
        super.showHelp();
    }

    // package private getters to use for tests

    boolean isShowColumnHeaders() {
        return showColumnHeaders;
    }

    String getFormat() {
        return format;
    }

    OrderByCategory getOrderBy() {
        return orderBy;
    }

    List<String> getUplinkRequestIds() {
        // defensive copy
        return Collections.unmodifiableList(uplinkRequestIds);
    }

    // login options - exposing individual getters rather than the mutable AccessControlParams object

    LoginEnum getLoginMethod() {
        return loginMethod;
    }

    String getUsername() {
        return username;
    }

    String getKeytabFile() {
        return keytabFile;
    }


}
