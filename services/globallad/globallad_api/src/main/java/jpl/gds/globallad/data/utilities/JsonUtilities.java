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
package jpl.gds.globallad.data.utilities;

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Builder;

import jpl.gds.globallad.GlobalLadException;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.container.IGlobalLadDeltaQueryable.DeltaQueryStatus;
import jpl.gds.globallad.data.json.views.GlobalLadSerializationViews;

/**
 * Jackson serialization utilities.  Static instances of object mappers configured with the available views are 
 * created and cached to be used with any call to the serialization methods. 
 */
public class JsonUtilities {
	/**
	 * Create static instances since they are thread safe and are heavy weight to create every request. The Jackson 
	 * documentation recommends doing this, so we listened.
	 */
	private static final ObjectMapper serializationMapper;
	private static final ObjectMapper requestMapper;
	private static final ObjectMapper newMapper;
	
	static {
		serializationMapper = createSerializerObjectMapperWithIndent();
		requestMapper = createRequestObjectMapperWithIndent();
		newMapper = createNewObjectMapperWithIndent();
	}
	
	/**
	 * @return the object mapper configured with the serialization view.
	 */
	public static ObjectMapper getSerializationMapper() {
		return serializationMapper;
	}

	/**
	 * @return the object mapper configured with the request view.
	 */
	public static ObjectMapper getRequestMapper() {
		return requestMapper;
	}
	
	/**
	 * @return the object mapper configured with the new view.  
	 */
	public static ObjectMapper getNewMapper() {
		return newMapper;
	}

	/**
	 * Creates an object mapper.  Sets the default view to viewClass. The type info mixin will be added for map and collections.
	 * 
	 * @param viewClass
	 * @param disableViewInclusion - If true only properties marked with JsonView will be serialized / deserialized.
	 * @param indentOutput
	 * 
	 * @return a new object mapper with view viewClass and inclusion and indent options set based on inputs.
	 */
	public static ObjectMapper createMapper(final Class<?> viewClass, final boolean disableViewInclusion, final boolean indentOutput) {
		return createMapper(viewClass, disableViewInclusion, indentOutput, true);
	}

	/**
	 * Creates an object mapper.  Sets the default view to viewClass.
	 * @param viewClass
	 * @param disableViewInclusion - If true only properties marked with JsonView will be serialized / deserialized.
	 * @param indentOutput
	 * @param typeInfo - If true will add the mixin for collection and map to include the class information.
	 * 
	 * @return a new object mapper with view viewClass and inclusion and indent options set based on inputs.
	 */
	public static ObjectMapper createMapper(final Class<?> viewClass, final boolean disableViewInclusion, final boolean indentOutput, final boolean typeInfo) {
		final ObjectMapper mapper = new ObjectMapper();
		
		if (typeInfo) {
			mapper.addMixIn(Collection.class, IncludeTypeMixin.class);
			mapper.addMixIn(Map.class, IncludeTypeMixin.class);
		}

		mapper.setConfig(mapper.getSerializationConfig().withView(viewClass));
		
		if (disableViewInclusion) {
			mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
		}
		
		if (indentOutput) {
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
		}
		
		return mapper;
	}
	
	/**
	 * This mixin is used to disable the including the type in the output json.  This is used only
	 * for the request mapper because the json will not and can not be deserialized.
	 */
	@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, include = JsonTypeInfo.As.PROPERTY, property = "@class")
	public abstract class RequestMixIn {}

	/**
	 * Use this mixin to make sure that the type information is included in the serialization for types
	 * such collections or maps, etc.
	 */
	@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
	abstract class IncludeTypeMixin {}
	
	/**
	 * Mixin for serialization that will ignore methods when serializing global lad data.  This is needed since
	 * the data now extends IGlobalLadContainer but there are methods mapped to JSON that need to be ignored.
	 */
	abstract class DataContainerMixin {
		
		@JsonIgnore
		public abstract Object getContainerIdentifier();
			
		@JsonIgnore
		public abstract String getContainerType();

	}
	
	/**
	 * @return a new object mapper with view NewView.
	 */
	public static ObjectMapper createNewObjectMapperWithIndent() {
		final ObjectMapper mapper = createMapper(GlobalLadSerializationViews.NewView.class, true, true);
		mapper.addMixIn(IGlobalLADData.class, RequestMixIn.class);
		return mapper;
	}
	
	/**
	 * @return a new object mapper with view RequestView.
	 */
	public static ObjectMapper createRequestObjectMapperNoIndent() {
		final ObjectMapper mapper = createMapper(GlobalLadSerializationViews.RestRequestView.class, true, false, false);
		mapper.addMixIn(IGlobalLADData.class, RequestMixIn.class);
		return mapper;
	}

	/**
	 * @return a new object mapper with view RequestView and indent on.
	 */
	public static ObjectMapper createRequestObjectMapperWithIndent() {
		final ObjectMapper mapper = createMapper(GlobalLadSerializationViews.RestRequestView.class, true, true, false);
		mapper.addMixIn(IGlobalLADData.class, RequestMixIn.class);
		return mapper;
	}
	
	/**
	 * @return a new object mapper with view SerializationView and indent off.
	 */
	public static ObjectMapper createSerializerObjectMapper() {
		final ObjectMapper mapper = createMapper(GlobalLadSerializationViews.SerializationView.class, true, false);
		mapper.addMixIn(IGlobalLADData.class, DataContainerMixin.class);
		
		return mapper;
	}
	
	/**
	 * @return a new object mapper with view SerializationView and indent on.
	 */
	public static ObjectMapper createSerializerObjectMapperWithIndent() {
		final ObjectMapper mapper = createMapper(GlobalLadSerializationViews.SerializationView.class, true, true);
		mapper.addMixIn(IGlobalLADData.class, DataContainerMixin.class);
		
		return mapper;
	}
	
	/**
	 * An aweful lot of working around had to be done to get the CSV mapper to work properly. A bug was submitted
	 * to the development team because one of the ways to create the schema for an object is to do the following:
	 * 
	 * CsvSchema scheam = csvMapper.schemaFor(Pojo.class)
	 * 
	 * However, this was not working. Turns out that this method ignores all views when creating schemas. This is
	 * bad because we rely on views to get the subsets of the data we want to see.  There is supposed to be
	 * a fix in a later version, but as of version 2.5.3 of the jackson-csv it is not fixed.  That is why all of these 
	 * extra mixin classes had to be created, and all of the different mappers had to be used.  
	 * 
	 * I just wanted to make a record for whoever looks at this, including myself.
	 */
	
	/**
	 * Mixin for csv output of the global lad data.  This is where the csv output columns are defined.  Because
	 * it is in an annotation there is no way to control this by configuration.
	 */
	@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, include = JsonTypeInfo.As.PROPERTY, property = "@class")
	public abstract class CsvMixin  extends DataContainerMixin {}


	/**
	 * Mixin for verified query results. This is used for the csv output.
	 */
	abstract class Complete extends JsonUtilities.CsvMixin {
		@JsonProperty("verified")
		public abstract DeltaQueryStatus complete();

		@JsonIgnore
		public abstract DeltaQueryStatus incomplete();
		
		@JsonIgnore
		public abstract DeltaQueryStatus unknown();
	}

	/**
	 * Mixin for unverified query results.  This is used for the csv output.
	 */
	abstract class Incomplete extends JsonUtilities.CsvMixin {
		@JsonIgnore
		public abstract DeltaQueryStatus complete();

		@JsonProperty("verified")
		public abstract DeltaQueryStatus incomplete();
		
		@JsonIgnore
		public abstract DeltaQueryStatus unknown();
	}
	
	/**
	 * Mixin for unknown query results.  This is used for the csv output.
	 */
	abstract class Unknown extends JsonUtilities.CsvMixin {
		@JsonIgnore
		public abstract DeltaQueryStatus complete();

		@JsonIgnore
		public abstract DeltaQueryStatus incomplete();
		
		@JsonProperty("verified")
		public abstract DeltaQueryStatus unknown();
	}

	/**
	 * Creates a csv schema with columns.
	 * 
	 * @param columns
	 * @return new csv schema with the column order from columns d
	 */
	public static CsvSchema createCsvSchema(final Collection<String> columns) {
		final Builder schema = CsvSchema.builder();

		
		for (final String column : columns) {
			schema.addColumn(column);
		}
		
		return schema.build();
	}

	/**
	 * Creates a csv schema with the configured column headers from the global lad configuration. 
	 * @return new csv schema.
	 * @throws GlobalLadException - No columns are configured in the configuration.
	 */
	public static CsvSchema createEhaCsvSchema() throws GlobalLadException {
		return createEhaCsvSchema(false);
	}

	/**
	 * Create a csv schema configured with the check channel csv output columns.
	 *
	 * @return new csv schema
	 * @throws GlobalLadException
	 */
	public static CsvSchema createLMCsvSchema() throws GlobalLadException {
		/*
		 * Creates a schema using the csv columns for chill_check_channel.
		 */

		final Collection<String> cols = GlobalLadProperties.getGlobalInstance().getLMCsvColumnNamesEha();

		if (cols.isEmpty()) {
			throw new GlobalLadException("LM EHA CSV columns were not configured correctly and are empty.");
		}

		return createCsvSchema(cols).withoutQuoteChar().withoutEscapeChar();

	}

	/**
	 * Creates a csv schema with the configured column headers from the global lad configuration.
	 *
	 * @param includeVerified - Includes verified column as the last column if true.
	 * @return new csv schema
	 * @throws GlobalLadException
	 */
	public static CsvSchema createEhaCsvSchema(final boolean includeVerified) throws GlobalLadException {
		final Collection<String> cols = GlobalLadProperties.getGlobalInstance().getCsvColumnNamesEha();
		
		if (cols.isEmpty()) {
			throw new GlobalLadException("EHA CSV columns were not configured correctly and are empty.");
		}
		
		final CsvSchema schema = createCsvSchema(cols);
		return includeVerified ? schema.rebuild().addColumn("verified").build() : schema;

	}

	/**
	 * Creates a csv schema with the configured column headers from the global lad configuration. 
	 * @return new csv schema
	 * @throws GlobalLadException - No columns are configured in the configuration.
	 */
	
	public static CsvSchema createEvrCsvSchema() throws GlobalLadException {
		return createEvrCsvSchema(false);
	}

	/**
	 * Creates a csv schema with the configured column headers from the global lad configuration. 
	 * 
	 * @param includeVerified - Includes verified column as the last column if true.
	 * @return new csv schema
	 * @throws GlobalLadException
	 */
	public static CsvSchema createEvrCsvSchema(final boolean includeVerified) throws GlobalLadException {
		final Collection<String> cols = GlobalLadProperties.getGlobalInstance().getCsvColumnNamesEvr();
		
		if (cols.isEmpty()) {
			throw new GlobalLadException("EVR CSV columns were not configured correctly and are empty.");
		}
		
		final CsvSchema schema = createCsvSchema(cols);
		
		return includeVerified ? schema.rebuild().addColumn("verified").build() : schema;
	}

	/**
	 * Creates a CSV object mapper that will not output a verified output.
	 * @return new csv mapper configured to not output the verified column.
	 */
	public static CsvMapper createCsvObjectMapper() {
		return createCsvObjectMapper(CsvMixin.class);
	}
	
	/**
	 * Creates a CSV object mapper to be used to output verified sets of data.
	 * @return new csv mapper configured to output the verified column.  This will set the value of the verified column to "complete".
	 */
	public static CsvMapper createCsvObjectMapperVerified() {
		return createCsvObjectMapper(Complete.class);
	}

	/**
	 * Creates a CSV object mapper to be used to output unverified sets of data.
	 * @return new csv mapper configured to output the verified column.  This will set the value of the verified column to "incomplete".
	 */
	public static CsvMapper createCsvObjectMapperUnverified() {
		return createCsvObjectMapper(Incomplete.class);
	}
	
	/**
	 * @return new csv mapper configured to output the verified column.  This will set the value of the verified column to "unknown".
	 * @return
	 */
	public static CsvMapper createCsvObjectMapperUnknown() {
		return createCsvObjectMapper(Unknown.class);
	}


	/**
	 * Creates a csv mapper set up to use the columns from the request view.
	 * 
	 * @return new csv mapper with mixinClass added as a mixin for IGlobalLADData.class. 
	 */
	public static CsvMapper createCsvObjectMapper(final Class<?> mixinClass) {
		final CsvMapper mapper = new CsvMapper();
		
		/**
		 * Must enable or any columns that are returned and are not in the schema will cause an exception.  This just makes those 
		 * fields ignored.
		 */
		mapper.enable(JsonGenerator.Feature.IGNORE_UNKNOWN);
		mapper.enable(CsvGenerator.Feature.ALWAYS_QUOTE_STRINGS);
		/**
		 * Add the mix-in to ignore the container id and type and the class.
		 */
		mapper.addMixIn(IGlobalLADData.class, mixinClass);

		return mapper;
	}

	/**
	 * Creates a csv mapper to be used for check channel output.
	 * @return new mapper.
	 */
	public static CsvMapper createLMCsvMapper() {
		/*
		 * Use the normal csv object mapper just disable quoting all strings.  This will just quote strings when required.
		 */
		final CsvMapper mapper = createCsvObjectMapper();

		// Only want to quote string that need quoting.
		mapper.disable(CsvGenerator.Feature.ALWAYS_QUOTE_STRINGS);

		return mapper;



	}
}
