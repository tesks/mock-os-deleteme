/*
 * Copyright 2006-2021. California Institute of Technology.
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
package jpl.gds.globallad.io.socket;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.container.GlobalLadUtilities;
import jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory;
import jpl.gds.globallad.disruptor.IDisruptorProducer;
import jpl.gds.globallad.io.IGlobalLadDataSource;
import jpl.gds.shared.log.Tracer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Socket server that is responsible for managing connections from client applications (downlinks).  It will 
 * listen for connections and start a data consumer to handle the incoming messages as well as monitor the client
 * connections to detect dead connections.
*/
public class GlobalLadSocketServer implements IGlobalLadDataSource {
	private static final String NAME = "GlobalLadSocketServer";
	private static final String CLIENT_NAME_BASE = "GLADClient";
	
	/**
	 * Basic timing and size constants.  These should never need to be changed.
	 */
	private static final Integer SOCKET_TIMEOUT = 5000;
	private static final Integer MAX_ERRORS = 5;
	private static final Integer READ_BUFFER_SIZE = 4096;
	private static final long RUNNING_CHECK_TIME_MS = 2000L; 

	private ServerSocket server;
	
	private final Map<Integer, GladClientMapEntry> clients;

	private int closedClients;
	private int openClients;
	
	/**
	 * A temp file is used to write stats about a client after it has been closed.  It is expected that 
	 * the LAD will be running for long periods of time, and will have many clients.  This allows stats 
	 * to be kept for all clients without taking up a ton of heap.
	 */
	private final File clientJsonTempFile;
	private final int port;
	private final int clientRingBufferSize;
	
	/**
	 * This will be passed to all clients so that global lad data can be added to the global lad.
	 */
	private final ExecutorService executor;

	private final IDisruptorProducer<IGlobalLADData> dataProducer;
	private final IGlobalLadDataFactory factory;
	private final Tracer log;

	/**
	 * @param dataProducer dataproducer 
	 * @param config global lad configuration object
	 * @param factory data factory instance
	 * @param port socket server port number. 
	 * @param log logger
	 * @throws IOException 
	 */
	public GlobalLadSocketServer(final IDisruptorProducer<IGlobalLADData> dataProducer,
			final GlobalLadProperties config, 
			final IGlobalLadDataFactory factory, 
			final int port,
			final Tracer log) throws IOException {

		this.dataProducer = dataProducer;
		this.factory = factory;
		this.log = log;
		this.port = port;
				
		this.clientRingBufferSize = config.getClientRingBufferSize();
				

		clients = new ConcurrentHashMap<Integer, GlobalLadSocketServer.GladClientMapEntry>(5, (float) 0.9, 1);

		/**
		 * This executor has no max on the number of threads needed.
		 */
		executor = Executors.newCachedThreadPool(GlobalLadUtilities.createThreadFactory("glad-data-creation-thread-%d")); 
		
		server = null;
		closedClients = 0;
		clientJsonTempFile = File.createTempFile("clientServerStats", "json");
		clientJsonTempFile.deleteOnExit();
	}
	
	/**
	 * @return true if clients are connected
	 */
	public boolean hasClientConnections() {
		return !clients.isEmpty();
	}
	
	/**
	 * Drops all currently connected clients.
	 * 
	 * @throws IOException
	 */
	public void dropAllClients() throws IOException {
		log.warn("Global LAD socket server received request to close all drop all clients.");
		closeClients();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
    public void run() {
		Thread.currentThread().setName(NAME);
		final long lastCheck = System.currentTimeMillis();
		
		/**
		 * MPCS-7879 - triviski 2/9/2016 - Since this will be managed by Spring and 
		 * it now is going to implement the Closable interface, not using a shutdown 
		 * flag but expecting this to be interrupted when it is time to finish.
		 */
		try {
			server = new ServerSocket(port);
			server.setSoTimeout(SOCKET_TIMEOUT);
			
            log.info("Started Global LAD socket server on port ", port);
			
			// Infinite, waiting to be interrupted.
			while (!Thread.currentThread().isInterrupted()) {
				/**
				 * This accept will return null if nothing connects after socket timeout in order to check the shutdown 
				 * flag.  
				 */
				Socket clientSocket = null;
				try {
					/**
					 * This accept will wait the socket timeout if nothing connects.  There is no need
					 * to do any other waits in this loop.
					 */
					clientSocket = server.accept();
					
                    log.info("Client application connection created for global LAD server.  Local connection on port ",
                            clientSocket.getPort());
				} catch (final SocketTimeoutException e) {
					// Nothing connected, no issue.
				}
				
				if (clientSocket != null) {
					/**
					 * A client connected so start listening to it.  Create a client thread, start it and add to the collection.
					 */
					final GlobalLadSocketClient client = new GlobalLadSocketClient(
							clientSocket.getInputStream(), 
							CLIENT_NAME_BASE + "-" + clientSocket.getPort(), 
							clientRingBufferSize,
							MAX_ERRORS, 
							READ_BUFFER_SIZE, 
							factory,
							executor,
							dataProducer);
					
					client.setDaemon(true);
					client.start();
					
					clients.put(clientSocket.getPort(), new GladClientMapEntry(client, clientSocket));
					openClients++;
				}
				
				/**
				 * Checks the client ports and kills any that are not running anymore.
				 */
				if (System.currentTimeMillis() - lastCheck > RUNNING_CHECK_TIME_MS) {
					for (final Integer port : clients.keySet()) {
						final GladClientMapEntry client = clients.get(port);
						
						if (! client.gladClient.isAlive()) {
							log.debug(String.format("Found a dead client for port %d.  Joining and removing.", port));
							try {
								closeClient(client);
							} catch (final Exception e) {
                                log.error("Failed to close client socket:" + e.getMessage(), e.getCause());
							}
						}
					}
				}
			}
		} catch (final IOException e) {
            log.error("Socket server experienced an IO Error: " + e.getMessage(), e.getCause());
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		/**
		 * Shutdown the client executor.
		 */
		executor.shutdown();
		closeClients();
		
		if (server != null) {
			server.close();
		}
	}

	/**
	 * Closes the client, joins for 30 seconds and adds the client json file. 
	 * 
	 * @param client the client to close
	 * @throws IOException
	 */
	private synchronized void closeClient(final GladClientMapEntry client) throws IOException {
		final int port = client.clientSocket.getPort();
		
		/**
		 * Do not want to do this if already closed.
		 */
		if (!clients.containsKey(port)) {
			return;
		}
		
		try {
            log.info("Closing client on port ", port);
			
			client.gladClient.shutDown();
			/**
			 * Give a max join time
			 */
			client.gladClient.join(30000L);
			closedClients++;
			openClients--;
			
            log.info("Finished closing client on port ", port);
			
			updateClientFile(port, client.gladClient);
		} catch (final InterruptedException e) {
			// Move on.
		} finally {
			// Close the socket in any event.
			client.clientSocket.close();
		}
		clients.remove(port);
	}
	
	/**
	 * Reads the contents of the file, adds the new json to the list and dumps the list back to the JSON file.
	 * 
	 * Synchronizes on the client JSON file.
	 * 
	 * @param port clients port number
	 * @param client client to add to the file
	 */
	private void updateClientFile(final int port, final GlobalLadSocketClient client) {
		synchronized (clientJsonTempFile) {
			final JsonObject json = getClientJsons()
					.add(String.valueOf(port), client.getStats())
					.build();
			try {
				GlobalLadUtilities.writeJsonToFile(clientJsonTempFile, json, false);
			} catch (final FileNotFoundException e2) {
                log.warn("Failed to open/locate the file " + e2.getMessage(), e2.getCause());
				e2.printStackTrace();
			} catch (final IOException e2) {
                log.warn("I/O exception has occured while trying to write Json to file " + e2.getMessage(),
                        e2.getCause());
				e2.printStackTrace();
			}
		}
	}
	
	/**
	 * Reads the data from the client file.
	 * 
	 * Synchronizes on the client JSON file.
	 * @return Object builder with all of the client JSON objects from the client file added
	 */
	private  JsonObjectBuilder getClosedClientJsons() {
		synchronized (clientJsonTempFile) {
			if (clientJsonTempFile.length() == 0) {
				return Json.createObjectBuilder();
			} else {
				try (final FileInputStream is = new FileInputStream(clientJsonTempFile)) {
					final JsonObject clientStats = Json.createReader(is).readObject();
					final JsonObjectBuilder newClients = Json.createObjectBuilder();
					
					for (final Entry<String, JsonValue> entry : clientStats.entrySet()) {
						newClients.add(entry.getKey(), entry.getValue());
					}
					
					return newClients;
				} catch (final Exception e) {
					e.printStackTrace();
				}
				
				return null;
			}
		}
	}
	
	/**
	 * @return JSON builder with all of the open client jsons added.
	 */
	private JsonObjectBuilder getOpenClientsJson() {
		final JsonObjectBuilder builder = Json.createObjectBuilder();
		/**
		 * Add the open clients to the builder with the port number as the key.
		 */
		for (final Entry<Integer, GladClientMapEntry> client : clients.entrySet()) {
			builder.add(client.getKey().toString(),  client.getValue().gladClient.getStats());
		}
		
		return builder;
	}
	
	/**
	 * @return JSON builder with both the open and closed clients added
	 */
	private synchronized JsonObjectBuilder getClientJsons() {
		final JsonObjectBuilder builder = getClosedClientJsons();
		
		/**
		 * Add the open clients to the builder with the port number as the key.
		 */
		for (final Entry<Integer, GladClientMapEntry> client : clients.entrySet()) {
			builder.add(client.getKey().toString(),  client.getValue().gladClient.getStats());
		}
		
		return builder;
	}
	
	/**
	 * @throws IOException - Error closing client socket.
	 */
	public void closeClients() throws IOException {
		for (final GladClientMapEntry clientEntry : clients.values()) {
			closeClient(clientEntry);
		}
		
		log.info("Global lad server done closing and joining client connection");
	}
	
	/**
	 * @return the number of open clients
	 */
	public int numberOpenClients() {
		return openClients;
	}
	
	/**
	 * @return number of closed clients
	 */
	public int numberClosedClients() {
		return closedClients;
	}
	
	/**
	 * @return sum of open and closed clients
	 */
	public int totalNumberClientConnections() {
		return numberOpenClients() + numberClosedClients();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JsonObject getStats() {
		return Json.createObjectBuilder()
				.add("openClients", numberOpenClients())
				.add("closedClients", numberClosedClients())
				.add("totalClients", totalNumberClientConnections())
				.add("closedClients", getClosedClientJsons())
				.add("openClients", getOpenClientsJson())
				.build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JsonObject getMetadata(final IGlobalLadContainerSearchAlgorithm matcher) {
		return getStats();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getJsonId() {
		return "GlobalLadSocketServer";
	}
	
	/**
	 * Class to keep track of open clients.
	 */
	private class GladClientMapEntry {
		GlobalLadSocketClient gladClient;
		Socket clientSocket;
		
		/**
		 * @param gladClient
		 * @param clientSocket
		 */
		public GladClientMapEntry(final GlobalLadSocketClient gladClient,
				final Socket clientSocket) {
			this.gladClient = gladClient;
			this.clientSocket = clientSocket;
		}
	}
}
