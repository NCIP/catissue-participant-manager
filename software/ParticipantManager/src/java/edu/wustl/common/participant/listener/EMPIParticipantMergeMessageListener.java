

package edu.wustl.common.participant.listener;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.query.generator.ColumnValueBean;
import edu.wustl.dao.query.generator.DBTypes;

/**
 * The listener interface for receiving EMPIParticipantMergeMessage events.
 * The class that is interested in processing a EMPIParticipantMergeMessage
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addEMPIParticipantMergeMessageListener<code> method. When
 * the EMPIParticipantMergeMessage event occurs, that object's appropriate
 * method is invoked.
 *
 * @see EMPIParticipantMergeMessageEvent
 */
public class EMPIParticipantMergeMessageListener implements MessageListener
{

	/** The Constant logger. */
	private static final Logger LOGGER = Logger
			.getCommonLogger(EMPIParticipantMergeMessageListener.class);

	/**
	 *
	 * This method will be called as soon as the participant demographic message arrives in the queue.
	 * Read teh message from queue and update the participant.
	 *
	 */
	public void onMessage(final Message message)
	{
		String token = null;
		String hl7EventType = "";
		String mrgEmpiId = "";
		String pidEmpiId = "";
		String mergeMessage = "";

		JDBCDAO jdbcdao = null;
		try
		{
			if (message instanceof TextMessage)
			{
				mergeMessage = ((TextMessage) message).getText();
				final StringTokenizer strTokenizer = new StringTokenizer(mergeMessage, "\r");
				while (strTokenizer.hasMoreTokens())
				{
					token = strTokenizer.nextToken().trim();
					//System.out.println(token);

					final StringTokenizer strTokenizer1 = new StringTokenizer(token, "|");
					final String token1 = strTokenizer1.nextToken().trim();
					if (token1.equalsIgnoreCase("EVN"))
					{
						hl7EventType = strTokenizer1.nextToken();
					}
					if (token1.equalsIgnoreCase("PID"))
					{
						strTokenizer1.nextToken();
					    pidEmpiId = strTokenizer1.nextToken();
						strTokenizer1.nextToken();
					}
					if (token1.equalsIgnoreCase("MRG"))
					{
						strTokenizer1.nextToken();
						mrgEmpiId = strTokenizer1.nextToken();

					}
				}
				if (hl7EventType.equalsIgnoreCase(Constants.HL7_MERGE_EVENT_TYPE_A34)
						|| hl7EventType.equalsIgnoreCase(Constants.HL7_MERGE_EVENT_TYPE_A30))
				{
					pidEmpiId = getEMPIId(pidEmpiId);
					mrgEmpiId = getEMPIId(mrgEmpiId);
					try
					{

						jdbcdao = ParticipantManagerUtility.getJDBCDAO();
						if (isEMPIIdExists(jdbcdao, pidEmpiId, mrgEmpiId))
						{
							storeMergeMessage(jdbcdao, mergeMessage, hl7EventType);
						}
					}
					finally
					{
						jdbcdao.closeSession();
					}
				}
			}
		}
		catch (DAOException e)
		{

			LOGGER.info("Error while storing the following participant merge messages\n"
					+ mergeMessage);
			LOGGER.info(e.getMessage());
		}
		catch (JMSException exp)
		{

			LOGGER.error(exp.getMessage());
		}
	}

	/**
	 * Checks if is eMPI id exists.
	 *
	 * @param jdbcdao the jdbcdao
	 * @param pidEmpiId the pid empi id
	 * @param mrgEmpiId the mrg empi id
	 *
	 * @return true, if is eMPI id exists
	 *
	 * @throws DAOException the DAO exception
	 */
	private boolean isEMPIIdExists(final JDBCDAO jdbcdao, final String pidEmpiId,
			final String mrgEmpiId) throws DAOException
	{
		boolean isIdExists = false;
        String query = null;
		try
		{
			query = "SELECT EMPI_ID FROM CATISSUE_PARTICIPANT WHERE EMPI_ID IN (?,?)";
			final LinkedList<ColumnValueBean> colValBeanList = new LinkedList<ColumnValueBean>();
			colValBeanList.add(new ColumnValueBean("CATISSUE_PARTICIPANT", pidEmpiId,
					DBTypes.VARCHAR));
			colValBeanList.add(new ColumnValueBean("CATISSUE_PARTICIPANT", mrgEmpiId,
					DBTypes.VARCHAR));
			final List empiIdList = jdbcdao.executeQuery(query,null,colValBeanList);
			if (!empiIdList.isEmpty() && !((List) empiIdList.get(0)).isEmpty())
			{
				isIdExists = true;
			}
		}
		catch (DAOException e)
		{
			LOGGER.info(e.getMessage());
			LOGGER.info("Error while storing the participant merge messages\n");
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}
		return isIdExists;
	}

	/**
	 * Gets the eMPI id.
	 *
	 * @param id the id
	 *
	 * @return the eMPI id
	 */
	private String getEMPIId(final String identifier)
	{
		String empiId = "";
		final StringTokenizer strTokenizer = new StringTokenizer(identifier, "^^^");
		empiId = strTokenizer.nextToken();
		empiId = removePreAppZeroes(empiId);
		return empiId;
	}

	/**
	 * Removes the pre app zeroes.
	 *
	 * @param id the id
	 *
	 * @return the string
	 */
	private String removePreAppZeroes(final String identifier)
	{
		String empiId;
		for (empiId = identifier; empiId.length() > 0 && empiId.charAt(0) == '0'; empiId = empiId
				.substring(1))
		{
		}
		return empiId;
	}

	/**
	 * Store merge message.
	 *
	 * @param jdbcdao the jdbcdao
	 * @param hl7Message the hl7 message
	 * @param messageType the message type
	 * @param pidEmpiId the pid empi id
	 * @param mrgEmpiId the mrg empi id
	 *
	 * @throws DAOException the DAO exception
	 */
	private void storeMergeMessage(final JDBCDAO jdbcdao, final String hl7Message,
			final String messageType)
			throws DAOException
	{
		long idenifier = 0L;
		String insQuery = "";
		String identifier = "";
		String query =null;
		try
		{
			query = "SELECT MAX(IDENTIFIER) from PARTICIPANT_MERGE_MESSAGES";
			final List maxIdList = jdbcdao.executeQuery(query,null,null);
			if (!maxIdList.isEmpty())
			{
				final List idList = (List) maxIdList.get(0);
				if (!idList.isEmpty() && idList.get(0) != null && !"".equals(idList.get(0)))
				{
					identifier = (String) idList.get(0);
					idenifier = Long.valueOf(identifier).longValue() + 1L;
				}
			}
			final Calendar cal = Calendar.getInstance();
			final java.util.Date date = cal.getTime();
			if (idenifier == 0L)
			{
				idenifier = 1L;
			}
			insQuery = "INSERT INTO PARTICIPANT_MERGE_MESSAGES VALUES(?,?,?,?,?)";
			final LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
			final LinkedList<ColumnValueBean> colValBeanList = new LinkedList<ColumnValueBean>();
			colValBeanList.add(new ColumnValueBean("IDENTIFIER", Long.valueOf(idenifier),DBTypes.INTEGER));
			colValBeanList.add(new ColumnValueBean("MESSAGE_TYPE", messageType, DBTypes.VARCHAR));
			colValBeanList.add(new ColumnValueBean("MESSAGE_DATE", date, DBTypes.DATE));
			colValBeanList.add(new ColumnValueBean("HL7_MESSAGE", hl7Message,DBTypes.VARCHAR));
			colValBeanList.add(new ColumnValueBean("MESSAGE_STATUS", "false", DBTypes.VARCHAR));
			columnValueBeans.add(colValBeanList);
			jdbcdao.executeUpdate(insQuery, columnValueBeans);
			jdbcdao.commit();
			LOGGER.info("\n \n  ----------- STORED MERGE MESSAGE ----------  \n\n");
			LOGGER.info(hl7Message);
		}
		catch (DAOException e)
		{
			LOGGER
					.info("\n \n --------  ERROR WHILE STORING THE FOLLOWING MERGE MESSAGE ----------\n\n\n");
			LOGGER.info(hl7Message);
			LOGGER.info(e.getMessage());
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}
	}

}
