package edu.wustl.common.participant.utility;
import java.io.IOException;

import com.ibm.mq.MQException;

import com.ibm.mq.*;

public class MQMessageWriter
{
	private String hostName;
	private int port;
	private String qManagerName;
	private String channelName;
	private String qName;
	
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	public String getHostName() {
		return hostName;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	public int getPort() {
		return port;
	}
	
	public void setQManagerName(String qManagerName) {
		this.qManagerName = qManagerName;
	}
	public String getQManagerName() {
		return qManagerName;
	}
	
	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}
	public String getChannelName() {
		return channelName;
	}
	
	public void setQName(String qName) {
		this.qName = qName;
	}
	public String getQName() {
		return qName;
	}
	/**
	 * Method to send MQ text messages
	 * @param messageStr Message String
	 */
	@SuppressWarnings("unchecked")
	public void sendTextMessage(String messageStr) {
		try {
			MQEnvironment.hostname = getHostName(); 
			MQEnvironment.channel = getChannelName(); 
			MQEnvironment.port = getPort();
			MQEnvironment.properties.put(MQC.TRANSPORT_PROPERTY, MQC.TRANSPORT_MQSERIES);
			
			MQQueueManager mqManager = new MQQueueManager(getQManagerName());
			
			//int openOptions = MQC.MQOO_INPUT_AS_Q_DEF | MQC.MQOO_OUTPUT;
			int openOptions = MQC.MQOO_OUTPUT;
			MQQueue queue = mqManager.accessQueue(getQName(), openOptions);
			
			MQMessage message = new MQMessage();
			message.format = MQC.MQFMT_STRING;
			message.expiry = MQC.MQEI_UNLIMITED;
			message.writeString(messageStr);
			
			queue.put(message);
			
			//close queue
			queue.close(); 
			// Disconnect from the mq manager 
			mqManager.disconnect(); 
		} catch (IOException e) {
			new RuntimeException("Can not write message to the queue" + e);
		} catch (MQException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
}
