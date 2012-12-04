

package edu.wustl.common.participant.listener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.participant.bizlogic.CommonParticipantBizlogic;
import edu.wustl.common.participant.dao.EMPIParticipantDAO;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerException;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.participant.utility.PropertyHandler;
import edu.wustl.common.util.global.CommonServiceLocator;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.exception.DAOException;

// TODO: Auto-generated Javadoc
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

	private EMPIParticipantDAO empiDAO = new EMPIParticipantDAO(CommonServiceLocator.getInstance().getAppName(),null);
	
	/**
	 * This method will be called as soon as the participant demographic message arrives in the queue.
	 * Read the message from queue and update the participant.
	 *
	 * @param message the message
	 */
	public void onMessage(final Message message)
	{

		String mergeMessage = "";
		Map<String, String> messageValueMap = null;
		UserTransaction transaction = null;
		try
		{
			transaction = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
			if (transaction.getStatus() == Status.STATUS_NO_TRANSACTION)
			{
				LOGGER.info("=========== Starting a new Transaction ================");
				transaction.begin();
			}
			if (message instanceof TextMessage)
			{
				mergeMessage = ((TextMessage) message).getText();
				LOGGER.info("Received merge message---------------------- \n \n");
				LOGGER.info(mergeMessage);
				messageValueMap = new HashMap<String, String>();
				getMessageToknized(mergeMessage, messageValueMap);
				processMergeMessage(messageValueMap);
				LOGGER.info("Processed merge message--------------------");
				transaction.commit();
			}
		}
		catch (JMSException exp)
		{
			LOGGER.error(Constants.PROCESS_ERROR + mergeMessage);
			LOGGER.error(exp.getMessage(),exp);
		}
		catch (Exception be)
		{
			LOGGER.error(Constants.PARTICIPANT_ERROR + messageValueMap.get(Constants.OLD_EMPIID));
			LOGGER.error(be.getMessage(),be);
			LOGGER.error("Error during participant timer task", be);
			try
			{
				if(transaction!=null)
					transaction.rollback();
			}
			catch (final Exception rollbackFailed)
			{
				LOGGER.error("Transaction failed !", rollbackFailed);
			}
		}

	}

	/**
	 * Create a map for the corrosponding value in message.
	 *
	 * @param mergeMessage the merge message received from CDR
	 * @param valueMap of the different parameters received in message
	 */
	private void getMessageToknized(final String mergeMessage, final Map<String, String> valueMap)
	{
		final StringTokenizer strTokenizer = new StringTokenizer(mergeMessage, "\r");
		valueMap.put(Constants.MERGE_MESSAGE, mergeMessage);
		while (strTokenizer.hasMoreTokens())
		{
			final String token = strTokenizer.nextToken().trim();
			if(!"".equals(token))
			{
			final StringTokenizer strTokenizer1 = new StringTokenizer(token, "|");
			final String token1 = strTokenizer1.nextToken().trim();
			if (token1.equalsIgnoreCase("EVN"))
			{
				valueMap.put(Constants.hl7EventType, strTokenizer1.nextToken());
			}
			else if (token1.equalsIgnoreCase("PID"))
			{
				strTokenizer1.nextToken();
				final String newId = getEMPIId(strTokenizer1.nextToken());
				valueMap.put(Constants.NEW_EMPIID, newId);
				if (valueMap.get(Constants.hl7EventType).equalsIgnoreCase("A34"))
				{
					final String[] siteMRN = getSiteMRN(strTokenizer1.nextToken());
					valueMap.put(Constants.NEW_SITE, siteMRN[1]);
					valueMap.put(Constants.NEW_MRN, siteMRN[0]);
				}
			}
			else if (token1.equalsIgnoreCase("MRG"))
			{
				strTokenizer1.nextToken();
				final String oldId = getEMPIId(strTokenizer1.nextToken());
				valueMap.put(Constants.OLD_EMPIID, oldId);
			}
			}
		}
	}

	/**
	 * Gets the site and MRN from siteMRN string.
	 *
	 * @param newSiteMRN the newSiteMRN String which contains site and MRN.
	 *
	 * @return  string array of site and MRN
	 */

	private String[] getSiteMRN(final String newSiteMRN)
	{
		String[] siteMRN = new String[2];
		StringTokenizer strTokenizer = new StringTokenizer(newSiteMRN, "^^^");
		siteMRN[0] = strTokenizer.nextToken();
		final String siteString = strTokenizer.nextToken();
		strTokenizer = new StringTokenizer(siteString, "&");
		strTokenizer.nextToken();
		siteMRN[1] = strTokenizer.nextToken();
		return siteMRN;
	}


	/**
	 * Gets the eMPI id.
	 *
	 * @param identifier the identifier
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
	 * @param identifier the identifier
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

//	/**
//	 * Store merge message.
//	 *
//	 * @param jdbcdao the jdbcdao
//	 * @param hl7Message the hl7 message
//	 * @param messageType the message type
//	 * @param status the status
//	 *
//	 * @throws DAOException the DAO exception
//	 */
//	private void storeMergeMessage( final String hl7Message,
//			final String messageType ,final String status)
//			throws DAOException
//	{
//		long idenifier = 0L;
//		String insQuery = "";
//		String identifier = "";
//		String query =null;
//		JDBCDAO jdbcdao = ParticipantManagerUtility.getJDBCDAO();
//		try
//		{
//			query = "SELECT MAX(IDENTIFIER) from PARTICIPANT_MERGE_MESSAGES";
//			final List maxIdList = jdbcdao.executeQuery(query,null,null);
//			if (!maxIdList.isEmpty())
//			{
//				final List idList = (List) maxIdList.get(0);
//				if (!idList.isEmpty() && idList.get(0) != null && !"".equals(idList.get(0)))
//				{
//					identifier = (String) idList.get(0);
//					idenifier = Long.valueOf(identifier).longValue() + 1L;
//				}
//			}
//			final Calendar cal = Calendar.getInstance();
//			final java.util.Date date = cal.getTime();
//			if (idenifier == 0L)
//			{
//				idenifier = 1L;
//			}
//			insQuery = "INSERT INTO PARTICIPANT_MERGE_MESSAGES VALUES(?,?,?,?,?)";
//			final LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
//			final LinkedList<ColumnValueBean> colValBeanList = new LinkedList<ColumnValueBean>();
//			colValBeanList.add(new ColumnValueBean("IDENTIFIER", Long.valueOf(idenifier),DBTypes.INTEGER));
//			colValBeanList.add(new ColumnValueBean("MESSAGE_TYPE", messageType, DBTypes.VARCHAR));
//			colValBeanList.add(new ColumnValueBean("MESSAGE_DATE", date, DBTypes.DATE));
//			colValBeanList.add(new ColumnValueBean("HL7_MESSAGE", hl7Message,DBTypes.VARCHAR));
//			colValBeanList.add(new ColumnValueBean("MESSAGE_STATUS", status, DBTypes.VARCHAR));
//			columnValueBeans.add(colValBeanList);
//			jdbcdao.executeUpdate(insQuery, columnValueBeans);
//			jdbcdao.commit();
//			jdbcdao.closeSession();
//			LOGGER.info("\n \n  ----------- STORED MERGE MESSAGE ----------  \n\n");
//			LOGGER.info(hl7Message);
//		}
//		catch (DAOException e)
//		{
//			LOGGER
//					.info("\n \n --------  ERROR WHILE STORING THE FOLLOWING MERGE MESSAGE ----------\n\n\n");
//			LOGGER.info(hl7Message);
//			LOGGER.info(e.getMessage());
//			throw new DAOException(e.getErrorKey(), e, e.getMessage());
//		}
//	}

	/**
	 * Process merge message.
	 *
	 * @param mergeMessageMap the merge message map
	 *
	 * @return true, if successful
	 * @throws DAOException
	 * @throws BizLogicException
	 * @throws DAOException
	 * @throws BizLogicException
	 * @throws BizLogicException
	 * @throws DAOException
	 */
	private void processMergeMessage(final Map<String, String> mergeMessageMap)
			throws BizLogicException, DAOException,ApplicationException,ParticipantManagerException

	{
		final CommonParticipantBizlogic bizLogic = new CommonParticipantBizlogic();
		IParticipant participant = null;
		participant = bizLogic.getParticipant(mergeMessageMap.get(Constants.OLD_EMPIID));
		String status = Constants.UNRESOLVED;
		if ((participant == null))
		{
			LOGGER.info(Constants.USER_NOT_EXIST_CLINPORTAL);
		}
		else
		{
			/*Condition to handle A30 message merge
			  In case we get message from CDR that eMPI id changed for existing participant.
			  in ClinPortal database
			*/
			final IParticipant oldParticipant = participant;
			try
			{
				if (mergeMessageMap.get(Constants.hl7EventType).equalsIgnoreCase(
						Constants.HL7_MERGE_EVENT_TYPE_A30))
				{
					participant.setEmpiId(mergeMessageMap.get(Constants.NEW_EMPIID));
				}
				else if (mergeMessageMap.get(Constants.hl7EventType).equalsIgnoreCase(
						Constants.HL7_MERGE_EVENT_TYPE_A34))
				{
					updateMRN(participant, mergeMessageMap);
				}
				/*
				 *Updating participant in database using common  participantbizlogic.
				 */
					bizLogic.updateParticipant(participant, oldParticipant);
					status = Constants.RESOLVED;
			}
			//Status for failure reason while updating participant.
			catch (BizLogicException be)
			{
				status = Constants.ERROR_PARTICIPANT_UPDATE;
			}
			//Status for failure reason while updating MRN.
			catch (ApplicationException e)
			{
				status = e.getMsgValues();
			}
			//Status for failure reason while updating participant or fetching site in updateMRN.
			catch (ParticipantManagerException e)
			{
				status = Constants.PARTICIPANT_ERROR;
			}
			empiDAO.storeMergeMessage(mergeMessageMap.get(Constants.MERGE_MESSAGE), mergeMessageMap
					.get(Constants.hl7EventType), status);
		}

	}



	private void updateMRN(final IParticipant participant,
			final Map<String, String> mergeMessageMap) throws ApplicationException, ParticipantManagerException
	{

		if (mergeMessageMap.get(Constants.OLD_EMPIID).equals(
				mergeMessageMap.get(Constants.NEW_EMPIID)))
		{
			Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> pmiCollection = participant
					.getParticipantMedicalIdentifierCollection();
			Iterator<IParticipantMedicalIdentifier<IParticipant, ISite>> itr = pmiCollection
					.iterator();
			/*
			 * checking the MRN with the given Site and to set new MRN.
			 */
			IParticipantMedicalIdentifier<IParticipant, ISite> pmiToBeUpdated = null;
			while (itr.hasNext())
			{
				IParticipantMedicalIdentifier<IParticipant, ISite> pmi = itr.next();
				String site = (pmi.getSite()).getName();
				String mrn = pmi.getMedicalRecordNumber();
				if (site.equals(mergeMessageMap.get(Constants.NEW_SITE)))
				{
					pmiToBeUpdated = pmi;
					if (!mrn.equals(mergeMessageMap.get(Constants.NEW_MRN)))
					{
						pmi.setMedicalRecordNumber(mergeMessageMap.get(Constants.NEW_MRN));
						break;
					}
					else
					{
						throw new ApplicationException(null, null,Constants.MRN_EXIST);
					}
				}
			}

			if (pmiToBeUpdated == null)
			{
				try
				{
					ISite site = new CommonParticipantBizlogic().getSite(mergeMessageMap
							.get(Constants.NEW_SITE));
					String pmiClassName = PropertyHandler.getValue(Constants.PMI_CLASS);
					pmiToBeUpdated = (IParticipantMedicalIdentifier<IParticipant, ISite>) ParticipantManagerUtility
							.getObject(pmiClassName);
					pmiToBeUpdated.setMedicalRecordNumber(mergeMessageMap.get(Constants.NEW_MRN));
					pmiToBeUpdated.setSite(site);
					pmiToBeUpdated.setParticipant(participant);
					pmiCollection.add(pmiToBeUpdated);
				}
				catch (BizLogicException be)
				{
					throw new ApplicationException(null, be,Constants.SITE_ERROR);
				}
			}
		}
		else
		{
			LOGGER.info(Constants.USECASE_NOTSUPPORTED);
			throw new ApplicationException(null, null,Constants.USECASE_NOTSUPPORTED);
		}
	}
}
