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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.SslConfigurator;
import org.springframework.context.ApplicationContext;

import jpl.gds.globallad.GlobalLadException;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.GlobalLadDataIterator;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.container.IGlobalLadDeltaQueryable.DeltaQueryStatus;
import jpl.gds.globallad.data.factory.GenericGlobalLadDataFactory;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory;
import jpl.gds.globallad.data.utilities.GlobalLadQueryParams.GlobalLadQueryParamsBuilder;
import jpl.gds.globallad.rest.resources.ResourceUris;
import jpl.gds.security.ssl.ISslConfiguration;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Base class for querying the global lad within MPCS.
 */
public class BaseGlobalLadQuery implements IGlobalLadQuery {
	private static final String SEP = "/";
	
	/**
	 * URI for the global lad base resource.
	 */
	private final IGlobalLadDataFactory factory;
	
	/**
	 * Must keep track of the socket port, REST port and host.
	 * When getting the  query URL we must check to see if either has changed and to update the url.  
	 * We don't want to do this for every query so keep track of the values when the url is
	 * updated.
	 */
	private String host;
	private int restPort;
	private int socketServerPort;
	
	private String url;
    protected final ApplicationContext  appContext;

    private final Tracer                log;

	/**
	 * @throws Exception
	 */
    public BaseGlobalLadQuery(final ApplicationContext appContext) throws Exception {
        this(new GenericGlobalLadDataFactory(), appContext);
	}
	
	/**
	 * @param factory
	 */
    public BaseGlobalLadQuery(final IGlobalLadDataFactory factory, final ApplicationContext appContext) {
		final GlobalLadProperties config = GlobalLadProperties.getGlobalInstance();
        this.appContext = appContext;
		restPort = config.getRestPort();
		socketServerPort = config.getSocketServerPort();
		host = config.getServerHost();
		
		url = config.getFormattedRestURI();
        this.log = TraceManager.getTracer(appContext, Loggers.GLAD);
		
		this.factory = factory;
	}
	
	/**
	 * Checks to see if a URL update is required, meaning the rest or socket server ports were changed or
	 * the server host was changed.  If it is all values will be updated.
	 */
	public void checkUrl() {
		final GlobalLadProperties config = GlobalLadProperties.getGlobalInstance();
		
		final int restPort_ = config.getRestPort();
		final int socketServerPort_ = config.getSocketServerPort();
		final String host_ = config.getServerHost();

        if (config.getRestPort() != restPort || config.getSocketServerPort() != socketServerPort
                || !config.getServerHost().equals(host)) {
			
			restPort = config.getRestPort();
			socketServerPort = config.getSocketServerPort();
			host = config.getServerHost();
			url = config.getFormattedRestURI();
		}
        if (config.isHttpsEnabled() && !url.startsWith("https")) {
            url = StringUtils.replace(url, "http", "https");
        }
	}
	
	/**
	 * Builds the request and creates a response stream for the binary data response.
	 * 
	 * This is no longer assumed to be a binary response.
	 * 
	 * @param params
	 * @return
	 */
	public InputStream getResponseStream(final GlobalLadQueryParams params, final boolean binaryResponse) {
		
		/**
		 * Construct the path for the query.
		 */
		if (params.getQueryType() == null || 
			params.getSource() == null || 
			params.getRecordedState() == null || 
			params.getTimeType() == null) {
			throw new IllegalStateException("Query type, source, recorded state and time type must be set for a Global LAD query.");
		}
		
		final StringBuilder pathBuilder = new StringBuilder();
		pathBuilder.append(params.getQueryType().toString());
		pathBuilder.append(SEP);

		pathBuilder.append(params.getSource().toString());
		pathBuilder.append(SEP);

		pathBuilder.append(params.getRecordedState().toString());
		pathBuilder.append(SEP);

		// The enums for the time types are upper case and the query expects only lower. 
		pathBuilder.append(params.getTimeType().toString().toLowerCase());
		
		final String path = pathBuilder.toString();

        final GlobalLadProperties config = GlobalLadProperties.getGlobalInstance();
        final ClientBuilder clientBuilder = ClientBuilder.newBuilder();

        /**
         * Check if ssl is enabled. If it is, update he client to use an ssl context.
         * https://stackoverflow.com/questions/38173883/behavior-of-httpsurlconnectioin-with-default-implementation-of-hostnameverifier
         * 
         * Retrieve HTTPS (SSL/TLS) properties from, and set its secure status.
         */
        final ISslConfiguration springSslConfig = appContext.getBean(ISslConfiguration.class);
        springSslConfig.setSecure(config.isHttpsEnabled());
        log.debug(springSslConfig);

        if (springSslConfig.isSecure()) {
            final SslConfigurator sslConfig = SslConfigurator.newInstance()
                                                             .trustStoreFile(springSslConfig.getTruststorePath())
                                                             .trustStorePassword(springSslConfig.getTruststorePassword())
                                                             .keyStoreFile(springSslConfig.getKeystorePath())
                                                             .keyPassword(springSslConfig.getKeystorePassword());

            final SSLContext sslContext = sslConfig.createSSLContext();
            clientBuilder.sslContext(sslContext);
            clientBuilder.hostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());
        }

        final Client client = clientBuilder.build();
		
		/**
		 * Check and update the url if necessary.
		 */
		checkUrl();
		
		WebTarget wt = client.target(url).path(path);
		
		// All collections can be null or empty.
		if (params.hasChannelIdRegexes()) {
			wt = wt.queryParam(ResourceUris.channelIdQP, params.getChannelIdRegexes().toArray());
		}

		/**
		 * Adding evr IDs.
		 */
		if(params.hasEvrIds()) {
			wt = wt.queryParam(ResourceUris.evrIdQP, params.getEvrIds().toArray());
		}
		
		if (params.hasEvrLevels()) {
			wt = wt.queryParam(ResourceUris.evrLevelQP, params.getEvrLevelRegexes().toArray());
		}

		if (params.hasEvrNameRegexes()) {
			wt = wt.queryParam(ResourceUris.evrNameQP, params.getEvrNameRegexes().toArray());
		}

		if (params.hasVcids()) {
			wt = wt.queryParam(ResourceUris.vcidQP, params.getVcids().toArray());
		}

		if (params.hasDssIds()) {
			wt = wt.queryParam(ResourceUris.dssIdQP, params.getDssIds().toArray());
		}

		if (params.hasVenueRegexes()) {
			wt = wt.queryParam(ResourceUris.venueQP, params.getVenueRegexes().toArray());
		}

		if (params.hasHostRegexes()) {
			wt = wt.queryParam(ResourceUris.hostQP, params.getHostRegexes().toArray());
		}

		if (params.hasSessionIds()) {
			wt = wt.queryParam(ResourceUris.sessionIdQP, params.getSessionIds().toArray());
		}

		if (params.hasMessagesRegexes()) {
			wt = wt.queryParam(ResourceUris.evrMessageQP, params.getMessageRegexes().toArray());
		}
		
		if (params.getScid() != null) {
			wt = wt.queryParam(ResourceUris.scidQP, params.getScid());
		}

		if (params.getLowerBoundTimeString() != null) {
			wt = wt.queryParam(ResourceUris.lowerBoundTimeQP, params.getLowerBoundTimeString());
		}

		if (params.getUpperBoundTimeString() != null) {
			wt = wt.queryParam(ResourceUris.upperBoundTimeQP, params.getUpperBoundTimeString());
		}
		
		/**
		 * Adding output format.
		 */
		wt = wt.queryParam(ResourceUris.outputFormatQP, params.getOutputFormat());
		
		final Integer maxResults = params.getMaxResults();
		if (maxResults != null && maxResults > 0) {
			wt = wt.queryParam(ResourceUris.maxResultsQP, maxResults);
		}

		wt = wt.queryParam(ResourceUris.verifiedQP, params.isVerified());
		wt = wt.queryParam(ResourceUris.showColHeadersQP, params.isShowColumnHeaders());
		
        log.debug("GlobalLad @ ", url, path, "&params=", params);
		
		return wt.queryParam(ResourceUris.binaryResponseQP, binaryResponse)
				.request(new MediaType[] {MediaType.APPLICATION_OCTET_STREAM_TYPE})
				.get(InputStream.class);
	}

    /**
     * @param path
     *            the path
     * @param params
     *            query parameters
     * @param binaryResponse
     *            true for binary response.
     * @return streaming response from the server
     */
    public InputStream getResponseStream(final String path, final GlobalLadQueryParams params,
                                         final boolean binaryResponse) {

        final GlobalLadProperties config = GlobalLadProperties.getGlobalInstance();
        final ClientBuilder clientBuilder = ClientBuilder.newBuilder();

        /*
         * Retrieve HTTPS (SSL/TLS) properties from, and set its secure status.
         * NOTE: Setting secure status to true will set all JAVAX System Properties appropriately.
         * Setting secure status to false will NOT set any JAVAX System Properties
         */
        final ISslConfiguration springSslConfig = appContext.getBean(ISslConfiguration.class);
        springSslConfig.setSecure(config.isHttpsEnabled());

        if (springSslConfig.isSecure()) {
            final SslConfigurator sslConfig = SslConfigurator.newInstance()
                                                             .trustStoreFile(springSslConfig.getTruststorePath())
                                                             .trustStorePassword(springSslConfig.getTruststorePassword())
                                                             .keyStoreFile(springSslConfig.getKeystorePath())
                                                             .keyPassword(springSslConfig.getKeystorePassword());

            final SSLContext sslContext = sslConfig.createSSLContext();
            clientBuilder.sslContext(sslContext);
            clientBuilder.hostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());
        }

        /**
         * Check and update the url if necessary.
         */
        checkUrl();

        final Client client = ClientBuilder.newClient();
        WebTarget wt = client.target(url).path(path);

        // All collections can be null or empty.
        if (params.hasChannelIdRegexes()) {
            wt = wt.queryParam(ResourceUris.channelIdQP, params.getChannelIdRegexes().toArray());
        }

        /**
         * Adding evr IDs.
         */
        if (params.hasEvrIds()) {
            wt = wt.queryParam(ResourceUris.evrIdQP, params.getEvrIds().toArray());
        }

        if (params.hasEvrLevels()) {
            wt = wt.queryParam(ResourceUris.evrLevelQP, params.getEvrLevelRegexes().toArray());
        }

        if (params.hasEvrNameRegexes()) {
            wt = wt.queryParam(ResourceUris.evrNameQP, params.getEvrNameRegexes().toArray());
        }

        if (params.hasVcids()) {
            wt = wt.queryParam(ResourceUris.vcidQP, params.getVcids().toArray());
        }

        if (params.hasDssIds()) {
            wt = wt.queryParam(ResourceUris.dssIdQP, params.getDssIds().toArray());
        }

        if (params.hasVenueRegexes()) {
            wt = wt.queryParam(ResourceUris.venueQP, params.getVenueRegexes().toArray());
        }

        if (params.hasHostRegexes()) {
            wt = wt.queryParam(ResourceUris.hostQP, params.getHostRegexes().toArray());
        }

        if (params.hasSessionIds()) {
            wt = wt.queryParam(ResourceUris.sessionIdQP, params.getSessionIds().toArray());
        }

        if (params.hasMessagesRegexes()) {
            wt = wt.queryParam(ResourceUris.evrMessageQP, params.getMessageRegexes().toArray());
        }

        if (params.getScid() != null) {
            wt = wt.queryParam(ResourceUris.scidQP, params.getScid());
        }

        if (params.getLowerBoundTimeString() != null) {
            wt = wt.queryParam(ResourceUris.lowerBoundTimeQP, params.getLowerBoundTimeString());
        }

        if (params.getUpperBoundTimeString() != null) {
            wt = wt.queryParam(ResourceUris.upperBoundTimeQP, params.getUpperBoundTimeString());
        }

        /**
         * Adding output format.
         */
        wt = wt.queryParam(ResourceUris.outputFormatQP, params.getOutputFormat());

        final Integer maxResults = params.getMaxResults();
        if (maxResults != null && maxResults > 0) {
            wt = wt.queryParam(ResourceUris.maxResultsQP, maxResults);
        }

        wt = wt.queryParam(ResourceUris.verifiedQP, params.isVerified());
        wt = wt.queryParam(ResourceUris.showColHeadersQP, params.isShowColumnHeaders());

        return wt.queryParam(ResourceUris.binaryResponseQP, binaryResponse)
                 .request(new MediaType[] { MediaType.APPLICATION_OCTET_STREAM_TYPE }).get(InputStream.class);
    }
		
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.utilities.IGlobalLadQuery#ladQuery(jpl.gds.globallad.data.utilities.GlobalLadQueryParams.GlobalLadQueryParamsBuilder)
	 */
	@Override
	public Collection<IGlobalLADData> ladQuery(final GlobalLadQueryParamsBuilder builder) throws GlobalLadException {
		/**
		 * This is an unverified query so make sure verified is false in the builder.
		 */
		final InputStream is = getResponseStream(builder.setVerified(false).build(), true);

		try (GlobalLadDataIterator di = new GlobalLadDataIterator(is, factory, false)) {
			return di.getAllData();
		} catch (final IOException e) {
			throw new GlobalLadException(e);
		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.utilities.IGlobalLadQuery#ladQueryVerified(jpl.gds.globallad.data.utilities.GlobalLadQueryParams.GlobalLadQueryParamsBuilder)
	 */
	@Override
	public Map<DeltaQueryStatus, Collection<IGlobalLADData>> ladQueryVerified(final GlobalLadQueryParamsBuilder builder) throws GlobalLadException {
		/**
		 * This is a verified query so make sure verified is true in the builder.
		 */
		final InputStream is = getResponseStream(builder.setVerified(true).build(), true);

		try (GlobalLadDataIterator di = new GlobalLadDataIterator(is, factory, true)) {
			return di.getAllDataVerified();
		} catch (final IOException e) {
			throw new GlobalLadException(e);
		}

	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.globallad.data.utilities.IGlobalLadQuery#getLadQueryIterator(jpl.gds.common.globallad.data.utilities.GlobalLadQueryParams.GlobalLadQueryParamsBuilder)
	 */
	@Override
	public GlobalLadDataIterator getLadQueryIterator(final GlobalLadQueryParamsBuilder builder, final boolean isVerified) {
		final InputStream is = getResponseStream(builder.build(), true);
		
		return new GlobalLadDataIterator(is, factory, isVerified);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.globallad.data.utilities.IGlobalLadQuery#getLadQueryIterator(java.lang.String, jpl.gds.globallad.data.utilities.GlobalLadQueryParams.GlobalLadQueryParamsBuilder, boolean)
	 */
	@Override
	public GlobalLadDataIterator getLadQueryIterator(final String path, final GlobalLadQueryParamsBuilder builder, final boolean isVerified) {
        final InputStream is = getResponseStream(path, builder.build(), true);

		return new GlobalLadDataIterator(is, factory, isVerified);
	}
}
