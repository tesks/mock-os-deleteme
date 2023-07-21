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
package jpl.gds.shared.algorithm;

import jpl.gds.shared.config.GdsHierarchicalXml;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;

import java.util.*;

/**
 * This class is a central loader for algorithm definition files.
 * It should be agnostic of the types of algorithms declared within each file;
 * clients of this class need to know the string that identifies the algorithm
 * type they are interested in.  For example, a class that will instantiate
 * sclk extractor algorithms will call getAlgorithmsOfType("sclk_extractor")
 * if sclk_extractor is the key used in the XML config file. This allows
 * all algorithm definitions to be consolidated in a single file type instead
 * of having users configure algorithms across many different files, each consisting
 * of a single algorithm type.
 */
public class AlgorithmConfig extends GdsHierarchicalXml {
    
    private static final String PROPERTY_PREFIX = "algorithm.";
	
	private static final String ALGORITHM_ID_KEY = "[@id]";
	private static final String CLASSNAME_KEY = "java_class";
	
	private static final String FILE_NAME = "algorithms.xml";
	private static final String SCHEMA_NAME = "algorithms.rnc";
	private final Map<String, Map<String, AlgorithmDefinition>> algorithmsByType = new HashMap<>();


	/**
     * Test constructor
     */
	public AlgorithmConfig() {
        this(new SseContextFlag(), TraceManager.getTracer(Loggers.CONFIG));

    }

    /**
     * Default constructor for algorithm config.
     * It will succeed even if no algorithm config file is found.
     * 
     * @param sseFlag
     *            The SSE context flag
     * @param log
     *            Tracer to log with
     */
    public AlgorithmConfig(final SseContextFlag sseFlag, final Tracer log) {
        super(FILE_NAME, SCHEMA_NAME, false, sseFlag);
        for (final ImmutableHierarchicalConfiguration algorithmList : cc.childConfigurationsAt("")) {
            final Map<String, AlgorithmDefinition> algoDefinitionById = new HashMap<>();
            algorithmsByType.put(algorithmList.getRootElementName(), algoDefinitionById);
            for (final ImmutableHierarchicalConfiguration algorithmInstance : algorithmList.immutableChildConfigurationsAt("")) {
                final String id = algorithmInstance.getString(ALGORITHM_ID_KEY, "");
                final String className = algorithmInstance.getString(CLASSNAME_KEY, "");
                final Map<String, Object> staticArgs = new HashMap<>();

                final ImmutableConfiguration argConfig = algorithmInstance.immutableSubset("static_args");
                argConfig.getKeys().forEachRemaining(key -> {
                    // The config has one empty-string key if there are no elements in the immutableSubset.
                    // The map should be empty in this case, so just return
                    if (key.equals(""))
                        return;
                    staticArgs.put(key, argConfig.getProperty(key));
                });

                final List<String> varArgs = algorithmInstance.getList(String.class, "varArgs",
                                                                       Collections.emptyList());
                algoDefinitionById.put(id, new AlgorithmDefinition(id, className, staticArgs, varArgs));
            }
        }
        // Create a static decommutator that will provide parameterized EVR Builder functionally in the decom map
        this.algorithmsByType.putIfAbsent("decommutators", new HashMap<>());
        this.algorithmsByType.get("decommutators")
                             .putIfAbsent("mm-evr-builder",new AlgorithmDefinition("mm-evr-builder",
                                     "jpl.gds.core.decom.service.HybridParameterizedEvrDecom",
                                     new HashMap<>(),
                                     new ArrayList<>()));
	}

	
	/**
	 * Get algorithm definitions for a particular type of algorithm.
	 * The type is some string that the client class knows and expects to match entries
	 * in algorithm config files
	 * @param algorithmType a string expected to match an algorithm type
	 * @return unmodifiable mapping of algorithm IDs to definitions or an empty map if there are no algorithms of the given type
	 */
	public Map<String, AlgorithmDefinition> getAlgorithms(final String algorithmType) {
		return Collections.unmodifiableMap(algorithmsByType.getOrDefault(algorithmType, Collections.emptyMap()));
	}
    
	@Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }
	
	@Override
    public boolean supportsFlatProperties() {
        return false;
    }
    

}
