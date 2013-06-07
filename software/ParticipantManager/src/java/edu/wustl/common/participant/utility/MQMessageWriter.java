/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue-participant-manager/LICENSE.txt for details.
 */

package edu.wustl.common.participant.utility;

import java.io.IOException;

import com.ibm.mq.MQC;
import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;

import edu.wustl.common.util.logger.Logger;


/**
 * The Class MQMessageWriter.
 */
public class MQMessageWriter
{

	/** The host name. */
	private String hostName;

	/** The port. */
	private int port;

	/** The q manager name. */
	private String qManagerName;

	/** The channel name. */
	private String channelName;

	/** The q name. */
	private String qName;

	/** The Constant logger. */
	private static final Logger logger = Logger
			.getCommonLogger(MQMessageWriter.class);

	/**
	 * Sets the host name.
	 *
	 * @param hostName the new host name
	 */
	public void setHostName(String hostName)
	{
		this.hostName = hostName;
	}

	/**
	 * Gets the host name.
	 *
	 * @return the host name
	 */
	public String getHostName()
	{
		return hostName;
	}

	/**
	 * Sets the port.
	 *
	 * @param port the new port
	 */
	public void setPort(int port)
	{
		this.port = port;
	}

	/**
	 * Gets the port.
	 *
	 * @return the port
	 */
	public int getPort()
	{
		return port;
	}

	/**
	 * Sets the q manager name.
	 *
	 * @param qManagerName the new q manager name
	 */
	public void setQManagerName(String qManagerName)
	{
		this.qManagerName = qManagerName;
	}

	/**
	 * Gets the q manager name.
	 *
	 * @return the q manager name
	 */
	public String getQManagerName()
	{
		return qManagerName;
	}

	/**
	 * Sets the channel name.
	 *
	 * @param channelName the new channel name
	 */
	public void setChannelName(String channelName)
	{
		this.channelName = channelName;
	}

	/**
	 * Gets the channel name.
	 *
	 * @return the channel name
	 */
	public String getChannelName()
	{
		return channelName;
	}

	/**
	 * Sets the q name.
	 *
	 * @param qName the new q name
	 */
	public void setQName(String qName)
	{
		this.qName = qName;
	}

	/**
	 * Gets the q name.
	 *
	 * @return the q name
	 */
	public String getQName()
	{
		return qName;
	}

	/**
	 * Method to send MQ text messages.
	 *
	 * @param messageStr Message String
	 */
	@SuppressWarnings("unchecked")
	public void sendTextMessage(String messageStr)
	{
		try
		{
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
		}
		catch (IOException e)
		{
			new RuntimeException("Can not write message to the queue" + e);
		}
		catch (MQException e)
		{
			// TODO Auto-generated catch block
			logger.info(e.getMessage());
		}
	}

}
