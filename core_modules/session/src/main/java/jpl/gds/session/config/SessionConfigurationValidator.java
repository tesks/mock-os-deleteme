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
package jpl.gds.session.config;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.common.config.connection.IDownlinkConnection;
import jpl.gds.common.config.connection.IFileConnectionSupport;
import jpl.gds.common.config.connection.IUplinkConnection;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.shared.util.HostPortUtility;

/**
 * A possibly temporary R8 replacement for the SessionParameterValidator. Intended to
 * validate only those things that command line parsing cannot.
 * 
 *
 * @since R8
 *
 */
public class SessionConfigurationValidator {
    
    private static final String IS_UNDEFINED = " is undefined in the session configuration";
    
    private final List<String> errors = new ArrayList<String>();

    private final SessionConfiguration config;
    
    /**
     * Constructor.
     * 
     * @param sc the SessionConfiguration to validate.
     */
    public SessionConfigurationValidator(final SessionConfiguration sc) {
    	config = sc;

    }

    /** 
     * Validates the SessionConfiguration. If validation fails, error messages can be
     * obtained afterwards via one of the methods to fetch error messages.
     * 
     * @param isMonitor true if validating for a monitoring client
     * @param autoRun true if in auto-run (no session config GUI) mode, false if not
     * @return true if valid, false if not
     */
    public boolean validate(final boolean isMonitor, final boolean autoRun) {
    	boolean success = true;
    	errors.clear();

      final IVenueConfiguration vc = config.getVenueConfiguration();
      if (vc.getVenueType() == null && autoRun) {
          
          errors.add("Venue Type" + IS_UNDEFINED);
          success = false;
          
      } else if (vc.getVenueType() != null) {
          
          if (autoRun && vc.getVenueType().hasTestbedName() && vc.getTestbedName() == null) {
              errors.add("Testbed Name" + IS_UNDEFINED);
              success = false;
          }
          
          
          if (config.isDownlinkOnly() || !config.isUplinkOnly()) {
        	  
        	  final IDownlinkConnection dc = config.getConnectionConfiguration().getDownlinkConnection();
              if (autoRun && !isMonitor && (dc.getDownlinkConnectionType() == null || 
            		  dc.getDownlinkConnectionType() == TelemetryConnectionType.UNKNOWN)) {

                  errors.add("Downlink connection type" + IS_UNDEFINED);
                  success = false;

              } else if (autoRun && dc instanceof IFileConnectionSupport &&
            		  ((IFileConnectionSupport)dc).getFile() == null && !isMonitor) {
                  errors.add("Downlink connection type is " + TelemetryConnectionType.FILE + 
                          " but the downlink input file" + IS_UNDEFINED);
                  success = false;
              }

              // Does the venue support streams?
              final boolean venueHasStreams = vc.getVenueType().hasStreams();
              
              // Does the connection support streams?
              final boolean connectionHasStreams = dc.getDownlinkConnectionType().usesStreamId();
              
              // If this is a monitor application, any non-null stream ID is ok.
              final boolean monitorOk = isMonitor && vc.getDownlinkStreamId() != null;
              
              // Is the stream ID defined and NOT set to N/A?
              final boolean streamOk = vc.getDownlinkStreamId() != null && vc.getDownlinkStreamId() != DownlinkStreamType.NOT_APPLICABLE;

              // 9/23/2019 - JFWagner - skip this block for SSE, since it doesn't have streams at all
              //
              // If the Venue has streams and the connection has streams, and we are in autorun mode,
              if (venueHasStreams && connectionHasStreams && !config.isSseDownlinkOnly()) {
                  // then either monitorOk or streamOk must be set.
                  if (autoRun && !(monitorOk || streamOk)) {
                      errors.add("autoRun detected for venue " + vc.getVenueType() + " and stream id " + vc.getDownlinkStreamId() 
                      + " using a downlink connection type " + dc.getDownlinkConnectionType() + " and downlink stream "
                       + (!streamOk ? "StreamId " + vc.getDownlinkStreamId() + " cannot be null or "
                      + DownlinkStreamType.NOT_APPLICABLE : "This monitor application has a null stream id"));
                      success = false;
                  }
               }
                else {
                    // Venue or connection type does not support streams
                    // if DownlinkStreamType is NOT NA AND this is not a monitor app, fail!
                    if (streamOk && !monitorOk) {
                        errors.add("Conflicting connection and venue configuration for streams!\nVenue '"
                                + vc.getVenueType() + (venueHasStreams ? "' DOES " : "' DOES NOT ")
                                + "support streams. " + "Connection '" + dc.getDownlinkConnectionType()
                                + (connectionHasStreams ? "' DOES " : "' DOES NOT ") + "support streams.");
                        success = false;
                    }

              }
              
              if (vc.getVenueType().hasStreams() && vc.getDownlinkStreamId() != null &&
            		  vc.getDownlinkStreamId() == DownlinkStreamType.COMMAND_ECHO &&
            		  dc.getInputType() != null && (!dc.getInputType().equals(TelemetryInputType.CMD_ECHO))) {
            	  errors.add("Downlink stream ID " + DownlinkStreamType.COMMAND_ECHO + 
            			  " can only be used when the telemetry input format is " +  TelemetryInputType.CMD_ECHO);
            	  success = false;
              }
              
              if (!config.getConnectionConfiguration().getConnectionProperties().
                           getAllowedDownlinkConnectionTypes(vc.getVenueType(),
                                                             config.getSseContextFlag())
                           .
            		  contains(dc.getDownlinkConnectionType())) {
            	  errors.add(dc.getDownlinkConnectionType() + " is not an allowed value for " +
            			  "downlink connection type when the venue is " +
            			  vc.getVenueType());
            	  success = false;
              }

          }

          final IUplinkConnection uc = config.getConnectionConfiguration().getUplinkConnection();

          if ((config.isUplinkOnly() || !config.isDownlinkOnly()) && !isMonitor) {
              if (autoRun && uc == null || uc.getUplinkConnectionType() == UplinkConnectionType.UNKNOWN) {
                  errors.add("Uplink connection type" + IS_UNDEFINED);
                  success = false;
              }
              
              if (uc.getPort() != HostPortUtility.UNDEFINED_PORT && !config.getConnectionConfiguration().getConnectionProperties().
                                  getAllowedUplinkConnectionTypes(vc.getVenueType(),
                                                                  config.getSseContextFlag())
                                  .
            		  contains(uc.getUplinkConnectionType())) {
            	  errors.add(uc.getUplinkConnectionType() + " is not an allowed value for " +
            			  "uplink connection type when the venue is " +
            			  vc.getVenueType());
            	  success = false;
              }
          }
      }
      return success;
   }

    /**
     * Gets the session validation error messages as a list.
     * @return List of error strings
     */
    public List<String> getErrors() {
    	return this.errors;
    }

    /**
     * Gets the session validation errors as a multi-line block of text.
     * @return error text
     */
    public String getErrorsAsMultilineString() {
    	final StringBuilder sb = new StringBuilder();
    	errors.forEach((s)->sb.append(s + "\n"));
    	return sb.toString();
    }

}
