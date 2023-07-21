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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Algorithm Manager is a generic class that can be used for instantiating and
 * looking up instances of some GenericAlgorithm type. It is an ideal place to
 * encapsulate information specific to an IGenericAlgorithm subinterface; for
 * example, the key to use for lookup the subinterface's configurations from
 * AlgorithmConfiguration.
 * 
 * AlgorithmManager handles reflection for configured algorithm instances, but
 * may also instantiate multimission algorithm implementations directly, but
 * only if parameters are known outside of the AlgorithmConfig.
 * 
 * When an algorithm instance is initialized for a certain ID, the ID will
 * always reference the same instance. Consequently, an algorithm implementation
 * that keeps state will not be thread safe and could cause problems in multiple
 * processing contexts. For this reason, clients of AlgorithmManager instances
 * should consider creating their own instance and be careful about sharing with
 * other clients.
 * 
 * Classes may extend this or instantiate this class directly. Extension may be ideal
 * if multimission algorithm implementation classes are being used in addition
 * to those configured in an external algorithm configuration file.
 * 
 * @param <T>
 *            any algorithm class that extends GenericAlgorithm
 */
public class AlgorithmManager<T extends IGenericAlgorithm> {

	private final AlgorithmConfig config;
	private final String algorithmType;
	
	/**
	 * Map of algorithms whose invocation has failed 
	 */
	protected final Map<String, String> algorithmFailures = new HashMap<>();
	
	private final Map<String, String> algorithmClassFailures = new HashMap<>();

	private final Class<T> algoInterface;

	/**
	 * Map of algorithms by algorithm ID.
	 */
	protected Map<String, T> algorithmsById = new HashMap<String, T>();

	/**
	 * Create an algorithm manager for the super type indicated by the type
	 * parameter.
	 * 
	 * @param algoInterface
	 *            the interface that algorithm instances implement
	 * @param algorithmType
	 *            the string identifying the block declaring instances of this
	 *            algorithm in the algorithm config file
	 * @param config
	 *            the algorithm config object to get algorithm definitions from
	 */
	public AlgorithmManager(final Class<T> algoInterface, final String algorithmType, final AlgorithmConfig config) {
		this.algoInterface = algoInterface;
		this.algorithmType = algorithmType;
		this.config = config;
	}

	/**
	 * Instantiates an instance of the class whose name is passed in. Does not
	 * throw, but caches failures which can later be looked up.
	 * 
	 * @param algoClassName
	 *            the fully qualified name of the algorithm class to instantiate
	 * @return an empty Optional there was a problem instantiating the
	 *         algorithm, or an Optional containing the algorithm instance
	 */
	private Optional<T> createInstance(final String algoClassName) {
		if (algorithmClassFailures.containsKey(algoClassName)) {
			return Optional.empty();
		}

		Class<?> algoClass = null;
		try {
			algoClass = Class.forName(algoClassName);
			if (!algoInterface.isAssignableFrom(algoClass)) {
				algorithmClassFailures.put(algoClassName,
						String.format("Algorithm class %s is not a subtype of algorithm type %s",
								algoClassName,
								algoInterface.getName()
						));
				return Optional.empty();
			}
		} catch (final ClassNotFoundException e) {
			algorithmClassFailures.put(algoClassName, String.format("Algorithm class not found: %s", algoClassName));
			return Optional.empty();
		} catch	(final LinkageError e) {
			algorithmClassFailures.put(algoClassName,
					String.format("Linkage error when laoding algorithm class %s, message: %s",
						algoClassName,
						e.getMessage()
					)
			);
			return Optional.empty();
		}

		Constructor<?> constructor = null; 
		try {
			constructor = algoClass.getConstructor();
		} catch (NoSuchMethodException | SecurityException e) {
			algorithmClassFailures.put(algoClassName, String.format("Could not access default constructor for class %s", algoClassName));;
			return Optional.empty();
		}

		Optional<T> result = Optional.empty();
		try {
			final T instance = algoInterface.cast(constructor.newInstance((Object[]) null));
			result = Optional.of(instance);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | ClassCastException e) {
			algorithmClassFailures.put(algoClassName, String.format("Could not instantiate algorithm class %s, reason: %s", algoClassName,
					e.getMessage()));
			return Optional.empty();
		}
		return result;
	}

	/**
	 * Retrieve an algorithm instance with parameters set according to
	 * known configuration.  If there is any problem, an empty Optional will be returned
	 * and the failure cause can be retrieved by calling {@link #getFailureCauseFor(String)}
	 * @param customAlgoId the Id that can later be used to identify this algorithm instance
	 * @param configuredId the ID of an actual algorithm configuration block. May be the same as customAlgoId
	 * @return Optional containing instantiated, configured algorithm or empty
	 */
	private Optional<T> initializeAlgorithm(final String customAlgoId, final String configuredId) {
		final Map<String, AlgorithmDefinition> algosConfigsById = config.getAlgorithms(this.algorithmType);
		if (algosConfigsById == null) {
			algorithmFailures.put(customAlgoId,
				String.format("No configuration found for algorithm ID %s", configuredId)
			);
			return Optional.empty();
		}

		final AlgorithmDefinition def = algosConfigsById.get(configuredId);
		if (def == null) {
			algorithmFailures.put(configuredId, String.format("No configuration found for algorithm ID %s", configuredId));
			return Optional.empty();
		}

		Optional<T> algoInstance = createInstance(def.getAlgorithmClass());
		if (algoInstance.isPresent()) {
			try {
				algoInstance.get().setStaticArgs(def.getStaticArgs());
			} catch (final RuntimeException e) {
				// Catch exceptions that may occur because of specific parameters
				// There's no way to know if the class itself is bad, so cache the
				// failure in the algorithm instance failure map instead of the algorithm
				// class failure map
				algorithmFailures.put(customAlgoId,
						String.format("Method %s#setStaticArgs(String, Object) failed: %s", def.getAlgorithmClass(), e.getCause())
				);
				algoInstance = Optional.empty();
			}
		}
		return algoInstance; 
		
	}

	/**
	 * Retrieve an algorithm instance with parameters set according to
	 * known configuration.  If there is any problem, an empty Optional will be returned
	 * and the failure cause can be retrieved by calling {@link #getFailureCauseFor(String)}
	 * 
	 * Each algorithm ID corresponds to one instance, so if this method has been called
	 * previously, a cached instance is returned.
	 * @param algoId the ID identifying the algorithm instance to retrieve
	 * @return Optional containing instantiated, configured algorithm or empty
	 */
	public Optional<T> getAlgorithm(final String algoId) {
		return getAlgorithm(algoId, algoId);
	}

	/**
	 * Get an algorithm instance. Allows aliasing of algorithm configurations.
	 * This method enables instantiating multiple instances of the same algorithm with different
	 * aliases.
	 * @param customAlgoId The ID used to alias to a configured algorithm ID
	 * May match the configured algorithm ID, if aliases are not being used.
	 * @param configuredAlgoId the algorithm ID as it actually appears in the algorithm config
	 * @return Optional containing instantiated, configured algorithm or empty
	 */
	protected Optional<T> getAlgorithm(final String customAlgoId, final String configuredAlgoId) {
		if (algorithmsById.containsKey(customAlgoId)) {
			return Optional.of(algorithmsById.get(customAlgoId));
		}
		if (algorithmFailures.containsKey(customAlgoId) || algorithmFailures.containsKey(configuredAlgoId)) {
			return Optional.empty();
		}

		final Optional<T> algoInstance = initializeAlgorithm(customAlgoId, configuredAlgoId);
		if (algoInstance.isPresent()) {
			algorithmsById.putIfAbsent(customAlgoId, algoInstance.get());
		}
		return algoInstance;
	}
	
	/**
	 * Retrieve information regarding the failure of some algorithm.
	 * Appropriate for error logging.
	 * @param algoId the algorithm ID to look up failure information for
	 * @return the failure reason or an empty string
	 */
	public String getFailureCauseFor(final String algoId) {
		String algoFailureCause = algorithmFailures.get(algoId);
		if (algoFailureCause == null) {
			final AlgorithmDefinition def = config.getAlgorithms(this.algorithmType).get(algoId);
			if (def != null) {
				algoFailureCause = algorithmClassFailures.getOrDefault(def.getAlgorithmClass(), "");
			} else {
				algoFailureCause = "";
			}
		}
		return algoFailureCause;
	}
	
}
