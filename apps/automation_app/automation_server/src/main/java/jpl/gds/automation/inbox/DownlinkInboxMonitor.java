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
package jpl.gds.automation.inbox;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jpl.gds.automation.inbox.InboxProperties.RunConfig;
import jpl.gds.common.error.ErrorCode;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.process.ProcessLauncher;
import jpl.gds.shared.process.StdoutLineHandler;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.sys.QuitSignalHandler;
import jpl.gds.shared.thread.SleepUtilities;

/**
 * DownlinkInboxMonitor is the main workhorse for the downlink inbox monitoring application. This
 * class monitors a specified directory for data files and submits those
 * products to chill_down for processing.
 * 
 * @see jpl.gds.automation.inbox.DownlinkInboxMonitorApp
 * @see jpl.gds.automation.inbox.InboxProperties
 * 
 */
public class DownlinkInboxMonitor implements IQuitSignalHandler
{

    private static final String PARAM_DATA_REPLACE_TOKEN = "\\[data-file-token\\]";

    private enum CmdResults
    {
        CMD_NO_ERR, // no error
        CMD_ERR_NO_CMD, // error: no cmd line to submit
        CMD_ERR_LAUNCH // error: problem with the cmd line submission
    }

    private static final long FIVE_SECONDS = 5000l;
    private static final long HALF_SECOND = 500;

    private final Tracer log;
    private File cmdLogFile = null;
    private PrintWriter cmdLogPrinter = null;
    private FileWriter cmdLogWriter = null;

    private final RunConfig config;

    private final File monDir;
    private final String monDirAbsolutePath;

    private final String triggerToken;
    private final FilenameFilter filter;

    private final long msTriggerWait; // value of -1 => no sleep between checks
    private final long msHeartbeatWait; // value of Long.MAX_VALUE => no sleep " " "

    private ExecutorService execMonitor = null;
    private ExecutorService execHeartBeat = null;
    private boolean continueProcessing = true;

    private String procName = "InboxMonitor";

    /**
     * Create a monitor for the specific RunConfig. A RunConfig is
     * created for each inbox specified in the configuration.  
     * 
     * 
     * @param configInfo the configuration object for one inbox
     */
    public DownlinkInboxMonitor(final RunConfig configInfo)
    {

        Runtime.getRuntime().addShutdownHook(new Thread(new QuitSignalHandler(this)));

        config = configInfo;

        log = config.getLogger();

        procName += "-" + config.getActiveConfigBlockName() + "- ";

        // setup Monitoring directory
        monDir = checkDir(config.getDirectoryToMonitor());
        if (monDir == null)
        {
            monDirAbsolutePath = "-no-monitor-directory-specified-";
            continueProcessing = false;
        } else
        {
            monDirAbsolutePath = monDir.getAbsolutePath();
        }

        // setup Trigger file information
        triggerToken = config.getProcessDataTriggerId();
        if (triggerToken == null)
        {
            log.error(procName
                    + "Unable to ascertain the Trigger Identifier from the configuration file.  This process will terminate.");
            continueProcessing = false;
        }

        filter = new StatusMonitorFilenameFilter();

        // setup wait times //
        msTriggerWait = config.getTimeBetweenTriggerChecks();
        msHeartbeatWait = config.getTimeBetweenHeartbeats();
    }
    
    private class StatusMonitorFilenameFilter implements FilenameFilter
    {
    	@Override
        public boolean accept(final File arg0, final String arg1)
        {
            log.debug(procName + "filter, test filename: " + arg1);
            return arg1.endsWith(triggerToken);
        }
    }

    /**
     * Creates a directory [and its' parents] if it doesn't exist. Note: doesn't
     * check permission
     * 
     * @param path
     *            absolute path to a directory
     * @return File or null if the directory doesn't exist or bad value from
     *         configuration file
     */
    private File checkDir(final String path)
    {

        File res = null;

        if (path == null)
        {
            log.error(procName
                    + "Unable to ascertain the Directory from the configuration file.  Process will terminate.");
            return res;
        }

        res = new File(path);
        if (!res.exists())
        {
            log.error(procName + "Specified Station directory (" + res + ") doesn't exist.  Process will terminate");
            res = null;
        }

        return res;
    }

    /**
     * Called from the inbox monitoring application {or main} after all configuration is
     * completed. The main 'kick-off' of the Monitor process
     */
    public void monitorInbox()
    {
        execMonitor = Executors.newSingleThreadExecutor();
        if (execMonitor == null)
        {
            log.error(procName
                    + "Not able to create thread.  Cannot monitor/process data files.");
            return;
        }
        execMonitor.submit(new MonitorAndProcessData());
    }

    /**
     * Private class to monitor for and submit to process Data files.
     * 
     */
    private class MonitorAndProcessData implements Runnable
    {

        @Override
        public void run()
        {
            log.info(procName + "process has been started");
            heartBeat();

            while (continueProcessing)
            {
            	boolean shouldBreak = false;
                if (nap()) {// returns true if interrupted
                    shouldBreak = true;
                    
                } else
                {
					final File[] lst = triggerFiles();
					if (lst == null) {
						shouldBreak = true; // a [Very unlikely] fatal condition
											// occurred
					} else {
						processDataFiles(lst);
					}
                }
                
                if(shouldBreak)
                {
                	break;
                }
            }

            log.info(procName + "process has finished");

        }
        
        /**
         * Create an array of Trigger Files which are located in the
         * monitor-directory
         * 
         * @return a sorted array of trigger files contained in the monitor
         *         directory
         */
        private File[] triggerFiles()
        {
      
            File[] files = null;

            if (monDir == null || filter == null)
            {
                // Very unlikely, but retentive
                log.error(procName
                        + "Monitor directory and/or Trigger file filter are invalid.  Process will terminate.");
                return files;
            }

            files = monDir.listFiles(filter);

            // file creation time ordered
            Arrays.sort(files, new TriggerFilesComparator());

            return files;
        }
        
        /**
         * This method submits each Data file to chill_down and deletes, as
         * appropriate, the associated trigger and data file.
         * 
         * @param Array
         *            of Data-Trigger File objects
         * 
         */
        private void processDataFiles(final File[] lst)
        {

            for (final File entry : lst) {// for each Trigger file in the monitor directory
                if (!continueProcessing)
                {
                    break;
                }

                final String dataTriggerName = entry.getName();
                log.info(procName + "Found trigger file: " + dataTriggerName);

                final String dataName = dataTriggerName.substring(0,
                        dataTriggerName.lastIndexOf(triggerToken));
                final File dataFile = new File(
                        (new StringBuilder(monDirAbsolutePath)
                                .append(File.separator).append(dataName))
                                .toString());

                // verify associated data file exists
                if (!dataFile.exists())
                {
                    log.error(new StringBuilder(procName)
                            .append("Expected data file (")
                            .append(dataFile.getAbsolutePath())
                            .append(") ")
                            .append("However, it doesn't exist. - NOT able to process data file: ")
                            .append(dataName));

                    // remove the trigger file
                    rmFile(entry, false);
                } else
                {

                    /**
                     * Wait before submitting commands to prevent
                     * duplicate session insertion into the.
                     * Use sleep constant
                     */
                     try
					{
						Thread.sleep(HALF_SECOND);
					} catch (final InterruptedException e)
					{
						log.debug("DownlinkInboxMonitor.processDataFiles sleep was interrupted. Reason: " + e.getMessage());
					}

					log.info(procName + "Processing data file:" + dataName);

					final String cmd = buildDwnlinkCmd(dataName);
					if (submitCmd(cmd) != CmdResults.CMD_NO_ERR)
					{
						/**
                         * stop processing if
						 * command has errors
						 */
						log.error(procName + "Error while trying to process " + dataTriggerName
								+ " (see above). No further " + "monitoring will be done for this inbox.");
						continueProcessing = false;
						return;
					}

					// remove the trigger file
					rmFile(entry, false);

					// remove the data file
					rmFile(dataFile, false);
                }

            }
        }
        
        /**
         * Launch the chill_down command via ProcessLauncher
         * 
         * @see jpl.gds.shared.process.ProcessLauncher
         * @param cmd
         * @return status of the launcher or local error
         */
        private CmdResults submitCmd(final String cmd)
        {

        	try
        	{
    			if (cmd == null)
    			{
    				// error - no cmd to submit
    				log.error(procName + "There is no command to submit to the downlink processor");
    				return CmdResults.CMD_ERR_NO_CMD;
    			}

    			// Removed unused/unnecessary "dev/debug" code that was commented out

    			log.info(new StringBuilder(procName).append("Submitting Command: ").append(cmd));

    			setupCmdLog();

    			final ProcessLauncher launcher = new ProcessLauncher();
    			launcher.setOutputHandler(new StdoutCapture(cmdLogPrinter));
    			launcher.setErrorHandler(new StderrCapture(cmdLogPrinter));
    			try
    			{
                    launcher.launch(cmd);
    			} catch (final IOException e)
    			{
    				log.error(procName + "Error launching command: " + e);
    				// **** throw here ?
    				return CmdResults.CMD_ERR_LAUNCH;
    			}

    			int stat = 1; // init to error condition
    			try
    			{
    				stat = launcher.waitForExit();
    			} catch (final Exception e)
    			{
    				log.error(procName + "Error waiting for command completion: " + e);

    				return CmdResults.CMD_ERR_LAUNCH;
    			}

    			/**
    			 * Stop monitoring if command has
    			 * errors
    			 */
    			if (stat == 1)
    			{
    				log.error(new StringBuilder(procName).append(" Error launching chill_down: ").append(ErrorCode.get(stat).toString()));
    				return CmdResults.CMD_ERR_LAUNCH;
    			}

    			/* Print correct error code */
    			final String terminalMsg = new StringBuilder(procName).append("Chill_down finished with the following status: ").append(ErrorCode.get(stat)).toString();
    			
    			if(stat == 0)
    			{
    				log.info(terminalMsg);
    			} else
    			{
    				log.warn(terminalMsg);
    			}

    			return CmdResults.CMD_NO_ERR;
        	}
        	finally
        	{
        		closeCmdLog();
        	}
        }
        
        /**
         * create a local chill-down log file
         */
        private void setupCmdLog()
        {
            final String cmdLogName = (new StringBuilder(procName.substring(0,
                    procName.length() - 13)).append('.').append(
                    System.currentTimeMillis()).append(".chill.log")).toString();
            final String cmdLogPath = (new StringBuilder(config.getLoggerDir())
                    .append(File.separator)).toString();

            // by this point in the process, the logging directory already exists
            cmdLogFile = new File(cmdLogPath + cmdLogName);
            try
            {
            	cmdLogWriter = new FileWriter(cmdLogFile, true);
                cmdLogPrinter = new PrintWriter(cmdLogWriter);
            } catch (final IOException e)
            {
            	cmdLogWriter = null;
                cmdLogPrinter = null;
                log.error(procName + "Error creating chill-down cmd logger: " + e);
            }
        }

        /**
         * flush/close the locally created chill-down log file
         */
        private void closeCmdLog()
        {
        	if(cmdLogWriter != null)
        	{
        		try
        		{
					cmdLogWriter.close();
				} catch (final IOException e)
        		{
					// don't care, just closing up things.
				}
        	}
            if (cmdLogPrinter != null)
            {
                log.info(procName + "CHILL-DOWN log file for " + procName
                        + " is located at: " + cmdLogFile.getAbsolutePath());
                cmdLogPrinter.close();
            } else
            {
                log.info(procName
                        + "CHILL-DOWN log file for "
                        + procName
                        + " was not created.  Chill-down information (if any) printed at command line.");
            }
        }
        
        /**
         * Create the chill_down command line with the appropriate parameters
         * 
         * @param name
         *            -of-data-file
         * @return cmd-line
         */
        private String buildDwnlinkCmd(final String dataName)
        {
            final String res = config.getChillDownParams();
            final StringBuilder cmd = new StringBuilder(config.getGdsDirectory())
                    .append(File.separator).append("bin").append(File.separator)
                    .append("chill_down ");

            if (res != null)
            {
                cmd.append(res.replaceAll(PARAM_DATA_REPLACE_TOKEN, new StringBuilder(
                        monDirAbsolutePath).append(File.separator).append(dataName)
                        .toString()));
            }

            return cmd.toString();
        }

        /**
         * A local 'nice' feature
         */
        private boolean nap()
        {
            boolean interruptedState = false;

            if (!continueProcessing)
            {
                interruptedState = true;
            } else if (msTriggerWait != -1l)
            {
                interruptedState = jpl.gds.shared.thread.SleepUtilities
                        .checkedSleep(msTriggerWait);
            }

            return interruptedState;
        }

    } // end private class for MonitorAndProcessSafs

    /**
     * Util: Remove a file and post results as appropriate
     * 
     * @param file
     *            -object
     */
    private void rmFile(final File fObj, final boolean returning)
    {
    	final String fromDir = ") from directory: ";
        if (fObj.delete())
        {
            log.info(procName + "Deleting file: " + fObj.getName());
        } else
        {
            log.error(new StringBuilder(procName)
                    .append("Unable to delete file (")
                    .append(fObj.getName() + fromDir)
                    .append(fObj.getAbsolutePath()));

            if (returning) // => already tried to delete a second time:
                           // post failure & term this thread
            {
                // a 'fatal' condition and stop processing
                log.error(new StringBuilder(procName).append(" *** SECOND try Failure to delete file (")
                        .append(fObj.getName() + fromDir).append(fObj.getAbsolutePath()).append("***"));
                shutdown();
            } else
            // (!returning) => this is the first failure to delete the file
            {
                log.error(new StringBuilder(procName)
                        .append(" ***TRYING ONLY 1 MORE TIME to delete (")
                        .append(fObj.getName() + fromDir)
                        .append(fObj.getAbsolutePath()).append("***"));

                jpl.gds.shared.thread.SleepUtilities.checkedSleep(FIVE_SECONDS);
                rmFile(fObj, true); // only try this 1 more time
            }
        }
    }
    
    private class TriggerFilesComparator implements Comparator<File>
    {
    	@Override
		public int compare(final File f1, final File f2)
        {
            // sorts: oldest first
            return (int) (f1.lastModified() - f2.lastModified());
        }
	}

    /**
     * class that attaches to stdout of chill-down and logs the information
     */
    private class StdoutCapture extends StdoutLineHandler
    {
        private final PrintWriter logStdout;

        /**
         * @param log
         *            file to direct stdout to
         */
        public StdoutCapture(final PrintWriter log)
        {
            logStdout = log;
        }

        @Override
        public void handleLine(final String line) throws IOException
        {
            if (logStdout != null)
            {
                logStdout.println("chill-down stdout: " + line);
                
                // Flush log to write content
                // right away
                logStdout.flush();
            } else
            {
                log.info("chill-down stdout:" + line);
            }
        }
    } // eoc StdoutCapture

    /**
     * class that attaches to stderr of chill-down and logs the information
     */
    private class StderrCapture extends StdoutLineHandler
    {
        private final PrintWriter logStderr;

        /**
         * @param log
         *            file to direct stderr to
         */
        public StderrCapture(final PrintWriter log)
        {
            logStderr = log;
        }

        @Override
        public void handleLine(final String line) throws IOException
        {
            if (logStderr != null)
            {
                logStderr.println("chill-down *STDERR*: " + line);
                
                // Flush log to write content
                // right away
                logStderr.flush();
            } else
            {
                log.error(" chill-down stdERR:" + line);
            }
        }
    } // eoc StderrCapture

    /**
     * Check configuration of monitor
     * 
     * @return boolean: true => all setup
     */
    public boolean inboxMonitorConfigured()
    {
        return !(monDir == null || triggerToken == null || !continueProcessing);
    }

    /**
     * Gracefully shutdown monitor process
     * 
     */
    public void shutdown()
    {
        continueProcessing = false;
        log.info(procName + "shutting down");

        if (execMonitor != null)
        {
            execMonitor.shutdown();
        }

        if (execHeartBeat != null)
        {
            execHeartBeat.shutdown();
        }
    }

    /**
     * Initiated from MonitorAndProcessData(::run) class thread
     */
    public void heartBeat()
    {
        execHeartBeat = Executors.newSingleThreadExecutor();
        if (execHeartBeat == null)
        {
            log.error(procName, " Not able to create thread.  Cannot generate heartbeat log information.");
            return;
        }
        execHeartBeat.submit(new HeartBeat());
    }

    /**
     * Private class to provide a heart-beat indicator in the log file.
     * Improvement possibility: set processing time in processDataFiles; compare
     * that with lastTimeTag > (1.5 times msHeartbeatWait); true=>waiting for
     * chill-down to finish
     */
    private class HeartBeat implements Runnable
    {

        private long lastTimeTag = 0;

        @Override
        public void run()
        {
        	long tmp;

            while (continueProcessing)
            {
                tmp = System.currentTimeMillis();
                if ((tmp - lastTimeTag) > msHeartbeatWait)
                {
                    lastTimeTag = tmp;

                    log.info(new StringBuilder(procName).append(
                            "heartbeat monitoring for trigger files in :")
                            .append(monDirAbsolutePath));
                }

                /**
                 * Moved this try/catch block outside the if statement
                 * to reduce the number of polls.
                 */
                    try
                    {
						SleepUtilities.fullSleep(FIVE_SECONDS);
					} catch (final ExcessiveInterruptException e)
                    {
						log.debug("DownlinkInboxMonitor.Heartbeat.run() full sleep was interrupted. Reason: " + e.getMessage());
					}

            }

        }

    } // end private class for HearBeat monitor

    @Override
    public void exitCleanly() {
        try {
            log.info(procName + "Recieved shutdown request.");
            shutdown();
        } catch (final Exception e) {
            log.error(procName + " Exception happened while trying to shutdown DownlinkInboxMonitor:" + e.toString());
        }

    }

} // end of class DownlinkMonitor
