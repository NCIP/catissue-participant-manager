
package edu.wustl.common.participant.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;

import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQQueueConnectionFactory;

import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.bizlogic.DefaultBizLogic;
import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.exception.ErrorKey;
import edu.wustl.common.lookup.DefaultLookupParameters;
import edu.wustl.common.lookup.DefaultLookupResult;
import edu.wustl.common.participant.client.IParticipantManager;
import edu.wustl.common.participant.client.IParticipantManagerLookupLogic;
import edu.wustl.common.participant.dao.CommonParticipantDAO;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.IRace;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.domain.IUser;
import edu.wustl.common.participant.listener.EMPIParticipantListener;
import edu.wustl.common.participant.listener.EMPIParticipantMergeMessageListener;
import edu.wustl.common.util.Utility;
import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.common.util.global.CommonServiceLocator;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.patientLookUp.domain.PatientInformation;
import edu.wustl.patientLookUp.lookUpServiceBizLogic.PatientInfoLookUpService;
import edu.wustl.patientLookUp.queryExecutor.IQueryExecutor;
import edu.wustl.patientLookUp.util.PatientLookUpFactory;
import edu.wustl.patientLookUp.util.PatientLookupException;

// TODO: Auto-generated Javadoc
/**
 * The Class ParticipantManagerUtility.
 */
public class ParticipantManagerUtility
{

	/** The logger. */
	private static final Logger LOGGER = Logger.getCommonLogger(ParticipantManagerUtility.class);

	/** class level instance for Queue connection*/
	private QueueConnection connectionToQueue = null;

	/** class level instance for Queue session*/
	private QueueSession inBoundQueueSession = null;


	/** class level instance for Queue session*/
	private QueueSession mergeQueueSession = null;

	private static String getAppName()
	{
		return CommonServiceLocator.getInstance().getAppName();
	}
	
	/**
	 * Register wmq listener.
	 *
	 * @throws JMSException the JMS exception
	 * @throws BizLogicException the biz logic exception
	 */
	public void registerWMQListener() throws JMSException, BizLogicException
	{
		String hostName = null;
		String qmgName = null;
		String channel = null;
		String inBoundQueueName = null;
		String mergeMessageQueueName = null;
		int port = 0;
		try
		{
			hostName = XMLPropertyHandler.getValue(Constants.WMQ_SERVER_NAME);
			qmgName = XMLPropertyHandler.getValue(Constants.WMQ_QMG_NAME);
			channel = XMLPropertyHandler.getValue(Constants.WMQ_CHANNEL);
			if (XMLPropertyHandler.getValue(Constants.WMQ_PORT) != null
					&& !"".equals(XMLPropertyHandler.getValue(Constants.WMQ_PORT)))
			{
				port = Integer.parseInt(XMLPropertyHandler.getValue(Constants.WMQ_PORT));
			}

			LOGGER.info("WMQ_SERVER_NAME ----------- : " + hostName);
			LOGGER.info("WMQ_QMG_NAME ----------- : " + qmgName);
			LOGGER.info("WMQ_CHANNEL ----------- : " + channel);
			LOGGER.info("WMQ_PORT ----------- : " + port);

			final MQQueueConnectionFactory factory = new MQQueueConnectionFactory();
			factory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
			factory.setQueueManager(qmgName);
			factory.setHostName(hostName);
			factory.setChannel(channel);
			factory.setPort(port);

			connectionToQueue = factory.createQueueConnection();

			connectionToQueue.start();

			inBoundQueueSession = connectionToQueue.createQueueSession(false,
					javax.jms.Session.AUTO_ACKNOWLEDGE);
			inBoundQueueName = XMLPropertyHandler.getValue(Constants.IN_BOUND_QUEUE_NAME);
			final Queue inBoundQueue = inBoundQueueSession.createQueue("queue:///" + inBoundQueueName);

			LOGGER.info("IN_BOUND_QUEUE_NAME ----------- : " + inBoundQueueName);

			QueueReceiver queueReceiver = inBoundQueueSession.createReceiver(inBoundQueue);

			final EMPIParticipantListener listener = new EMPIParticipantListener();

			queueReceiver.setMessageListener(listener);

			// Set the merge message queue listener.
			mergeMessageQueueName = XMLPropertyHandler.getValue(Constants.MERGE_MESSAGE_QUEUE);
			mergeQueueSession = connectionToQueue.createQueueSession(false,
					javax.jms.Session.AUTO_ACKNOWLEDGE);
			final Queue mrgMessageQueue = mergeQueueSession.createQueue("queue:///" + mergeMessageQueueName);
			queueReceiver = mergeQueueSession.createReceiver(mrgMessageQueue);
			final EMPIParticipantMergeMessageListener mrgMesListener = new EMPIParticipantMergeMessageListener();
			queueReceiver.setMessageListener(mrgMesListener);
		}
		catch (JMSException e)
		{

			LOGGER
					.error(" -------------  ERROR WHILE INITIALISING THE MESSAGE QUEUES \n \n ------------- ");
			LOGGER.error(e.getMessage());
			LOGGER.error(e.getStackTrace());
			e.printStackTrace();
			e.getLinkedException();
			LOGGER.error(e.getLinkedException());
			LOGGER.error(e.getLinkedException().getMessage());
			LOGGER.error(e.getLinkedException().getStackTrace());
			throw e;

		}
		catch (Exception e)
		{
			e.printStackTrace();
			LOGGER.error(e.getMessage());
		}
	}

	/**
	 * Initialize participant match scheduler.
	 */
	public static void initialiseParticiapntMatchScheduler()
	{
		final ParticipantMatchingTimerTask timerTask = new ParticipantMatchingTimerTask();
		final Timer scheduleTime = new Timer();
		final String delay = XMLPropertyHandler
				.getValue(Constants.PARTICIPANT_MATCHING_SCHEDULAR_DELAY);
		scheduleTime.schedule(timerTask, 0x1d4c0L, Long.parseLong(delay));
	}

	/**
	 * Gets the participant medical identifier obj.
	 *
	 * @param mrn the mrn
	 * @param facilityId the facility id
	 *
	 * @return the participant medical identifier obj
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws ParticipantManagerException the participant manager exception
	 * @throws Exception the exception
	 */
	public static IParticipantMedicalIdentifier<IParticipant, ISite> getParticipantMedicalIdentifierObj(
			final String mrn, final String facilityId) throws BizLogicException,
			ParticipantManagerException
	{

		ISite site = null;
		site = getSiteObject(facilityId);
		IParticipantMedicalIdentifier<IParticipant, ISite> partiMedId = null;
		if (site != null)
		{
			partiMedId = getPMIInstance();
			partiMedId.setMedicalRecordNumber(mrn);
			partiMedId.setSite(site);
		}
		return partiMedId;
	}
	/**
	 *
	 * @param patientInformation
	 * @return
	 */
	public static List<Long> getFacilityIds (PatientInformation patientInformation)
	{
		List<Long> facilityIdList =  new ArrayList<Long>();
		Collection <IParticipantMedicalIdentifier<IParticipant, ISite>>  pmiColl = patientInformation.getPmiCollection();
		for (IParticipantMedicalIdentifier<IParticipant, ISite> pmi : pmiColl)
		{
			String mrn= pmi.getMedicalRecordNumber();
			if (mrn != null)
			{
				/*Bug#20878:facility Id is passed to get participants on based of facilityIds and MRN.
				*/
				String facilityId = String.valueOf(pmi.getSite().getFacilityId());
				if(edu.wustl.patientLookUp.util.Utility.isNumeric(facilityId))
				{
					facilityIdList.add(Long.valueOf(facilityId));
				}
			}
		}
		return facilityIdList;
	}

	/**
	 * Gets the site object.
	 *
	 * @param facilityId the facility id
	 *
	 * @return the site object
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws ParticipantManagerException the participant manager exception
	 * @throws Exception the exception
	 */
	public static ISite getSiteObject(final String facilityId) throws BizLogicException,
			ParticipantManagerException
	{
		ISite site = null;
		try
		{
			List siteObject = getParticipantMgrImplObj().getSiteObject(facilityId);
			if (siteObject != null && siteObject.size() > 0)
			{
				Object siteList[] = (Object[]) siteObject.get(0);
				Long siteId = (Long) siteList[0];
				String siteName = (String) siteList[1];
				site = (ISite) ParticipantManagerUtility.getSiteInstance();
				site.setId(siteId);
				site.setName(siteName);
			}
			return site;
		}
		catch (DAOException e)
		{
			LOGGER.error("Error occurred in getSiteObject: "+e.getMessage(),e);
			throw new BizLogicException(e);
		}
		catch (ApplicationException e)
		{
			LOGGER.error("Error occurred in getSiteObject: "+e.getMessage(),e);
			throw new BizLogicException(null,e,e.getMessage());
		}
//		String sourceObjectName = ISite.class.getName();
//		String selectColumnNames[] = {"id", "name"};
//		DefaultBizLogic bizLogic = new DefaultBizLogic();
//		ISite site = null;
//		QueryWhereClause queryWhereClause = new QueryWhereClause(sourceObjectName);
//		try
//		{
//			queryWhereClause.addCondition(new EqualClause("facilityId", '?'));
//		}
//		catch (DAOException e)
//		{
//			throw new BizLogicException(e);
//		}
//
//		List<ColumnValueBean> columnValueBeans = new ArrayList<ColumnValueBean>();
//		columnValueBeans.add(new ColumnValueBean(facilityId));
//		List siteObject = bizLogic.retrieve(sourceObjectName, selectColumnNames, queryWhereClause,
//				columnValueBeans);
//
//		if (siteObject != null && siteObject.size() > 0)
//		{
//			Object siteList[] = (Object[]) siteObject.get(0);
//			Long siteId = (Long) siteList[0];
//			String siteName = (String) siteList[1];
//			site = (ISite) ParticipantManagerUtility.getSiteInstance();
//			site.setId(siteId);
//			site.setName(siteName);
//		}
//		return site;
	}

	/**
	 * Gets the object.
	 *
	 * @param bizLogicFactoryName the biz logic factory name
	 *
	 * @return the object
	 *
	 * @throws BizLogicException the biz logic exception
	 */
	public static Object getObject(final String bizLogicFactoryName) throws BizLogicException
	{
		try
		{
			Class className = Class.forName(bizLogicFactoryName);
			Object newobj = className.newInstance();
			return newobj;
		}
		catch (IllegalAccessException e)
		{
			LOGGER.info(e.getMessage());
			throw new BizLogicException(null, null, "IllegalAccessException",
					"IllegalAccessException");
		}
		catch (InstantiationException e)
		{
			LOGGER.info(e.getMessage());
			throw new BizLogicException(null, null, "InstantiationException",
					"InstantiationException");
		}
		catch (ClassNotFoundException e)
		{
			LOGGER.info(e.getMessage());
			throw new BizLogicException(null, null, "ClassNotFoundException",
					"ClassNotFoundException");
		}

	}

	/**
	 * This function takes identifier as parameter and returns corresponding
	 * Participant.
	 *
	 * @param identifier the identifier
	 *
	 * @return - Participant object
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws Exception 	 */
	public static IParticipant getParticipantById(final Long identifier) throws BizLogicException
	{
		IParticipant participant = null;
		try
		{
			participant = getParticipantMgrImplObj().getParticipantById(identifier);
		}
		catch (Exception e)
		{
			LOGGER.error("Exception occurred in getParticipantById: "+e.getMessage(),e);
			throw new BizLogicException(null,e,e.getMessage());
		}
		if(participant==null)
		{
			throw new BizLogicException(ErrorKey.getErrorKey("errors.item.invalid.values"), null,
					"Participant Id");
		}
		return participant;

	}

	/**
	 * Checks if is participant valid for empi.
	 *
	 * @param LName the l name
	 * @param FName the f name
	 * @param dob the dob
	 * @param ssn the ssn
	 * @param mrn the mrn
	 *
	 * @return true, if is participant valid for empi
	 */
	public static boolean isParticipantValidForEMPI(final String LName, final String FName,
			final Date dob, final String ssn, final String mrn)
	{
		boolean isValid = false;
		if ((LName != null && !"".equals(LName)) && (FName != null && !"".equals(FName)))
		{
			if (dob != null)
			{
				isValid = true;
			}
			if (ssn != null && !"".equals(ssn))
			{
				isValid = true;
			}
			if (mrn != null && !"".equals(mrn))
			{
				isValid = true;
			}
		}
		return isValid;
	}

	/**
	 * Generates key for ParticipantMedicalIdentifierMap.
	 *
	 * @param idx serial number
	 * @param keyFor Attribute based on which respective key is to generate
	 *
	 * @return key for map attribute
	 */
	public static String getParticipantMedicalIdentifierKeyFor(int idx, String keyFor)
	{
		return (Constants.PARTICIPANT_MEDICAL_IDENTIFIER + idx + keyFor);
	}

	/**
	 * Gets the mrn value.
	 *
	 * @param medIdcol the med idcol
	 *
	 * @return the mrn value
	 */
	public static String getMrnValue(Collection medIdcol)
	{
		String mrn = null;

		if (medIdcol != null && !medIdcol.isEmpty())
		{
			Iterator iterator = medIdcol.iterator();
			while (iterator.hasNext())
			{
				IParticipantMedicalIdentifier<IParticipant, ISite> partMedId = (IParticipantMedicalIdentifier<IParticipant, ISite>) iterator
						.next();
				mrn = partMedId.getMedicalRecordNumber();
				break;
			}
		}
		return mrn;
	}


	/**
	 * Gets the list of matching participants.
	 *
	 * @param participant the participant
	 * @param lookupAlgorithm the lookup algorithm
	 * @param protocolId the protocol id
	 * @param threshHold cut off points for running matching algorithm
	 * @return the list of matching participants
	 *
	 * @throws Exception the exception
	 * @throws PatientLookupException the patient lookup exception
	 */
	public static List<DefaultLookupResult> getListOfMatchingParticipants(IParticipant participant,
			String lookupAlgorithm, Long protocolId,Integer threshHold) throws PatientLookupException
	{
		List<DefaultLookupResult> matchParticipantList = null;
		try
		{
			//get all the associated CS ids for the MISC if  match within MICS is enabled
			Set<Long> protocolIdList = getProtocolIdLstForMICSEnabledForMatching(protocolId);
			String participantCode = participant.getParticipantCode();
			if (participantCode!=null && !"".equals(participantCode))
			{
				lookupAlgorithm = Constants.PARTICIPANT_HASHCODE_LOOKUP_ALGO;

			}
			matchParticipantList = findMatchedParticipants(participant, lookupAlgorithm,
					protocolIdList,threshHold);
		}
		catch (Exception exp)
		{
			LOGGER.error(exp.getMessage(),exp);
			throw new PatientLookupException(exp.getMessage(), exp);
		}
		return matchParticipantList;
	}

	/**
	 * Gets the list of matching participants.
	 *
	 * @param participant the participant
	 * @param lookupAlgorithm the lookup algorithm
	 * @param protocolIdSet the protocol id set
	 * @param threshHold cut off points for running matching algorithm
	 * @return the list of matching participants
	 *
	 * @throws Exception the exception
	 * @throws PatientLookupException the patient lookup exception
	 */
	public static List<DefaultLookupResult> findMatchedParticipants(IParticipant participant,
			String lookupAlgorithm, Set<Long> protocolIdSet,Integer threshHold) throws PatientLookupException
	{
		List<DefaultLookupResult> matchParticipantList = null;
		try
		{
			IParticipantManagerLookupLogic partLookupLgic = getLookUPLogicObj(lookupAlgorithm);
			if (partLookupLgic.isCallToLookupLogicNeeded(participant))
			{
				DefaultLookupParameters params = new DefaultLookupParameters();
				params.setObject(participant);
				params.setThreshold(threshHold);
				matchParticipantList = partLookupLgic.lookup(params, protocolIdSet);
			}

		}
		catch (Exception exp)
		{
			throw new PatientLookupException(exp.getMessage(), exp);
		}
		return matchParticipantList;
	}

	/**
	 * Gets the look up logic obj.
	 *
	 * @param lookupAlgorithm the lookup algorithm
	 *
	 * @return the look up logic obj
	 */
	private static IParticipantManagerLookupLogic getLookUPLogicObj(String lookupAlgorithm)
	{
		IParticipantManagerLookupLogic partLookupLgic = null;
		if (lookupAlgorithm == null)
		{
			partLookupLgic = (IParticipantManagerLookupLogic) Utility.getObject(XMLPropertyHandler
					.getValue(Constants.PARTICIPANT_LOOKUP_ALGO));
		}
		else
		{
			partLookupLgic = (IParticipantManagerLookupLogic) Utility.getObject(XMLPropertyHandler
					.getValue(lookupAlgorithm));
		}
		return partLookupLgic;
	}

	/**
	 * Process list for match within cs.
	 *
	 * @param matchPartpantLst the match partpant lst
	 * @param csIdList the cs id list
	 *
	 * @return the list
	 *
	 * @throws DAOException the DAO exception
	 */
	public static List processListForMatchWithinCS(List<DefaultLookupResult> matchPartpantLst,
			Set csIdList) throws DAOException
	{
		List idList = getPartcipantIdsList(csIdList);
		matchPartpantLst = filerMatchedList(matchPartpantLst, idList);
		return matchPartpantLst;
	}

	/**
	 * Filer matched list.
	 *
	 * @param matchPartpantLst the match partpant lst
	 * @param idList the id list
	 *
	 * @return the list< default lookup result>
	 */
	public static List<DefaultLookupResult> filerMatchedList(
			List<DefaultLookupResult> matchPartpantLst, List idList)
	{

		List<DefaultLookupResult> matchPartpantLstFiltred = new ArrayList<DefaultLookupResult>();
		Iterator<DefaultLookupResult> itr = matchPartpantLst.iterator();
		if (!idList.isEmpty() && idList.get(0) != null && !"".equals(String.valueOf(idList.get(0))))
		{
			List participantIdList = idList;
			while (itr.hasNext())
			{
				DefaultLookupResult result = itr.next();
				IParticipant participant = (IParticipant) result.getObject();
				if (participantIdList.contains(String.valueOf(participant.getId().longValue())))
				{
					matchPartpantLstFiltred.add(result);
				}
			}
		}
		return matchPartpantLstFiltred;
	}

	/**
	 * Checks if is participant match within cscp enable.
	 *
	 * @param id the id
	 *
	 * @return true, if is participant match within cscp enable
	 *
	 * @throws DAOException the DAO exception
	 */
	public static boolean isParticipantMatchWithinCSCPEnable(Long id) throws DAOException
	{
		boolean status = false;
		List list = new CommonParticipantDAO(getAppName(),null).isParticipantMatchWithinCSCPEnable(id);
		if (!list.isEmpty() && !"".equals(list.get(0)))
		{
			//List statusList = (List) list.get(0);
			Object value = list.get(0);
			if (value!=null && value.toString().equals("1"))
			{
				status = true;
			}
		}
		return status;
//		boolean status = false;
//		JDBCDAO dao = null;
//		String query = null;
//		try
//		{
//			query = "SELECT SP.PARTCIPNT_MATCH_WITHIN_CSCP FROM  CATISSUE_SPECIMEN_PROTOCOL SP WHERE SP.IDENTIFIER=?";
//			dao = getJDBCDAO();
//			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//			columnValueBeanList.add(new ColumnValueBean("IDENTIFIER", id, DBTypes.LONG));
//			List list = dao.executeQuery(query, null, columnValueBeanList);
//			if (!list.isEmpty() && !"".equals(list.get(0)))
//			{
//				List statusList = (List) list.get(0);
//				if (!statusList.isEmpty() && ((String) statusList.get(0)).equals("1"))
//				{
//					status = true;
//				}
//			}
//		}
//		catch (DAOException exp)
//		{
//			LOGGER.info("ERROR WHILE GETTING THE EMPI STATUS");
//			throw new DAOException(exp.getErrorKey(), exp, exp.getMsgValues());
//		}
//		finally
//		{
//			dao.closeSession();
//		}
//		return status;
	}

	/**
	 * Gets the partcipant ids list.
	 *
	 * @param cpIdList the cp id list
	 *
	 * @return the partcipant ids list
	 *
	 * @throws DAOException the DAO exception
	 */
	public static List<String> getPartcipantIdsList(Set cpIdList) throws DAOException
	{
		List<String> idList = new ArrayList<String>();
		List idListArray = new CommonParticipantDAO(getAppName(),null).getPartcipantIdsList(cpIdList);
		for(Object obj : idListArray)
		{
			idList.add(obj.toString());
		}
		return idList;
//		List idListArray =null;
//		List<String> idList = new ArrayList<String>();
//		JDBCDAO dao = null;
//		String query = null;
//		try
//		{
//			query = "SELECT PARTICIPANT_ID FROM CATISSUE_CLINICAL_STUDY_REG WHERE CLINICAL_STUDY_ID=?";
//			dao = getJDBCDAO();
//
//			if (cpIdList != null && !cpIdList.isEmpty())
//			{
//				Iterator<Long> iterator = cpIdList.iterator();
//				while (iterator.hasNext())
//				{
//					Long id = iterator.next();
//					LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//					columnValueBeanList.add(new ColumnValueBean("CLINICAL_STUDY_ID", id,
//							DBTypes.LONG));
//
//					idListArray = dao.executeQuery(query, null, columnValueBeanList);
//					if (!idListArray.isEmpty() && !"".equals(idListArray.get(0)))
//					{
//						for (Iterator<List> itr = idListArray.iterator(); itr.hasNext();)
//						{
//							idList.add(String.valueOf(itr.next().get(0)));
//						}
//					}
//				}
//			}
//		}
//		catch (DAOException exp)
//		{
//
//			throw new DAOException(exp.getErrorKey(), exp, exp.getMsgValues());
//		}
//		finally
//		{
//			dao.closeSession();
//		}
//		return idList;
	}

	/**
	 * Gets the race instance.
	 *
	 * @return the race instance
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws ParticipantManagerException the participant manager exception
	 */
	public static Object getRaceInstance() throws BizLogicException, ParticipantManagerException
	{
		String raceclassName = PropertyHandler.getValue(Constants.RACE_CLASS);
		return getObject(raceclassName);
	}

	/**
	 * Gets the site instance.
	 *
	 * @return the site instance
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws ParticipantManagerException the participant manager exception
	 */
	public static Object getSiteInstance() throws BizLogicException, ParticipantManagerException
	{
		String siteClassName = PropertyHandler.getValue(Constants.SITE_CLASS);
		return getObject(siteClassName);
	}

	/**
	 * Gets the participant instance.
	 *
	 * @return the participant instance
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws ParticipantManagerException the participant manager exception
	 */
	public static Object getParticipantInstance() throws BizLogicException,
			ParticipantManagerException
	{
		String participantClassName = PropertyHandler
				.getValue(Constants.PARTICIPANT_CLASS);
		return getObject(participantClassName);
	}

	/**
	 * Gets the pMI instance.
	 *
	 * @return the pMI instance
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws ParticipantManagerException the participant manager exception
	 */
	public static IParticipantMedicalIdentifier<IParticipant, ISite> getPMIInstance()
			throws BizLogicException, ParticipantManagerException
	{
		String pmiClassName = PropertyHandler.getValue(Constants.PMI_CLASS);
		Object PMIInstance = getObject(pmiClassName);
		return (IParticipantMedicalIdentifier<IParticipant, ISite>) PMIInstance;
	}

//	/**
//	 * Sets the empi id status.
//	 *
//	 * @param participantId the participant id
//	 * @param status the status
//	 *
//	 * @throws DAOException the DAO exception
//	 */
//	public static void setEMPIIdStatus(Long participantId, String status) throws DAOException
//	{
//		JDBCDAO jdbcDao = null;
//		try
//		{
//			jdbcDao = getJDBCDAO();
//			setEMPIIdStatus(participantId, status, jdbcDao);
//			jdbcDao.commit();
//		}
//		catch (DAOException e)
//		{
//			LOGGER.info("ERROE WHILE UPDATING THE PARTICIPANT EMPI STATUS");
//			throw new DAOException(e.getErrorKey(), e, e.getMsgValues());
//		}
//		finally
//		{
//			jdbcDao.closeSession();
//		}
//	}

	/**
	 * Gets the sSN.
	 *
	 * @param ssn the ssn
	 *
	 * @return the sSN
	 */
	public static String getSSN(String ssn)
	{
		String ssnA = "";
		String ssnB = "";
		String ssnC = "";
		boolean result = false;
		Pattern pattern = Pattern.compile("[0-9]{3}-[0-9]{2}-[0-9]{4}", 2);
		Matcher mat = pattern.matcher(ssn);
		result = mat.matches();
		if (result)
		{
			return ssn;
		}
		if (ssn.length() >= 9)
		{
			ssnA = ssn.substring(0, 3);
			ssnB = ssn.substring(3, 5);
			ssnC = ssn.substring(5, 9);
		}
		else if (ssn.length() >= 4)
		{
			ssnC = ssn.substring(0, 3);
		}
		else
		{
			return ssn;
		}
		ssn = ssnA + "-" + ssnB + "-" + ssnC;
		return ssn;
	}

	/**
	 * Gets the column list.
	 *
	 * @param columnList the column list
	 *
	 * @return the column list
	 *
	 * @throws DAOException the DAO exception
	 */
	public static List<String> getColumnList(List<String> columnList) throws DAOException
	{
		List<String> displayList = new ArrayList<String>();
		List<Object[]> list = new CommonParticipantDAO(getAppName(),null).getColumnList();
		for (Iterator iterator1 = columnList.iterator(); iterator1.hasNext();)
		{
			String colName1 = (String) iterator1.next();
			for(Object[] obj: list)
			{
				String colName2 = obj[0].toString();
				if (colName1.equals(colName2))
				{
					displayList.add((String) obj[1]);
				}
			}
		}
		
//		for (Iterator iterator1 = columnList.iterator(); iterator1.hasNext();)
//		{
//			String colName1 = (String) iterator1.next();
//			Iterator iterator2 = list.iterator();
//			while (iterator2.hasNext())
//			{
//				List rowList = (List) iterator2.next();
//				String colName2 = (String) rowList.get(0);
//				if (colName1.equals(colName2))
//				{
//					displayList.add((String) rowList.get(1));
//				}
//			}
//		}
		return displayList;
//		List<String> displayList = new ArrayList<String>();
//		JDBCDAO jdbcDao = null;
//		try
//		{
//			jdbcDao = getJDBCDAO();
//
//			String sql = "SELECT  columnData.COLUMN_NAME,displayData.DISPLAY_NAME FROM CATISSUE_INTERFACE_"
//					+ "COLUMN_DATA columnData,CATISSUE_TABLE_RELATION relationData,CATISSUE_QUERY_TABLE"
//					+ "_DATA tableData,CATISSUE_SEARCH_DISPLAY_DATA displayData where relationData.CHIL"
//					+ "D_TABLE_ID = columnData.TABLE_ID and relationData.PARENT_TABLE_ID = tableData.TA"
//					+ "BLE_ID and relationData.RELATIONSHIP_ID = displayData.RELATIONSHIP_ID and column"
//					+ "Data.IDENTIFIER = displayData.COL_ID and tableData.ALIAS_NAME = 'Participant'";
//			LOGGER.debug("DATA ELEMENT SQL : " + sql);
//			List list = jdbcDao.executeQuery(sql, null, null);
//			for (Iterator iterator1 = columnList.iterator(); iterator1.hasNext();)
//			{
//				String colName1 = (String) iterator1.next();
//				Iterator iterator2 = list.iterator();
//				while (iterator2.hasNext())
//				{
//					List rowList = (List) iterator2.next();
//					String colName2 = (String) rowList.get(0);
//					if (colName1.equals(colName2))
//					{
//						displayList.add((String) rowList.get(1));
//					}
//				}
//			}
//		}
//		finally
//		{
//			jdbcDao.closeSession();
//		}
//
//		return displayList;
	}

//	/**
//	 * Gets the query.
//	 *
//	 * @param csId the cs id
//	 *
//	 * @return the query
//	 *
//	 * @throws DAOException the DAO exception
//	 * @throws ParticipantManagerException
//	 * @throws BizLogicException
//	 */
//	private static String getQueryForEmpiEnabled() throws DAOException, ParticipantManagerException,
//			BizLogicException
//	{
//
//
//		IParticipantManager participantManagerImplObj = getParticipantMgrImplObj();
//
//		return participantManagerImplObj.getIsEmpiEnabledQuery();
//	}

	/**
	 * Cs empi status.
	 *
	 * @param cpId the cp id
	 *
	 * @return true, if successful
	 *
	 * @throws ApplicationException 	 * @throws BizLogicException the biz logic exception
	 */
	public static boolean isEMPIEnable(Long participantId) throws BizLogicException
	{
		boolean status = false;
		try
		{
			List statusList = getParticipantMgrImplObj().isEmpiEnabled(participantId);
			if (statusList != null&&!statusList.isEmpty())
			{
				for (int i = 0; i < statusList.size(); i++)
				{
					Object value = statusList.get(i);
					if(value!=null&&value.toString().equals("1"))
					{
						status = true;
						break;

					}
//					if (!idList.isEmpty() && !"".equals((idList.get(0))))
//					{
//						if (((String) idList.get(0)).equals("1"))
//						{
//							status = true;
//							break;
//						}
//					}
				}
			}
		}
//		catch (DAOException exp)
//		{
//			LOGGER.error(exp.getMessage(), exp);
//			throw new BizLogicException(exp);
//		}
		catch (ParticipantManagerException e)
		{
			LOGGER.error(e.getMessage(), e);
			throw new BizLogicException(null,e,e.getMessage());
		}
		catch (ApplicationException e)
		{
			LOGGER.error(e.getMessage(), e);
			throw new BizLogicException(null,e,e.getMessage());
		}

		return status;
//		JDBCDAO dao = null;
//		status = false;
//		String query = null;
//		try
//		{
//			dao = getJDBCDAO();
//			query = ParticipantManagerUtility.getQueryForEmpiEnabled();
//			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//			columnValueBeanList.add(new ColumnValueBean("IDENTIFIER", participantId, DBTypes.LONG));
//			List statusList = dao.executeQuery(query, null, columnValueBeanList);
//			if (!statusList.isEmpty() && statusList != null)
//			{
////				List idList = (List) statusList.get(0);
////				if (!idList.isEmpty() && !"".equals((idList.get(0))))
////				{
//					for (int i = 0; i < statusList.size(); i++)
//					{
//						List idList = (List) statusList.get(i);
//						if (!idList.isEmpty() && !"".equals((idList.get(0))))
//						{
//							if (((String) idList.get(0)).equals("1"))
//							{
//								status = true;
//								break;
//							}
//						}
//					}
//				}
////			}
//		}
//		catch (DAOException exp)
//		{
//			throw new BizLogicException(exp);
//		}
//		catch (ParticipantManagerException e)
//		{
//			throw new BizLogicException(null,e,e.getMessage());
//		}
//		finally
//		{
//			try
//			{
//				dao.closeSession();
//			}
//			catch (DAOException exp)
//			{
//				throw new BizLogicException(exp);
//			}
//		}
//		return status;
	}

	/**
	 * Gets the parti empi status.
	 *
	 * @param participantId the participant id
	 *
	 * @return the parti empi status
	 *
	 * @throws DAOException the DAO exception
	 */
	public static String getPartiEMPIStatus(long participantId) throws DAOException
	{
		return new CommonParticipantDAO(getAppName(),null).getPartiEMPIStatus(participantId);
//		JDBCDAO dao = null;
//		String eMPIStatus = "";
//		try
//		{
//			dao = getJDBCDAO();
//			String query = "SELECT EMPI_ID_STATUS FROM CATISSUE_PARTICIPANT  WHERE IDENTIFIER=?";
//			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//			columnValueBeanList.add(new ColumnValueBean("IDENTIFIER", participantId, DBTypes.LONG));
//			List list = dao.executeQuery(query, null, columnValueBeanList);
//			if (!list.isEmpty() && !"".equals(list.get(0)))
//			{
//				List statusList = (List) list.get(0);
//				if (!statusList.isEmpty())
//				{
//					eMPIStatus = (String) statusList.get(0);
//				}
//			}
//		}
//		catch (DAOException exp)
//		{
//			LOGGER.info("ERROR WHILE GETTING THE EMPI STATUS");
//			throw new DAOException(exp.getErrorKey(), exp, exp.getMsgValues());
//		}
//		finally
//		{
//			dao.closeSession();
//		}
//		return eMPIStatus;
	}

	/**
	 * Checks if is call to lookup logic needed.
	 *
	 * @param participant the participant
	 *
	 * @return true, if is call to lookup logic needed
	 */
	public static boolean isCallToLookupLogicNeeded(IParticipant participant)
	{
		boolean flag = true;
		if ((participant.getFirstName() == null || participant.getFirstName().length() == 0)
				&& (participant.getMiddleName() == null || participant.getMiddleName().length() == 0)
				&& (participant.getLastName() == null || participant.getLastName().length() == 0)
				&& (participant.getSocialSecurityNumber() == null || participant
						.getSocialSecurityNumber().length() == 0)
				&& participant.getBirthDate() == null
				&& (participant.getParticipantMedicalIdentifierCollection() == null || participant
						.getParticipantMedicalIdentifierCollection().size() == 0))
		{
			flag = false;
		}
		return flag;
	}

	/**
	 * Gets the column heading list.
	 *
	 * @return the column heading list
	 *
	 * @throws DAOException the DAO exception
	 */
	public static List<String> getColumnHeadingList() throws DAOException
	{
		String columnHeaderList[] = {"LAST_NAME", "FIRST_NAME", "MIDDLE_NAME", "BIRTH_DATE",
				"DEATH_DATE", "VITAL_STATUS", "GENDER", "SOCIAL_SECURITY_NUMBER",
				"MEDICAL_RECORD_NUMBER"};
		List<String> columnList = Arrays.asList(columnHeaderList);

		List<String> displayList = new ArrayList<String>();
		displayList.add("eMPI Id");
		List<String> displayListTemp = getColumnList(columnList);
		displayList.addAll(displayListTemp);
		return displayList;
	}

	/**
	 * Gets the participant display list.
	 *
	 * @param participantList the participant list
	 *
	 * @return the participant display list
	 */
	public static List<List<String>> getParticipantDisplayList(
			List<DefaultLookupResult> participantList)
	{
		List<List<String>> pcpantDisplaylst = new ArrayList<List<String>>();
		Iterator<DefaultLookupResult> itr = participantList.iterator();
		String medicalRecordNo = "";
		List<String> participantInfo;
		for (; itr.hasNext(); pcpantDisplaylst.add(participantInfo))
		{
			DefaultLookupResult result = itr.next();
			IParticipant participant = (IParticipant) result.getObject();
			participantInfo = new ArrayList<String>();
			participantInfo.add(participant.getEmpiId());
			participantInfo.add(ParticipantManagerUtility.modifyNameWithProperCase(participant
					.getLastName()));
			participantInfo.add(ParticipantManagerUtility.modifyNameWithProperCase(participant
					.getFirstName()));
			participantInfo.add(ParticipantManagerUtility.modifyNameWithProperCase(participant
					.getMiddleName()));
			participantInfo.add(Utility.parseDateToString(participant.getBirthDate(),
					Constants.DATE_FORMAT));
			participantInfo.add(Utility.parseDateToString(participant.getDeathDate(),
					Constants.DATE_FORMAT));
			participantInfo.add(Utility.toString(participant.getVitalStatus()));
			participantInfo.add(Utility.toString(participant.getGender()));
			participantInfo.add(Utility.toString(participant.getSocialSecurityNumber()));
			if (participant.getParticipantMedicalIdentifierCollection() != null)
			{
				medicalRecordNo = getParticipantMrnDisplay(participant);
			}
			participantInfo.add(Utility.toString(medicalRecordNo));
			participantInfo.add(String.valueOf(participant.getId()));
		}

		return pcpantDisplaylst;
	}

	/**
	 * Gets the participant mrn display.
	 *
	 * @param participant the participant
	 *
	 * @return the participant mrn display
	 */
	private static String getParticipantMrnDisplay(IParticipant participant)
	{
		StringBuffer mrn = new StringBuffer();
		if (participant.getParticipantMedicalIdentifierCollection() != null)
		{
			Iterator pmiItr = participant.getParticipantMedicalIdentifierCollection().iterator();
			do
			{
				if (!pmiItr.hasNext())
				{
					break;
				}
				IParticipantMedicalIdentifier<IParticipant, ISite> participantMedicalIdentifier = (IParticipantMedicalIdentifier<IParticipant, ISite>) pmiItr
						.next();
				if (participantMedicalIdentifier.getSite() != null
						&& (participantMedicalIdentifier.getSite()).getId() != null)
				{
					String siteName = participantMedicalIdentifier.getSite().getName();
					mrn.append(siteName);
					mrn.append(": ");
					mrn.append(participantMedicalIdentifier.getMedicalRecordNumber());
					//mrn.append(siteName);
					mrn.append("\n<br>");
				}
			}
			while (true);
		}
		return mrn.toString();
	}
//
//	/**
//	 * Gets the jDBCDAO.
//	 *
//	 * @return the jDBCDAO
//	 *
//	 * @throws DAOException the DAO exception
//	 */
//	public static JDBCDAO getJDBCDAO() throws DAOException
//	{
//		String appName = CommonServiceLocator.getInstance().getAppName();
//		IDAOFactory daoFactory = DAOConfigFactory.getInstance().getDAOFactory(appName);
//		JDBCDAO jdbcdao = null;
//		jdbcdao = daoFactory.getJDBCDAO();
//		jdbcdao.openSession(null);
//		return jdbcdao;
//	}
//
//	/**
//	 * Gets the dAO.
//	 *
//	 * @return the dAO
//	 *
//	 * @throws DAOException the DAO exception
//	 */
//	public static DAO getDAO() throws DAOException
//	{
//		DAO dao = null;
//		String appName = CommonServiceLocator.getInstance().getAppName();
//		IDAOFactory daoFactory = DAOConfigFactory.getInstance().getDAOFactory(appName);
//		dao = daoFactory.getDAO();
//		dao.openSession(null);
//		return dao;
//	}

	/**
	 * Adds the participant to process message queue.
	 *
	 * @param participantId the participant id
	 * @param userIdSet the user id set
	 *
	 * @throws DAOException the DAO exception
	 */
	public static void addParticipantToProcessMessageQueue(LinkedHashSet<Long> userIdSet,
			Long participantId) throws DAOException
	{
		new CommonParticipantDAO(getAppName(),null).addParticipantToProcessMessageQueue(userIdSet, participantId);
//		JDBCDAO jdbcdao = null;
//		String query = null;
//		try
//		{
//			jdbcdao = getJDBCDAO();
//			// delete old data from DB to start up clean with matching process
//			// Bug fix 18823
//			deleteProcessedParticipant(participantId, jdbcdao);
//
//			Calendar cal = Calendar.getInstance();
//			Date date = cal.getTime();
//			LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
//			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//
//			query = "INSERT INTO MATCHED_PARTICIPANT_MAPPING(NO_OF_MATCHED_PARTICIPANTS,"
//					+ "CREATION_DATE,SEARCHED_PARTICIPANT_ID) VALUES(?,?,?)";
//
//			columnValueBeanList.add(new ColumnValueBean("NO_OF_MATCHED_PARTICIPANTS", Integer
//					.valueOf(-1), DBTypes.LONG));
//			columnValueBeanList.add(new ColumnValueBean("CREATION_DATE", date, DBTypes.DATE));
//			columnValueBeanList.add(new ColumnValueBean("SEARCHED_PARTICIPANT_ID", participantId,
//					DBTypes.LONG));
//			columnValueBeans.add(columnValueBeanList);
//			jdbcdao.executeUpdate(query, columnValueBeans);
//
//			jdbcdao.commit();
//
//			updateParticipantUserMapping(jdbcdao, userIdSet, participantId);
//		}
//		catch (DAOException e)
//		{
//			jdbcdao.rollback();
//			throw new DAOException(e.getErrorKey(), e, e.getMessage());
//		}
//		finally
//		{
//			jdbcdao.closeSession();
//		}
	}

//	/**
//	 * Update participant user mapping.
//	 *
//	 * @param jdbcdao the jdbcdao
//	 * @param userIdSet the user id set
//	 * @param participantId the participant id
//	 *
//	 * @throws DAOException the DAO exception
//	 */
//	private static void updateParticipantUserMapping(JDBCDAO jdbcdao,
//			LinkedHashSet<Long> userIdSet, Long participantId) throws DAOException
//	{
//
//		Iterator iterator = userIdSet.iterator();
//		while (iterator.hasNext())
//		{
//			String query = "INSERT INTO EMPI_PARTICIPANT_USER_MAPPING VALUES(?,?)";
//			Long userId = (Long) iterator.next();
//			LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
//			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//
//			columnValueBeanList.add(new ColumnValueBean("PARTICIPANT_ID", participantId,
//					DBTypes.LONG));
//			columnValueBeanList.add(new ColumnValueBean("USER_ID", userId, DBTypes.LONG));
//			columnValueBeans.add(columnValueBeanList);
//			jdbcdao.executeUpdate(query, columnValueBeans);
//			jdbcdao.commit();
//		}
//
//	}
		
	/**
	 * Populate patient object.
	 *
	 * @param participant the participant
	 * @param protocolIdSet the protocol id set
	 *
	 * @return the patient information
	 */
	public static PatientInformation populatePatientObject(IParticipant participant,
			Set<Long> protocolIdSet)
	{
		PatientInformation patientInformation = new PatientInformation();
		patientInformation.setLastName(participant.getLastName());
		patientInformation.setFirstName(participant.getFirstName());
		patientInformation.setMiddleName(participant.getMiddleName());
		String ssn = participant.getSocialSecurityNumber();
		if (ssn != null)
		{
			String ssnValue[] = ssn.split("-");
			ssn = ssnValue[0];
			ssn = ssn + ssnValue[1];
			ssn = ssn + ssnValue[2];
		}
		patientInformation.setSsn(ssn);
		if (participant.getBirthDate() != null)
		{
			patientInformation.setDob(participant.getBirthDate());
		}
		patientInformation.setGender(participant.getGender());
		//Collection<String> participantInfoMedicalIdentifierCollection = new LinkedList<String>();
		Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> participantInfoMedicalIdentifierCollection = new LinkedList<IParticipantMedicalIdentifier<IParticipant,ISite>>();
		Collection participantMedicalIdentifierCollection = participant
				.getParticipantMedicalIdentifierCollection();
		if (participantMedicalIdentifierCollection != null)
		{
			Iterator itr = participantMedicalIdentifierCollection.iterator();
			do
			{
				if (!itr.hasNext())
				{
					break;
				}
				IParticipantMedicalIdentifier<IParticipant, ISite> participantMedicalIdentifier = (IParticipantMedicalIdentifier<IParticipant, ISite>) itr
						.next();
				if (participantMedicalIdentifier.getMedicalRecordNumber() != null)
				{
					participantInfoMedicalIdentifierCollection.add(participantMedicalIdentifier);
//							.getMedicalRecordNumber());
//					participantInfoMedicalIdentifierCollection.add(String
//							.valueOf((participantMedicalIdentifier.getSite()).getId()));
					//participantInfoMedicalIdentifierCollection.add(((ISite) participantMedicalIdentifier.getSite()).getName());
				}
			}
			while (true);
		}
//		patientInformation
//				.setParticipantMedicalIdentifierCollection(participantInfoMedicalIdentifierCollection);
		patientInformation.setPmiCollection(participantInfoMedicalIdentifierCollection);
		Collection<String> participantInfoRaceCollection = new HashSet<String>();
		Collection participantRaceCollection = participant.getRaceCollection();
		if (participantRaceCollection != null)
		{
			Iterator itr = participantRaceCollection.iterator();
			do
			{
				if (!itr.hasNext())
				{
					break;
				}
				IRace<IParticipant> race = (IRace<IParticipant>) itr.next();
				if (race != null)
				{
					participantInfoRaceCollection.add(race.getRaceName());
				}
			}
			while (true);
		}
		patientInformation.setRaceCollection(participantInfoRaceCollection);

		patientInformation.setProtocolIdSet(protocolIdSet);

		return patientInformation;
	}

	/**
	 * Delete processed participant.
	 *
	 * @param id the id
	 *
	 * @return true, if successful
	 *
	 * @throws DAOException the DAO exception
	 */
	public static void deleteProcessedParticipant(Long id) throws DAOException
	{
		new CommonParticipantDAO(getAppName(),null).deleteProcessedParticipant(id);
//		JDBCDAO jdbcdao = null;
//		try
//		{
//			jdbcdao = getJDBCDAO();
//			deleteProcessedParticipant(id, jdbcdao);
//			jdbcdao.commit();
//		}
//		catch (DAOException e)
//		{
//			jdbcdao.rollback();
//			throw new DAOException(e.getErrorKey(), e, e.getMessage());
//		}
//		finally
//		{
//			jdbcdao.closeSession();
//		}
	}

//	public static void deleteProcessedParticipant(Long id, JDBCDAO jdbcdao) throws DAOException
//	{
//		LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
//		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//		columnValueBeanList.add(new ColumnValueBean(id));
//		columnValueBeans.add(columnValueBeanList);
//		String query = "DELETE FROM MATCHED_PARTICIPANT_MAPPING WHERE SEARCHED_PARTICIPANT_ID=?";
//		jdbcdao.executeUpdate(query, columnValueBeans);
//	}	

	/**
	 * Updates the count of matched participants as 0 in case no matching was found.
	 *
	 * @param id the id
	 * @return true, if successful
	 * @throws DAOException the DAO exception
	 */
	public static void updateProcessedParticipant(Long id) throws DAOException
	{
		new CommonParticipantDAO(getAppName(),null).updateProcessedParticipant(id);
//		JDBCDAO jdbcdao = null;
//		try
//		{
//			jdbcdao = getJDBCDAO();
//			LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
//			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//			columnValueBeanList.add(new ColumnValueBean(id));
//			columnValueBeans.add(columnValueBeanList);
//			String query = "UPDATE MATCHED_PARTICIPANT_MAPPING SET NO_OF_MATCHED_PARTICIPANTS=0 "
//					+ "WHERE SEARCHED_PARTICIPANT_ID=?";
//			jdbcdao.executeUpdate(query, columnValueBeans);
//			jdbcdao.commit();
//		}
//		catch (DAOException e)
//		{
//			jdbcdao.rollback();
//			throw new DAOException(e.getErrorKey(), e, e.getMessage());
//		}
//		finally
//		{
//			jdbcdao.closeSession();
//		}
	}

	/**
	 * Gets the old participant.
	 *
	 * @param dao the dao
	 * @param identifier the identifier
	 *
	 * @return the old participant
	 *
	 * @throws BizLogicException the biz logic exception
	 */
	public static IParticipant getOldParticipant(Long identifier) throws BizLogicException
	{
		IParticipant oldParticipant;
		oldParticipant = new CommonParticipantDAO(CommonServiceLocator.getInstance().getAppName(),null).findById(identifier);
//		try
//		{
//			oldParticipant = (IParticipant) dao.retrieveById(
//					"edu.wustl.clinportal.domain.Participant", identifier);
//		}
//		catch (DAOException e)
//		{
//			LOGGER.debug(e.getMessage(), e);
//			throw new BizLogicException(e.getErrorKey(), e, e.getMsgValues());
//		}
		return oldParticipant;
	}

	/**
	 * Checks if is participant is processing.
	 *
	 * @param id the id
	 *
	 * @return true, if is participant is processing
	 *
	 * @throws DAOException the DAO exception
	 */
	public static boolean isParticipantIsProcessing(Long id) throws DAOException
	{
		return new CommonParticipantDAO(getAppName(),null).isParticipantIsProcessing(id);
//		boolean status = false;
//		JDBCDAO dao = null;
//		String query = null;
//		try
//		{
//			//query = "SELECT * FROM MATCHED_PARTICIPANT_MAPPING WHERE SEARCHED_PARTICIPANT_ID=? AND NO_OF_MATCHED_PARTICIPANTS!=?";
//			query = "SELECT * FROM MATCHED_PARTICIPANT_MAPPING WHERE SEARCHED_PARTICIPANT_ID = ? "
//					+ "AND NO_OF_MATCHED_PARTICIPANTS != 0";
//			dao = getJDBCDAO();
//			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//			columnValueBeanList
//					.add(new ColumnValueBean("SEARCHED_PARTICIPANT_ID", id, DBTypes.LONG));
//			//columnValueBeanList.add(new ColumnValueBean("NO_OF_MATCHED_PARTICIPANTS", 0,DBTypes.INTEGER));
//			List list = dao.executeQuery(query, null, columnValueBeanList);
//			if (!list.isEmpty() && !"".equals(list.get(0)))
//			{
//				status = true;
//			}
//		}
//		catch (DAOException exp)
//		{
//			LOGGER.info("ERROR WHILE GETTING THE EMPI STATUS");
//			throw new DAOException(exp.getErrorKey(), exp, exp.getMsgValues());
//		}
//		finally
//		{
//			dao.closeSession();
//		}
//		return status;
	}

	/**
	 * Gets the processed matched participant ids.
	 *
	 * @param userId the user id
	 *
	 * @return List of matching participant Ids for EMPI generation
	 *
	 * @throws DAOException the DAO exception
	 */
	public static List<Long> getProcessedMatchedParticipantIds(Long userId) throws DAOException
	{
		
		return new CommonParticipantDAO(getAppName(),null).getProcessedMatchedParticipantIds(userId);
//		JDBCDAO dao = null;
//		List<Long> particpantIdColl = new ArrayList<Long>();
//		try
//		{
//
//			dao = ParticipantManagerUtility.getJDBCDAO();
//
//			String query = "SELECT SEARCHED_PARTICIPANT_ID FROM  MATCHED_PARTICIPANT_MAPPING PARTIMAPPING  "
//					+ " JOIN EMPI_PARTICIPANT_USER_MAPPING ON PARTIMAPPING.SEARCHED_PARTICIPANT_ID=EMPI_PARTICIPANT_USER_MAPPING.PARTICIPANT_ID"
//					+ " WHERE EMPI_PARTICIPANT_USER_MAPPING.USER_ID=? AND PARTIMAPPING.NO_OF_MATCHED_PARTICIPANTS!=?";
//
//			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//			columnValueBeanList.add(new ColumnValueBean("USER_ID", userId, DBTypes.LONG));
//			columnValueBeanList.add(new ColumnValueBean("NO_OF_MATCHED_PARTICIPANTS", "-1",
//					DBTypes.INTEGER));
//
//			List resultSet = dao.executeQuery(query, null, columnValueBeanList);
//
//			for (Object object : resultSet)
//			{
//				ArrayList particpantIdList = (ArrayList) object;
//				if (particpantIdList != null && !particpantIdList.isEmpty())
//				{
//					particpantIdColl.add(Long.valueOf(particpantIdList.get(0).toString()));
//				}
//			}
//		}
//		finally
//		{
//			dao.closeSession();
//		}
//		return particpantIdColl;
	}

	/**
	 * Modify name with proper case.
	 *
	 * @param name the name
	 *
	 * @return the string
	 */
	public static String modifyNameWithProperCase(String name)
	{
		String modifiedName = "";
		if (name != null && !"".equals(name))
		{
			modifiedName = name.substring(0, 1).toUpperCase()
					+ name.substring(1, name.length()).toLowerCase();
		}
		return modifiedName;
	}

	/**
	 * Check whether the object has been changed.
	 *
	 * @param oldParticipant the old participant
	 * @param currentParticipant the current participant
	 *
	 * @return boolean
	 */
	public static boolean isParticipantEdited(IParticipant oldParticipant,
			IParticipant currentParticipant)
	{
		boolean isEdited = false;

		isEdited = isLastNameEdited(currentParticipant, oldParticipant);
		if (isEdited)
		{
			return isEdited;
		}
		isEdited = isFirstNameEdited(currentParticipant, oldParticipant);
		if (isEdited)
		{
			return isEdited;
		}
		isEdited = isMiddelNameEdited(currentParticipant, oldParticipant);
		if (isEdited)
		{
			return isEdited;
		}
		isEdited = isSSNEdited(currentParticipant, oldParticipant);
		if (isEdited)
		{
			return isEdited;
		}
		isEdited = isGenderEdited(currentParticipant, oldParticipant);
		if (isEdited)
		{
			return isEdited;
		}

		isEdited = isRaceChanged(currentParticipant.getRaceCollection(), oldParticipant
				.getRaceCollection());

		return isEdited;
	}

	/**
	 * Checks if is gender edited.
	 *
	 * @param currentParticipant the current participant
	 * @param oldParticipant the old participant
	 *
	 * @return true, if is gender edited
	 */
	private static boolean isGenderEdited(IParticipant currentParticipant,
			IParticipant oldParticipant)
	{
		if (currentParticipant.getGender() != null && !"".equals(currentParticipant.getGender()))
		{
			if (!currentParticipant.getGender().equals(oldParticipant.getGender()))
			{
				return true;
			}
		}
		else if (oldParticipant.getGender() != null && !"".equals(oldParticipant.getGender()))
		{
			return true;
		}
		return false;
	}

	/**
	 * Checks if is sSN edited.
	 *
	 * @param currentParticipant the current participant
	 * @param oldParticipant the old participant
	 *
	 * @return true, if is sSN edited
	 */
	private static boolean isSSNEdited(IParticipant currentParticipant, IParticipant oldParticipant)
	{

		if (currentParticipant.getSocialSecurityNumber() != null
				&& !"".equals(currentParticipant.getSocialSecurityNumber()))
		{
			if (!currentParticipant.getSocialSecurityNumber().equals(
					oldParticipant.getSocialSecurityNumber()))
			{
				return true;
			}
		}
		else if (oldParticipant.getSocialSecurityNumber() != null
				&& !"".equals(oldParticipant.getSocialSecurityNumber()))
		{
			return true;
		}
		return false;
	}

	/**
	 * Checks if is dOB edited.
	 *
	 * @param currentParticipant the current participant
	 * @param oldParticipant the old participant
	 *
	 * @return true, if is dOB edited
	 */
	private static boolean isDOBEdited(IParticipant currentParticipant, IParticipant oldParticipant)
	{

		if (currentParticipant.getBirthDate() != null
				&& !"".equals(currentParticipant.getBirthDate()))
		{
			if (!currentParticipant.getBirthDate().equals(oldParticipant.getBirthDate()))
			{
				return true;
			}
		}
		else if (oldParticipant.getBirthDate() != null && !"".equals(oldParticipant.getBirthDate()))
		{
			return true;
		}
		return false;
	}

	/**
	 * Checks if is middel name edited.
	 *
	 * @param currentParticipant the current participant
	 * @param oldParticipant the old participant
	 *
	 * @return true, if is middel name edited
	 */
	private static boolean isMiddelNameEdited(IParticipant currentParticipant,
			IParticipant oldParticipant)
	{
		if (currentParticipant.getMiddleName() != null
				&& !"".equals(currentParticipant.getMiddleName()))
		{
			if (!currentParticipant.getMiddleName().equals(oldParticipant.getMiddleName()))
			{
				return true;
			}
		}
		else if (oldParticipant.getMiddleName() != null
				&& !"".equals(oldParticipant.getMiddleName()))
		{
			return true;
		}
		return false;
	}

	/**
	 * Checks if is first name edited.
	 *
	 * @param currentParticipant the current participant
	 * @param oldParticipant the old participant
	 *
	 * @return true, if is first name edited
	 */
	private static boolean isFirstNameEdited(IParticipant currentParticipant,
			IParticipant oldParticipant)
	{
		if (currentParticipant.getFirstName() != null
				&& !"".equals(currentParticipant.getFirstName()))
		{
			if (!currentParticipant.getFirstName().equals(oldParticipant.getFirstName()))
			{
				return true;
			}
		}
		else if (oldParticipant.getFirstName() != null && !"".equals(oldParticipant.getFirstName()))
		{
			return true;
		}
		return false;
	}

	/**
	 * Checks if is last name edited.
	 *
	 * @param currentParticipant the current participant
	 * @param oldParticipant the old participant
	 *
	 * @return true, if is last name edited
	 */
	private static boolean isLastNameEdited(IParticipant currentParticipant,
			IParticipant oldParticipant)
	{
		if (currentParticipant.getLastName() != null
				&& !"".equals(currentParticipant.getLastName()))
		{
			if (!currentParticipant.getLastName().equals(oldParticipant.getLastName()))
			{
				return true;
			}
		}
		else if (oldParticipant.getLastName() != null && !"".equals(oldParticipant.getLastName()))
		{
			return true;
		}
		return false;
	}

	/**
	 * Checks whether race value has been changed.
	 *
	 * @param raceColNew the race col new
	 * @param raceColOld the race col old
	 *
	 * @return boolean
	 */
	private static boolean isRaceChanged(Collection raceColNew, Collection raceColOld)
	{

		String raceNameNew = null;
		String raceNameOld = null;
		boolean found = false;
		if (raceColNew != null && !raceColNew.isEmpty())
		{

			if (raceColOld == null || raceColOld.isEmpty())
			{
				return true;
			}
			if ((raceColNew.size() > raceColOld.size()) || (raceColOld.size() > raceColNew.size()))
			{
				return true;
			}
			Iterator<IRace<IParticipant>> iterNew = raceColNew.iterator();
			while (iterNew.hasNext())
			{
				IRace<IParticipant> raceNew = iterNew.next();
				raceNameNew = raceNew.getRaceName();
				Iterator<IRace<IParticipant>> iterOld = raceColOld.iterator();
				found = false;
				while (iterOld.hasNext())
				{
					IRace<IParticipant> raceOld = iterOld.next();
					raceNameOld = raceOld.getRaceName();
					if (raceNameNew.equals(raceNameOld))
					{
						found = true;
					}
				}
				if (!found)
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Method gets all the protocol ids for the MICS to which the protocol is is associated with
	 * Suppose  protocolId associated with MICSId and MICSId enabled for participant
	 * match within mics then this method will return all the protocol ids associated with the MICSid.
	 *
	 * @param protocolId the protocol id
	 *
	 * @return the associated mutli inst protocol id list
	 *
	 * @throws ParticipantManagerException the participant manager exception
	 * @throws ApplicationException the application exception
	 */
	private static Set<Long> getProtocolIdLstForMICSEnabledForMatching(Long protocolId)
			throws ParticipantManagerException, ApplicationException
	{

		IParticipantManager participantManagerImplObj = getParticipantMgrImplObj();
		Set<Long> protocolIdList = participantManagerImplObj
				.getProtocolIdLstForMICSEnabledForMatching(protocolId);
		return protocolIdList;
	}

	/**
	 * Gets the protocol ids string.
	 *
	 * @param protocolIdSet the protocol id set
	 *
	 * @return the protocol ids string
	 */
	public static String getProtocolIdsString(Set<Long> protocolIdSet)
	{
		String protocolIdsStr = new String();
		if (protocolIdSet != null && !protocolIdSet.isEmpty())
		{
			Iterator<Long> iterator = protocolIdSet.iterator();
			while (iterator.hasNext())
			{
				Long protocolId = iterator.next();
				protocolIdsStr = protocolIdsStr.concat(String
						.valueOf((protocolIdsStr.length() == 0) ? protocolId : "," + protocolId));
			}
		}
		return protocolIdsStr;
	}

	/**
	 * Gets the participant mgr impl obj.
	 *
	 * @return the participant mgr impl obj
	 *
	 * @throws ParticipantManagerException the participant manager exception
	 */
	public static IParticipantManager getParticipantMgrImplObj() throws ParticipantManagerException
	{
		IParticipantManager participantManagerImplObj = null;
		try
		{
			String PartiManagerImplClassName = edu.wustl.common.participant.utility.PropertyHandler
					.getValue(edu.wustl.common.participant.utility.Constants.PARTICIPANT_MANAGER_IMPL_CLASS);
			participantManagerImplObj = (IParticipantManager) ParticipantManagerUtility
					.getObject(PartiManagerImplClassName);
		}
		catch (BizLogicException e)
		{
			// TODO Auto-generated catch block
			throw new ParticipantManagerException(e.getMessage(), e);
		}
		return participantManagerImplObj;

	}

//	/**
//	 * Gets the last name query.
//	 *
//	 * @param protocolIdSet the protocol id set
//	 * @param participantObjName the participant object name
//	 *
//	 * @return the last name query
//	 *
//	 * @throws ParticipantManagerException the participant manager exception
//	 */
//	public static String getLastNameQry(Set<Long> protocolIdSet, String participantObjName)
//			throws ParticipantManagerException
//	{
//		String fetchByNameQry = null;
//		IParticipantManager participantManagerImplObj = getParticipantMgrImplObj();
//
//		fetchByNameQry = participantManagerImplObj.getLastNameQuery(protocolIdSet);
//
//		return fetchByNameQry;
//	}
//
//	/**
//	 * Gets the meta phone query.
//	 *
//	 * @param protocolIdSet the protocol id set
//	 * @param participantObjName the participant object name
//	 *
//	 * @return the meta phone query
//	 *
//	 * @throws ParticipantManagerException the participant manager exception
//	 */
//	public static String getMetaPhoneQry(Set<Long> protocolIdSet, String participantObjName)
//			throws ParticipantManagerException
//	{
//		String fetchByMetaPhoneQry = null;
//		IParticipantManager participantManagerImplObj = getParticipantMgrImplObj();
//		fetchByMetaPhoneQry = participantManagerImplObj.getMetaPhoneCodeQuery(protocolIdSet);
//		return fetchByMetaPhoneQry;
//	}
//
//	/**
//	 * Gets the sSN query.
//	 *
//	 * @param protocolIdSet the protocol id set
//	 * @param participantObjName the participant obj name
//	 *
//	 * @return the sSN query
//	 *
//	 * @throws ParticipantManagerException the participant manager exception
//	 */
//	public static String getSSNQuery(Set<Long> protocolIdSet, String participantObjName)
//			throws ParticipantManagerException
//	{
//		String fetchBySSNQry = null;
//		IParticipantManager participantManagerImplObj = getParticipantMgrImplObj();
//
//		fetchBySSNQry = participantManagerImplObj.getSSNQuery(protocolIdSet);
//
//		return fetchBySSNQry;
//
//	}
//
//	/**
//	 * Gets the mRN query.
//	 *
//	 * @param protocolIdSet the protocol id set
//	 * @param pmiObjName the pmi object name
//	 *
//	 * @return the mRN query
//	 *
//	 * @throws ParticipantManagerException the participant manager exception
//	 */
//	public static String getMRNQuery(Set<Long> protocolIdSet, String pmiObjName)
//			throws ParticipantManagerException
//	{
//		IParticipantManager participantManagerImplObj = getParticipantMgrImplObj();
//		String fetchByMRNQry = null;
//		fetchByMRNQry = participantManagerImplObj.getMRNQuery(protocolIdSet);
//		return fetchByMRNQry;
//	}
//
//	/**
//	 *
//	 * @param protocolIdSet
//	 * @param participantObjName
//	 * @return
//	 * @throws ParticipantManagerException
//	 */
//	public static String getParticipantCodeQry(Set<Long> protocolIdSet)
//	throws ParticipantManagerException
//	{
//		String fetchByNameQry = null;
//		IParticipantManager participantManagerImplObj = getParticipantMgrImplObj();
//
//		fetchByNameQry = participantManagerImplObj.getParticipantCodeQuery(protocolIdSet);
//
//		return fetchByNameQry;
//	}
	/**
	 * Fetch the PI and coordinators IDs.
	 * @param participantId
	 * @return
	 * @throws ApplicationException
	 */
	public static LinkedHashSet<Long> getParticipantPICordinators(long participantId)
			throws ApplicationException
	{
		IParticipantManager participantManagerImplObj;
		try
		{
			participantManagerImplObj = getParticipantMgrImplObj();
		}
		catch (ParticipantManagerException e)
		{
			throw new ApplicationException(null, e, e.getMessage());
		}
		return participantManagerImplObj.getParticipantPICordinators(participantId);
	}

	/**
	 * Closes the session and the connection to the WMQ queues when the server is shut down.
	 * @throws JMSException
	 */
	public void unregisterWMQListener() throws JMSException
	{
		try
		{
			// closing session for queue
			if (null != inBoundQueueSession)
			{
				inBoundQueueSession.close();
			}
			// closing session for queue
			if (null != mergeQueueSession)
			{
				mergeQueueSession.close();
			}
			//closing queue connection
			if (null != connectionToQueue)
			{
				connectionToQueue.close();
			}
		}
		catch (JMSException e)
		{
			LOGGER.error(" -------------  ERROR WHILE CLOSING THE MESSAGE QUEUES"
					+ " \n \n ------------- ");
			LOGGER.error(e.getMessage());
			LOGGER.error(e.getStackTrace());
			e.printStackTrace();
			e.getLinkedException();
			LOGGER.error(e.getLinkedException());
			LOGGER.error(e.getLinkedException().getMessage());
			LOGGER.error(e.getLinkedException().getStackTrace());
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			LOGGER.error(e.getMessage());
		}
	}

	/**
	 * Method to insert tempMRN entry in case eMPI ID was earlier generated
	 * @param participantId
	 * @param empiId
	 * @param jdbcdao
	 * @throws DAOException
	 */
	public void updateOldEMPIDetails(Long participantId, String empiId)
			throws DAOException
	{
		new CommonParticipantDAO(getAppName(),null).updateOldEMPIDetails(participantId, empiId);
//		final LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
//		final LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//
//		String temporaryParticipantId = participantId + "T";
//		columnValueBeanList.add(new ColumnValueBean("PERMANENT_PARTICIPANT_ID", participantId,
//				DBTypes.VARCHAR));
//		columnValueBeanList.add(new ColumnValueBean("TEMPARARY_PARTICIPANT_ID",
//				temporaryParticipantId, DBTypes.VARCHAR));
//		columnValueBeanList.add(new ColumnValueBean("OLD_EMPI_ID", empiId, DBTypes.VARCHAR));
//		columnValueBeanList.add(new ColumnValueBean("TEMPMRNDATE", new Timestamp(System
//				.currentTimeMillis()), DBTypes.TIMESTAMP));
//
//		final String sql = "INSERT INTO PARTICIPANT_EMPI_ID_MAPPING VALUES(?,?,?,?)";
//		columnValueBeans.add(columnValueBeanList);
//		jdbcdao.executeUpdate(sql, columnValueBeans);
	}

	/**
	 *
	 * @param participantId
	 * @param status
	 * @param jdbcDao
	 * @throws DAOException
	 */
	public static void setEMPIIdStatus(Long participantId, String status)
			throws DAOException
	{
		new CommonParticipantDAO(getAppName(),null).setEMPIIdStatus(participantId, status);
//		LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
//		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//		columnValueBeanList.add(new ColumnValueBean(status));
//		columnValueBeanList.add(new ColumnValueBean(participantId));
//		columnValueBeans.add(columnValueBeanList);
//		String sql = "UPDATE CATISSUE_PARTICIPANT SET EMPI_ID_STATUS=? WHERE IDENTIFIER=?";
//		jdbcDao.executeUpdate(sql, columnValueBeans);
	}
	/**
	 * Gets the session data bean.
	 *
	 * @param validUser the valid user
	 *
	 * @return the session data bean
	 */
	public static SessionDataBean getSessionDataBean(final IUser validUser)
	{
		final SessionDataBean sessionData = new SessionDataBean();
		sessionData.setAdmin(validUser.getAdminuser());
		sessionData.setUserName(validUser.getLoginName());
		sessionData.setUserId(validUser.getId());
		sessionData.setFirstName(validUser.getFirstName());
		sessionData.setLastName(validUser.getLastName());
		sessionData.setCsmUserId(validUser.getCsmUserId().toString());
		return sessionData;
	}

	/**
	 * this method will return the xquery executir instance configured from DB values
	 * @return xquery executor configred from clinportal_properties file.
	 * @throws Exception exception.
	 */
	public static  IQueryExecutor getXQueryExecutor() throws Exception
	{

		final String dbURL = XMLPropertyHandler.getValue(Constants.EMPIDBURL);
		final String dbUser = XMLPropertyHandler.getValue(Constants.EMPIDBUSERNAME);
		final String dbPassword = XMLPropertyHandler.getValue(Constants.EMPIDBUSERPASSWORD);
		final String dbDriver = XMLPropertyHandler.getValue(Constants.EMPIDBDRIVERNAME);
		final String dbSchema = XMLPropertyHandler.getValue(Constants.EMPIDBSCHEMA);

			final PatientInfoLookUpService lookUpEMPI = new PatientInfoLookUpService();
			final edu.wustl.patientLookUp.queryExecutor.IQueryExecutor xQueyExecutor = PatientLookUpFactory
					.getQueryExecutorImpl(dbURL, dbUser, dbPassword, dbDriver, dbSchema);
			return xQueyExecutor;
	}

	/**
	 * This method will return valid session data bean created for user specified in HL7 listener admin property
	 * @return valid session data beam.
	 * @throws Exception exception if user does not have right previlages.
	 */
	public static SessionDataBean getValidSessionBean() throws Exception
	{
		SessionDataBean sessionData=null;
		String loginName = XMLPropertyHandler.getValue(Constants.HL7_LISTENER_ADMIN_USER);
		//loginName = Constants.CLINPORTAL_EMPI_ADMIN_LOGIN_ID;
		IUser validUser = getUser(loginName, Constants.ACTIVITY_STATUS_ACTIVE);

		if (validUser != null)
		{
			sessionData = getSessionDataBean(validUser);
		}
		else
		{
			checkUserAccount(loginName);
		}
		return sessionData;
	}

		/**
		 * Gets the user.
		 *
		 * @param loginName the login name
		 * @param activityStatus the activity status
		 *
		 * @return the user
		 *
		 * @throws BizLogicException the biz logic exception
		 * @throws ParticipantManagerException
		 */
		public static IUser getUser(final String loginName, final String activityStatus)
				throws BizLogicException, ParticipantManagerException,ApplicationException
		{
			try
			{
				return getParticipantMgrImplObj().getUserByLoginNameAndActivityStatus(loginName, activityStatus);
			}
			catch(DAOException daoExp)
			{
				throw new BizLogicException(daoExp);
			}
		}

		/**
		 * Check user account.
		 *
		 * @param loginName the login name
		 * @throws Exception
		 * @throws BizLogicException
		 *
		 * @throws BizLogicException the biz logic exception
		 * @throws Exception the exception
		 */
		private static void checkUserAccount(final String loginName) throws BizLogicException, Exception
		{
			if (getUser(loginName, Constants.ACTIVITY_STATUS_CLOSED) != null)
			{
				throw new ParticipantManagerException(loginName + " Closed user. Sending back to the login Page",null);
			}
			else
			{
				throw new ParticipantManagerException(loginName + "Invalid user. Sending back to the login Page",null);
			}
		}

}
