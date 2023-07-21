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
package jpl.gds.globallad.spring.mvc.resources;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jpl.gds.globallad.GlobalLadException;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.LocalSolarTime;
import jpl.gds.shared.time.SclkFmt;
import jpl.gds.shared.time.SclkScetUtility;
import jpl.gds.shared.time.TimeProperties;

/**
 * Abstract class with convenience methods useful for resource classes.
 */
public abstract class AbstractGlobalLadMVCResource {

	protected static final String URL_WILD_CARD_REGEX = "*";
	protected static final String URL_WILD_CARD_REGEX_REPLACE=".+";
	protected static final String TIME_TYPES_REGEX = StringUtils.join(GlobalLadPrimaryTime.values(), '|');
	protected static final SclkFmt<?> sclkFmt = TimeProperties.getInstance().getSclkFormatter();

	@Autowired
	GlobalLadProperties config;

	/**
	 * Converts the URL regex character in any of the regexes with the java equivalent which would be .+.
	 * 
	 * @param regexes
	 */
	protected Collection<String> convertUrlRegexToRegex(final Collection<String> regexes) {
		if (regexes != null) {
			final Collection<String> converted = new ArrayList<String>();

			final Iterator<String> it = regexes.iterator();

			while (it.hasNext()) {
				converted.add(StringUtils.replace(it.next(), 
						URL_WILD_CARD_REGEX, 
						URL_WILD_CARD_REGEX_REPLACE));
			}

			return converted;
		} else {
			return Collections.<String>emptyList();
		}
	}

	/**
	 * @param regex
	 * @return
	 */
	protected Collection<String> convertUrlRegexToRegex(final String regex) {
		return regex == null ? Collections.<String>emptyList() : convertUrlRegexToRegex(Arrays.asList(regex));
	}

	protected IAccurateDateTime convertTimeString(final String timeString, final GlobalLadPrimaryTime timeType, final Integer scid) throws ParseException, GlobalLadException {
		if (timeString == null) {
			return null;
		}

		switch(timeType) {
		case LST:
			if (scid == null) {
				throw new GlobalLadException("SCID must be supplied when time type is LST");
			}

			/**
			 * Create a LST object and convert it to scet.
			 */
			final ILocalSolarTime lst = new LocalSolarTime(scid, timeString);
			return lst.toScet();
		case SCLK:
			if (scid == null) {
				throw new GlobalLadException("SCID must be supplied when time type is SCLK");
			}
			return SclkScetUtility.getScet(sclkFmt.valueOf(timeString), null, scid);
		case ALL:
		case ERT:
		case EVENT:
		case SCET:
		default:
			return new AccurateDateTime(timeString);
		}

	}

	/**
	 * Checks the time string and will make sure if it is set it is valid or it is enabled.  If either of the former
	 * is true this throw an exception, else it converts the value to the time type and returns it.
	 * 
	 * @param timeType
	 * @return Time type enum for the input string value.
	 * @throws GlobalLadException
	 */
	public GlobalLadPrimaryTime getTimeType(final String timeType) throws GlobalLadException {
		final String timeTypeUpper = timeType.toUpperCase();
		if (!config.isRestTimeTypeEnabled(timeTypeUpper)) {
			throw new GlobalLadException("REST query time type is not enabled: " + timeTypeUpper);
		} else if (timeType != null && !timeType.toUpperCase().matches(TIME_TYPES_REGEX)) {
			throw new GlobalLadException("Unsupported time type: " + timeTypeUpper);
		} else {
			return GlobalLadPrimaryTime.valueOf(timeTypeUpper);
		}
	}
	public ResponseEntity<Object> ok() {
		return ok(null);
	}
	
	public ResponseEntity<Object> serverError(Object body) {
		return getResponse(HttpStatus.INTERNAL_SERVER_ERROR, body);
	}

	public ResponseEntity<Object> ok(Object body) {
		return getResponse(HttpStatus.OK, body);
	}

	public ResponseEntity<Object> notFound(Object body) {
		return getResponse(HttpStatus.NOT_FOUND, body);
	}

	/**
	 * Conv method to send back an empty not found response.
	 * @return
	 */
	public ResponseEntity<Object> notFound() {
		return emptyResponse(HttpStatus.NOT_FOUND);
	}
	
	public ResponseEntity<Object> getResponse(HttpStatus status, Object body) {
		return ResponseEntity.status(status).body(body);
	}

	public ResponseEntity<Object> emptyResponse(HttpStatus status) {
		return getResponse(status, null);
	}
}

