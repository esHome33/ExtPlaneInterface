package org.cutre.soft;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.cutre.soft.epi.command.CommandMessage;
import org.cutre.soft.epi.command.DataRefCommand;
import org.cutre.soft.epi.command.ExtPlaneCommand;
import org.cutre.soft.epi.communication.ExtPlaneTCPReceiver;
import org.cutre.soft.epi.communication.ExtPlaneTCPSender;
import org.cutre.soft.epi.data.DataRef;
import org.cutre.soft.epi.data.DataRefRepository;
import org.cutre.soft.epi.data.DataRefRepositoryImpl;
import org.cutre.soft.epi.data.MessageRepository;
import org.cutre.soft.epi.data.MessageRepositoryImpl;
import org.cutre.soft.epi.util.Constants.DataType;
import org.cutre.soft.epi.util.ObservableAware;
import org.cutre.soft.epi.util.Observer;
import org.cutre.soft.exception.ConnectionException;

/**
 * 
 * Copyright (C) 2015  Pau G.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Pau G.
 */
public class ExtPlaneInterface {
    
    private static final Logger LOGGER = Logger.getLogger(ExtPlaneInterface.class);
    
    private Socket socket;
    
    private DataRefRepository dataRefrepository;
    private MessageRepository messageRepository;
    
    private ExtPlaneTCPReceiver receive = null;
    private ExtPlaneTCPSender sender = null;
    
    private String server;
    private int port;
    private int poolSize = 2;
    
    //public static final PatternLayout LOGGER_LAYOUT = new PatternLayout("%d{ISO8601} [%t] %-5p %c %x - %m%n");
    
    public ExtPlaneInterface(String server, int port) {
    	
    	//PropertyConfigurator.configure("log4j.properties");
    	//LOGGER.addAppender(new ConsoleAppender(LOGGER_LAYOUT));
    	//LOGGER.info("Log4j correctly configured !");
        
        this.server = server;
        this.port = port;
        
        this.initDataRefRepository();
        this.initMessageRepository();
        
    }
    
    public void excludeDataRef(String dataRefName) {
        this.sendMessage(new DataRefCommand(DataRefCommand.DATAREF_ACTION.UNSUBSCRIBE,dataRefName));
    }
    
    public DataRef getDataRef(String dataRef) {
        return this.dataRefrepository.getDataRef(dataRef);
    }
    
    public DataType getDataRefType(String dataRefName) {
        DataRef dr = this.dataRefrepository.getDataRef(dataRefName);
        
        if(dr!=null) 
            return dr.getDataType();
        
        return null;
    }
    
    public String[] getDataRefValue(String dataRefName) {
        DataRef dr = this.dataRefrepository.getDataRef(dataRefName);
        
        if(dr!=null) 
            return dr.getValue();
        
        return null;
    }
    
    public void includeDataRef(String dataRefName) {
        this.includeDataRef(dataRefName, null);
    }
    
    public void includeDataRef(String dataRefName, Float accuracy) {
        DataRefCommand drc = new DataRefCommand(DataRefCommand.DATAREF_ACTION.SUBSCRIBE,dataRefName);
        
        if(accuracy!=null)
            drc.setAccuracy(accuracy);
        
        this.sendMessage(drc);
    }
    
    public void setDataRefValue(String dataRefName, String... value) {
        this.sendMessage(new DataRefCommand(DataRefCommand.DATAREF_ACTION.SET, dataRefName, value));
    }
    
    public void sendMessage(CommandMessage message) {
        this.messageRepository.sendMessage(message);
    }
    
    public void setExtPlaneUpdateInterval(String interval) {
        this.sendMessage(new ExtPlaneCommand(ExtPlaneCommand.EXTPLANE_SETTING.UPDATE_INTERVAL, interval));
    }
    
    public void start() throws Exception {
        try {
            this.connect();
            this.startSending();
            this.startReceiving();
        } catch(Exception e) {
            LOGGER.error("Error starting services.", e);
            this.stopReceiving();
            this.stopSending();
            throw e;
        }
    }
    
    public void stop() {
        this.stopReceiving();
        this.stopSending();
    }
    
    public void observeDataRef(String dataRefName, Observer<DataRef> observer) {
        ObservableAware.getInstance().addObserver(dataRefName, observer);
    }
    
    public void unObserveDataRef(String dataRefName, Observer<DataRef> observer) {
        ObservableAware.getInstance().removeObserver(dataRefName, observer);
    }
    
    private void connect() throws ConnectionException {
        
        try {
            socket = new Socket(server, port);
        } catch (UnknownHostException e) {
            LOGGER.error("[ExtPlaneInterface::connect] Error connecting host " + server, e);
            throw new ConnectionException("Error connecting host -> " + server, e);
        } catch (IOException e) {
            LOGGER.error("[ExtPlaneInterface::connect] Error connecting host " + server, e);
            throw new ConnectionException("Error connecting host -> " + server, e);
        }
    }
    
    private void initDataRefRepository() {
        this.dataRefrepository = new DataRefRepositoryImpl();
    }
    
    private void initMessageRepository() {
        this.messageRepository = new MessageRepositoryImpl();
    }
    
    private void startReceiving() {
        receive = new ExtPlaneTCPReceiver(socket, dataRefrepository, poolSize);
        receive.start();
    }
    
    private void startSending() {
        sender = new ExtPlaneTCPSender(socket, messageRepository);
        sender.start();
    }
    
    private void stopReceiving() {
        if(this.receive!=null) 
            this.receive.setKeep_running(false);
        
        this.receive = null;
    }
    
    private void stopSending() {
        if(this.sender!=null) 
            this.sender.setKeep_running(false);
        
        this.sender = null;
    }
    
    /**
     * Checking if XPlane is running or not. This function doesn't work because a broken socket 
     * doesn't make any change in the runnables "sender" and "receive"
     * 
     * @return true or false if one of receiver or sender is down.
     */
/*    private boolean isAlive() {
    	if(this.sender.isAlive() && receive.isAlive()) {
    		return true;
    	} else {
    		return false;
    	}
    }
*/
    
    
    /**
     * Checking if XPlane is running or down : with this function, applications that are using 
     * ExtPlaneInterface can bail out properly in case XPlane has unexpectedly shut down. 
     * Otherwise, this app will wait a long time and perhaps crash.
     *
     * This function uses the explanation about checking sockets in Java :
     * https://stackoverflow.com/questions/17147352/checking-if-server-is-online-from-java-code
     * 
     * The check method : create a new socket between us and ExtPlane and wait enough for the 
     * connection to achieve, in order to receive good news (or not) !
     * 
     * @param timout_duration 	timout duration in ms before an Exception is raised. Use 200 ms or a little more 
     * 							it depends on your system and WiFi (be careful not to set too long !). 
     * 							0 is immediate : your connexion will never be online because the connect 
     * 							function cannot return something immediately !
     * 
     * @return true 			if XPlane is running, false if not.
     */
    public boolean isOnline(int timout_duration) {
        boolean b = true;
        try{
            InetSocketAddress sa = new InetSocketAddress(server, port);
            Socket ss = new Socket();
            ss.connect(sa, timout_duration);  
            ss.close();
            sa = null;
            ss = null;
        }catch(Exception e) {
        	//LOGGER.error("isOnline " + e.getMessage());
            b = false;
        }
        return b;
    }
    
}
