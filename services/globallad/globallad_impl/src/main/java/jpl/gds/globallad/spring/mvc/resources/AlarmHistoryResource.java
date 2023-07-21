package jpl.gds.globallad.spring.mvc.resources;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;

import javax.ws.rs.core.MediaType;

import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jpl.gds.globallad.data.AbstractGlobalLadData;
import jpl.gds.globallad.data.AlarmHistoryGlobalLadData;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.container.GlobalLadUtilities;
import jpl.gds.globallad.data.container.IGlobalLadSearchAlgorithm;
import jpl.gds.globallad.data.container.search.query.BasicQuerySearchAlgorithm;
import jpl.gds.globallad.message.handler.IGlobalLad;
import jpl.gds.globallad.rest.resources.ResourceUris;
import jpl.gds.serialization.globallad.data.Proto3GlobalLadTransport;
import jpl.gds.shared.cli.CliUtility;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Spring MVC resource class to query for the alarm history tables.
 * The response will ALWAYS be a serialized proto buffer binary message.
 */
@RestController
@Scope("request")
@Api
public class AlarmHistoryResource extends AbstractGlobalLadMVCResource {
	@Autowired
	IGlobalLad glad;

    /**
     * @param host
     *            the session host
     * @param venue
     *            <VenueType>
     * @param sessionNumbers
     *            optional list of session ID(s)
     * @return <StreamingResponseBody> serialized proto buffer binary message containing alarm history
     * @throws ParseException
     *             If an exception occurs parsing the request
     */
	@GetMapping(path=ResourceUris.alarmHistoryURI,
			produces={MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_PLAIN})
	@ApiOperation(value="Query the global lad.", responseContainer="Binary proto buffer")
	public StreamingResponseBody getDataWithQueryParams(
			@ApiParam(value="Session host", required=true, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.hostQP, required=true) final String host,
			@ApiParam(value="Session venues", required=false, allowableValues=ResourceUris.venueTypeSwagger)
			@RequestParam(name=ResourceUris.venueQP, required=false) final String venue, 
			@ApiParam(value="Session ID.  Will take a list or range but only the latest value is returned.", required=false, defaultValue="", allowMultiple=false)
			@RequestParam(name=ResourceUris.sessionIdQP, required=false) final String sessionNumbers
			) throws ParseException {
		
		final Collection<Long> sids = CliUtility.expandCsvRangeLong(sessionNumbers);
		
		/**
		 * Create a builder and find the alarm history object.
		 */
		final IGlobalLadSearchAlgorithm query = BasicQuerySearchAlgorithm.createBuilder()
				.setVenue(venue)
				.setSessionNumbers(sids)
				.setHost(host)
				.setUserDataType(AlarmHistoryGlobalLadData.ALARM_HISTORY_GLAD_USER_DATA_TYPE)
				.build();
		
        final Tracer log = TraceManager.getTracer(Loggers.GLAD);
        log.debug("Received request for ", ResourceUris.alarmHistoryURI,
                  " with params=", query);

		final StreamingResponseBody stream = new StreamingResponseBody() {
			
			@Override
			public void writeTo(final OutputStream output) throws IOException {
				final Map<Object, Collection<IGlobalLADData>> results = glad.get(query, 2);

				final TreeSet<IGlobalLADData> flat = new TreeSet<IGlobalLADData>(GlobalLadUtilities.flattenMap(GlobalLadPrimaryTime.EVENT, results, true));
				
				log.trace("Flattened map= ", flat.size(), " from ", results.size(), " results: ", results);
				
				IGlobalLADData last = null;
				IGlobalLADData nextLast = null;

				if(!flat.isEmpty()) {
					last = flat.pollLast();

					writeAlarmObjectToStream(output, last, log);

					while(!flat.isEmpty()) {
						nextLast = flat.pollLast();

						if(last.getIdentifier().equals(nextLast.getIdentifier())) {
							writeAlarmObjectToStream(output, nextLast, log);
						}
						else {
							break;
						}
					}
				}
			}
		};

		return stream;
	}
	
	// MPCS-10659 03/20/19 - streamlined by moving the stuff that makes the packet to IGlobalLADData
	private void writeAlarmObjectToStream(final OutputStream output, final IGlobalLADData alarms, final Tracer log) throws IOException {
		if (alarms != null) {
            log.debug("serialized ", alarms.serialize().toString(), " from flat =", alarms.serialize().toByteArray());
            output.write(alarms.toPacketByteArray());
		} else {
			//still want to return something, even if it's empty
			byte[] wireData = Proto3GlobalLadTransport.newBuilder().build().toByteArray();

			output.write(AbstractGlobalLadData.GLAD_PACKET_START_WORD);
			output.write(ByteBuffer.allocate(Integer.BYTES).putInt(wireData.length + Integer.BYTES).array());
			output.write(wireData);
		}
	}
	

}
	
