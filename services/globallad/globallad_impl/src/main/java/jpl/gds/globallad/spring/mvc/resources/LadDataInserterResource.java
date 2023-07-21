package jpl.gds.globallad.spring.mvc.resources;
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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import jpl.gds.globallad.data.storage.DataInsertionManager;
import jpl.gds.globallad.io.IBinaryLoadHandler;
import jpl.gds.globallad.spring.beans.GlobalLadBinaryLoadHandlerProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.EhaGlobalLadData;
import jpl.gds.globallad.data.EvrGlobalLadData;
import jpl.gds.globallad.data.GladAlarmValueSet;
import jpl.gds.globallad.data.GlobalLadUserDatatypeConverter;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.container.IGlobalLadSearchAlgorithm;
import jpl.gds.globallad.data.container.search.query.BasicQuerySearchAlgorithm;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.DataSource;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.RecordedState;
import jpl.gds.globallad.message.handler.IGlobalLad;
import jpl.gds.globallad.rest.resources.ResourceUris;

/**
 * Resource for inserting data into the global lad for testing purposes.
 */
@RestController
@Scope("request")
//@Api(tags={"data"})
@Api
public class LadDataInserterResource extends AbstractGlobalLadMVCResource {
	/**
	 * The global lad will be injected by Jersey.
	 */
	@Autowired
	IGlobalLad glad;

	@Autowired
	GlobalLadProperties config;

	@Autowired
	GlobalLadBinaryLoadHandlerProvider binaryParseHandlerProvider;

	@Autowired
	DataInsertionManager dataInsertionManager;

	/**
	 * The blank classes below are used to let Jackson parse the JSON sent with the PUT.  With these
	 * we can easily take an input JSON from the request and convert it into a global lad data object
	 * to be inserted into the global lad.
	 */
	private static class InsertBlank {
		public long sclkCoarse;
		public long sclkFine;
		public long ertMilliseconds;
		public long ertNanoseconds;
		public long scetMilliseconds;
		public long scetNanoseconds;
		public String venue;
		public long sessionNumber;
		public int scid;
		public byte dssId;
		public byte vcid;
		public String host;


		/**
		 * @param sclkCoarse
		 * @param sclkFine
		 * @param ertMilliseconds
		 * @param ertNanoseconds
		 * @param scetMilliseconds
		 * @param scetNanoseconds
		 * @param venue
		 * @param sessionNumber
		 * @param scid
		 * @param dssId
		 * @param vcid
		 * @param host
		 */
		public InsertBlank(long sclkCoarse, long sclkFine,
				long ertMilliseconds, long ertNanoseconds,
				long scetMilliseconds, long scetNanoseconds, String venue,
				long sessionNumber, int scid, byte dssId, byte vcid, String host) {
			super();
			this.sclkCoarse = sclkCoarse;
			this.sclkFine = sclkFine;
			this.ertMilliseconds = ertMilliseconds;
			this.ertNanoseconds = ertNanoseconds;
			this.scetMilliseconds = scetMilliseconds;
			this.scetNanoseconds = scetNanoseconds;
			this.venue = venue;
			this.sessionNumber = sessionNumber;
			this.scid = scid;
			this.dssId = dssId;
			this.vcid = vcid;
			this.host = host;
		}
	}

	private static class EhaBlank extends InsertBlank {
		public String channelId;
		public String dnType;
		public Object dn;
		public Double eu;
		public String status;
		public ChannelType type;

		@JsonCreator
		public EhaBlank(
				@JsonProperty("sclkCoarse") long sclkCoarse, 
				@JsonProperty("sclkFine") long sclkFine,
				@JsonProperty("ertMilliseconds") long ertMilliseconds, 
				@JsonProperty("ertNanoseconds") long ertNanoseconds,
				@JsonProperty("scetMilliseconds") long scetMilliseconds, 
				@JsonProperty("scetNanoseconds") long scetNanoseconds, 
				@JsonProperty("venue") String venue,
				@JsonProperty("sessionNumber") long sessionNumber,
				@JsonProperty("scid") int scid,
				@JsonProperty("dssId") byte dssId, 
				@JsonProperty("vcid") byte vcid, 
				@JsonProperty("host") String host,
				@JsonProperty("channelId") String channelId,
				@JsonProperty("dnType") String dnType, 
				@JsonProperty("status") String status) {
			super(sclkCoarse, sclkFine, ertMilliseconds,
					ertNanoseconds, scetMilliseconds, scetNanoseconds, venue,
					sessionNumber, scid, dssId, vcid, host);
			this.channelId = channelId;
			this.dnType = dnType;
			this.status = status;
		}

		@JsonProperty("eu") 
		public void setEu(String euStr) {
			this.eu = Double.valueOf(euStr);
		}

		@JsonProperty("dn")
		public void setDn(String dnStr) {
			type = ChannelType.valueOf(this.dnType.toUpperCase());

			switch(type) {
			case FLOAT:
				this.dn = Double.valueOf(dnStr);
				break;
			case STATUS:
			case SIGNED_INT:
				this.dn = Integer.valueOf(dnStr);
				break;
			case TIME:
			case DIGITAL:
			case UNSIGNED_INT:
			case BOOLEAN:
				/**
				 * MPCS-8018 triviski 3/16/2016 - Treat boolean the same as unsigned.
				 */
				this.dn = Integer.toUnsignedLong(Integer.valueOf(dnStr));
				break;
			case UNKNOWN:
			case ASCII:
			default:
				this.dn = dnStr;
				break;
			}
		}
	}

	/*
	 * MPCS-7888 - triviski 1/21/2016 - Updated the evr blank to take the new metadata fields.
	 */
	private static class EvrBlank extends InsertBlank {
		public String evrLevel;
		public String evrName;
		public long evrId;
		public String evrMessage;
		public String taskName;
		public String sequenceId;
		public String categorySequenceId;
		public String addressStack;
		public String source;
		public String taskId;
		public String errno;

		/**
		 * @param sclkCoarse
		 * @param sclkFine
		 * @param ertMilliseconds
		 * @param ertNanoseconds
		 * @param scetMilliseconds
		 * @param scetNanoseconds
		 * @param venue
		 * @param sessionNumber
		 * @param dssId
		 * @param vcid
		 * @param host
		 * @param evrLevel
		 * @param evrName
		 * @param evrId
		 * @param evrMessage
		 */
		@JsonCreator
		public EvrBlank(
				@JsonProperty("sclkCoarse") long sclkCoarse, 
				@JsonProperty("sclkFine") long sclkFine,
				@JsonProperty("ertMilliseconds") long ertMilliseconds, 
				@JsonProperty("ertNanoseconds") long ertNanoseconds,
				@JsonProperty("scetMilliseconds") long scetMilliseconds, 
				@JsonProperty("scetNanoseconds") long scetNanoseconds, 
				@JsonProperty("venue") String venue,
				@JsonProperty("sessionNumber") long sessionNumber, 
				@JsonProperty("scid") int scid,
				@JsonProperty("dssId") byte dssId, 
				@JsonProperty("vcid") byte vcid, 
				@JsonProperty("host") String host,
				@JsonProperty("evrLevel") String evrLevel,
				@JsonProperty("evrName") String evrName, 
				@JsonProperty("evrId") long evrId, 
				@JsonProperty("evrMessage") String evrMessage,
				@JsonProperty("taskName") String taskName,
				@JsonProperty("sequenceId") String sequenceId,
				@JsonProperty("categorySequenceId") String categorySequenceId,
				@JsonProperty("addressStack") String addressStack,
				@JsonProperty("source") String source,
				@JsonProperty("taskId") String taskId,
				@JsonProperty("errno") String errno
				) {
			super(sclkCoarse, sclkFine, ertMilliseconds,
					ertNanoseconds, scetMilliseconds, scetNanoseconds, venue,
					sessionNumber, scid, dssId, vcid, host);
			this.evrLevel = evrLevel;
			this.evrName = evrName;
			this.evrId = evrId;
			this.evrMessage = evrMessage;
			this.taskName = taskName;
			this.sequenceId = sequenceId;
			this.categorySequenceId = categorySequenceId;
			this.addressStack = addressStack;
			this.source = source;
			this.taskId = taskId;
			this.errno = errno;
		}
	}

	/**
	 * Add an evr entry into the global lad.
	 * 
	 * @param source
	 * @param state
	 * @param blank
	 * @return Response with status code set.
	 * @throws Exception
	 */
	@PutMapping(path=ResourceUris.evrInsertURI, consumes=MediaType.APPLICATION_JSON, produces=MediaType.APPLICATION_JSON)
	@ApiOperation(value="Add an EVR object into the global lad",
	notes="All expected data should be included in the request body as JSON")
	public ResponseEntity<Object> addEvr(
			@ApiParam(value="The data source", allowableValues=ResourceUris.sourceSwagger, required=true) 
			@PathVariable("source") DataSource source, 

			@ApiParam(value="The recorded state", allowableValues=ResourceUris.recordedStatSwagger, required=true) 
			@PathVariable("recordedState") RecordedState state,
			@RequestBody EvrBlank blank
			) throws Exception {

		boolean isRealTime = true;
		boolean isFsw = false;

		switch(source) {
		case all:
			throw new Exception("Data source can not be 'all' when inserting data.");
		case fsw:
			isFsw = true;

			switch(state) {
			case both:
				throw new Exception("Recorded stat can 'both' when inserting data.");
			case recorded:
				isRealTime = false;
				break;
			default:
				break;
			}
			break;
		default:
			break;
		}
		/*
		 * MPCS-7888 - triviski 1/21/2016 - Include the metadata fields.
		 */
		IGlobalLADData evr = new EvrGlobalLadData(blank.evrId, 
				blank.evrLevel, 
				blank.evrName, 
				isRealTime, isFsw, 
				blank.evrMessage, 
				blank.taskName,
				blank.sequenceId,
				blank.categorySequenceId,
				blank.addressStack,
				blank.source,
				blank.taskId,
				blank.errno,
				blank.sclkCoarse, 
				blank.sclkFine, 
				blank.ertMilliseconds, 
				blank.ertNanoseconds, 
				blank.scetMilliseconds, 
				blank.scetNanoseconds, 
				blank.sessionNumber, 
				blank.scid,
				blank.venue, 
				blank.dssId, 
				blank.vcid, 
				blank.host);

		GlobalLadUserDatatypeConverter.setUserDataTypeFromData(evr);

		glad.getMasterContainer().insert(evr);
		return ok();
	}

	/**
	 * Add an eha entry into the global lad.
	 * 
	 * @param source
	 * @param state
	 * @param blank
	 * @return Response with status code set.
	 * @throws Exception
	 */
	@PutMapping(path=ResourceUris.ehaInsertURI, consumes=MediaType.APPLICATION_JSON, produces=MediaType.APPLICATION_JSON)
	@ApiOperation(value="Add an EHA object into the global lad",
	notes="All expected data should be included in the request body as JSON")
	public ResponseEntity<Object> addEHa(
			@ApiParam(value="The data source", allowableValues=ResourceUris.sourceSwagger, required=true) 
			@PathVariable("source") DataSource source, 
			@ApiParam(value="The recorded state", allowableValues=ResourceUris.recordedStatSwagger, required=true) 
			@PathVariable("recordedState") RecordedState state, 
			@RequestBody EhaBlank blank
			) throws Exception {
		boolean isRealTime = true;
		boolean isSse = false;
		boolean isMonitor = false;
		boolean isHeader = false;
		boolean isFsw = false;

		switch(source) {
		case all:
			throw new Exception("Data source can not be 'all' when inserting data.");
		case fsw:
			isFsw = true;

			switch(state) {
			case both:
				throw new Exception("Recorded stat can 'both' when inserting data.");
			case recorded:
				isRealTime = false;
				break;
			default:
				break;
			}
			break;
		case header:
			isHeader = true;
			break;
		case monitor:
			isMonitor = true;
			break;
		case sse:
			isSse = true;
			break;
		default:
			break;
		}

		IGlobalLADData eha = new EhaGlobalLadData(blank.type, 
				blank.channelId, 
				blank.dn, 
				blank.eu, 
				new GladAlarmValueSet(),
				isRealTime, isHeader, isMonitor, isSse, isFsw, 
				blank.sclkCoarse, 
				blank.sclkFine, 
				blank.ertMilliseconds, 
				blank.ertNanoseconds, 
				blank.scetMilliseconds, 
				blank.scetNanoseconds, 
				blank.sessionNumber, 
				blank.scid,
				blank.venue, 
				blank.dssId, 
				blank.vcid, 
				blank.host, 
				blank.status);

		GlobalLadUserDatatypeConverter.setUserDataTypeFromData(eha);
		glad.getMasterContainer().insert(eha);
		return ok();
	}

	/**
	 * Gets all data values held in the global lad, converts them to binary and writes the to the 
	 * Response output as an octet stream.
	 * 
	 * @return Response with entity set as the data output stream.
	 */
	@GetMapping(path="binary/dump", produces=MediaType.APPLICATION_OCTET_STREAM)
	@ApiOperation(value="Stream the entire contents of the global lad")
	public StreamingResponseBody binaryDump() {
		StreamingResponseBody stream = new StreamingResponseBody() {

			@Override
			public void writeTo(OutputStream output) throws IOException {
				/**
				 * MPCS-8173 - triviski 5/10/2016 - Need to set time type to all for 
				 * doing dumps in order to make sure that everything is returned.
				 */
				IGlobalLadSearchAlgorithm qa = BasicQuerySearchAlgorithm
						.createBuilder()
						.setTimeType(GlobalLadPrimaryTime.ALL)
						.build();

				Map<Object, Collection<IGlobalLADData>> data = glad.getAll(qa);

				DataOutputStream dos = new DataOutputStream(output);
				for (Collection<IGlobalLADData> datas : data.values()) {
					for (IGlobalLADData d : datas) {
						dos.write(d.toPacketByteArray());
					}
				}
			}
		};

		return stream;
	}

	/**
	 * Takes a binary input stream (binary dump) and will insert it into the global lad as as a client input.
	 * 
	 * @param input
	 * @return Response with status code set.  
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	@PostMapping(path="binary/load", consumes=MediaType.APPLICATION_OCTET_STREAM)
	@ApiOperation(value="Load the lad from the contents of a dump file")
	public ResponseEntity<Object> binaryLoad(final InputStream input) throws Exception {

		final IBinaryLoadHandler inserter = binaryParseHandlerProvider.getBinaryLoadHandler(input, dataInsertionManager);

		inserter.execute();

		return ok();
	}
}
