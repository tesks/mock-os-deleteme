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

/**
 * This interface works in conjunction with the ProcessLauncher class to provide the capability for a user of the ProcessLauncher to
 * be notified by a call-back through this interface's handleProcessTermination() method when the launched process terminates, and
 * what its termination exit code is.
 * <p>
 * The notification contains a reference to the ProcessLauncher that has terminated, its exit code, and any exception that was
 * thrown by the process.
 * 
 *
 */
public interface IProcessTerminationHandler {
	/**
	 * If an instance of this interface is provided to the ProcessLauncher when it is instantiated, this method will called
	 * asynchronously by the ProcessLauncher when the process it has launched has terminated.
	 * 
	 * @param launcher
	 *            The instance of ProcessLauncher that has terminated
	 * @param exitCode
	 *            The exit code of the Process that has terminated, 
	 *            or null if the process fails to launch.
	 * @param t
	 *            An exception that was thrown by the Process, or null if none
	 */
	public void handleProcessTermination(ProcessLauncher launcher, Integer exitCode, Throwable t);
}
