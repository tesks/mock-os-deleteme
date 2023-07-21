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
package jpl.gds.shared.process;

import java.io.File;
import java.io.IOException;

import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.thread.SleepUtilities;

/**
 *
 * The ProcessLauncher class has been enhanced with the following properties:
 * 
 * <ul>
 * <li>Automatically closes all StdErr and StdOut streams upon process
 * termination. It is no longer the responsibility of the caller to manage this.
 * </li>
 * <li>Total encapsulation of Process object. It is no longer visible by
 * callers.</li>
 * <li>Provides the capability of providing an instance of
 * IProcessTerminationHandler interface. This may be used to perform any
 * caller-specific clean-up upon process termination.</li>
 * <li>In the event that a LineHandler is not specified, previous code did not
 * give stdin and stderr any data sink into which to write their output streams.
 * As a result, these writers would eventually block when the output buffer was
 * full, causing intermittent hangs of the spawned process.
 * <p>
 * The ProcessLauncher now provides a BIT_BUCKET output stream for such
 * circumstances that will drain the output streams and throw away the data.</li>
 * </ul
 * 
 * 
 * Executes a process and starts threads to read output and error streams from
 * the running process. If a caller is interested in reading the output or error
 * streams, it must provide an object implementing LineHandler for each stream
 * that it is interested in. The LineHandler's handleLine() method will be
 * called for each line of text received from the process's stream.
 * <p>
 * For example:
 * 
 * <pre>
 * String[] command = { &quot;/bin/echo&quot;, &quot;ok&quot; };
 * ProcessLauncher launcher = new ProcessLauncher();
 * launcher.setOutputHandler(new StdoutLineHandler());
 * launcher.setErrorHandler(new StderrLineHandler());
 * try {
 * 	launcher.launch(command);
 * }
 * catch (IOException e) {
 * 	log.error(&quot;Error running process&quot;, e);
 * 	return;
 * }
 * int exitValue = launcher.waitForExit();
 * </pre>
 * 
 * The output and error streams can be ignored by calling launch() without first
 * calling the setOutputHandler() and setErrorHandler() methods.
 * <p>
 * Simple classes StdoutLineHandler and StderrLineHandler are provided to print
 * lines to stdout and stderr.
 * 
 * Added sync versions of all public
 * ProcessLauncher launch() method patterns. These methods will have the same
 * characteristics of their async versions, except they will block until the
 * launched process completes, and will return its status code.
 * 
 * 
 * @see jpl.gds.shared.process.LineHandler
 * @see jpl.gds.shared.process.BufferLineHandler
 * @see jpl.gds.shared.process.StdoutLineHandler
 * @see jpl.gds.shared.process.StderrLineHandler
 * @see jpl.gds.shared.process.IProcessTerminationHandler
 */
public class ProcessLauncher {
	/** Logger for ProcessLauncher output and debug */
    private static final Tracer      log                          = TraceManager.getTracer(Loggers.UTIL);

	/**
	 * Named ThreadGroup for all threads that are waiting on processes to
	 * terminate
	 */
	private static final ThreadGroup PROCESS_WAITER_THREAD_GROUP = new ThreadGroup("ProcessWaiterThreadGroup");

	/**
	 * Time to pause (in milliseconds) after launching a process before
	 * continuing.
	 */
	private static final int DELAY_AFTER_PROCESS_LAUNCH = 1000;

	/** Time to pause (in milliseconds) after destruction of the Process */
	private static final long WAIT_TIME_TO_DESTROY_PROCESS = 5000L;

	/** Static process name when the process name is not known */
	private static final String UNKNOWN = "<UNKNOWN>";

	/** Data output sinc for spawned processes' stderr and stdout streams */
	private static final LineHandler BIT_BUCKET = new LineHandler() {
		@Override
		public void handleLine(final String line) throws IOException {
			// Do nothing -- throw it all away!
		}
	};

	/** Counter of open processes (mostly used for debug) */
	private static int openProcesses = 0;

	/** The internal Process object of the launched process */
	private Process process;

	/** The originally supplied command line for the launched process */
	private String commandLine = null;

	/** Thread and run() method for reading launched process' stdout stream */
	private LineReaderThread outputReader;

	/** Thread and run() method for reading launched process' stdout stream */
	private LineReaderThread errorReader;

	/** Internal holder of the launched process' stdout stream */
	private LineHandler outputHandler = BIT_BUCKET;

	/** Internal holder of the launched process' stderr stream */
	private LineHandler errorHandler = BIT_BUCKET;

	/** Thread that is waiting on the launched Process' termination */
	private Thread launcherThread = null;

	/** The exception (if any) generated by the launched process */
	private volatile IOException exception = null;

	/**
	 * Boolean flag indicating whether the ProcessLauncher has launched the
	 * process
	 */
	private volatile boolean started = false;

	/**
	 * Boolean flag indicating whether the ProcessLauncher's Process has
	 * completed (terminated)
	 */
	private volatile boolean complete = false;

	/**
	 * Exit value is null until set by process' exit value
	 */
	private volatile Integer exitValue = null;

	/**
	 * No-Arg Constructor
	 */
	public ProcessLauncher() {
	}

	/**
	 * Sets an object that will handle lines from stdout of the process
	 * 
	 * @param h
	 *            the LineHandler to be used for stdout output from the launched
	 *            process
	 */
	public void setOutputHandler(final LineHandler h) {
		outputHandler = (null == h) ? BIT_BUCKET : h;
	}

	/**
	 * Sets an object that will handle lines from stderr of the process
	 * 
	 * @param h
	 *            the LineHandler to be used for stderr output from the launched
	 *            process
	 */
	public void setErrorHandler(final LineHandler h) {
		errorHandler = (null == h) ? BIT_BUCKET : h;
	}

	/**
     *  Execute the provided command line
     * synchronously, i.e. wait for process completion before returning
     * 
     * @param commandLine
     *            The command line for the spawned process in an argv style
     *            array
     * @param workingDir
     *            The current working directory to set on behalf of the started
     *            process
     * @return the process' exit code
     * 
     * @throws IOException
     *             Exception occurred launching process
     */
    public synchronized int launchSync(final String[] commandLine, final String workingDir) throws IOException {
        return launch(commandLine, workingDir) ? waitForExit() : -1;
	}

	/**
     * Execute the provided command line asynchronously, i.e. return immediately
     * 
     * @param commandLine
     *            The command line for the spawned process in an argv style
     *            array
     * @param workingDir
     *            The current working directory to set on behalf of the started
     *            process
     * @return true if process launches successfully<br>
     *         false if process fails to launch
     * @throws IOException
     *             Exception occurred launching process
     */
    public boolean launch(final String[] commandLine, final String workingDir) throws IOException {
        return launch(commandLine, workingDir, (IProcessTerminationHandler) null);
	}

	/**
     * Execute the provided command line
     * synchronously, i.e. wait for process completion before returning
     * 
     * @param commandLine
     *            The command line for the spawned process in an argv style
     *            array
     * @param workingDir
     *            The current working directory to set on behalf of the started
     *            process
     * @param handler
     *            An IProcessTerminationHandler to be called when the process
     *            terminates.
     * @return the process' exit code
     * 
     * @throws IOException
     *             Exception occurred launching process
     */
    public synchronized int launchSync(final String[] commandLine, final String workingDir,
                                       final IProcessTerminationHandler handler)
            throws IOException {
        return launch(commandLine, workingDir, handler) ? waitForExit() : -1;
	}

	/**
     * Execute the provided command line asynchronously, i.e. return immediately
     * 
     * @param commandLine
     *            The command line for the spawned process in an argv style
     *            array
     * @param workingDir
     *            The current working directory to set on behalf of the started
     *            process
     * @param handler
     *            An IProcessTerminationHandler to be called when the process
     *            terminates.
     * @return true if the process was started, false if not
     * @throws IOException
     *             Exception occurred launching process
     */
    public boolean launch(final String[] commandLine, final String workingDir, final IProcessTerminationHandler handler)
            throws IOException {
		if (started) {
			if (log.isDebugEnabled()) {
				log.debug(">>>>> ALREADY STARTED: " + this);
				Thread.dumpStack();
			}
			return false;
		}

		launcherThread = new Thread(PROCESS_WAITER_THREAD_GROUP, getProcessName(commandLine)) {
			@Override
			public void run() {
				try {
                    if (internalLaunch(commandLine, workingDir)) {
						internalWaitForExit();
					}
					else if (log.isDebugEnabled()) {
						log.debug("Failed to launch: " + this);
						Thread.dumpStack();
					}
					if (null != handler) {
						handler.handleProcessTermination(ProcessLauncher.this, exitValue, null);
					}
				}
				catch (final IOException e) {
					exception = e;
					if (null != handler) {
						handler.handleProcessTermination(ProcessLauncher.this, exitValue, e);
					}
				}
				finally {
					cleanUp();
				}
			}
		};
		launcherThread.setDaemon(false);
		launcherThread.start();
		synchronized (this) {
			try {
				wait(DELAY_AFTER_PROCESS_LAUNCH);
			}
			catch (final InterruptedException e) {
                log.warn("Timed-out waiting for process to spawn: " + this, e);
			}
		}
		if (null != exception) {
			throw exception;
		}
		return started;
	}

	/**
     * Starts the process, where command and args are passed in an array
     * 
     * @param commandLine
     *            The command line for the spawned process in an argv style
     *            array
     * @param workingDir
     *            The current working directory to set on behalf of the started
     *            process
     * @return true if process launches successfully<br>
     *         false if process fails to launch
     * @throws IOException
     *             Exception occurred launching process
     */
    private boolean internalLaunch(final String[] commandLine, final String workingDir) throws IOException {
		final StringBuilder sb = new StringBuilder();
		for (final String s : commandLine) {
			sb.append(s);
			sb.append(' ');
		}
		this.commandLine = sb.toString();

		final Runtime runtime = Runtime.getRuntime();

		final File dir = new File(workingDir);
		if (!dir.exists()) {
			throw new IOException("ProcessLauncher: Specified working directory does not exist: \n" + workingDir);
		}

		if (runtime == null) {
			log.error("Runtime object is null in process launcher: " + this);
			return false;
		}
		process = runtime.exec(commandLine, null, dir);

        outputReader = new LineReaderThread(process.getInputStream());
		outputReader.setLineHandler(outputHandler);
		outputReader.start();

        errorReader = new LineReaderThread(process.getErrorStream());
		errorReader.setLineHandler(errorHandler);
		errorReader.start();

		setStarted();
		return true;
	}

	/**
     * Execute the provided command line
     * synchronously, i.e. wait for process completion before returning
     * 
     * @param commandLine
     *            The command line for the spawned process in an argv style
     *            array
     * @return the process' exit code
     * 
     * @throws IOException
     *             Exception occurred launching process
     */
    public synchronized int launchSync(final String[] commandLine) throws IOException {
        return launch(commandLine) ? waitForExit() : -1;
	}

	/**
     * Execute the provided command line asynchronously, i.e. return immediately
     * 
     * @param commandLine
     *            The command line for the spawned process in an argv style
     *            array
     * @return true if process launches successfully<br>
     *         false if process fails to launch
     * @throws IOException
     *             Exception occurred launching process
     */
    public boolean launch(final String[] commandLine) throws IOException {
        return launch(commandLine, (IProcessTerminationHandler) null);
	}

	/**
     * Execute the provided command line
     * synchronously, i.e. wait for process completion before returning
     * 
     * @param commandLine
     *            The command line for the spawned process in an argv style
     *            array
     * @param handler
     *            handler to call when the process terminates
     * @return the process' exit code
     * 
     * @throws IOException
     *             Exception occurred launching process
     */
    public synchronized int launchSync(final String[] commandLine, final IProcessTerminationHandler handler)
            throws IOException {
        return launch(commandLine, handler) ? waitForExit() : -1;
	}

	/**
     * Execute the provided command line asynchronously, i.e. return immediately
     * 
     * @param commandLine
     *            The command line for the spawned process in an argv style
     *            array
     * @param handler
     *            An IProcessTerminationHandler to be called when the process
     *            terminates.
     * @return true if process launches successfully<br>
     *         false if process fails to launch
     * @throws IOException
     *             Exception occurred launching process
     */
    public boolean launch(final String[] commandLine, final IProcessTerminationHandler handler) throws IOException {
		if (started) {
			if (log.isDebugEnabled()) {
				log.debug(">>>>> ALREADY STARTED: " + this);
				Thread.dumpStack();
			}
			return false;
		}

		launcherThread = new Thread(PROCESS_WAITER_THREAD_GROUP, getProcessName(commandLine)) {
			@Override
			public void run() {
				try {
                    if (internalLaunch(commandLine)) {
						internalWaitForExit();
					}
					else if (log.isDebugEnabled()) {
						log.debug("Failed to launch: " + this);
						Thread.dumpStack();
					}
					if (null != handler) {
						handler.handleProcessTermination(ProcessLauncher.this, exitValue, null);
					}
				}
				catch (final IOException e) {
					exception = e;
					if (null != handler) {
						handler.handleProcessTermination(ProcessLauncher.this, exitValue, e);
					}
				}
				finally {
					cleanUp();
				}
			}
		};
		launcherThread.setDaemon(false);
		launcherThread.start();
		synchronized (this) {
			try {
				wait(DELAY_AFTER_PROCESS_LAUNCH);
			}
			catch (final InterruptedException e) {
				log.warn("Timed-out waiting for process to spawn: " + this, e);
			}
		}
		if (null != exception) {
			throw exception;
		}
		return started;
	}

	/**
     * Starts the process, where command and args are passed in an array
     * 
     * @param commandLine
     *            The command line for the spawned process in an argv style
     *            array
     * @param trace
     *            The application context tracer
     * @return true if process launches successfully<br>
     *         false if process fails to launch
     * @throws IOException
     *             Exception occurred launching process
     */
    private boolean internalLaunch(final String[] commandLine) throws IOException {
		final StringBuilder sb = new StringBuilder();
		for (final String s : commandLine) {
			sb.append(s);
			sb.append(' ');
		}
		this.commandLine = sb.toString();

		final Runtime runtime = Runtime.getRuntime();
		if (runtime == null) {
			log.error("Runtime object is null in process launcher: " + this);
			return false;
		}
		process = runtime.exec(commandLine);

        outputReader = new LineReaderThread(process.getInputStream());
		outputReader.setLineHandler(outputHandler);
		outputReader.start();

        errorReader = new LineReaderThread(process.getErrorStream());
		errorReader.setLineHandler(errorHandler);
		errorReader.start();

		setStarted();
		return true;
	}

	/**
     * Execute the provided command line
     * synchronously, i.e. wait for process completion before returning
     * 
     * @param commandLine
     *            The command line for the spawned process as a String
     * @return process' error code
     * @throws IOException
     *             Exception occurred launching process
     */
    public synchronized int launchSync(final String commandLine) throws IOException {
        return launch(commandLine, (IProcessTerminationHandler) null) ? waitForExit() : -1;
	}

	/**
     * Execute the provided command line asynchronously, i.e. return immediately
     * 
     * @param commandLine
     *            The command line for the spawned process as a String
     * @return true if process launches successfully<br>
     *         false if process fails to launch
     * @throws IOException
     *             Exception occurred launching process
     */
    public boolean launch(final String commandLine) throws IOException {
        return launch(commandLine, (IProcessTerminationHandler) null);
	}

	/**
     * Execute the provided command line
     * synchronously, i.e. wait for process completion before returning
     * 
     * @param commandLine
     *            The command line for the spawned process as a String
     * @param handler
     *            An IProcessTerminationHandler to be called when the process
     *            terminates.
     * @return process' error code
     * @throws IOException
     *             Exception occurred launching process
     */
    public synchronized int launchSync(final String commandLine, final IProcessTerminationHandler handler)
            throws IOException {
        return launch(commandLine, handler) ? waitForExit() : -1;
	}

	/**
     * Execute the provided command line asynchronously, i.e. return immediately
     * 
     * @param commandLine
     *            The command line for the spawned process as a String
     * @param handler
     *            An IProcessTerminationHandler to be called when the process
     *            terminates.
     * @return true if process launches successfully<br>
     *         false if process fails to launch
     * @throws IOException
     *             Exception occurred launching process
     */
    public boolean launch(final String commandLine, final IProcessTerminationHandler handler) throws IOException {
		if (started) {
			if (log.isDebugEnabled()) {
				log.debug(">>>>> ALREADY STARTED: " + this);
				Thread.dumpStack();
			}
			return false;
		}

		launcherThread = new Thread(PROCESS_WAITER_THREAD_GROUP, getProcessName(commandLine)) {
			@Override
			public void run() {
				try {
                    if (internalLaunch(commandLine)) {
						internalWaitForExit();
					}
					else if (log.isDebugEnabled()) {
						log.debug("Failed to launch: " + this);
						Thread.dumpStack();
					}
					if (null != handler) {
						handler.handleProcessTermination(ProcessLauncher.this, exitValue, null);
					}
				}
				catch (final IOException e) {
					exception = e;
					if (null != handler) {
						handler.handleProcessTermination(ProcessLauncher.this, exitValue, e);
					}
				}
				finally {
					cleanUp();
				}
			}
		};
		launcherThread.setDaemon(false);
		launcherThread.start();
		synchronized (this) {
			try {
				wait(DELAY_AFTER_PROCESS_LAUNCH);
			}
			catch (final InterruptedException e) {
                TraceManager.getDefaultTracer().warn("Timed-out waiting for process to spawn: " + this, e);
			}
		}
		if (null != exception) {
			throw exception;
		}
		return started;
	}

	/**
     * Starts the process, where command is passed in as one string
     * 
     * @param commandLine
     *            The command line for the spawned process as a String
     * @return true if process launches successfully<br>
     *         false if process fails to launch
     * @throws IOException
     *             Exception occurred launching process
     */
    private boolean internalLaunch(final String commandLine) throws IOException {
		this.commandLine = commandLine;
		final Runtime runtime = Runtime.getRuntime();

		if (runtime == null) {
			log.error("Runtime object is null in process launcher: " + this);
			return false;
		}
		process = runtime.exec(commandLine);

        outputReader = new LineReaderThread(process.getInputStream());
		outputReader.setLineHandler(outputHandler);
		outputReader.start();

        errorReader = new LineReaderThread(process.getErrorStream());
		errorReader.setLineHandler(errorHandler);
		errorReader.start();

		setStarted();
		return true;
	}

	/**
	 * Blocks execution until the launched process has terminated.
	 * 
	 * @return process' exit code
	 */
	public int waitForExit() {
		try {
			log.debug(">>>>>> waitForExit JOIN   : " + this);
			join();
			log.debug(">>>>>> waitForExit RELEASE: " + this);
		}
		catch (final ExcessiveInterruptException e) {
			e.printStackTrace();
		}
		return exitValue;
	}

	/**
	 * Blocks execution until the launched process has terminated.
	 */
	private void internalWaitForExit() {
		if (started && !complete && (process != null)) {
			try {
				log.debug(">>>>>> WAITING: " + this);
				exitValue = SleepUtilities.fullWaitFor(process).getStatus();
				log.debug("=====> exitValue=" + exitValue);
				complete = true;
				openProcesses--;
				log.debug(">>>>>> COMPLETED: " + this);
			}
			catch (final ExcessiveInterruptException eie) {
                TraceManager.getDefaultTracer().error("ProcessLauncher.waitForExit Error waiting: " + this + ": "
                        + ExceptionTools.rollUpMessages(eie));
			}
			finally {
				if (outputReader != null) {
					outputReader.waitFor();
				}

				if (errorReader != null) {
					errorReader.waitFor();
				}
			}
		}
	}

	/**
	 * Set process started flag, increment open
	 * process counter, and notifyAll().
	 */
	private synchronized void setStarted() {
		started = true;
		notifyAll();
		openProcesses++;
		log.debug(">>>>>> STARTED: " + this);
	}

	/**
	 * Alias for cleanUp().
	 */
	public void destroy() {
		cleanUp();
	}

	/**
	 * Kills the process and stops the reader threads.
	 */
	private void cleanUp() {
		if (outputReader != null) {
			outputReader.stopReading();
			outputReader = null;
		}
		else if (null != process) {
			try {
				process.getOutputStream().close();
			}
			catch (final IOException e) {
				// don't care
			}
		}

		if (errorReader != null) {
			errorReader.stopReading();
			errorReader = null;
		}
		else if (null != process) {
			try {
				process.getErrorStream().close();
			}
			catch (final IOException e) {
				// don't care
			}
		}

		if (process != null) {
			try {
				process.getInputStream().close();
			}
			catch (final IOException e) {
				// don't care
			}
			process.destroy();
			try {
				join(WAIT_TIME_TO_DESTROY_PROCESS);
			}
			catch (final ExcessiveInterruptException e) {
                log.warn("Timed-out waiting for process to die: " + this, e);
			}
			process = null;
		}
	}

	/**
     * Synchronously execute a command in the simplest possible way:
     * <p>
     * The command is a string array; no output is expected. The return code is
     * returned.
     * 
     * @param cmd
     *            Command to be executed
     * @return Status of command
     * 
     * @throws IOException
     *             Exception occurred launching process
     */
    public static int launchSimple(final String[] cmd) throws IOException {
        return launchSimple(cmd, null);
	}

	/**
     * Synchronously execute a command in the simplest possible way:
     * <p>
     * The command is a string array; no output is expected. The return code is
     * returned.
     * 
     * @param cmd
     *            Command to be executed as an argv[] style array
     * @param handler
     *            Handler for post process processing, or null for no handler.
     * @return Status of command
     * 
     * @throws IOException
     *             Exception occurred launching process
     */
    public static int launchSimple(final String[] cmd, final IProcessTerminationHandler handler) throws IOException {
		final ProcessLauncher launcher = new ProcessLauncher();
		log.debug(">>>>>> SIMPLE LAUNCH: " + launcher);
        launcher.launch(cmd, handler);
		final int retVal = launcher.waitForExit();
		log.debug(">>>>>> SIMPLE COMPLETED: " + launcher);
		return retVal;
	}

	/**
	 * Blocks execution on the calling thread until the launched process has
	 * terminated.
	 * 
	 * @throws ExcessiveInterruptException
	 */
	public void join() throws ExcessiveInterruptException {
		// Do not try and join with own thread
		if ((null != launcherThread) && (Thread.currentThread() != launcherThread)) {
			SleepUtilities.fullJoin(launcherThread);
		}
	}

	/**
	 * Blocks execution for a maximum duration of <code>millis</code>
	 * milliseconds on the calling thread until the launched process has
	 * terminated.
	 * @param millis delay time, milliseconds
	 * 
	 * @throws ExcessiveInterruptException
	 */
	public void join(final long millis) throws ExcessiveInterruptException {
		// Do not try and join with own thread
		if ((null != launcherThread) && (Thread.currentThread() != launcherThread)) {
			SleepUtilities.fullJoin(launcherThread, millis);
		}
	}

	/**
	 * Determine if launched process is complete (terminated).
	 * 
	 * @return true if complete, false if not complete.
	 */
	public boolean isComplete() {
		return complete;
	}

	/**
	 * Return the exit code of the launched process.
	 * <p>
	 * This method has the same semantics as that of Process.exitCode(), in that
	 * it will throw an IllegalTreadStateExceptio if the process is still
	 * running.
	 * 
	 * @return the exit value of the terminated launched process.
	 * 
	 * @throws IllegalThreadStateException
	 */
	public int getExitValue() throws IllegalThreadStateException {
		if (null != exitValue) {
			return exitValue.intValue();
		}
		else {
			throw new IllegalThreadStateException("Process has not yet terminated: " + this);
		}
	}

	/**
	 * Internal method to create a descriptive string describing the launched
	 * process and its arguments.
	 * 
	 * @param commandLine
	 *            a String representing the launched process' command line.
	 * 
	 * @return a descriptive string describing the launched process and its
	 *         arguments.
	 */
	private String getProcessName(final String[] commandLine) {
		if (null == commandLine) {
			return UNKNOWN;
		}
		final StringBuilder sb = new StringBuilder();
		for (final String s : commandLine) {
			sb.append(s);
			sb.append(' ');
		}
		return getProcessName(sb.toString());
	}

	/**
	 * Internal method to create a descriptive string describing the launched
	 * process and its arguments.
	 * 
	 * @param commandLine
	 *            an argv[] style array representing the launched process'
	 *            command line.
	 * 
	 * @return a descriptive string describing the launched process and its
	 *         arguments.
	 */
	private String getProcessName(final String commandLine) {
		if (null == commandLine) {
			return UNKNOWN;
		}
		final String[] temp = commandLine.split(" ")[0].split("/");
		return (temp.length > 0) ? temp[temp.length - 1] : UNKNOWN;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": (" + getProcessName(commandLine) + ") [openProcesses=" + openProcesses + ", started=" + started + ", complete=" + complete + ", exitValue="
				+ ((null == exitValue) ? "N/A" : exitValue.intValue()) + ", outputReader=" + ((null == outputReader) ? "NO" : "YES") + ", errorReader=" + ((null == errorReader) ? "NO" : "YES") + "]";
	}
	
}
