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
package jpl.gds.watcher.responder.app;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.ExitWithSessionOption;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.OutputFormatOption;
import jpl.gds.shared.cli.options.numeric.CsvUnsignedIntOption;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.template.MessageTemplateManager;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.watcher.IResponderAppHelper;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import java.util.*;

/**
 * This is the new implementation for the chill_product_watcher application helper class 
 * for a message responder that displays Products from the message bus
 */
public class ProductWatcherApp implements IResponderAppHelper {
	
	/** chill_product_watch application name. */
    private static final String APP_NAME = ApplicationConfiguration.getApplicationName(ResponderAppName.PRODUCT_WATCHER_APP_NAME.getAppName());
  
    private boolean exitWithContext = false;
    
    /** Whether or not a PartialProducts should be watched */
    public static final String HANDLE_PARTIALS_LONG = "handlePartials";
    private boolean watchPartials   = false;
    
    /** Default template name */
    public static final String DEFAULT_TEMPLATE = "onelinesummary";
    private String templateName = null;
    
    /** APID short option */
    public static final String APID_SHORT = "a";
    /** APID long option */
    public static final String APID_LONG = "apid";
    private Set<String> apids;
    
    /** Context Configuration object*/
    private IContextConfiguration contextConfig;

	private final ApplicationContext appContext;
	
	private final ExitWithSessionOption exitOption = new ExitWithSessionOption();
	private final OutputFormatOption formatOption = new OutputFormatOption(false);
	private final FlagOption handlePartialsOption = new FlagOption(null, HANDLE_PARTIALS_LONG, "Watch for PartialProduct messages", false);
	private final CsvUnsignedIntOption apidsOption = new CsvUnsignedIntOption(APID_SHORT, APID_LONG, "apid list", "Product apids to watch",
	        true, true, false);
	
    private final Tracer                log;

	/**
	 * Constructor.
	 * 
	 * @param appContext the current application context
	 */
	public ProductWatcherApp(final ApplicationContext appContext) {
		this.appContext = appContext;
        this.log = TraceManager.getTracer(appContext, Loggers.WATCHER);
	}
    
	@Override
	public void configure(final ICommandLine commandLine) throws ParseException {
		
	    exitWithContext = exitOption.parse(commandLine);
        watchPartials = handlePartialsOption.parse(commandLine);
        templateName = formatOption.parseWithDefault(commandLine, false, true);
	    final Collection<UnsignedInteger> tempApids = apidsOption.parse(commandLine);

		if (!tempApids.isEmpty()) { 
			apids = new HashSet<>();
			for (final UnsignedInteger i: tempApids) {
			    apids.add(i.toString());
			}
		} else { 
            log.warn("No APID filter detected. Watching all products");
			this.apids = new HashSet<>();
		}
		
	}  

	@Override
	public void addAppOptions(final BaseCommandOptions opt) {
	    opt.addOption(exitOption);
	    formatOption.setDefaultValue(DEFAULT_TEMPLATE);
	    opt.addOption(formatOption);
        opt.addOption(handlePartialsOption);
        opt.addOption(apidsOption);
	}

    /**
     * @{inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getAdditionalHelpText()
     */
    @Override
	public String getAdditionalHelpText() {
    	final String[] styles = getTemplateStyles();
    	if (styles.length == 0)
    	{
    		// OK to system.out this rather than trace; it's part of the help text
    		return("\nA list of formatting styles in not currently available.");
    	}
        final StringBuilder result = new StringBuilder("\nAvailable formatting styles are:");
    	for (int i = 0; i < styles.length; i++)
    	{
    		if (i % 4 == 0)
    		{
    			result.append("\n");
    			result.append("   ");
    		}
    		result.append(styles[i] + " ");
    	}
    	result.append("\n");
    	result.append("Note: Complete and Partial Products will use the same formatting style");
    	result.append(getTemplateDirectories());
    	return result.toString();
	}
	
    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IResponderAppHelper#getUsageText()
     */
    @Override
    public String getUsageText() {
        
        final StringBuilder sb = new StringBuilder("Usage: " + APP_NAME + " [session options] [jms options] [database options] [--printLog\n");
        sb.append("                             --apid <apid-list> --handlePartials --outputFormat <format>]\n");
        sb.append("       " + APP_NAME + " --topics <topic-list> [jms options] [database options] [--printLog\n");
        sb.append("                             --apid <apid-list> --handlePartials --outputFormat <format>]\n");
        return sb.toString();
    }
    
	/**
	 * 
	 * @return HashMap containing template names and its available styles
	 * 	(or empty if there are none)
	 */
    private String[] getTemplateStyles()
    {
    
        final String[] completeProductStyles = MessageTemplateManager.getTemplateStylesForHelp(ProductMessageType.ProductAssembled);
        final String[] partialProductStyles = MessageTemplateManager.getTemplateStylesForHelp(ProductMessageType.PartialProduct);
        final String[] allStyles = new String[completeProductStyles.length + partialProductStyles.length];
        System.arraycopy(completeProductStyles, 0, allStyles, 0, completeProductStyles.length);
        System.arraycopy(partialProductStyles, 0, allStyles, completeProductStyles.length, partialProductStyles.length);
        final SortedSet<String> allUnique = new TreeSet<>(Arrays.asList(allStyles));
        return allUnique.toArray(new String[]{});
    }
    
    /**
     * 
     * @return an output String containing template directories for message types:
     * 	ProductAssembledMessage and PartialProductMessage
     *  for the current mission
     *  
     */
    private String getTemplateDirectories() {
   	    MessageTemplateManager templateManager = null;
   	    final StringBuilder result = new StringBuilder();
        try
        {
            templateManager = new MessageTemplateManager(GdsSystemProperties.getSystemMission());
          
            final List<String> directories = templateManager.getTemplateDirectories(MessageRegistry.getMessageConfig(ProductMessageType.ProductAssembled));
            directories.addAll(templateManager.getTemplateDirectories(MessageRegistry.getMessageConfig(ProductMessageType.PartialProduct)));
            
            result.append("\nTemplate directories searched are:\n");
            for (final String d: directories) {
           	   result.append("   " + d + "\n");
            }
        }
        catch (final TemplateException e)
        {
            TraceManager.getDefaultTracer().warn("Unable to determine template directories\n");

        }
        return result.toString();
   }
    
    /**
     * Sets the output template name
     * @param s Template name
     */
    public void setTemplateName(final String s) { 
    	this.templateName = s;
    }
    
    
    /**
     * Gets the name of the template being used
     * @return Output template name
     */
    public String getTemplateName() {
		return this.templateName;
	}

	@Override
	public String[] getOverrideTypes() {
		return watchPartials ? 
            new String[] { ProductMessageType.ProductAssembled.getSubscriptionTag(),
                SessionMessageType.StartOfSession.getSubscriptionTag(),
                SessionMessageType.SessionHeartbeat.getSubscriptionTag(),
                SessionMessageType.EndOfSession.getSubscriptionTag(),
                ProductMessageType.PartialProduct.getSubscriptionTag() } : new String[] {
                ProductMessageType.ProductAssembled.getSubscriptionTag(),
                SessionMessageType.StartOfSession.getSubscriptionTag(),
                SessionMessageType.SessionHeartbeat.getSubscriptionTag(),
                SessionMessageType.EndOfSession.getSubscriptionTag() };
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.watcher.IResponderAppHelper#getContextConfiguration()
	 */
	@Override
	public IContextConfiguration getContextConfiguration() {
		return this.contextConfig;
	}


	/**
     * Gets the flag indicating whether the application should exit when the
     * context ends.
     * @return true if the application should exit when an end of context
     *         message is received
     */
    public boolean isExitContext() {
        return exitWithContext;
    }
    
    /**
     * Gets the list of APIDs this product watcher is interested in.
     * 
     * @return APID list
     */
    public Set<String> getApids() { 
    	return apids;
    }
    
    /**
     * Gets the flag indicating whether the application should watch partial products
     * 
     * @return true if the application should watch partial products
     */
    public boolean isWatchingPartials() { 
    	return watchPartials;
    }

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.watcher.IResponderAppHelper#setContextConfiguration(IContextConfiguration)
	 */
	@Override
	public void setContextConfiguration(final IContextConfiguration config) {
		 this.contextConfig = config;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.watcher.IResponderAppHelper#getApplicationContext()
	 */
	@Override
	public ApplicationContext getApplicationContext() {
		return this.appContext;
	}
}
