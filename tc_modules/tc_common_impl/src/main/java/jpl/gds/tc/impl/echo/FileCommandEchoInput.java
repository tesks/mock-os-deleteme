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
package jpl.gds.tc.impl.echo;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.context.ApplicationContext;

import jpl.gds.tc.api.echo.ICommandEchoMessage;

/**
 * Opens the supplied file and generates CommandEchoMessages with the data
 * inside. The CommandEchoInput is stopped when the end of the file is reached
 * or an I/O is encountered
 * 
 *
 */
public class FileCommandEchoInput extends AbstractCommandEchoInput {
    
    private File inputFile;
    
    FileInputStream fileInputStream;
    private DataInputStream inputStream;
    
    /**
     * Constructor. Requires the file to be read from
     * @param appContext the current ApplicaitonContext
     * @param inputFile the File to be read
     */
    public FileCommandEchoInput(ApplicationContext appContext, File inputFile){
        super(appContext);
        this.inputFile = inputFile;
    }
    
    @Override
    public boolean connect(){
        
        try{
            fileInputStream = new FileInputStream(inputFile);
            inputStream = new DataInputStream(fileInputStream);
            trace.info("File " + inputFile.getPath() + " opened for command echo");
        } catch (IOException e) {
            trace.warn("Command Echo encountered an error with the file " + inputFile.getPath(), e);
        }
        
        if(!isConnected()){
            stopSource();
        }
        
        return isConnected();
    }

    @Override
    public boolean isConnected() {
        return inputStream != null;
    }

    @Override
    protected void disconnect(){
        super.disconnect();
        
        if(inputStream != null){
            try {
                inputStream.close();
            } catch (IOException e) {
                //doesn't matter
            }
            inputStream = null;
        }
        if(fileInputStream != null){
            try {
                fileInputStream.close();
            } catch (IOException e) {
                //doesn't matter
            }
            fileInputStream = null;
        }
        
        trace.info("File " + inputFile.getPath() + " has been closed by command echo");
    }
    
    @Override
    public void ingestData() {
        
        int readBytes = 64;
        byte[] bytes = new byte[readBytes];
        
        ICommandEchoMessage msg;
        
        if(inputStream != null){
        
            trace.info("Begin reading from file...");
            while(readBytes >= 0){
                try {
                    readBytes = inputStream.read(bytes);

                    if(readBytes > 0){
                        msg = msgFactory.createCommandEchoMessage(bytes, 0, readBytes);
                        msgBus.publish(msg);
                    } else if(readBytes == -1){
                        trace.info("End of file reached");
                    }
                } catch (IOException e) {
                    trace.warn("Command Echo encountered an error reading from file " + inputFile.getPath(), e);
                    break;
                }
            }
        }
        stopSource();
    }
}
