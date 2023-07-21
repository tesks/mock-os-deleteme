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
/*
 * Title: CommandProperties.java
 *
 * Author: dan
 * Created: Feb 7, 2006
 *
 */
package jpl.gds.tc.api.config;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.tc.api.ICommandFileLoad;

/**
 * This class facilitates retrieval of properties for commanding related tasks
 * 
 *
 * MPCS-8822 - 05/23/17 - Changed to extend GdsHierarchicalProperties, completely overhauled to new design
 * MPCS-9107 - 10/02/17 - Added properties for command echo app
 * MPCS-10834 - 05/03/19 - Added property for path to AMMOS/MPSA CTS cxml command dictionary compiler
 */
public class CommandProperties extends GdsHierarchicalProperties {


    /** Default property file name */
	public static final String PROPERTY_FILE = "command.properties";
	
	/** All properties for commanding begin with this prefix */
	private static final String PROPERTY_PREFIX = "command.";
	
	private static final String ARGUMENTS_BLOCK = PROPERTY_PREFIX + "arguments.";
	private static final String DICTIONARY_BLOCK = PROPERTY_PREFIX + "dictionary.";
	private static final String INTERNAL_EXTERNAL_BLOCK = PROPERTY_PREFIX + "internal.external.";
	private static final String EXTERNAL_BLOCK = PROPERTY_PREFIX + "external.";
	private static final String FILE_LOAD_BLOCK = PROPERTY_PREFIX + "FileLoad.";
	private static final String GUI_BLOCK = PROPERTY_PREFIX + "gui.";
	private static final String JMS_BLOCK = PROPERTY_PREFIX + "messaging.";
	private static final String OUTPUT_BLOCK = PROPERTY_PREFIX + "internal.output.";
	private static final String SSE_BLOCK = PROPERTY_PREFIX + "SSE.";
	private static final String ECHO_BLOCK = PROPERTY_PREFIX + "echo."; 
	
	private static final String ENUMERATED_ARGUMENTS_BLOCK = ARGUMENTS_BLOCK + "enumerated.";
	private static final String ENUM_ARGS_BIT_VALUE_BLOCK = ENUMERATED_ARGUMENTS_BLOCK + "bitValue.";
	
	private static final String FILE_LOAD_VALUES_BLOCK = FILE_LOAD_BLOCK + "values.";
	
	private static final String GUI_TABS_BLOCK = GUI_BLOCK + "tabs.";
	
	private static final String OUTPUT_ADAPTER_BLOCK = OUTPUT_BLOCK + "adapter.";
	
	private static final String ENUM_ARGS_FORMAT_PROPERTY = "format";
	private static final String USE_PROPERTY = "use";
	
	private static final String ARGUMENTS_DEFAULT_VALUE_PROPERTY = ARGUMENTS_BLOCK + "default";
	private static final String ENUM_ARGS_BIT_VALUE_FORMAT_PROPERTY = ENUM_ARGS_BIT_VALUE_BLOCK + ENUM_ARGS_FORMAT_PROPERTY;
	private static final String ENUM_ARGS_ALLOW_BIT_VALUE_PROPERTY = ENUM_ARGS_BIT_VALUE_BLOCK + USE_PROPERTY;
	private static final String ENUM_ARGS_ALLOW_DICT_VALUE_PROPERTY = ENUMERATED_ARGUMENTS_BLOCK + "dictionaryValue." + USE_PROPERTY;
	private static final String ENUM_ARGS_ALLOW_FSW_VALUE_PROPERTY = ENUMERATED_ARGUMENTS_BLOCK + "fswValue." + USE_PROPERTY;
	private static final String ENUM_ARGS_OUTPUT_VALUE_FORMAT_PROPERTY = ENUMERATED_ARGUMENTS_BLOCK + "outputValue." + ENUM_ARGS_FORMAT_PROPERTY;
	
	private static final String DICT_SUPPORTS_CMD_MODULE_PROPERTY = DICTIONARY_BLOCK + "commandModule." + USE_PROPERTY;
	private static final String DICT_VALIDATE_COMMANDS_PROPERTY = DICTIONARY_BLOCK + "validate";
	private static final String DICT_CTS_COMPILER_PATH_PROPERTY = DICTIONARY_BLOCK + "cts.compiler.path";
    private static final String DICT_CTS_VALIDATE_SCHEMA_PROPERTY = DICTIONARY_BLOCK + "cts.validate.schema";
	
	private static final String EXTERNAL_APPLICATIONS_PROPERTY = EXTERNAL_BLOCK + "applications";
	private static final String APPLICATION_CLASS_PROPERTY = ".class";
	private static final String APPLICATION_ENABLE_PROPERTY = ".enable";
	private static final String APPLICATION_SCRIPT_PROPERTY = ".script";
	private static final String APPLICATION_WAIT_FOR_EXIT_PROPERTY = ".waitForExit";
	
	private static final String FILE_CHUNK_SIZE_PROPERTY = FILE_LOAD_BLOCK + "chunkSize";
	private static final String FILE_TYPES_PROPERTY = FILE_LOAD_BLOCK + "types";
	
	private static final String RADIATION_LIST_ORDER_PROPERTY = GUI_BLOCK + "radiationListOrder";
	private static final String SHOW_GUI = GUI_BLOCK + "show"; 
	private static final String SHOW_GUI_STRING_ID_SELECTOR_PROPERTY = GUI_BLOCK + "showStringIdSelector";

	private static final String JMS_SEND_ON_SOCKET_FAILURE_PROPERTY = JMS_BLOCK + "sendOnSocketFailure";
	
	private static final String SSE_PREFIX_PROPERTY = SSE_BLOCK + "prefix";
	private static final String SSE_PRODUCT_DIR_COMMAND_PROPERTY = SSE_BLOCK + "productDirCommnad";
	
	private static final String ECHO_SOCKET_BLOCK = ECHO_BLOCK + "socket.";
	private static final String ECHO_SOCKET_RETRY_BLOCK = ECHO_SOCKET_BLOCK + "retry.";
	
	private static final String ECHO_SOCKET_RETRY_INTERVAL_MS = ECHO_SOCKET_RETRY_BLOCK + "intervalMS";
	private static final String ECHO_SOCKET_RETRY_COUNT = ECHO_SOCKET_RETRY_BLOCK + "retries";
	private static final String ECHO_DEFAULT_HOST = ECHO_BLOCK + "defaultHost";
	private static final String ECHO_DEFAULT_PORT = ECHO_BLOCK + "defaultPort";
	
	private final static RadiationListOrder DEFAULT_RADIATION_LIST_ORDER =
            RadiationListOrder.TOP_DOWN;
	
	private static final int DEFAULT_ECHO_SOCKET_RETRY_INTERVAL_MS = 1000;
	private static final int DEFAULT_ECHO_SOCKET_RETRY_COUNT = -1;
	private static final String DEFAULT_ECHO_DEFAULT_HOST = "localhost";
	private static final int DEFAULT_ECHO_DEFAULT_PORT = 54321;
	
	
	private boolean validateCommands = true;
	private String ssePrefix = "sse:";
	private boolean showGui = false;
	private boolean sendJmsOnFail = true;
	private String binaryOutputFilename = null;
	private List<Double> selectedBitRates = new ArrayList<>();
	private List<String> validRatesList = new ArrayList<>();
	private boolean debug = false;
	private String ctsDictCompilerPath;

	/**
	 * Enum detailing the allowable types of uplink GUI tabs
	 */
	public enum GuiTab {
		
		IMMEDIATE_COMMAND,
		COMMAND_BUILDER,
		SEND_FILE_LOAD,
		SEND_FILE_CFDP,
		SEND_SCMF,
		SEND_RAW_DATA_FILE,
		FAULT_INJECTION;
	}

    /**
     * Enumeration that defines the preferred format for the "bit values" of the
     * enumeration INPUT. The output format is defined by the "format" member.
     */
    public enum BitValueFormat {
        /**
         * Enum value indicating the bit value is decimal.
         */
        DECIMAL,
        /**
         * Enum value indicating the bit value is binary.
         */
        BINARY,
        /**
         * Enum value indicating the bit value is hex.
         */
        HEX,
        /**
         * Enum value indicating the bit value is unspecified.
         */
        UNSPECIFIED
    }

	
    /**
     * Default constructor. Retrieves properties from "command.properties" file.
     * The standard AMPCS hierarchical property retrieval and declaration will
     * be utilized
     * 
     * @param mprops
     *            the mission properties used by the current context
     * @param sseFlag
     *            The current SSE context flag
     */
    public CommandProperties(final MissionProperties mprops, final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
        validRatesList = mprops.getAllowedUplinkBitrates();
        resetConfiguration();
    }

	/**
	 * Return all configurable properties to their default (property file) value
	 */
	public void resetConfiguration(){
		this.binaryOutputFilename = null;
		this.debug = false;
		this.selectedBitRates = new ArrayList<>();
		this.sendJmsOnFail = this.getBooleanProperty(JMS_SEND_ON_SOCKET_FAILURE_PROPERTY, true);
		this.showGui = this.getBooleanProperty(SHOW_GUI, false);
		this.ssePrefix = this.getProperty(SSE_PREFIX_PROPERTY,"sse:");
		this.validateCommands = this.getBooleanProperty(DICT_VALIDATE_COMMANDS_PROPERTY, true);
        this.ctsDictCompilerPath = this.getProperty(DICT_CTS_COMPILER_PATH_PROPERTY, "/ammos/cts/bin/cxml");
	}
	
	/**
     * Get the configured default value string
     * 
     * @return The string used by a user to specify the default value for an argument
     */
    public synchronized String getDefaultValueString() {
        return this.getProperty(ARGUMENTS_DEFAULT_VALUE_PROPERTY,"[default]");
    }
    
    /**
     * Get the format of the bit value for enumerated arguments
     * @return the format of the bit value for enumerated arguments
     */
    public BitValueFormat getEnumBitValueFormat() {
        String bvf = this.getProperty(ENUM_ARGS_BIT_VALUE_FORMAT_PROPERTY);

        BitValueFormat tmp = BitValueFormat.UNSPECIFIED;

        try {
            tmp = BitValueFormat.valueOf(bvf);
        } catch(final IllegalArgumentException | NullPointerException e) {
            //no donthing;
        }

        return tmp;
    }


        
    /**
     * Sets the the allowEnumBitValue.
     * @return the allowEnumBitValue
     */
    public synchronized boolean isAllowEnumBitValue() {
        return this.getBooleanProperty(ENUM_ARGS_ALLOW_BIT_VALUE_PROPERTY, true);
    }
    
    /**
     * Returns the allowEnumDictValue.
     * @return the allowEnumDictValue.
     */
    public synchronized boolean isAllowEnumDictValue() {
        return this.getBooleanProperty(ENUM_ARGS_ALLOW_DICT_VALUE_PROPERTY, true);
    }
    
    /**
     * Returns the allowEnumFswValue
     * @return the allowEnumFswValue
     */
    public synchronized boolean isAllowEnumFswValue() {
        return this.getBooleanProperty(ENUM_ARGS_ALLOW_FSW_VALUE_PROPERTY, true);
    }
    
    /**
     * Gets the preferred output for command enumeration bit values.
     * 
     * @return the configured OutputFormatType
     */
    public OutputFormatType getEnumerationOutputFormat() {

        final String val = this.getProperty(ENUM_ARGS_OUTPUT_VALUE_FORMAT_PROPERTY, OutputFormatType.STRING.name());
        try {
            return OutputFormatType.valueOf(val);
        } catch (final IllegalArgumentException e) {
            e.printStackTrace();
        }
        return OutputFormatType.STRING;
    }
    
	/**
	 * Gets if the command dictionary supports modules
	 * 
	 * @return true if modules are supported, false if not
	 */
    public boolean supportsCommandModule() {
        return this.getBooleanProperty(DICT_SUPPORTS_CMD_MODULE_PROPERTY, true);
    }
    
    /**
     * Accessor for the validate XML
     *
     * @return Returns the validateXml.
     */
    public synchronized boolean getValidateCommands() {
        return this.validateCommands;
    }
    
	/**
	 * Set the validate commands property
	 * 
	 * @param validateCommands
	 *            true if commands are to be validated, false otherwise
	 */
    public synchronized void setValidateCommands(final boolean validateCommands){
    	this.validateCommands = validateCommands;
    }

    /**
     * Accessor for the CTS command dictionary compiler path
     *
     * @return CTS command dictionary compiler path
     */
    public String getCtsCommandDictCompilerPath() {
        return ctsDictCompilerPath;
    }

    /**
     * Accessor for the switch for CTS schema validation
     *
     * @return true if CTS is to validate the command dictionary
     */
    public boolean getCtsValidateSchema() {
        return this.getBooleanProperty(DICT_CTS_VALIDATE_SCHEMA_PROPERTY, true);
    }

    /**
     * Set the CTS command dictionary compiler path
     *
     * @param compilerPath the path to the CTS command dictionary compiler
     */
    public void setCtsCommandDictCompilerPath(final String compilerPath) {
        this.ctsDictCompilerPath = compilerPath;
    }

    /**
	 * Get a list of all external applications that are supported. This is their
	 * short name, not class name
	 * 
	 * @return a list of the String names of external applications
	 */
    public String[] getExternalApplications() {
    	return this.getListProperty(EXTERNAL_APPLICATIONS_PROPERTY, null, ",").toArray(new String[]{});
    }
    
	/**
	 * Get the classname for an external application.
	 * 
	 * @param externalApplicationClassname
	 *            the external application being queried
	 * @return the String representation of an external classname. If the
	 *         property is not present, null is returned
	 */
   public String getExternalApplicationClass(final String externalApplicationClassname) {
	   return this.getProperty(INTERNAL_EXTERNAL_BLOCK + externalApplicationClassname + APPLICATION_CLASS_PROPERTY);
   }
   
	/**
	 * Get if an external application is enabled or not
	 * 
	 * @param externalApplicationClassname
	 *            the external application being queried
	 * @return TRUE if the application is to be enabled, false if not
	 */
   public boolean getExternalApplicationEnable(final String externalApplicationClassname) {
	   return this.getBooleanProperty(EXTERNAL_BLOCK + externalApplicationClassname.toLowerCase() + APPLICATION_ENABLE_PROPERTY, false);
   }
   
	/**
	 * Get the script name for an external application
	 * 
	 * @param externalApplicationClassname
	 *            the external application being queried
	 * @return the string name of the script that will execute the external
	 *         application
	 */
   public String getExternalApplicationScript(final String externalApplicationClassname) {
	   return this.getProperty(INTERNAL_EXTERNAL_BLOCK + externalApplicationClassname.toLowerCase() + APPLICATION_SCRIPT_PROPERTY);
   }
   
   /**
    * Get if the primary application should wait for an exit response from the external application
    * @param externalApplicationClassname the external application being queried
    * @return TRUE if an exit response should be waited for, FALSE otherwise
    */
   public boolean isExternalApplicationWaitForExit(final String externalApplicationClassname) {
	   return this.getBooleanProperty(EXTERNAL_BLOCK + externalApplicationClassname.toLowerCase() + APPLICATION_WAIT_FOR_EXIT_PROPERTY, false);
   }
   
   /**
    * Gets the configured chunk size for Uplink command files
    * 
    * @return the chunkSize
    */
   public synchronized int getChunkSize() { 
   	return this.getIntProperty(FILE_CHUNK_SIZE_PROPERTY, ICommandFileLoad.MAX_FILE_LOAD_DATA_BYTE_SIZE);
   }
   
   /**
    * Get a list of the known file load types
    * 
    * @return A list of the known file load types for this mission
    */
   public String[] getFileLoadTypes() {
       return this.getListProperty(FILE_TYPES_PROPERTY,null,",").toArray(new String[]{});
   }
   
   /**
    * Given the name of a file load type, map that name to the integer value that should be
    * transmitted to the spacecraft in the file load header.
    * 
    * @param inputType A string name identifying a file load type
    * 
    * @return The integer corresponding to the input file load type
    * 
    * @throws FileLoadParseException If the given input doesn't map to a proper file load type
    */
   public byte mapFileLoadTypeToInteger(final String inputType) throws FileLoadParseException {
       if(inputType == null) {
           throw new IllegalArgumentException("Null input type");
       }

       final String[] types = getFileLoadTypes();
       if(types != null) {
           for(final String type : types) {
               if(type.equalsIgnoreCase(inputType)) {
                   return (byte)this.getIntProperty(FILE_LOAD_VALUES_BLOCK + type,-1);
               }
           }
       }

       try {
           /** MPCS-7399 01/11/16 Disallow negative values */

           final byte result = Byte.parseByte(inputType);

           if (result >= 0) {
               return result;
           }
       }
       catch (final NumberFormatException nfe) {
           //ignore
       }

       throw new FileLoadParseException("The input file load type of \"" + inputType + "\" does not match a known file type and is not a valid integer file type.");
   }


   /**
    * Given the numeric value of a file load type, map that value to the string name that describes
    * that file load type.
    * 
    * @param fileType The integer identifier of a file load type
    * 
    * @return The String name of the file load type corresponding to the input number or the
    * empty string if none exists.
    */
   public String mapFileLoadTypeToString(final byte fileType) {

       final String[] types = getFileLoadTypes();
       if(types != null) {
           for(final String type : types) {
               final byte byteVal = (byte)this.getIntProperty(FILE_LOAD_VALUES_BLOCK + type,-1);
               if(byteVal == fileType) {
                   return(type);
               }
           }
       }

       return("");
   }
   
   /**
    * Get the order of the radiation list table widget. This does not determine
    * the actual order of exeuction, it just determines how the radiation list
    * is displayed.
    *
    * @return RadiationListOrder the configured radiation list order
    */
   public RadiationListOrder getRadiationListOrder() {
       final String radListOrder =
               this.getProperty(
                       RADIATION_LIST_ORDER_PROPERTY);

       if (radListOrder != null) {
           try {
               return RadiationListOrder.valueOf(radListOrder);
           } catch (final Exception e) {
               return DEFAULT_RADIATION_LIST_ORDER;
           }
       } else {
           return DEFAULT_RADIATION_LIST_ORDER;
       }
   }
   
   /**
    * Returns the showGui.
    * @return the showGui.
    */
   public synchronized boolean getShowGui()
   {
       return this.showGui;
   }
   
   /**
    * Sets the showGui
    * @param showGui TRUE if GUI should be shown, FALSE if not
    */
   public synchronized void setShowGui(final boolean showGui){
	   this.showGui = showGui;
   }
   
   /**
    * Get if the GUI should show a string ID selector
    * @return true if hte GUI element should be shown, FALSE if not
    */
   public boolean showStringIdSelector() {
       return this.getBooleanProperty(SHOW_GUI_STRING_ID_SELECTOR_PROPERTY, false);
   }
   
   /**
    * Get if a specific GUI tab should be shown. Returns false if the venue or gui tab name do not exist in the proeprties
    * @param venueName the venue being used by the uplink application
    * @param guiTabType the name of the gui tab in question
    * @return TRUE if the tab with the specified name in the specified venue should be shown, FALSE otherwise
    */
   public boolean isGuiTabRequired(final String venueName, final GuiTab guiTabType) {
	   return this.getBooleanProperty(GUI_TABS_BLOCK + venueName.toUpperCase() + "." + guiTabType.toString(), false);
   }
   
   /**
    * Returns the sendJmsMessageOnSocketFailure.
    * @return the sendJmsMessageOnSocketFailure.
    */
   public synchronized boolean getSendJmsMessageOnSocketFailure() {
       return this.sendJmsOnFail;
   }
   
   /**
    * Sets the sendJmsMessageOnSocketFailure property
    * @param sendJmsOnFail boolean
    */
   public synchronized void setSendJmsMessageOnSocketFailure(final boolean sendJmsOnFail){
	   this.sendJmsOnFail = sendJmsOnFail;
   }
   
   /**
    * Get the output adapter class for the specified connection type
    * @param connTypeName the connection type in question
    * @return the full classname for the connection type supplied
    */
   public String getOutputAdapterClass(final String connTypeName) {
	   return this.getProperty(OUTPUT_ADAPTER_BLOCK + connTypeName.toUpperCase());
   }
   
   /**
    * Get the name of the binary output file used by the file output adapter
    * @return the full filepath name of the file used by file output adapter
    */
   public synchronized String getBinaryOutputFile(){
	   return this.binaryOutputFilename;
   }
   
   /**
    * Setthe name of the binary output file used by the file output adapter
    * @param binaryOutputFilename the full filepath name of the file used by file output adapter
    */
   public synchronized void setBinaryOutputFile(final String binaryOutputFilename){
	   this.binaryOutputFilename = binaryOutputFilename;
   }
   
   /**
    * Get the prefix value that will be attached to an SSE command to differentiate it from a normal command
    *
    * @return The prefix value ("sse:" by default) used to start an SSE command string or null if there's an
    * error reading the prefix value from the configuration
    *
    */
   public synchronized String getSseCommandPrefix() {
       return this.ssePrefix;
   }
   
   /**
    * Sets SSE command prefix.
    * @param ssePrefix The prefix to set.  
    */
   public synchronized void setSseCommandPrefix(final String ssePrefix)
   {
       if(ssePrefix == null)
       {
           throw new IllegalArgumentException("Null input prefix");
       }
       else if(ssePrefix.trim().length() == 0)
       {
           throw new IllegalArgumentException("Input SSE Command Prefix is empty");
       }

       this.ssePrefix = ssePrefix;
   }
   
   /**
    * Get the list of bit rates for outgoing requests
    *
    * @return a list of bit rates for outgoing requests
    */
   public List<Double> getSelectedBitRates() {
	   return this.selectedBitRates;
   }
   
   /**
    * Set the list of bit rates for outgoing requests
    *
    * @param selectedBitRates the list of bit rates for outgoing requests
    */
   public void setSelectedBitRates(final List<Double> selectedBitRates) {
       this.selectedBitRates = selectedBitRates;
   }
   
  /**
   * Set the uplink rate(s) to attach to subsequent uplinks to the DSN command
   * service
   *
   * @param uplinkRates the uplink rates to attach to subsequent uplinks
   * @throws UplinkParseException an error with setting uplink rates is encountered
   */
  public void setUplinkRates(final String... uplinkRates) throws UplinkParseException {
      /*
       * MPCS-5192  8/19/13 Moved setUplinkRates to
       * CommandProperties (from AbstractUplinkApp) so calling this method
       * does not require adding SWT libraries to classpath
       */
	   /* R8 Refactor TODO - Do not want this session access here.  It is really
	    * quite harmless to set the uplink rate in any configuration, I think. If not,
	    * then the caller should not do it.
	    */
//      if (!SessionConfiguration.getGlobalInstance()
//              .getUplinkConnectionType()
//              .equals(UplinkConnectionType.COMMAND_SERVICE)) {
//          TraceManager.getDefaultTracer().warn("Ignoring uplink rates because uplink connection type is not COMMAND_SERVICE");
//          return;
//      }

      final List<Double> selectedBitRates = new ArrayList<Double>();

      if ((validRatesList == null) || (validRatesList.isEmpty())) {
          final String errorMessage =
                  "No valid uplink rates are configured for this mission.";
            TraceManager.getDefaultTracer().error(errorMessage);
          throw new UplinkParseException(errorMessage);
      }


      // validate rates
      for (final String ur : uplinkRates) {
          if (ur.equalsIgnoreCase("ANY")) {
              setSelectedBitRates(selectedBitRates);
              return;
          }

          try {
              final double rate = Double.parseDouble(ur);
                // MPCS-9426 - FT - 02/01/18 - compare as string
                if (validRatesList.contains(ur)) {
                  selectedBitRates.add(rate);
              } else {
                  throw new UplinkParseException("Invalid uplink rate: " + ur);
              }
          } catch (final NumberFormatException e) {
              throw new UplinkParseException("Unable to parse uplink rate: " + ur);
          }
      }

      setSelectedBitRates(selectedBitRates);
  }
  
  /**
   * If the command echo app is using a socket connection and when a
   * connection attempt fails, this returns the amount of time that should
   * pass in milliseconds before another attempt is made
   * 
   * @return time in milliseconds to wait between command echo app socket
   *         connection attempts
   */
  public int getEchoSocketRetryInterval(){
      return this.getIntProperty(ECHO_SOCKET_RETRY_INTERVAL_MS, DEFAULT_ECHO_SOCKET_RETRY_INTERVAL_MS);
  }
  
  
  /**
   * If the command echo app is using a socket connection, the number of times
   * to attempt establishing the connection before giving up. If the value is
   * -1, then it will try until the connection is established or the
   * application is terminated
   * 
   * @return the maximum number of connection attempts, or -1 if infinite
   */
  public int getEchoSocketRetryCount(){
      return this.getIntProperty(ECHO_SOCKET_RETRY_COUNT, DEFAULT_ECHO_SOCKET_RETRY_COUNT);
  }
  
  /**
   * Get the configured default host name to be used by the command echo app
   * @return the default host name for command ehco
   */
  public String getDefaultEchoHost(){
      return this.getProperty(ECHO_DEFAULT_HOST, DEFAULT_ECHO_DEFAULT_HOST);
  }
  
  /**
   * Get the configured default port to be used by the command echo app
   * @return the default port number for command echo
   */
  public int getDefaultEchoPort(){
      return this.getIntProperty(ECHO_DEFAULT_PORT, DEFAULT_ECHO_DEFAULT_PORT);
  }
  
  /**
   * Returns the debug.
   * @return the debug.
   */
  public synchronized boolean isDebug()
  {
      return this.debug;
  }

  /**
   * Sets the debug
   * @param verbose Sets the Verbose.
   */
  public synchronized void setDebug(final boolean verbose)
  {
      this.debug = verbose;
  }
  
  @Override
  public String getPropertyPrefix() {
      return PROPERTY_PREFIX;
  }
}
