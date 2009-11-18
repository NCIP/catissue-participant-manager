
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
import edu.wustl.common.util.global.CommonServiceLocator;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.daofactory.DAOConfigFactory;
import edu.wustl.dao.daofactory.IDAOFactory;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.query.generator.ColumnValueBean;

public class EMPIParticipantMergeMessageListener implements MessageListener
{

	private static final Logger logger = Logger
			.getCommonLogger(EMPIParticipantMergeMessageListener.class);

	public EMPIParticipantMergeMessageListener()
	{
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
	 */
	public void onMessage(Message message)
	{
		// TODO Auto-generated method stub
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
				StringTokenizer strTokenizer = new StringTokenizer(mergeMessage, "\r");
				while (strTokenizer.hasMoreTokens())
				{
					token = (strTokenizer.nextToken()).trim();
					//System.out.println(token);

					StringTokenizer strTokenizer1 = new StringTokenizer(token, "|");
					String token1 = (strTokenizer1.nextToken()).trim();
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

						jdbcdao =ParticipantManagerUtility.getJDBCDAO();
						if (isEMPIIdExists(jdbcdao, pidEmpiId, mrgEmpiId))
						{
							storeMergeMessage(jdbcdao, mergeMessage, hl7EventType, pidEmpiId,
									mrgEmpiId);
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
			// TODO Auto-generated catch block
			logger.info("Error while storing the following participant merge messages\n"
					+ mergeMessage);
			logger.info(e.getMessage());
		}
		catch (JMSException exp)
		{
			// TODO Auto-generated catch block
			logger.error(exp.getMessage());
		}
	}

	private boolean isEMPIIdExists(JDBCDAO jdbcdao, String pidEmpiId, String mrgEmpiId)
			throws DAOException
	{
		boolean isIdExists = false;
		try
		{
			String query = (new StringBuilder()).append(
					"SELECT EMPI_ID FROM CATISSUE_PARTICIPANT WHERE EMPI_ID IN ('").append(
					pidEmpiId).append("','").append(mrgEmpiId).append("')").toString();
			List empiIdList = jdbcdao.executeQuery(query);
			if (!empiIdList.isEmpty() && !((List) empiIdList.get(0)).isEmpty())
			{
				isIdExists = true;
			}
		}
		catch (DAOException e)
		{
			logger.info(e.getMessage());
			logger.info("Error while storing the participant merge messages\n");
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}
		return isIdExists;
	}

	private String getEMPIId(String id)
	{
		String empiId = "";
		StringTokenizer strTokenizer = new StringTokenizer(id, "^^^");
		empiId = strTokenizer.nextToken();
		empiId = removePreAppZeroes(empiId);
		return empiId;
	}

	private String removePreAppZeroes(String id)
	{
		String empiId;
		for (empiId = id; empiId.length() > 0 && empiId.charAt(0) == '0'; empiId = empiId
				.substring(1))
		{
		}
		return empiId;
	}

	private void storeMergeMessage(JDBCDAO jdbcdao, String hl7Message, String messageType,
			String pidEmpiId, String mrgEmpiId) throws DAOException
	{
		long idenifier = 0L;
		String insQuery = "";
		String id = "";
		try
		{
			String query = "SELECT MAX(IDENTIFIER) from PARTICIPANT_MERGE_MESSAGES";
			List maxIdList = jdbcdao.executeQuery(query);
			if (!maxIdList.isEmpty())
			{
				List idList = (List) maxIdList.get(0);
				if (!idList.isEmpty() && idList.get(0) != null && idList.get(0) != "")
				{
					id = (String) idList.get(0);
					idenifier = Long.valueOf(id).longValue() + 1L;
				}
			}
			Calendar cal = Calendar.getInstance();
			java.util.Date date = cal.getTime();
			if (idenifier == 0L)
			{
				idenifier = 1L;
			}
			insQuery = "INSERT INTO PARTICIPANT_MERGE_MESSAGES VALUES(?,?,?,?,?)";
			LinkedList columnValueBeanList = new LinkedList();
			columnValueBeanList.add(new ColumnValueBean("IDENTIFIER", Long.valueOf(idenifier), 3));
			columnValueBeanList.add(new ColumnValueBean("MESSAGE_TYPE", messageType, 21));
			columnValueBeanList.add(new ColumnValueBean("MESSAGE_DATE", date, 13));
			columnValueBeanList.add(new ColumnValueBean("HL7_MESSAGE", hl7Message, 21));
			columnValueBeanList.add(new ColumnValueBean("MESSAGE_STATUS", "false", 21));
			jdbcdao.executeUpdate(insQuery, columnValueBeanList);
			jdbcdao.commit();
			logger.info((new StringBuilder()).append(" Stored merge messages\n").append(hl7Message)
					.toString());
		}
		catch (DAOException e)
		{
			logger.info("Error while storing the participant merge messages\n");
			logger.info(e.getMessage());
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}
	}

}
