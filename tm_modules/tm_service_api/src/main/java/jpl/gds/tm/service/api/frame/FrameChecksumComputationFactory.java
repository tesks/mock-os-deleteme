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

package jpl.gds.tm.service.api.frame;

import java.util.HashMap;
import java.util.Map;

import jpl.gds.dictionary.api.config.IFrameFormatDefinition;
import jpl.gds.shared.checksum.IChecksumCalculator;
import jpl.gds.shared.log.TraceManager;



/**
 * FrameChecksumComputationFactory is used to create mission-specific instances
 * of classes that implement IFrameChecksumComputation.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 * Instances of IFrameChecksumComputation are used by the telemetry processing
 * system to check for errors in frames. An appropriate
 * IFrameChecksumComputation object must be used for each mission, since frame
 * formats may differ. IFrameChecksumComputation objects should only be created
 * via the FrameChecksumComputationFactory. Direct creation of an
 * IFrameChecksumComputation object is a violation of multi-mission development
 * standards.
 * <p>
 * This class contains only static methods. The class of the actual object
 * instantiated by this factory is dictated by the supplied FrameFormat object.
 * <p>
 * 
 * @version 1.0 - Initial Implementation
 * @version 2.0 - No longer uses GDS configuration to determine checksum class;
 *          uses FrameFormat from the frame dictionary. ( MPCS-7993 - 3/30/16)
 * @version 2.1 - Checksum classes are cached after their first reflection call.
 *          Subsequent calls retrieve it and an instance is returned.
 *          (MPCS-8107 - 08/12/16)
 * 
 */
public class FrameChecksumComputationFactory {

	// MPCS-8107 08/12/16 - Added. Save some time in creating a class multiple times
	private static Map<String, Class<?>> checksumClasses = new HashMap<String, Class<?>>();
	
	/**
	 * Creates an IFrameChecksumComputation object appropriate for the current
	 * mission.
	 * 
	 * @param frameFormat
	 *            the FrameFormat object for the frame being processed
	 * 
	 * @return IFrameChecksumComputation object
	 * 
	 * MPCS-7993 - 3/30/16. Added frameFormat parameter, reworked method to use it.
	 */
    public static IChecksumCalculator create(IFrameFormatDefinition frameFormat) {
		String className = null;
		try {

			className = frameFormat.getFrameErrorControlClass();
			if ((className == null) || (className.equals(""))) {

				TraceManager.getDefaultTracer().warn("No frame checksum adaptation class is configured.");

			}

			/*
			 * MPCS-8107 - 08/12/16 - Now we cache a class the first time
			 * it's called. We then will return an instance each time create is called
			 */
			if ((className != null) && (!className.equals(""))) {
				/*
				 * Check the map. If the class isn't there, create it through
				 * reflection and store it.
				 */
				Class<?> c = checksumClasses.get(className);

				if (c == null) {
					c = Class.forName(className);
					checksumClasses.put(className, c);
				}

				Object instance = c.newInstance();
            return (IChecksumCalculator) instance;
			}
		} catch (ClassNotFoundException e) {
			TraceManager.getDefaultTracer().warn("Frame checksum adaptation class " + className + " is not found.");

		} catch (InstantiationException e) {
			TraceManager.getDefaultTracer().warn("Frame checksum class '" + className + "' could not be instantiated");

		} catch (IllegalAccessException e) {
			TraceManager.getDefaultTracer().warn("Frame checksum class '" + className

					+ "' could not be instantiated due to illegal access exception");
		} catch (ClassCastException e) {
			TraceManager.getDefaultTracer().warn("Frame checksum class '" + className

					+ "' is not compatable with the FrameChecksumComputation interface");
		}
		return null;
	}
}
