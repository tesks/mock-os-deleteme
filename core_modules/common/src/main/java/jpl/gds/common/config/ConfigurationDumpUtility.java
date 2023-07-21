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
package jpl.gds.common.config;

import jpl.gds.common.config.gdsdb.DatabaseProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.config.*;
import jpl.gds.shared.config.GdsHierarchicalProperties.PropertySet;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.template.TemplateManager;
import jpl.gds.shared.types.Pair;
import org.reflections.Reflections;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;

/**
 * A utility that assists in locating and displaying configuration properties.
 * 
 */
public class ConfigurationDumpUtility {
    
    /**
     * An enumeration of configuration property output formats.
     *
     */
    public enum PropertyDumpFormat {
        /** XML format */
        XML,
        /** Properties format (name=value) */
        PROPERTIES,
        /** Brief CSV format ("name","value") */
        CSV,
        /** Long CSV format (includes descriptive columns) */
        DOC_CSV
    }
    
    private static final String CONFIG_PATH_PROPERTY = "GdsConfigFullPath";
    private static final String TEMPLATE_PATH_PROPERTY = "GdsTemplateFullPath";
    private static final String UNDEFINED_FILE = "No match";
    private static final String EMPTY_COL = ",\"\"";
    private static final String INTERNAL_ONLY = "INTERNAL ONLY: Customers should not modify this configuration value.";
    private static final String VENUE_TYPE_REGEXP;
    private static final String UPLINK_CONN_REGEXP;
    private static final String DOWNLINK_CONN_REGEXP;
    private static final String STREAM_TYPE_REGEXP;
    private static final String DICT_TYPE_REGEXP;    
    private static final String STATION_NUMBER_REGEXP = "[0-9]+";
    private static final String GLAD_DATA_TYPE_REGEXP = "[0-9]";
    private static final String GENERAL_ALPHANUM_REGEXP = "[0-9a-zA-Z\\-_]+";
    private static final String GENERAL_ALPHA_REGEXP = "[a-zA-Z_]+";
    private static final String GLAD_CONTAINER_REGEXP ="master|host|scid|venue|sessionNumber|vcid|dssId|userDataType|identifier";
    
    private final ApplicationContext appContext;
    
    private final Set<Class<? extends IGdsConfiguration>> propClasses;
    
    private final SseContextFlag                          sseFlag;

    private final Tracer                                  log;


    static {
        VENUE_TYPE_REGEXP = buildRegexFromEnum(VenueType.class);
        UPLINK_CONN_REGEXP = buildRegexFromEnum(UplinkConnectionType.class);
        DOWNLINK_CONN_REGEXP = buildRegexFromEnum(TelemetryConnectionType.class);
        STREAM_TYPE_REGEXP = buildRegexFromEnum(DownlinkStreamType.class);
        DICT_TYPE_REGEXP = buildDictTypeRegexFromEnum();
    }
    
    private static String buildRegexFromEnum(final Class<? extends Enum<?>> c) {
        final StringBuilder tempRegex = new StringBuilder("(");
        boolean first = true;
        for (final Enum<?> v: c.getEnumConstants()) {
            if (!first) {
                tempRegex.append('|');
            } 
            tempRegex.append(v.name());
            first = false;
        }
        tempRegex.append(')');
        return tempRegex.toString();
    }
    
    private static String buildDictTypeRegexFromEnum() {
        final StringBuilder tempRegex = new StringBuilder("(");
        boolean first = true;
        for (final DictionaryType t: DictionaryType.values()) {
            if (!first) {
                tempRegex.append('|');
            } 
            tempRegex.append(t.getDictionaryName());
            first = false;
        }
        tempRegex.append(')');
        return tempRegex.toString();
    }
    
    /**
     * Constructor that takes no application context. All properties objects will be
     * created using direct invocation of constructors.
     */
    public ConfigurationDumpUtility() {
        this(null);
    }
    
    /**
     * Constructor that takes an application context. Will attempt to get properties
     * objects from that context, and if that fails, will use direct invocation of 
     * constructors.
     * @param appContext the current application context
     */
    public ConfigurationDumpUtility(final ApplicationContext appContext) {
        this.appContext = appContext;
             
        propClasses = getAllGdsConfigurationClasses();
        this.sseFlag = this.appContext == null ? new SseContextFlag() : appContext.getBean(SseContextFlag.class);
        this.log = this.appContext == null ? TraceManager.getTracer(Loggers.CONFIG)
                : TraceManager.getTracer(appContext, Loggers.CONFIG);
    }
    
    /**
     * Collects the set of property categories. This is the list of the prefixes
     * from all the configuration objects, with the trailing dots removed.
     * 
     * @return sorted set of categories
     */
    public SortedSet<String> collectCategories() {
        final SortedSet<String> result = new TreeSet<>();
        
        for (final IGdsConfiguration igc: getPropertyObjects(GdsHierarchicalProperties.PropertySet.NO_DESCRIPTIVES)) {
            String prefix = igc.getPropertyPrefix();
            if (prefix.endsWith(".")) {
                prefix = prefix.substring(0, prefix.length() - 1);
            }
            result.add(prefix);

        } 
        
        return result;
    }
    
    /**
     * Collects properties from all configuration objects.
     * 
     * @param includeSystem true to include Java system properties and full config path
     * @param includeTemplateDirs true to include the full template path
     * @param includeDescriptives true to include descriptive properties
     * @return sorted map of properties and their values
     */
    public SortedMap<String, String> collectProperties(final boolean includeSystem, final boolean includeTemplateDirs, final GdsHierarchicalProperties.PropertySet includeDescriptives) {
        return collectProperties(null, includeSystem, includeTemplateDirs, includeDescriptives);
    }
    

    /**
     * Collects properties from all configuration objects, matching property names to a regular expression.
     * @param regex Java regular expression to match property names
     * @param includeSystem true to include Java system properties and full config path
     * @param includeTemplateDirs true to include the full template path
     * @param includeDescriptives true to include descriptive properties
     * @return sorted map of properties and their values
     */
    public SortedMap<String, String> collectProperties(final String regex, final boolean includeSystem, final boolean includeTemplateDirs, final GdsHierarchicalProperties.PropertySet includeDescriptives) {
        
        final List<Properties> propertiesToDump = new ArrayList<>();

        if (includeSystem) {
            final Properties systemProperties = GdsSystemProperties.getCachedSystemProperties();
            // Adding PATH-like properties to the property dump so that
            // the order of the lookup for config and templates can be used by Python code, without actually
            // constructing the path from scratch.
            systemProperties.put(CONFIG_PATH_PROPERTY,
                                 GdsSystemProperties.getFullConfigPath(sseFlag.isApplicationSse()));
            propertiesToDump.add(systemProperties);
        }
        
        if(includeTemplateDirs) {
            final Properties p = new Properties();
            p.put(TEMPLATE_PATH_PROPERTY, getTemplateDirs());
            propertiesToDump.add(p);
        }
          
        for (final IGdsConfiguration igc: getPropertyObjects(includeDescriptives)) {
          
            if (igc != null && igc.supportsFlatProperties()) {
               propertiesToDump.add(igc.asProperties());
            }
        } 
      
        final SortedMap<String,String> sortedProps = new TreeMap<>();
        for (final Properties p: propertiesToDump) {
            for (final String name: p.stringPropertyNames()) {
                if (regex == null || name.matches(regex)) {
                    sortedProps.put(name, p.getProperty(name));
                }
            }
        }
        
        return sortedProps;  

    }
  
    /**
     * Retrieve path representing lookup order of the template directories.
     * @return a string representing directories to search for templates. Each directory delimited
     * by the operating system's path separator.
     */
    private String getTemplateDirs() {
        final TemplateManager templateManager = new TemplateManager(GdsSystemProperties.getSystemMissionIncludingSse(sseFlag.isApplicationSse()));
        final List<String> templateDirs = templateManager.getTemplateDirectories();
        final StringBuilder builder = new StringBuilder();
        for (final String dir : templateDirs) {
            builder.append(dir);
            builder.append(File.pathSeparator);
        }
        return builder.toString();
        
    }
    
    
    /**
     * Scans for all configuration class objects.
     * @return set of all configuration class objects
     */
    private Set<Class<? extends IGdsConfiguration>> getAllGdsConfigurationClasses() {
        final Reflections r = new Reflections("jpl.gds");
        return r.getSubTypesOf(IGdsConfiguration.class);
        
    }
    
    /**
     * Writes a property output header.
     * 
     * @param writer writer to write to
     * @param format format for the header
     */
    public void writeHeader(final PrintWriter writer, final PropertyDumpFormat format)
    {
        if (format == PropertyDumpFormat.XML) { 
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Properties>");
        } else if (format == PropertyDumpFormat.CSV) {
            writer.write("name,value\n");
        } else if (format == PropertyDumpFormat.DOC_CSV) {
            writer.write("categoryName,categoryDescription,blockName,blockDescription,propertyName,multimissionValue,description,isInternal,behavioralNotes,validValues,formatHint,example\n");
        }
    }
    
    /**
     * Writes a property output footer.
     * 
     * @param writer writer to write to
     * @param format format for the footer
     */
    public void writeFooter(final PrintWriter writer, final PropertyDumpFormat format)
    {
        if (format == PropertyDumpFormat.XML) {
            writer.write("</Properties>");
        }
    }

    /**
     * Writes a property name and value using the specified format.
     * 
     * @param key property name
     * @param value property value
     * @param writer writer to write to
     * @param format desired output format
     */
    public void writeProperty(final String key, final String value, final PrintWriter writer, final PropertyDumpFormat format)
    {
        final StringBuilder buffer = new StringBuilder(256);

            if (format == PropertyDumpFormat.CSV) {
                buffer.append("\"" + key+ "\"").append(",").append("\"" + value + "\"\n");
            } else if (format == PropertyDumpFormat.XML) {
                buffer.append("<Property>")
                    .append("<Name><![CDATA[")
                    .append(key)
                    .append("]]></Name>")
                    .append("<Value><![CDATA[")
                    .append(value)
                    .append("]]></Value>")
                    .append("</Property>");
            } else { 
                buffer.append(key)
                .append("=")
                .append(value + "\n");
            }
            writer.write(buffer.toString());
    }

    /**
     * Writes all property names and values using the specified format, using the given map
     * of properties.
     * 
     * @param props map of property name to property value
     * @param writer writer to write to
     * @param format desired output format
     */
    public void writeAllProperties(final Map<String, String> props, final PrintWriter writer, final PropertyDumpFormat format) 
    {
        if (format == PropertyDumpFormat.DOC_CSV) {
            final SortedMap<String, String> propOnlySet = new TreeMap<>();
            final Iterator<String> keySet = props.keySet().iterator();
            while(keySet.hasNext()) {
                final String key = keySet.next();
                if (!IGdsConfiguration.isDescriptiveProperty(key)) {
                    propOnlySet.put(key, props.get(key));
                }
            }
            final Map<String, String> categoryDescRegexProps = getBlockDescriptionProperties(props, IGdsConfiguration.CATEGORY_DESC_PROPERTY_REGEX);
            final Map<String, String> blockDescRegexProps = getBlockDescriptionProperties(props, IGdsConfiguration.BLOCK_DESC_PROPERTY_REGEX);
            final Map<String, String> descriptionRegexProps = getDescriptivePropertiesWithRegex(props, IGdsConfiguration.DESCRIPTION_SUFFIX);
            final Map<String, String> behaviorRegexProps = getDescriptivePropertiesWithRegex(props, IGdsConfiguration.BEHAVIOR_SUFFIX);
            final Map<String, String> hintRegexProps = getDescriptivePropertiesWithRegex(props, IGdsConfiguration.HINT_SUFFIX);
            final Map<String, String> validRegexProps = getDescriptivePropertiesWithRegex(props, IGdsConfiguration.VALID_SUFFIX);
            final Map<String, String> exampleRegexProps = getDescriptivePropertiesWithRegex(props, IGdsConfiguration.EXAMPLE_SUFFIX);
            
            for (final String key : propOnlySet.keySet()) {
                final Pair<String, String> categoryDescription = getBlockDescription(categoryDescRegexProps, key, IGdsConfiguration.CATEGORY_DESCRIPTION_SUFFIX);
                final Pair<String, String> blockDescription = getBlockDescription(blockDescRegexProps, key, IGdsConfiguration.BLOCK_DESCRIPTION_SUFFIX);
                final String description = getDescriptivePropertyValue(props, descriptionRegexProps, key, IGdsConfiguration.DESCRIPTION_SUFFIX);
                final String behavior = getDescriptivePropertyValue(props, behaviorRegexProps, key, IGdsConfiguration.BEHAVIOR_SUFFIX);
                final boolean internal = IGdsConfiguration.isInternalProperty(key);
                final String hint = getDescriptivePropertyValue(props, hintRegexProps, key, IGdsConfiguration.HINT_SUFFIX);
                final String valid = getDescriptivePropertyValue(props, validRegexProps, key, IGdsConfiguration.VALID_SUFFIX);
                final String example = getDescriptivePropertyValue(props, exampleRegexProps, key, IGdsConfiguration.EXAMPLE_SUFFIX);
                writeCsvPropertyWithDescriptives(key, props.get(key), categoryDescription, blockDescription, description, internal, behavior, hint, valid, example, writer);
            }
            
        } else {
            final Set<String> keySet = props.keySet();
            for (final String key : keySet) {
                writeProperty(key, props.get(key), writer, format);
            }
        }
    }
    
    private Pair<String, String> getBlockDescription(final Map<String, String> blockDescRegexProps,
            final String key, final String suffix) {
        String longestMatch = null;
        String blockKey = null;
        for (final String propDescKey: blockDescRegexProps.keySet()) {
            blockKey = propDescKey.substring(0, propDescKey.length() - (suffix.length() + 1));
            if (key.startsWith(blockKey)) {
                if (longestMatch == null || longestMatch.length() < blockKey.length()) {
                    longestMatch = blockKey;
                }
            }
        }
        if (longestMatch != null) {
            return new Pair<>(longestMatch, blockDescRegexProps.get(longestMatch + "." + suffix));
        }
        return null;
    }

    private String getDescriptivePropertyValue(final Map<String, String> allProps, final Map<String, String> regexProps, 
            final String mainProperty, final String suffix) {
        String descPropVal = allProps.get(mainProperty + "." + suffix);
        if (descPropVal == null) {
            for (final Entry<String, String> descRegex : regexProps.entrySet()) {
                if (mainProperty.matches(descRegex.getKey())) {
                    descPropVal = descRegex.getValue();
                    break;
                }
            }
        }
        return descPropVal;
    }
    
    private void writeCsvPropertyWithDescriptives(final String key, final String value, final Pair<String, String> catDesc,
            final Pair<String, String> blockDesc,
            final String description, final boolean internal, final String behavior,
            final String hint, final String valid, final String example, final PrintWriter writer) {
        
        final StringBuilder buffer = new StringBuilder();
        if (catDesc != null) {
            buffer.append("\"" + catDesc.getOne() + "\"").append(",").append("\"" + cleanupDescriptive(catDesc.getTwo()) + "\"");
        } else {
            buffer.append("\"\",\"\"");
        }
        if (blockDesc != null) {
            buffer.append(",\"" + blockDesc.getOne() + "\"").append(",").append("\"" + cleanupDescriptive(blockDesc.getTwo()) + "\"");
        } else {
            buffer.append(",\"\",\"\"");
        }
        buffer.append(",\"" + key + "\"").append(",").append("\"" + value + "\"");
        if (description != null) {
            buffer.append(",\"" + cleanupDescriptive(description) + "\"");
        } else {
            buffer.append(EMPTY_COL);
        }
        if (internal) {
            buffer.append(",\"" + internal + "\"");
        } else {
            buffer.append(",\"false\"");
        }
        
        String adjustedBehavior = cleanupDescriptive(behavior);
        if (internal) {
            if (behavior == null) {
                adjustedBehavior = INTERNAL_ONLY;
            } else {
                adjustedBehavior = adjustedBehavior + INTERNAL_ONLY;
            
            }
        }
        if (adjustedBehavior != null) {
            buffer.append(",\"" + adjustedBehavior + "\"");
        } else {
            buffer.append(EMPTY_COL);
        }
        if (valid != null) {
            buffer.append(",\"" + valid.trim().replace("\"", "'") + "\"");
        } else {
            buffer.append(EMPTY_COL);
        }
        if (hint != null) {
            buffer.append(",\"" + cleanupDescriptive(hint) + "\"");
        }  else {
            buffer.append(EMPTY_COL);
        }
        if (example != null) {
            buffer.append(",\"" + cleanupDescriptive(example) + "\"");
        }  else {
            buffer.append(EMPTY_COL);
        }
        
        writer.println(buffer.toString());
        
    }
    
    private String cleanupDescriptive(final String inValue) {
        if (inValue != null && !inValue.isEmpty()) {
            final StringBuilder outValue = new StringBuilder(inValue.trim().replace("\"", "'"));
            if (outValue.charAt(outValue.length() - 1) != '.') {
                outValue.append('.');
            }
            return outValue.toString();
        }
        return inValue;
    }

    /**
     * Collects and dumps all properties to the given Writer, including header and footer.
     * 
     * @param writer writer to write to
     * @param format desired output format
     * @param includeSystem true to include Java system properties and full config path
     * @param includeTemplateDirs true to include full template config path
     * @param includeDescriptives true to include descriptive properties
     */
    public void collectAndDumpAllProperties(final PrintWriter writer, final PropertyDumpFormat format, final boolean includeSystem, final boolean includeTemplateDirs, final GdsHierarchicalProperties.PropertySet includeDescriptives) {
        collectAndDumpAllProperties(writer, format, null, includeSystem, includeTemplateDirs, includeDescriptives);     
    }
    

    /**
     * Collects and dumps all properties that match the given regex to the given Writer, including header and footer.
     * 
     * @param writer writer to write to
     * @param format desired output format
     * @param regex Java regular expression to match property names against
     * @param includeSystem true to include Java system properties and full config path
     * @param includeTemplateDirs true to include full template config path
     * @param includeDescriptives true to include descriptive properties
     */
    public void collectAndDumpAllProperties(final PrintWriter writer, final PropertyDumpFormat format, final String regex, final boolean includeSystem, final boolean includeTemplateDirs, final GdsHierarchicalProperties.PropertySet includeDescriptives) {
        final Map<String, String> allProps = collectProperties(regex, includeSystem, includeTemplateDirs, includeDescriptives);
        writeHeader(writer, format);
        writeAllProperties(allProps, writer, format);
        writeFooter(writer, format);          
    }
    
    /**
     * Dumps one property name and value, including header and footer, to the supplied writer.
     * 
     * @param writer writer to write to
     * @param property property to write
     * @param format desired format
     */
    public void dumpOneProperty(final PrintWriter writer, final String property, final PropertyDumpFormat format) {
        final Map<String, String> allProps = collectProperties(null, false, false, GdsHierarchicalProperties.PropertySet.INCLUDE_DESCRIPTIVES);
        if (allProps.get(property) != null) {
            writeHeader(writer, format);
            writeProperty(property, allProps.get(property), writer, format);
            writeFooter(writer, format);
        }
    }


    private IGdsConfiguration instantiateConfigurationObject(final Class<? extends IGdsConfiguration> c) {
        final IGdsConfiguration argumentsToTry[] = {
                new MissionProperties(sseFlag),
                new DatabaseProperties(sseFlag)
        };


        IGdsConfiguration ghp = null;
        final List<Exception> exceptions = new ArrayList<>();


        if (appContext != null) {
            try {
                ghp = appContext.getBean(c);
            }
            catch (final Exception e) {
                // do nothing
            }
        }
        if (ghp == null) {
            try {
                ghp = (IGdsConfiguration) ReflectionToolkit.createObject(c, new Class[] {}, new Object[] {});
            }
            catch (final ReflectionException e) {
                exceptions.add(e);
                try {
                    ghp = (IGdsConfiguration) ReflectionToolkit.createObject(c,
                                                                             new Class[] { sseFlag.getClass() },
                                                                             new Object[] { sseFlag });
                }
                catch (final ReflectionException e1) {
                    exceptions.add(e1);
                }
                for (final IGdsConfiguration config : argumentsToTry) {
                    try {
                        ghp = (IGdsConfiguration) ReflectionToolkit.createObject(c,
                                                                                 new Class[] { config.getClass() },
                                                                                 new Object[] { config });
                        break;
                    }
                    catch (final ReflectionException e2) {
                        exceptions.add(e2);
                        try {
                            ghp = (IGdsConfiguration) ReflectionToolkit.createObject(c,
                                                                                     new Class[] { config.getClass(), sseFlag.getClass() },
                                                                                     new Object[] { config, sseFlag });
                            break;
                        }
                        catch (final ReflectionException e3) {
                            exceptions.add(e3);
                        }
                    }
                }
            }
        }

        if (ghp == null) {
            for (final Exception e : exceptions) {
                log.error("Unable to load properties object: " + c.getSimpleName());
                log.error(e.getLocalizedMessage(), e);
            }
        }
        return ghp;
    }
    
    /**
     * Attempts to locate the configuration file that should contain the given property.
     * Note that all this does is match the property name to the prefixes supported by 
     * the various configuration objects. This indicates WHERE it should be if it is a 
     * valid property, but not whether the property is actually there. This method 
     * will return UNDEFINED_FILE if the property name could not be matched to any known
     * prefix.  If it finds both a matching prefix and an actual property value, it
     * will return the configuration file name. If it finds a matching prefix but no
     * property, it will return the file name with a note that the property is undefined.
     * 
     * 
     * @param propertyName name of the property to match
     * @return string indicating the location of the property
     */
    public String getBestMatchToFileText(final String propertyName) {
        final IGdsConfiguration bestMatch = whichObject(propertyName);
      
        if (bestMatch == null) {
            return UNDEFINED_FILE;
        } else if (bestMatch.asProperties().getProperty(propertyName) != null) {
            return bestMatch.getBaseFilename();
        } else {
            return bestMatch.getBaseFilename() + " is best match, but property is undefined";
        }
    }
    
    /**
     * Attempts to locate the configuration object that should contain the given property.
     * Note that all this does is match the property name to the prefixes supported by 
     * the various configuration objects. This indicates WHERE it should be if it is a 
     * valid property, but not whether the property is actually there.
     * 
     * @param propertyName name of the property to match
     * @return IGdsConfiguration object that should contain the property, or null if no 
     *         match found based upon property prefix
     */
    public IGdsConfiguration whichObject(final String propertyName) {
        IGdsConfiguration bestMatch = null;
        for (final IGdsConfiguration c: getPropertyObjects(GdsHierarchicalProperties.PropertySet.INCLUDE_DESCRIPTIVES)) {
            if (propertyName.startsWith(c.getPropertyPrefix())) {
                if (bestMatch == null) {
                    bestMatch = c;
                } else {
                    if (c.getPropertyPrefix().length() > bestMatch.getPropertyPrefix().length()) {
                        bestMatch = c;
                    }
                }
            }
        }
        if (bestMatch == null) {
            return null;
        } else {
            return bestMatch;
        } 
    }
    
    private List<IGdsConfiguration> getPropertyObjects(final GdsHierarchicalProperties.PropertySet includeDescriptives) {
        
        GdsHierarchicalProperties.setPropertiesToLoad(includeDescriptives);
        
        final List<IGdsConfiguration> propInstances = new ArrayList<>(propClasses.size());

        for (final Class<? extends IGdsConfiguration> c: propClasses) {
            if (c.equals(GdsHierarchicalProperties.class) || c.equals(GdsHierarchicalXml.class)
                    || Modifier.isAbstract(c.getModifiers()) || c.isInterface() || c.getName().contains("Test")) {
                continue;
            }
            final IGdsConfiguration ghp = instantiateConfigurationObject(c);

            if (ghp != null) {
                propInstances.add(ghp);
            }
        } 
        
        GdsHierarchicalProperties.setPropertiesToLoad(PropertySet.NO_DESCRIPTIVES);
        
        return propInstances;
    }
    
    private Map<String, String> getBlockDescriptionProperties(final Map<String, String> props, final String regex) {
        final Map<String, String> result = new TreeMap<>();
        for (final Entry<String, String> prop : props.entrySet()) {
            final String key = prop.getKey();
            if (key.matches(regex)) {
                result.put(key, prop.getValue());
            }
     
        }
        return result;
    }
    
    private Map<String, String> getDescriptivePropertiesWithRegex(final Map<String, String> props, final String suffix) {
        final Map<String, String> result = new TreeMap<>();
        for (final Entry<String, String> prop : props.entrySet()) {
            final String key = prop.getKey();
            if (key.matches(".+\\." + suffix) && key.contains("[") && key.contains("]")) {
                final String newPropName = replaceTokensAndStripSuffix(key, suffix);
                result.put(newPropName, prop.getValue());
                
            }
        }
        return result;
    }

    private String replaceTokensAndStripSuffix(final String propName, final String suffix) {
        
        final String origProp = propName.substring(0, propName.length() - suffix.length() - 1);
        
        final StringBuilder newPropName = new StringBuilder();
        int currentIndex = 0;
        int openIndex = origProp.indexOf(".[");
        while (openIndex != -1) {
            final int closeIndex = origProp.indexOf(']', openIndex);
            if (closeIndex == -1) {
                break;
            }
            newPropName.append(origProp.substring(currentIndex, openIndex + 1));
            final String token = origProp.substring(openIndex + 1, closeIndex + 1);
            currentIndex = openIndex + token.length() + 1;
           
            switch (token) {
                case "[VENUE]":
                    newPropName.append(VENUE_TYPE_REGEXP);
                    break;
                case "[STREAM_TYPE]":
                    newPropName.append(STREAM_TYPE_REGEXP);
                    break;
                case "[UPLINK_CONNECTION_TYPE]":
                    newPropName.append(UPLINK_CONN_REGEXP);
                    break;
                case "[DOWNLINK_CONNECTION_TYPE]":
                    newPropName.append(DOWNLINK_CONN_REGEXP);
                    break;
                case "[DICTIONARY_TYPE]":
                    newPropName.append(DICT_TYPE_REGEXP);
                    break;
                case "[MESSAGE_TYPE]":
                case "[WATCHER_NAME]":
                case "[ICMD_PARAM_NAME]":
                case "[ICMD_REQUEST_NAME]":
                    newPropName.append(GENERAL_ALPHA_REGEXP);
                    break;
                case "[STATION_NUMBER]":
                    newPropName.append(STATION_NUMBER_REGEXP);
                    break;
                case "[GLAD_DATA_TYPE]":
                    newPropName.append(GLAD_DATA_TYPE_REGEXP);
                    break;
                case "[GLAD_CONTAINER_TYPE]":
                    newPropName.append(GLAD_CONTAINER_REGEXP);
                    break;
                case "[TESTBED_NAME]":
                case "[UPLINK_FILE_TYPE]":
                default:
                    newPropName.append(GENERAL_ALPHANUM_REGEXP);
                    break;            

            }
            
            openIndex = origProp.indexOf(".[", closeIndex);
        }

        newPropName.append(origProp.substring(currentIndex));
        return newPropName.toString().replaceAll("\\.", "\\\\.");
    }
 
}
