
package edu.wustl.common.participant.utility;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import edu.wustl.common.bizlogic.IBizLogic;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.factory.AbstractFactoryConfig;
import edu.wustl.common.factory.IFactory;
import edu.wustl.common.lookup.DefaultLookupParameters;
import edu.wustl.common.lookup.DefaultLookupResult;
import edu.wustl.common.lookup.LookupLogic;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.IRace;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.listener.EMPIParticipantListener;
import edu.wustl.common.participant.listener.EMPIParticipantMergeMessageListener;
import edu.wustl.common.util.Utility;
import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.common.util.global.CommonServiceLocator;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.DAO;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.daofactory.DAOConfigFactory;
import edu.wustl.dao.daofactory.IDAOFactory;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.query.generator.ColumnValueBean;
import edu.wustl.patientLookUp.domain.PatientInformation;
import edu.wustl.patientLookUp.util.PropertyHandler;

// TODO: Auto-generated Javadoc
/**
 * The Class ParticipantManagerUtility.
 */
public class ParticipantManagerUtility
{

	/** The logger. */
	private static final Logger logger = Logger.getCommonLogger(ParticipantManagerUtility.class);



	/**
	 * Register wmq listener.
	 *
	 * @throws JMSException the JMS exception
	 * @throws BizLogicException the biz logic exception
	 */
	public static void registerWMQListener() throws JMSException, BizLogicException
	{
		String hostName = null;
		String qmgName = null;
		String channel = null;
		String outBoundQueueName = null;
		String mergeMessageQueueName = null;
		int port = 0;
		try
		{
			hostName = XMLPropertyHandler.getValue(Constants.WMQ_SERVER_NAME);
			qmgName = XMLPropertyHandler.getValue(Constants.WMQ_QMG_NAME);
			channel = XMLPropertyHandler.getValue(Constants.WMQ_CHANNEL);
			if (XMLPropertyHandler.getValue(Constants.WMQ_PORT) != null
					&& XMLPropertyHandler.getValue(Constants.WMQ_PORT) != "")
			{
				port = Integer.parseInt(XMLPropertyHandler.getValue(Constants.WMQ_PORT));
			}
			MQQueueConnectionFactory factory = new MQQueueConnectionFactory();
			factory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
			factory.setQueueManager(qmgName);
			factory.setHostName(hostName);
			factory.setChannel(channel);
			factory.setPort(port);

			QueueConnection connection = factory.createQueueConnection();

			connection.start();

			QueueSession session = connection.createQueueSession(false,
					javax.jms.Session.AUTO_ACKNOWLEDGE);
			outBoundQueueName = XMLPropertyHandler.getValue(Constants.OUT_BOUND_QUEUE_NAME);
			Queue outBoundQueue = session.createQueue("queue:///" + outBoundQueueName);
			QueueReceiver queueReceiver = session.createReceiver(outBoundQueue);

			EMPIParticipantListener listener = new EMPIParticipantListener();

			queueReceiver.setMessageListener(listener);

			// Set the merge message queue listener.
			mergeMessageQueueName = XMLPropertyHandler.getValue(Constants.MERGE_MESSAGE_QUEUE);
			Queue mrgMessageQueue = session.createQueue("queue:///" + mergeMessageQueueName);
			queueReceiver = session.createReceiver(mrgMessageQueue);
			EMPIParticipantMergeMessageListener mrgMesListener = new EMPIParticipantMergeMessageListener();
			queueReceiver.setMessageListener(mrgMesListener);
		}
		catch (JMSException e)
		{
			// TODO Auto-generated catch block
			logger
					.error(" -------------  ERROR WHILE INITIALISING THE MESSAGE QUEUES \n \n ------------- ");
			logger.info(e.getMessage());
			throw new JMSException(e.getMessage());

		}
	}

	/**
	 * Initialise particiapnt match scheduler.
	 */
	public static void initialiseParticiapntMatchScheduler()
	{
		ParticipantMatchingTimerTask timerTask = new ParticipantMatchingTimerTask();
		Timer scheduleTime = new Timer();
		String delay = XMLPropertyHandler.getValue(Constants.PARTICIPANT_MATCHING_SCHEDULAR_DELAY);
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
	 * @throws Exception the exception
	 */
	public static IParticipantMedicalIdentifier getParticipantMedicalIdentifierObj(String mrn,
			String facilityId) throws Exception
	{

		ISite site = null;
		site = getSiteObject(facilityId);
		IParticipantMedicalIdentifier participantMedicalIdentifier = null;
		if (site != null)
		{
			participantMedicalIdentifier = (IParticipantMedicalIdentifier) getPMIInstance();
			participantMedicalIdentifier.setMedicalRecordNumber(mrn);
			participantMedicalIdentifier.setSite(site);
		}
		return participantMedicalIdentifier;
	}

	/**
	 * Gets the site object.
	 *
	 * @param facilityId the facility id
	 *
	 * @return the site object
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws Exception the exception
	 */
	public static ISite getSiteObject(String facilityId) throws BizLogicException
	{
		String sourceObjectName = ISite.class.getName();
		String selectColumnNames[] = {"id", "name"};
		String whereColumnName[] = {"facilityId"};
		String whColCondn[] = {"="};
		DefaultBizLogic bizLogic = new DefaultBizLogic();
		ISite site = null;
		Object whereColumnValue[] = {facilityId};
		List siteObject = bizLogic.retrieve(sourceObjectName, selectColumnNames, whereColumnName,
				whColCondn, whereColumnValue, null);
		if (siteObject != null && siteObject.size() > 0)
		{
			Object siteList[] = (Object[]) (Object[]) siteObject.get(0);
			Long siteId = (Long) siteList[0];
			String siteName = (String) siteList[1];
			site = (ISite) ParticipantManagerUtility.getSiteInstance();
			site.setId(siteId);
			site.setName(siteName);
		}
		return site;
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
	public static Object getObject(String bizLogicFactoryName) throws BizLogicException
	{
		try
		{
			Class className = Class.forName(bizLogicFactoryName);
			Object newobj = className.newInstance();
			return newobj;
		}
		catch (IllegalAccessException e)
		{
			logger.info(e.getMessage());
			throw new BizLogicException(null, null, "IllegalAccessException",
					"IllegalAccessException");
		}
		catch (InstantiationException e)
		{
			logger.info(e.getMessage());
			throw new BizLogicException(null, null, "InstantiationException",
					"InstantiationException");
		}
		catch (ClassNotFoundException e)
		{
			logger.info(e.getMessage());
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
	public static IParticipant getParticipantById(Long identifier) throws BizLogicException
	{
		// Initializing instance of IBizLogic
		IFactory factory = AbstractFactoryConfig.getInstance().getBizLogicFactory();

		IBizLogic bizLogic = factory.getBizLogic(Constants.DEFAULT_BIZ_LOGIC);
		String sourceObjectName = IParticipant.class.getName();
		// getting all the participants from the database
		List participantList = bizLogic.retrieve(sourceObjectName, "id", identifier);
		return (IParticipant) participantList.get(0);

	}

	/**
	 * Checks if is participant valid for empi.
	 *
	 * @param LName the l name
	 * @param FName the f name
	 * @param dob the dob
	 *
	 * @return true, if is participant valid for empi
	 */
	public static boolean isParticipantValidForEMPI(String LName, String FName, Date dob)
	{
		boolean isValid = true;
		if (LName == null || LName == "")
		{
			isValid = false;
		}
		else if (FName == null || FName == "")
		{
			isValid = false;
		}
		else if (dob == null)
		{
			isValid = false;
		}
		return isValid;
	}

	/**
	 * Gets the list of matching participants.
	 *
	 * @param participant the participant
	 * @param sessionDataBean the session data bean
	 * @param lookupAlgorithm the lookup algorithm
	 *
	 * @return the list of matching participants
	 *
	 * @throws Exception the exception
	 */
	public static List getListOfMatchingParticipants(IParticipant participant,
			SessionDataBean sessionDataBean, String lookupAlgorithm) throws Exception
	{
		List matchParticipantList = null;
		LookupLogic partLookupLgic = null;
		if (lookupAlgorithm == null)
		{
			partLookupLgic = (LookupLogic) Utility.getObject(XMLPropertyHandler
					.getValue(Constants.PARTICIPANT_LOOKUP_ALGO));
		}
		else
		{
			partLookupLgic = (LookupLogic) Utility.getObject(XMLPropertyHandler
					.getValue(lookupAlgorithm));
		}
		DefaultLookupParameters params = new DefaultLookupParameters();
		params.setObject(participant);
		matchParticipantList = partLookupLgic.lookup(params);
		return matchParticipantList;
	}

	/**
	 * Gets the race instance.
	 *
	 * @return the race instance
	 *
	 * @throws BizLogicException the biz logic exception
	 */
	public static Object getRaceInstance() throws BizLogicException
	{
		String application = applicationType();
		Object raceInstance = null;
		if (Constants.CLINPORTAL_APPLICATION_NAME.equals(application))
		{
			raceInstance = getObject("edu.wustl.clinportal.domain.Race");
		}
		else
		{
			raceInstance = getObject("edu.wustl.catissuecore.domain.Race");
		}
		return raceInstance;
	}

	/**
	 * Gets the site instance.
	 *
	 * @return the site instance
	 *
	 * @throws BizLogicException the biz logic exception
	 */
	public static Object getSiteInstance() throws BizLogicException
	{
		String application = applicationType();
		Object siteInstance = null;
		if (Constants.CLINPORTAL_APPLICATION_NAME.equals(application))
		{
			siteInstance = getObject("edu.wustl.clinportal.domain.Site");
		}
		else
		{
			siteInstance = getObject("edu.wustl.catissuecore.domain.Site");
		}
		return siteInstance;
	}

	/**
	 * Gets the participant instance.
	 *
	 * @return the participant instance
	 *
	 * @throws BizLogicException the biz logic exception
	 */
	public static Object getParticipantInstance() throws BizLogicException
	{
		String application = applicationType();
		Object partiicpantInstance = null;
		if (Constants.CLINPORTAL_APPLICATION_NAME.equals(application))
		{
			partiicpantInstance = getObject("edu.wustl.clinportal.domain.Participant");
		}
		else
		{
			partiicpantInstance = getObject("edu.wustl.catissuecore.domain.Participant");
		}
		return partiicpantInstance;
	}

	/**
	 * Gets the pMI instance.
	 *
	 * @return the pMI instance
	 *
	 * @throws BizLogicException the biz logic exception
	 */
	public static Object getPMIInstance() throws BizLogicException
	{
		String application = applicationType();
		Object PMIInstance = null;
		if (Constants.CLINPORTAL_APPLICATION_NAME.equals(application))
		{
			PMIInstance = getObject("edu.wustl.clinportal.domain.ParticipantMedicalIdentifier");
		}
		else
		{
			PMIInstance = getObject("edu.wustl.catissuecore.domain.ParticipantMedicalIdentifier");
		}
		return PMIInstance;
	}

	/**
	 * Application type.
	 *
	 * @return the string
	 *
	 * @throws BizLogicException the biz logic exception
	 */
	public static String applicationType() throws BizLogicException
	{
		String application = null;
		try
		{
			application = PropertyHandler.getValue("application");
		}
		catch (Exception e)
		{
			logger.info(e.getStackTrace());
			throw new BizLogicException(null, e,
					"Error while get value from PatientInfoLookUpService.properties");
		}
		return application;
	}

	/**
	 * Sets the empi id status.
	 *
	 * @param participantId the participant id
	 * @param status the status
	 *
	 * @throws DAOException the DAO exception
	 */
	public static void setEMPIIdStatus(Long participantId, String status) throws DAOException
	{
		JDBCDAO jdbcDao = null;
		try
		{
			jdbcDao = getJDBCDAO();
			String sql = (new StringBuilder()).append(
					"UPDATE CATISSUE_PARTICIPANT SET EMPI_ID_STATUS='").append(status).append(
					"' WHERE IDENTIFIER='").append(participantId).append("'").toString();
			jdbcDao.executeUpdate(sql);
			jdbcDao.commit();
		}
		catch (DAOException e)
		{
			logger.info("ERROE WHILE UPDATING THE PARTICIPANT EMPI STATUS");
			throw new DAOException(e.getErrorKey(), e, e.getMsgValues());
		}
		finally
		{
			jdbcDao.closeSession();
		}
	}

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
		ssn = (new StringBuilder()).append(ssnA).append("-").append(ssnB).append("-").append(ssnC)
				.toString();
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

		JDBCDAO jdbcDao = null;
		try
		{
			jdbcDao = getJDBCDAO();

			String sql = "SELECT  columnData.COLUMN_NAME,displayData.DISPLAY_NAME FROM CATISSUE_INTERFACE_"
					+ "COLUMN_DATA columnData,CATISSUE_TABLE_RELATION relationData,CATISSUE_QUERY_TABLE"
					+ "_DATA tableData,CATISSUE_SEARCH_DISPLAY_DATA displayData where relationData.CHIL"
					+ "D_TABLE_ID = columnData.TABLE_ID and relationData.PARENT_TABLE_ID = tableData.TA"
					+ "BLE_ID and relationData.RELATIONSHIP_ID = displayData.RELATIONSHIP_ID and column"
					+ "Data.IDENTIFIER = displayData.COL_ID and tableData.ALIAS_NAME = 'Participant'";
			logger
					.debug((new StringBuilder()).append("DATA ELEMENT SQL : ").append(sql)
							.toString());
			List list = jdbcDao.executeQuery(sql);
			for (Iterator iterator1 = columnList.iterator(); iterator1.hasNext();)
			{
				String colName1 = (String) iterator1.next();
				Iterator iterator2 = list.iterator();
				while (iterator2.hasNext())
				{
					List rowList = (List) iterator2.next();
					String colName2 = (String) rowList.get(0);
					if (colName1.equals(colName2))
					{
						displayList.add((String) rowList.get(1));
					}
				}
			}
		}
		finally
		{
			jdbcDao.closeSession();
		}

		return displayList;
	}

	/**
	 * Cs empi status.
	 *
	 * @param participantId the participant id
	 *
	 * @return true, if successful
	 *
	 * @throws DAOException the DAO exception
	 */
	public static boolean isEMPIEnable(long participantId) throws DAOException
	{
		boolean status;

		JDBCDAO dao = null;
		status = false;
		String application = null;
		String query = null;
		try
		{
			application = PropertyHandler.getValue("application");
		}
		catch (Exception e)
		{
			logger.info(e.getMessage());
			throw new DAOException(null, e,
					"Error while get value from PatientInfoLookUpService.properties");
		}

		dao = getJDBCDAO();

		try
		{
			if (Constants.CLINPORTAL_APPLICATION_NAME.equals(application))
			{
				query = " SELECT SP.IS_EMPI_ENABLE FROM  CATISSUE_SPECIMEN_PROTOCOL SP JOIN  CATISSUE_CLINICAL_STUDY_REG CSR  "
						+ " ON SP.IDENTIFIER = CSR.CLINICAL_STUDY_ID  WHERE CSR.PARTICIPANT_id='"
						+ participantId + "'";
			}
			else
			{
				query = " SELECT SP.IS_EMPI_ENABLE FROM  CATISSUE_SPECIMEN_PROTOCOL SP JOIN  CATISSUE_CLINICAL_STUDY_REG CSR  "
						+ " ON SP.IDENTIFIER = CSR.CLINICAL_STUDY_ID  WHERE CSR.PARTICIPANT_id='"
						+ participantId + "'";
			}
			List statusList = dao.executeQuery(query);
			if (!statusList.isEmpty() && statusList.get(0) != "")
			{
				List idList = (List) statusList.get(0);
				if (!idList.isEmpty() && ((String) idList.get(0)).equals("1"))
				{
					status = true;
				}
			}
		}
		finally
		{
			dao.closeSession();
		}
		return status;
	}

	/**
	 * Update parti empi id.
	 *
	 * @param participantId the participant id
	 * @param lastName the last name
	 * @param firstName the first name
	 * @param dob the dob
	 *
	 * @throws ParseException the parse exception
	 * @throws DAOException the DAO exception
	 */
	public static void updatePartiEMPIId(long participantId, String lastName, String firstName,
			Date dob) throws ParseException, DAOException
	{
		if (isParticipantValidForEMPI(lastName, firstName, dob))
		{
			JDBCDAO dao = null;
			try
			{
				dao = getJDBCDAO();
				String query = (new StringBuilder())
						.append(
								"UPDATE CATISSUE_PARTICIPANT SET EMPI_ID_STATUS='PENDING' WHERE IDENTIFIER='")
						.append(participantId).append("'").toString();
				dao.executeUpdate(query);
				dao.commit();

			}
			catch (DAOException exp)
			{
				logger.info("ERROR WHILE UPDATING THE EMPI STATUS");
				throw new DAOException(exp.getErrorKey(), exp, exp.getMsgValues());
			}
			finally
			{
				dao.closeSession();
			}

		}
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

		JDBCDAO dao = null;
		String eMPIStatus = "";
		try
		{
			dao = getJDBCDAO();
			String query = (new StringBuilder()).append(
					"SELECT EMPI_ID_STATUS FROM CATISSUE_PARTICIPANT  WHERE IDENTIFIER='").append(
					participantId).append("'").toString();
			List list = dao.executeQuery(query);
			if (!list.isEmpty() && list.get(0) != "")
			{
				List statusList = (List) list.get(0);
				if (!statusList.isEmpty())
				{
					eMPIStatus = (String) statusList.get(0);
				}
			}
			dao.commit();

		}
		catch (DAOException exp)
		{
			logger.info("ERROR WHILE GETTING THE EMPI STATUS");
			throw new DAOException(exp.getErrorKey(), exp, exp.getMsgValues());
		}
		finally
		{
			dao.closeSession();
		}
		return eMPIStatus;
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
		displayList.add("EMPI");
		displayList.add("EMPIID");
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
			DefaultLookupResult result = (DefaultLookupResult) itr.next();
			IParticipant participant = (IParticipant) result.getObject();
			participantInfo = new ArrayList<String>();
			participantInfo.add(participant.getIsFromEMPI());
			participantInfo.add(participant.getEmpiId());
			participantInfo.add(Utility.toString(participant.getLastName()));
			participantInfo.add(Utility.toString(participant.getFirstName()));
			participantInfo.add(Utility.toString(participant.getMiddleName()));
			participantInfo.add(Utility.toString(participant.getBirthDate()));
			participantInfo.add(Utility.toString(participant.getDeathDate()));
			participantInfo.add(Utility.toString(participant.getVitalStatus()));
			participantInfo.add(Utility.toString(participant.getGender()));
			participantInfo.add(Utility.toString(participant.getSocialSecurityNumber()));
			if (participant.getParticipantMedicalIdentifierCollection() != null)
			{
				medicalRecordNo = getParticipantMrnDisplay(participant);
			}
			participantInfo.add(Utility.toString(medicalRecordNo));
			//participantInfo.add(participant.getId());
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
				IParticipantMedicalIdentifier participantMedicalIdentifier = (IParticipantMedicalIdentifier) pmiItr
						.next();
				if (participantMedicalIdentifier.getSite() != null
						&& ((ISite) participantMedicalIdentifier.getSite()).getId() != null)
				{
					String siteName = ((ISite) participantMedicalIdentifier.getSite()).getName();
					mrn.append(participantMedicalIdentifier.getMedicalRecordNumber());
					mrn.append(':');
					mrn.append(siteName);
					mrn.append("\n<br>");
				}
			}
			while (true);
		}
		return mrn.toString();
	}

	/**
	 * Gets the jDBCDAO.
	 *
	 * @return the jDBCDAO
	 *
	 * @throws DAOException the DAO exception
	 */
	public static JDBCDAO getJDBCDAO() throws DAOException
	{
		String appName = CommonServiceLocator.getInstance().getAppName();
		IDAOFactory daoFactory = DAOConfigFactory.getInstance().getDAOFactory(appName);
		JDBCDAO jdbcdao = null;
		jdbcdao = daoFactory.getJDBCDAO();
		jdbcdao.openSession(null);
		return jdbcdao;
	}

	/**
	 * Gets the dAO.
	 *
	 * @return the dAO
	 *
	 * @throws DAOException the DAO exception
	 */
	public static DAO getDAO() throws DAOException
	{
		DAO dao = null;
		String appName = CommonServiceLocator.getInstance().getAppName();
		IDAOFactory daoFactory = DAOConfigFactory.getInstance().getDAOFactory(appName);
		dao = daoFactory.getDAO();
		dao.openSession(null);
		return dao;
	}

	/**
	 * Adds the participant to process message queue.
	 *
	 * @param userId the user id
	 * @param participantId the participant id
	 *
	 * @throws DAOException the DAO exception
	 */
	public static void addParticipantToProcessMessageQueue(Long userId, Long participantId)
			throws DAOException
	{

		JDBCDAO jdbcdao = null;
		String query = null;
		try
		{
			jdbcdao = getJDBCDAO();
			List idList = jdbcdao.executeQuery((new StringBuilder()).append(
					"SELECT SEARCHED_PARTICIPANT_ID FROM MATCHED_PARTICIPANT_MAPPING WHERE SEARCHED_P"
							+ "ARTICIPANT_ID='").append(participantId).append("'").toString());
			if (!idList.isEmpty() && idList.get(0) != "")
			{
				query = (new StringBuilder())
						.append(
								"UPDATE MATCHED_PARTICIPANT_MAPPING SET SEARCHED_PARTICIPANT_ID=?,NO_OF_MATCHED_P"
										+ "ARTICIPANTS=?,USER_ID=?,CREATION_DATE=? WHERE SEARCHED_PARTICIPANT_ID ='")
						.append(participantId).append("'").toString();
			}
			else
			{
				query = "INSERT INTO MATCHED_PARTICIPANT_MAPPING VALUES(?,?,?,?)";
			}
			Calendar cal = Calendar.getInstance();
			Date date = cal.getTime();
			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
			columnValueBeanList
					.add(new ColumnValueBean("SEARCHED_PARTICIPANT_ID", participantId, 3));
			columnValueBeanList.add(new ColumnValueBean("NO_OF_MATCHED_PARTICIPANTS", Integer
					.valueOf(-1), 3));
			columnValueBeanList.add(new ColumnValueBean("USER_ID", userId, 3));
			columnValueBeanList.add(new ColumnValueBean("CREATION_DATE", date, 13));
			jdbcdao.executeUpdate(query, columnValueBeanList);
			jdbcdao.commit();
		}
		catch (DAOException e)
		{
			jdbcdao.rollback();
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}
		finally
		{
			jdbcdao.closeSession();
		}
	}

	/**
	 * Populate patient object.
	 *
	 * @param participant the participant
	 *
	 * @return the patient information
	 */
	public static PatientInformation populatePatientObject(IParticipant participant)
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
			ssn = (new StringBuilder()).append(ssn).append(ssnValue[1]).toString();
			ssn = (new StringBuilder()).append(ssn).append(ssnValue[2]).toString();
		}
		patientInformation.setSsn(ssn);
		if (participant.getBirthDate() != null)
		{
			patientInformation.setDob(participant.getBirthDate());
		}
		patientInformation.setGender(participant.getGender());
		Collection<String> participantInfoMedicalIdentifierCollection = new LinkedList<String>();
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
				IParticipantMedicalIdentifier participantMedicalIdentifier = (IParticipantMedicalIdentifier) itr
						.next();
				if (participantMedicalIdentifier.getMedicalRecordNumber() != null)
				{
					participantInfoMedicalIdentifierCollection.add(participantMedicalIdentifier
							.getMedicalRecordNumber());
					participantInfoMedicalIdentifierCollection.add(String
							.valueOf(((ISite) participantMedicalIdentifier.getSite()).getId()));
					//participantInfoMedicalIdentifierCollection.add(((ISite) participantMedicalIdentifier.getSite()).getName());
				}
			}
			while (true);
		}
		patientInformation
				.setParticipantMedicalIdentifierCollection(participantInfoMedicalIdentifierCollection);
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
				IRace race = (IRace) itr.next();
				if (race != null)
				{
					participantInfoRaceCollection.add(race.getRaceName());
				}
			}
			while (true);
		}
		patientInformation.setRaceCollection(participantInfoRaceCollection);
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
	public static boolean deleteProcessedParticipant(Long id) throws DAOException
	{
		boolean status;
		status = false;

		JDBCDAO jdbcdao = null;
		try
		{
			jdbcdao = getJDBCDAO();
			jdbcdao.executeUpdate((new StringBuilder()).append(
					"DELETE FROM MATCHED_PARTICIPANT_MAPPING WHERE SEARCHED_PARTICIPANT_ID='")
					.append(id).append("'").toString());
			jdbcdao.commit();
		}
		catch (DAOException e)
		{
			jdbcdao.rollback();
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}
		finally
		{
			jdbcdao.closeSession();
		}

		return status;
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
	public static IParticipant getOldParticipant(DAO dao, Long identifier) throws BizLogicException
	{
		IParticipant oldParticipant;
		try
		{
			oldParticipant = (IParticipant) dao.retrieveById(
					"edu.wustl.clinportal.domain.Participant", identifier);
		}
		catch (DAOException e)
		{
			logger.debug(e.getMessage(), e);
			throw new BizLogicException(e.getErrorKey(), e, e.getMsgValues());
		}
		return oldParticipant;
	}

}
