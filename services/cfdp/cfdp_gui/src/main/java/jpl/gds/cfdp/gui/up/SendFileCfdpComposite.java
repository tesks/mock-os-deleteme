/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.cfdp.gui.up;

import jpl.gds.cfdp.clt.ampcs.properties.CfdpCltAmpcsProperties;
import jpl.gds.cfdp.common.GenericPropertiesResponse;
import jpl.gds.cfdp.common.GenericPropertiesSetRequest;
import jpl.gds.cfdp.common.GenericRequest;
import jpl.gds.cfdp.common.action.EActionCommandType;
import jpl.gds.cfdp.common.action.put.PutActionRequest;
import jpl.gds.cfdp.common.action.put.PutActionResponse;
import jpl.gds.cfdp.common.config.CfdpCommonProperties;
import jpl.gds.cfdp.common.config.EConfigurationPropertyKey;
import jpl.gds.cfdp.gui.config.CfdpGuiProperties;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.config.security.options.AccessControlCommandOptions;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.security.cam.AccessControl;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.types.Pair;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.types.UnsignedLong;
import jpl.gds.tc.api.ISendCompositeState;
import jpl.gds.tc.api.message.ICommandMessageFactory;
import jpl.gds.tc.api.message.ITransmittableCommandMessage;
import jpl.gds.tcapp.app.gui.AbstractSendComposite;
import jpl.gds.tcapp.app.gui.TransmitEvent;
import jpl.gds.tcapp.app.gui.UplinkExecutors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

import static jpl.gds.cfdp.clt.ENonActionCommandType.CONFIG;
import static jpl.gds.cfdp.common.action.EActionCommandType.MTU_MAP;

/**
 * This file configures and displays a CFDP file send GUI tab
 * in the uplink application
 *
 */
public class SendFileCfdpComposite extends AbstractSendComposite {

	/** title of the tab */
	public static final String TITLE = "Send File Cfdp";

	// Do NOT initialize this group!
	/** Name of the file on the local system */
	protected Text localFileText;
	/**
	 * button for displaying the system browser used to find the file to be sent
	 */
	protected Button browseButton;
	/** Name to be given to the file on the spacecraft */
	protected Text destinationFileText;
	/** drop-down combo box for the type of file being sent */
	protected Combo destinationEntityCombo;
	
	protected Combo serviceClassCombo;
	
	protected static final String SC_DEFAULT = "DEFAULT";

	protected Combo userMessageCombo;

	private FileCfdpEventHandler handler = null;

	private final CfdpCommonProperties cfdpProps;
	private final CfdpGuiProperties guiProps;
	
	private final HttpHeaders headers;
	private static AccessControl ACCESS_CONTROL = null;
	
	protected String cfdpProcessorHost;
    protected int cfdpProcessorPort;
    protected String cfdpProcessorUriRoot;
    protected String cfdpProcessorHttpType;
    
    protected Collection<String> allowableMessages = new ArrayList<>();
    

	/**
	 * Constructor
	 * 
	 * @param appContext
	 *            the ApplicationContext used by this uplink application
	 * @param parent
	 *            the parent composite window
	 */
	public SendFileCfdpComposite(final ApplicationContext appContext, final Composite parent) {
		super(appContext, parent, false);
		this.cfdpProps = appContext.getBean(CfdpCommonProperties.class);
		this.guiProps = appContext.getBean(CfdpGuiProperties.class);
		
		verifySecurityAccess();
        final String cookie = ACCESS_CONTROL == null ? "" : ACCESS_CONTROL.getSsoCookie().toString();
        headers = new HttpHeaders();
        headers.add("Cookie", cookie);
        
        final CfdpCltAmpcsProperties cfdpCltProperties = new CfdpCltAmpcsProperties();
        cfdpProcessorHost = cfdpCltProperties.getCfdpProcessorHostDefault();
        cfdpProcessorPort = cfdpCltProperties.getCfdpProcessorPortDefault();
        cfdpProcessorUriRoot = cfdpCltProperties.getCfdpProcessorUriRoot()
                + (cfdpCltProperties.getCfdpProcessorUriRoot().endsWith("/") ? "" : "/");
        cfdpProcessorHttpType = cfdpCltProperties.getCfdpProcessorHttpType();
        
        setDefaultState(); 
        
	}

	@Override
	public Point getSize() {
		return (new Point(625, 320));
	}

	@Override
	protected EventHandler getEventHandler() {
		if (handler == null) {
			handler = new FileCfdpEventHandler();
		}

		return (handler);
	}

	/**
	 * Creates the GUI controls.
	 */
	@Override
	protected Control createBodyControls() {
		
		final Label putFileLabel = new Label(this, SWT.LEFT);
		putFileLabel.setText("PUT:");
		final FormData pflfd = new FormData();
		pflfd.left = new FormAttachment(0);
		pflfd.right = new FormAttachment(0, SWT.LEFT);
		pflfd.top = new FormAttachment(0, 5);
		
		localFileText = new Text(this, SWT.SINGLE | SWT.BORDER);
		final FormData fd15 = SWTUtilities.getFormData(localFileText, 1, 15);
		fd15.left = new FormAttachment(25);
		fd15.right = new FormAttachment(80);
		fd15.top = new FormAttachment(putFileLabel, 0, SWT.LEFT);
		localFileText.setLayoutData(fd15);
		localFileText.addKeyListener(getEventHandler());
		
		final Label localFileLabel = new Label(this, SWT.LEFT);
		localFileLabel.setText("Local File (in uplink root):");
		final FormData fd16 = new FormData();
		fd16.left = new FormAttachment(0);
		fd16.right = new FormAttachment(localFileText, 10);
		fd16.top = new FormAttachment(localFileText, 0, SWT.CENTER);
		localFileLabel.setLayoutData(fd16);

		browseButton = new Button(this, SWT.PUSH);
		browseButton.setText("Browse...");
		final FormData bfd = new FormData();
		bfd.left = new FormAttachment(localFileText, 10);
		bfd.top = new FormAttachment(localFileText, 0, SWT.CENTER);
		bfd.right = new FormAttachment(100);
		browseButton.setLayoutData(bfd);
		browseButton.addSelectionListener(getEventHandler());

		destinationFileText = new Text(this, SWT.SINGLE | SWT.BORDER);
		final FormData dftFormData = SWTUtilities.getFormData(destinationFileText, 1, 15);
		dftFormData.left = new FormAttachment(25);
		dftFormData.right = new FormAttachment(80);
		dftFormData.top = new FormAttachment(localFileText, 0, SWT.LEFT);
		destinationFileText.setLayoutData(dftFormData);
		destinationFileText.addKeyListener(getEventHandler());

		final Label destinationFileLabel = new Label(this, SWT.LEFT);
		destinationFileLabel.setText("Destination File Name:");
		final FormData tflFormData = new FormData();
		tflFormData.left = new FormAttachment(0);
		tflFormData.right = new FormAttachment(localFileText, 10);
		tflFormData.top = new FormAttachment(destinationFileText, 0, SWT.CENTER);
		destinationFileLabel.setLayoutData(tflFormData);
		
		destinationEntityCombo = new Combo(this, SWT.DROP_DOWN);
		final List<String> entities = appContext.getBean(CfdpCommonProperties.class).getMnemonics();
		if (entities != null && !entities.isEmpty()) {
			for (final String type : entities) {
				destinationEntityCombo.add(type);
			}
		}
		final FormData dentFormData = new FormData();
		dentFormData.left = new FormAttachment(25);
		dentFormData.right = new FormAttachment(65);
		dentFormData.top = new FormAttachment(destinationFileText, 0, SWT.LEFT);
		destinationEntityCombo.setLayoutData(dentFormData);

		final Label destinationEntityLabel = new Label(this, SWT.LEFT);
		destinationEntityLabel.setText("Destination Entity:");
		final FormData delFormData = new FormData();
		delFormData.top = new FormAttachment(destinationEntityCombo, 0, SWT.CENTER);
		delFormData.left = new FormAttachment(0);
		delFormData.right = new FormAttachment(destinationEntityCombo);
		destinationEntityLabel.setLayoutData(delFormData);
		
		serviceClassCombo = new Combo(this, SWT.DROP_DOWN);
		serviceClassCombo.setTextLimit(20);
		serviceClassCombo.add(SC_DEFAULT);
		final List<UnsignedInteger> scTypes = appContext.getBean(CfdpCommonProperties.class).getServiceClasses();
		if (scTypes != null && !scTypes.isEmpty()) {
			for (final UnsignedInteger type : scTypes) {
				serviceClassCombo.add(type.toString());
			}
		}
		final FormData scFormData = new FormData();
		scFormData.left = new FormAttachment(25);
		scFormData.right = new FormAttachment(65);
		scFormData.top = new FormAttachment(destinationEntityCombo, 0, SWT.LEFT);
		serviceClassCombo.setLayoutData(scFormData);

		final Label serviceClassLabel = new Label(this, SWT.LEFT);
		serviceClassLabel.setText("Service Class:");
		final FormData sclFormData = new FormData();
		sclFormData.top = new FormAttachment(serviceClassCombo, 0, SWT.CENTER);
		sclFormData.left = new FormAttachment(0);
		sclFormData.right = new FormAttachment(serviceClassCombo);
		serviceClassLabel.setLayoutData(sclFormData);
		
		
		/* 
		 * MPCS-10532  - 05/16/19 - now a fixed field type, since the allowable
		 * values are fixed and coming from the server.
		 * Left as editable so users can input a value, but if it's allowed or not is validated
		 * against the server 
		 */
		
	    final CfdpGuiProperties tmpProps = appContext.getBean(CfdpGuiProperties.class);
		/*
		 * keep the combo user editable - CFDP server configuration is checked when Send is
		 * pressed to see if arbitrary values are allowed and if not, the invalid argument
		 * pop-up is shown to the user and transmission is blocked.
		 */
		userMessageCombo = new Combo(this, SWT.DROP_DOWN);
        final FormData mtuFormData = new FormData();
        mtuFormData.left = new FormAttachment(25);
        mtuFormData.right = new FormAttachment(65);
        mtuFormData.top = new FormAttachment(serviceClassCombo, 0, SWT.LEFT);
        userMessageCombo.setLayoutData(mtuFormData);
        /*
         * Not setting the values for the combo at this time.
         * This function is called as part of the super (AbstractSendComposite) constructor.
         * The last step of this class's constructor is to initialize the GUI elements to
         * the default state & taht's what will update and add the message to user combo
         * values.
         */

        final Label mtuLabel = new Label(this, SWT.LEFT);
        mtuLabel.setText(tmpProps.getUserMessageFieldTitle());
        final FormData mtulFormData = new FormData();
        mtulFormData.top = new FormAttachment(userMessageCombo, 0, SWT.CENTER);
        sclFormData.left = new FormAttachment(0);
        sclFormData.right = new FormAttachment(userMessageCombo);
        mtuLabel.setLayoutData(mtulFormData);


		return (userMessageCombo);
	}

	/**
	 * Utility function used to clear all file properties
	 */
	public void setDefaultState() {
		localFileText.setText("");
		destinationFileText.setText("");
		destinationEntityCombo.setText("");
		serviceClassCombo.setText(getCurrentServiceClass());
		
		updateMessageToUserField(appContext.getBean(CfdpGuiProperties.class).getUserMessageFieldDefault());
	}

	/**
	 * Get the title of this composite frame
	 * 
	 * @return the String name of this composite
	 * 
	 * @see ChillShell#getTitle()
	 */
	public String getTitle() {
		return TITLE;
	}

	@Override
	protected void send() {
	    
	    final ICommandMessageFactory msgFactory = appContext.getBean(ICommandMessageFactory.class);
	    
		final String localFile = localFileText.getText().trim();
		if (localFile.length() == 0) {
			throw new IllegalArgumentException("Local File field cannot be empty.");
		}

		final String targetFile = destinationFileText.getText().trim();
		if (targetFile.length() == 0) {
			throw new IllegalArgumentException("Target File field cannot be empty.");
		}

		final String deString = destinationEntityCombo.getText().trim().toUpperCase();
		long destinationEntity;
		try {
			destinationEntity = this.cfdpProps.translatePossibleMnemonic(deString);
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException("The supplied destination entity is not a valid entity name or value. " + e.getMessage());
		}
		
		final String userMessage =  this.userMessageCombo.getText();
		
		Byte scByte = null;
		final String scText = serviceClassCombo.getText().trim();
		if(scText != null && !scText.isEmpty() && !scText.equalsIgnoreCase(SC_DEFAULT)) {
			try {
				scByte = Byte.parseByte(scText);
			} catch (final NumberFormatException e) {
				throw new IllegalArgumentException("Supplied service class value of " + scText + " is not a valid value");
			}
		}
		
        IMessage msg = msgFactory.createClearUplinkGuiLogMessage();
        appContext.getBean(IMessagePublicationBus.class).publish(msg);
        logger.debug(msg);
        
		//IFileLoadInfo fileLoadInfo = null;
		final PutActionRequest req = new PutActionRequest();
		req.setRequesterId(GdsSystemProperties.getSystemUserName());
		req.setDestinationEntity(destinationEntity);
		req.setSourceFileName(localFile);
		if(scByte != null) {
			req.setServiceClass(scByte);
		}
		req.setDestinationFileName(targetFile);
		req.setSessionKey(UnsignedLong.valueOf(appContext.getBean(IContextIdentification.class).getNumber()));
		
		//if we don't allow the user to enter a value and it's not on the list...
		//check what the server has to say at the time of sending the request.
		if(!getMtuStrings().contains(userMessage) && !isAllowArbitraryMessageToUser() && isMessageToUserRequired()) {
		    
		    updateMessageToUserField(userMessage);
		    
		    if(userMessage.isEmpty() && isMessageToUserRequired()) {
	            throw new IllegalArgumentException("A value for \"" + guiProps.getUserMessageFieldTitle() + "\" must be supplied");
	        } else {
	            throw new IllegalArgumentException("Invalid value supplied for \"" + guiProps.getUserMessageFieldTitle() + "\". ");
	        }
		}
		
		//if we have an empty string no need to include it at this point if we don't NEED to return a message
		if(!userMessage.isEmpty() || isMessageToUserRequired() ) {
		    final Collection<String> mtu = new ArrayList<>(1);
		    mtu.add(userMessage);
		    req.setMessagesToUser(mtu);
		}
		

        msg = msgFactory.createUplinkGuiLogMessage("Executing send CFDP file: " + req.getSourceFileName() + "\n\n");
        appContext.getBean(IMessagePublicationBus.class).publish(msg);
        
		this.showProgressIndicatorDialog();

		UplinkExecutors.uplinkExecutor.execute(new Runnable() {
			@Override
			public void run() {
			    //TODO: check and update headers on the request?
				final TransmitEvent event = new TransmitEvent();
				final int transmitEventId = event.hashCode();

				final SendFileCfdpInfo sendFileCfdpInfo = new SendFileCfdpInfo(req);
				ResponseEntity<PutActionResponse> resp = null;
				try {
					
					resp = post(EActionCommandType.PUT.getRelativeUri(), req);

					event.setTransmitInfo(SendFileCfdpComposite.this, sendFileCfdpInfo, true,
							"Successfully relayed CFDP file");
					
					resp.getBody().printToTracer(logger);
		        } catch (HttpClientErrorException | HttpServerErrorException hee) {
		        	final String httpCode = "HTTP Status Code " + hee.getStatusCode();
		        	final String respString = hee.getResponseBodyAsString();
		        	final String message = httpCode + ": " + respString
                    + " [POST: " + getAbsoluteUri(EActionCommandType.PUT.getRelativeUri()) + "]";
		            logger.warn(message);
		            
		            event.setTransmitInfo(SendFileCfdpComposite.this, sendFileCfdpInfo, false, hee.getMessage());
		            
		            final List<String> partiallyParsed = Arrays.asList(respString.split("\n"));
		            final List<Pair<String,String>> respStringParsed = new ArrayList<>();
		            for(final String partial : partiallyParsed) {
		            	final String[] pair = partial.split(":");
		            	if(pair.length >= 2) {
		            		respStringParsed.add(new Pair<String,String>(pair[0].trim(), pair[1].trim()));
		            	}
		            }
		            
		            String errMsg = "";
		            
		            for(final Pair<String,String> errPairs : respStringParsed) {
		            	if(errPairs.getOne().equalsIgnoreCase("Error Message")) {
		            		errMsg = errPairs.getTwo();
		            	}
		            }
		            
		            final String dispErrMsg = httpCode + "\n" + errMsg;

		            displayError(dispErrMsg);
		        } catch (final RestClientException rce) {
		        	String message = rce.toString();
		            logger.warn(message);
		            
		            event.setTransmitInfo(SendFileCfdpComposite.this, sendFileCfdpInfo, false, rce.getMessage());
		            
		            if (headers == null) {
		            	final String authErr = "Server may require authentication. Please supply \""+ AccessControlCommandOptions.USER_LONG +"\" option.";
						logger.warn(authErr);
						message += "\n" + authErr;
					}
		            
		            displayError((headers == null ? "Server may require authentication.\n" : "") + rce.getCause().toString());
		            
				} catch (final Exception e) {
					final String message = e.getMessage() == null ? e.toString() : e.getMessage();
					appContext.getBean(IMessagePublicationBus.class).publish(msgFactory.createUplinkGuiLogMessage(
							"Exception encountered while sending CFDP file: " + message + "\n\n"));
					event.setTransmitInfo(SendFileCfdpComposite.this, sendFileCfdpInfo, false, e.getMessage());

					final String errorMessage = "Send CFDP file error: " + message;
					logger.error(errorMessage);

					displayError(message);
				} finally {
					ITransmittableCommandMessage msg = getCfdpFileCommandMessage(req, resp);

					notifyListeners(event);
					// MPCS-12371 1/2022: Updates local history GUI with CFDP request
					// actual message comes out of CFDP processor back end (PutController)
					// and does not get picked up by the history updater
					notifyListeners(msg);
					
					if(event.isSuccessful() && !SendFileCfdpComposite.this.isDisposed()) {
						SWTUtilities.safeAsyncExec(getDisplay(), TITLE, new Runnable() {
							@Override
							public void run() {
								setDefaultState();
							}

						});
					}

					logger.log(appContext.getBean(IStatusMessageFactory.class)
					                     .createPublishableLogMessage(TraceSeverity.INFO,
					                                                  msg.getOneLineSummary(),
					                                                  LogMessageType.UPLINK,
					                                                  CommonMessageType.Log));

					SendFileCfdpComposite.this.closeProgressIndicatorDialog();
				}
			}
		});
	}
	
	private void displayError(final String popUpMessage) {
		if (popUpMessage != null && !popUpMessage.isEmpty() && !SendFileCfdpComposite.this.isDisposed()) {
			SWTUtilities.safeAsyncExec(getDisplay(), TITLE, new Runnable() {
				@Override
				public void run() {
					SWTUtilities.showErrorDialog(getShell(), "Send CFDP File Error", popUpMessage);
				}

			});
		}
	}
	
	protected ResponseEntity<PutActionResponse> post(final String relativeUri, final GenericRequest req) {
        final String absoluteUri = getAbsoluteUri(relativeUri);
        final RequestEntity<GenericRequest> requestEntity = new RequestEntity<>(req, headers, HttpMethod.POST, URI.create(absoluteUri));
        return new RestTemplate().exchange(requestEntity, PutActionResponse.class);

    }
	
	protected String getAbsoluteUri(final String relativeUri) {
        return cfdpProcessorHttpType + "://" + cfdpProcessorHost + ":" + cfdpProcessorPort + cfdpProcessorUriRoot
                + relativeUri;
    }

	private ITransmittableCommandMessage getCfdpFileCommandMessage(final PutActionRequest request,
	                                                               final ResponseEntity<PutActionResponse> resp) {
		if(request == null) {
			throw new IllegalArgumentException("Null PutActionRequest");
		}
		return appContext.getBean(ICommandMessageFactory.class).createCfdpCommandMessage(
				request.toOneLineString(),
				resp != null && resp.getStatusCode().is2xxSuccessful(),
				UnsignedLong.valueOf(appContext.getBean(IContextIdentification.class).getContextKey().getNumber()));
	}
	
	
	
	/**
	 * Security check.
	 */
	public void verifySecurityAccess() {
		
		if(appContext.getBean(SecurityProperties.class).getEnabled()
        && appContext.getBean(IConnectionMap.class).getFswUplinkConnection().getUplinkConnectionType()
                     .equals(UplinkConnectionType.COMMAND_SERVICE)) {
		
		ACCESS_CONTROL = AccessControl.getInstance();
		
		}

	}

	public class FileCfdpEventHandler extends EventHandler {
		protected FileCfdpEventHandler() {
			super();
		}

		@Override
		public void widgetSelected(final SelectionEvent se) {
			super.widgetSelected(se);

			if (se.getSource() == browseButton) {
				String defaultDir = "/";
				String filename = "";
				
				do {
					
					defaultDir = getCurrentDefaultDir();
					
					if(!defaultDir.endsWith("/")) {
						defaultDir = defaultDir + "/";
					}
					
					filename = util.displayFileChooser(false, sendShell, "SendShell", defaultDir);

					if(filename != null) {
						if(filename.startsWith(defaultDir)) {
							filename = filename.substring((defaultDir).length());
							localFileText.setText(filename);
						} else if(!filename.isEmpty()) {
							filename = "";
							SWTUtilities.showMessageDialog(sendShell, "Invalid Directory", "Selected file must be in the directory \n \"" + defaultDir + "\".");
						}
					}
				} while(filename != null && filename.isEmpty());
				
				serviceClassCombo.setText(getCurrentServiceClass());
				
				//when the user has sleected a file, let's update the valid message to user options, since it might have been a while 
				updateMessageToUserField(userMessageCombo.getText());
			}
		}


		@Override
		public void keyPressed(final KeyEvent arg0) {
			//no special handling
		}
	}
	
	private String getCurrentDefaultDir() {
		String retVal = getCfdpServerProperty(EConfigurationPropertyKey.UPLINK_FILES_TOP_LEVEL_DIRECTORY_PROPERTY.getFullPropertyKeyStr());

		// MPCS-11707 - resolve real path
		try {
			retVal = Paths.get(retVal).toRealPath().toString();
		}
		catch (IOException e){
			logger.error("Could not convert default fir to real path", e);
		}
		return retVal == null ? "/" : retVal;
	}
	
	private String getCurrentServiceClass() {
		final String retVal = getCfdpServerProperty(EConfigurationPropertyKey.DEFAULT_SERVICE_CLASS_PROPERTY.getFullPropertyKeyStr());
		
		final boolean present = (retVal != null) && Arrays.asList(serviceClassCombo.getItems()).contains(retVal);
		
		return present ? retVal : SC_DEFAULT;
		
	}
	
	private boolean isMessageToUserRequired() {
	    return Boolean.valueOf(getCfdpServerProperty(EConfigurationPropertyKey.MESSAGES_TO_USER_ALWAYS_REQUIRED_PROPERTY.getFullPropertyKeyStr()));
	}
	
	private boolean isAllowArbitraryMessageToUser() {
	    return Boolean.valueOf(getCfdpServerProperty(EConfigurationPropertyKey.MESSAGES_TO_USER_DIRECT_INPUT_ENABLED_PROPERTY.getFullPropertyKeyStr()));
	}
	
	private String getCfdpServerProperty(final String property) {
		final Properties tmp = getCfdpServerProperties(CONFIG.getRelativeUri() + "/" + property);
		        
		return tmp != null ? tmp.getProperty(property) : "";

	}
	
	private Collection<String> getMtuStrings() {
	    final Properties tmp = getCfdpServerProperties(MTU_MAP.getRelativeUri());
	    
	    return tmp != null ? tmp.stringPropertyNames() : new ArrayList<>();
	}
	
	private Properties getCfdpServerProperties(final String relativeUri) {
	    final RestTemplate restTemplate = new RestTemplate();

	    final String httpMethod = "GET";
	    final String absoluteUri = getAbsoluteUri(relativeUri);
	    final RequestEntity<GenericPropertiesSetRequest> requestEntity = new RequestEntity<>(headers, HttpMethod.resolve(httpMethod), URI.create(absoluteUri));


	    try {
	        final ResponseEntity<GenericPropertiesResponse> resp = restTemplate.exchange(requestEntity, GenericPropertiesResponse.class);

	        final GenericPropertiesResponse body = resp.getBody();

	        return body.getProperties();

	    } catch (final Throwable e) {
	        return null;
	    }
	}
	
	private void updateMessageToUserField(final String defaultMsg) {
	    final Collection<String> validValues = getMtuStrings();
	    final boolean userEdit = isAllowArbitraryMessageToUser();
	    final boolean required = isMessageToUserRequired();
	    
	    if(!allowableMessages.containsAll(validValues) || !validValues.containsAll(allowableMessages)) {
	        this.userMessageCombo.removeAll();
	        
	        validValues.forEach((final String mtu) -> userMessageCombo.add(mtu));
	        
	        allowableMessages = validValues;
	    }
	    
	    if(allowableMessages.contains(defaultMsg) || userEdit) {
	        userMessageCombo.setText(defaultMsg);
	    } else if(required) {
	        userMessageCombo.setText(userMessageCombo.getItem(0));
	    } else {
	        userMessageCombo.setText("");
	    }
	}

	@Override
	public String getDisplayName() {
		return TITLE;
	}

	@Override
	public void setFieldsFromTransmitHistory(final TransmitEvent historyItem) {
		final ISendCompositeState stateObj = historyItem.getTransmitState();

		if (stateObj instanceof SendFileCfdpInfo) {
			final SendFileCfdpInfo info = (SendFileCfdpInfo) stateObj;
			final PutActionRequest oldPAR = info.getAction();
			localFileText.setText(oldPAR.getSourceFileName());
			destinationFileText.setText(oldPAR.getDestinationFileName());
			final String destinationText = cfdpProps.translateIdToMnemonic(String.valueOf(oldPAR.getDestinationEntity()));
			destinationEntityCombo.setText(destinationText);
			final String serviceClass = oldPAR.getServiceClass() > 0 ? String.valueOf(oldPAR.getServiceClass()) : SC_DEFAULT; 
			this.serviceClassCombo.setText(serviceClass);
			//PutActionRequest.messagesToUser supports multiple messages, but this tab only handles one
			final Collection<String> msgs = oldPAR.getMessagesToUser();
			if(msgs != null) {
				final String[] messages = new String[1];
				msgs.toArray(messages);
				this.userMessageCombo.setText(messages[0]);
			}
		}
	}

	@Override
	public boolean needSendButton() {
		return true;
	}
	
	protected static class SendFileCfdpInfo implements ISendCompositeState {

		private final PutActionRequest action;
		
		public SendFileCfdpInfo(final PutActionRequest action) {
			this.action = action;
		}
		
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder(1024);
			sb.append("SendFileCFDP(sourceFile=\"");
			sb.append(this.action.getSourceFileName());
			sb.append("\",requesterID");
			sb.append(this.action.getRequesterId());
			sb.append("\", destinationFile=\"");
			sb.append(this.action.getDestinationFileName());
			sb.append("\",destinationEntity");
			sb.append(this.action.getDestinationEntity());
			sb.append("\",class");
			sb.append(this.action.getClass());
			sb.append("\")");
			
			return sb.toString();
		}
		
		
		@Override
		public String getTransmitSummary() {
			return toString();
		}
		
		public PutActionRequest getAction() {
			return this.action;
		}
		
	}

}
