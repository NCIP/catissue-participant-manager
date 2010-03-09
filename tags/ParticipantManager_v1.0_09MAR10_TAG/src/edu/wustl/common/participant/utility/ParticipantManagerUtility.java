
package edu.wustl.common.participant.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import edu.wustl.dao.query.generator.DBTypes;
import edu.wustl.patientLookUp.domain.PatientInformation;
import edu.wustl.patientLookUp.util.PropertyHandler;

// TODO: Auto-generated Javadoc
/**
 * The Class ParticipantManagerUtility.
 */
public class ParticipantManagerUtility
{

	/** The logger. */
	private static final Logger LOGGER = Logger.getCommonLogger(ParticipantManagerUtility.class);

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

			final QueueConnection connection = factory.createQueueConnection();

			connection.start();

			final QueueSession session = connection.createQueueSession(false,
					javax.jms.Session.AUTO_ACKNOWLEDGE);
			inBoundQueueName = XMLPropertyHandler.getValue(Constants.IN_BOUND_QUEUE_NAME);
			final Queue inBoundQueue = session.createQueue("queue:///" + inBoundQueueName);

			LOGGER.info("IN_BOUND_QUEUE_NAME ----------- : " + inBoundQueueName);

			QueueReceiver queueReceiver = session.createReceiver(inBoundQueue);

			final EMPIParticipantListener listener = new EMPIParticipantListener();

			queueReceiver.setMessageListener(listener);

			// Set the merge message queue listener.
			mergeMessageQueueName = XMLPropertyHandler.getValue(Constants.MERGE_MESSAGE_QUEUE);
			final Queue mrgMessageQueue = session.createQueue("queue:///" + mergeMessageQueueName);
			queueReceiver = session.createReceiver(mrgMessageQueue);
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
	 * Initialise particiapnt match scheduler.
	 */
	public static void initialiseParticiapntMatchScheduler()
	{
		final ParticipantMatchingTimerTask timerTask = new ParticipantMatchingTimerTask();
		final Timer scheduleTime = new Timer();
		final String delay = XMLPropertyHandler.getValue(Constants.PARTICIPANT_MATCHING_SCHEDULAR_DELAY);
		scheduleTime.schedule(timerTask, 0x1d4c0L, Long.parseLong(delay));
	}

	/**
	 * Gets the participant medical identifier obj.
	 *
	 * @param mrn the mrn
	 * @param facilityId the facility id
	 *
	 * @return the participant medical identifier obj
	 * @throws BizLogicException
	 *
	 * @throws Exception the exception
	 */
	public static IParticipantMedicalIdentifier<IParticipant, ISite> getParticipantMedicalIdentifierObj(
			final String mrn, final String facilityId) throws BizLogicException
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
	 * Gets the site object.
	 *
	 * @param facilityId the facility id
	 *
	 * @return the site object
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws Exception the exception
	 */
	public static ISite getSiteObject(final String facilityId) throws BizLogicException
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
			Object siteList[] = (Object[]) siteObject.get(0);
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
	public static boolean isParticipantValidForEMPI(final String LName,final  String FName, final Date dob,
			final String ssn, final String mrn)
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
	 * @param sessionDataBean the session data bean
	 * @param lookupAlgorithm the lookup algorithm
	 * @param linkedCSCPId the linked cscp id
	 *
	 * @return the list of matching participants
	 *
	 * @throws Exception the exception
	 */
	public static List<DefaultLookupResult> getListOfMatchingParticipants(IParticipant participant,
			SessionDataBean sessionDataBean, String lookupAlgorithm, Long linkedCSCPId)
			throws Exception
	{
		List<DefaultLookupResult> matchParticipantList = null;
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
		String application = applicationType();
		//Remove this check, once this feature integrated with caTissueSuite
		if (!Constants.CLINPORTAL_APPLICATION_NAME.equals(application))
		{
			linkedCSCPId = null;
		}

		if (linkedCSCPId != null)
		{
			matchParticipantList = processListForMatchWithinCS(matchParticipantList, linkedCSCPId);
		}
		return matchParticipantList;
	}

	/**
	 * Process list for match within cs.
	 *
	 * @param matchPartpantLst the match partpant lst
	 * @param csId the cs id
	 *
	 * @return the list
	 *
	 * @throws DAOException the DAO exception
	 */
	public static List processListForMatchWithinCS(List<DefaultLookupResult> matchPartpantLst,
			Long csId) throws DAOException
	{
		if (ParticipantManagerUtility.isParticipantMatchWithinCSCPEnable(csId))
		{
			List idList = ParticipantManagerUtility.getPartcipantIdsList(csId);
			matchPartpantLst = filerMatchedList(matchPartpantLst, idList);
		}
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
		JDBCDAO dao = null;
		String query = null;
		try
		{
			query = "SELECT SP.PARTCIPNT_MATCH_WITHIN_CSCP FROM  CATISSUE_SPECIMEN_PROTOCOL SP WHERE SP.IDENTIFIER=?";
			dao = getJDBCDAO();
			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
			columnValueBeanList.add(new ColumnValueBean("IDENTIFIER", id, DBTypes.LONG));
			List list = dao.executeQuery(query, null, columnValueBeanList);
			if (!list.isEmpty() && !"".equals(list.get(0)))
			{
				List statusList = (List) list.get(0);
				if (!statusList.isEmpty() && ((String) statusList.get(0)).equals("1"))
				{
					status = true;
				}
			}
		}
		catch (DAOException exp)
		{
			LOGGER.info("ERROR WHILE GETTING THE EMPI STATUS");
			throw new DAOException(exp.getErrorKey(), exp, exp.getMsgValues());
		}
		finally
		{
			dao.closeSession();
		}
		return status;
	}

	/**
	 * Gets the partcipant ids list.
	 *
	 * @param id the id
	 *
	 * @return the partcipant ids list
	 *
	 * @throws DAOException the DAO exception
	 */
	public static List getPartcipantIdsList(Long id) throws DAOException
	{
		List idListArray = null;
		List<String> idList = new ArrayList<String>();
		JDBCDAO dao = null;
		String query = null;
		try
		{
			query = "SELECT PARTICIPANT_ID FROM CATISSUE_CLINICAL_STUDY_REG WHERE CLINICAL_STUDY_ID=?";
			dao = getJDBCDAO();

			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
			columnValueBeanList.add(new ColumnValueBean("CLINICAL_STUDY_ID", id, DBTypes.LONG));

			idListArray = dao.executeQuery(query, null, columnValueBeanList);
			if (!idListArray.isEmpty() && !"".equals(idListArray.get(0)))
			{
				for (Iterator<List> itr = idListArray.iterator(); itr.hasNext();)
				{
					idList.add(String.valueOf(itr.next().get(0)));
				}
			}
		}
		catch (DAOException exp)
		{

			throw new DAOException(exp.getErrorKey(), exp, exp.getMsgValues());
		}
		finally
		{
			dao.closeSession();
		}
		return idList;
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
	public static IParticipantMedicalIdentifier<IParticipant, ISite> getPMIInstance()
			throws BizLogicException
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
		return (IParticipantMedicalIdentifier<IParticipant, ISite>) PMIInstance;
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
			LOGGER.info(e.getStackTrace());
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
			LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
			columnValueBeanList.add(new ColumnValueBean(status));
			columnValueBeanList.add(new ColumnValueBean(participantId));
			columnValueBeans.add(columnValueBeanList);
			String sql = "UPDATE CATISSUE_PARTICIPANT SET EMPI_ID_STATUS=? WHERE IDENTIFIER=?";
			jdbcDao.executeUpdate(sql, columnValueBeans);
			jdbcDao.commit();
		}
		catch (DAOException e)
		{
			LOGGER.info("ERROE WHILE UPDATING THE PARTICIPANT EMPI STATUS");
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
			LOGGER.debug("DATA ELEMENT SQL : " + sql);
			List list = jdbcDao.executeQuery(sql, null, null);
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
			LOGGER.info(e.getMessage());
			throw new DAOException(null, e,
					"Error while get value from PatientInfoLookUpService.properties");
		}

		dao = getJDBCDAO();

		try
		{
			if (Constants.CLINPORTAL_APPLICATION_NAME.equals(application))
			{
				query = " SELECT SP.IS_EMPI_ENABLE FROM  CATISSUE_SPECIMEN_PROTOCOL SP JOIN  CATISSUE_CLINICAL_STUDY_REG CSR  "
						+ " ON SP.IDENTIFIER = CSR.CLINICAL_STUDY_ID  WHERE CSR.PARTICIPANT_Id=?";
			}
			else
			{
				query = " SELECT SP.IS_EMPI_ENABLE FROM  CATISSUE_SPECIMEN_PROTOCOL SP JOIN  CATISSUE_CLINICAL_STUDY_REG CSR  "
						+ " ON SP.IDENTIFIER = CSR.CLINICAL_STUDY_ID  WHERE CSR.PARTICIPANT_id=?";
			}

			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
			columnValueBeanList.add(new ColumnValueBean("PARTICIPANT_Id", participantId,
					DBTypes.LONG));
			List statusList = dao.executeQuery(query, null, columnValueBeanList);
			if (!statusList.isEmpty() && !"".equals(statusList.get(0)))
			{
				List idList = (List) statusList.get(0);
				if (!idList.isEmpty() && !"".equals(((String) idList.get(0))))
				{
					for (int i = 0; i < idList.size(); i++)
					{
						if (((String) idList.get(0)).equals("1"))
						{
							status = true;
							break;
						}
					}
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

	/*
		public static void updatePartiEMPIId(long participantId, String lastName, String firstName,
				Date dob) throws ParseException, DAOException
		{
			if (isParticipantValidForEMPI(lastName, firstName, dob))
			{
				JDBCDAO dao = null;
				try
				{
					dao = getJDBCDAO();
					LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
					LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
					columnValueBeanList.add(new ColumnValueBean(participantId));
					columnValueBeans.add(columnValueBeanList);
					String query = (new StringBuilder())
							.append(
									"UPDATE CATISSUE_PARTICIPANT SET EMPI_ID_STATUS='PENDING' WHERE IDENTIFIER=?")
							.toString();
					dao.executeUpdate(query, columnValueBeans);
					dao.commit();

				}
				catch (DAOException exp)
				{
					LOGGER.info("ERROR WHILE UPDATING THE EMPI STATUS");
					throw new DAOException(exp.getErrorKey(), exp, exp.getMsgValues());
				}
				finally
				{
					dao.closeSession();
				}

			}
		}
	*/

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
			String query = "SELECT EMPI_ID_STATUS FROM CATISSUE_PARTICIPANT  WHERE IDENTIFIER=?";

			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
			columnValueBeanList.add(new ColumnValueBean("IDENTIFIER", participantId, DBTypes.LONG));
			List list = dao.executeQuery(query, null, columnValueBeanList);
			if (!list.isEmpty() && !"".equals(list.get(0)))
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
			LOGGER.info("ERROR WHILE GETTING THE EMPI STATUS");
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
	 * @param participantId the participant id
	 * @param userIdSet the user id set
	 *
	 * @throws DAOException the DAO exception
	 */
	public static void addParticipantToProcessMessageQueue(LinkedHashSet<Long> userIdSet,
			Long participantId) throws DAOException
	{

		JDBCDAO jdbcdao = null;
		String query = null;
		try
		{
			jdbcdao = getJDBCDAO();
			query = "SELECT SEARCHED_PARTICIPANT_ID FROM MATCHED_PARTICIPANT_MAPPING WHERE SEARCHED_PARTICIPANT_ID=?";
			LinkedList<ColumnValueBean> colValueBeanList = new LinkedList<ColumnValueBean>();
			colValueBeanList.add(new ColumnValueBean("SEARCHED_PARTICIPANT_ID", participantId,
					DBTypes.LONG));
			List idList = jdbcdao.executeQuery(query, null, colValueBeanList);

			Calendar cal = Calendar.getInstance();
			Date date = cal.getTime();
			LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();

			if (!idList.isEmpty() && !"".equals(idList.get(0)))
			{
				query = "UPDATE MATCHED_PARTICIPANT_MAPPING SET SEARCHED_PARTICIPANT_ID=?,NO_OF_MATCHED_PARTICIPANTS=?,CREATION_DATE=? WHERE SEARCHED_PARTICIPANT_ID =?";
				columnValueBeanList.add(new ColumnValueBean("SEARCHED_PARTICIPANT_ID",
						participantId, DBTypes.LONG));
			}
			else
			{
				query = "INSERT INTO MATCHED_PARTICIPANT_MAPPING(NO_OF_MATCHED_PARTICIPANTS,CREATION_DATE,SEARCHED_PARTICIPANT_ID) VALUES(?,?,?)";
			}


			columnValueBeanList.add(new ColumnValueBean("NO_OF_MATCHED_PARTICIPANTS", Integer
					.valueOf(-1), DBTypes.LONG));
			columnValueBeanList.add(new ColumnValueBean("CREATION_DATE", date, DBTypes.DATE));
			columnValueBeanList.add(new ColumnValueBean("SEARCHED_PARTICIPANT_ID", participantId,
					DBTypes.LONG));
			columnValueBeans.add(columnValueBeanList);
			jdbcdao.executeUpdate(query, columnValueBeans);
			jdbcdao.commit();

			updateParticipantUserMapping(jdbcdao, userIdSet, participantId);
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
	 * Update participant user mapping.
	 *
	 * @param jdbcdao the jdbcdao
	 * @param userIdSet the user id set
	 * @param participantId the participant id
	 *
	 * @throws DAOException the DAO exception
	 */
	private static void updateParticipantUserMapping(JDBCDAO jdbcdao,
			LinkedHashSet<Long> userIdSet, Long participantId) throws DAOException
	{

		Iterator iterator = userIdSet.iterator();
		while (iterator.hasNext())
		{
			String query = "INSERT INTO EMPI_PARTICIPANT_USER_MAPPING VALUES(?,?)";
			Long userId = (Long) iterator.next();
			LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();

			columnValueBeanList.add(new ColumnValueBean("PARTICIPANT_ID", participantId,
					DBTypes.LONG));
			columnValueBeanList.add(new ColumnValueBean("USER_ID", userId, DBTypes.LONG));
			columnValueBeans.add(columnValueBeanList);
			jdbcdao.executeUpdate(query, columnValueBeans);
			jdbcdao.commit();
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
			ssn = ssn + ssnValue[1];
			ssn = ssn + ssnValue[2];
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
				IParticipantMedicalIdentifier<IParticipant, ISite> participantMedicalIdentifier = (IParticipantMedicalIdentifier<IParticipant, ISite>) itr
						.next();
				if (participantMedicalIdentifier.getMedicalRecordNumber() != null)
				{
					participantInfoMedicalIdentifierCollection.add(participantMedicalIdentifier
							.getMedicalRecordNumber());
					participantInfoMedicalIdentifierCollection.add(String
							.valueOf((participantMedicalIdentifier.getSite()).getId()));
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
				IRace<IParticipant> race = (IRace<IParticipant>) itr.next();
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
	public static void deleteProcessedParticipant(Long id) throws DAOException
	{

		JDBCDAO jdbcdao = null;
		try
		{
			jdbcdao = getJDBCDAO();
			LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
			columnValueBeanList.add(new ColumnValueBean(id));
			columnValueBeans.add(columnValueBeanList);
			String query = "DELETE FROM MATCHED_PARTICIPANT_MAPPING WHERE SEARCHED_PARTICIPANT_ID=?";
			jdbcdao.executeUpdate(query, columnValueBeans);
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
			LOGGER.debug(e.getMessage(), e);
			throw new BizLogicException(e.getErrorKey(), e, e.getMsgValues());
		}
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

		boolean status = false;
		JDBCDAO dao = null;
		String query = null;
		try
		{
			//query = "SELECT * FROM MATCHED_PARTICIPANT_MAPPING WHERE SEARCHED_PARTICIPANT_ID=? AND NO_OF_MATCHED_PARTICIPANTS!=?";
			query = "SELECT * FROM MATCHED_PARTICIPANT_MAPPING WHERE SEARCHED_PARTICIPANT_ID=?";
			dao = getJDBCDAO();
			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
			columnValueBeanList
					.add(new ColumnValueBean("SEARCHED_PARTICIPANT_ID", id, DBTypes.LONG));
			//columnValueBeanList.add(new ColumnValueBean("NO_OF_MATCHED_PARTICIPANTS", 0,DBTypes.INTEGER));
			List list = dao.executeQuery(query, null, columnValueBeanList);
			if (!list.isEmpty() && !"".equals(list.get(0)))
			{
				status = true;
			}
		}
		catch (DAOException exp)
		{
			LOGGER.info("ERROR WHILE GETTING THE EMPI STATUS");
			throw new DAOException(exp.getErrorKey(), exp, exp.getMsgValues());
		}
		finally
		{
			dao.closeSession();
		}
		return status;
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
		JDBCDAO dao = null;
		List<Long> particpantIdColl = new ArrayList<Long>();
		try
		{

			dao = ParticipantManagerUtility.getJDBCDAO();

			String query = "SELECT SEARCHED_PARTICIPANT_ID FROM  MATCHED_PARTICIPANT_MAPPING PARTIMAPPING  "
					+ " JOIN EMPI_PARTICIPANT_USER_MAPPING ON PARTIMAPPING.SEARCHED_PARTICIPANT_ID=EMPI_PARTICIPANT_USER_MAPPING.PARTICIPANT_ID"
					+ " WHERE EMPI_PARTICIPANT_USER_MAPPING.USER_ID=? AND PARTIMAPPING.NO_OF_MATCHED_PARTICIPANTS!=?";

			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
			columnValueBeanList.add(new ColumnValueBean("USER_ID", userId, DBTypes.LONG));
			columnValueBeanList.add(new ColumnValueBean("NO_OF_MATCHED_PARTICIPANTS", "-1",
					DBTypes.INTEGER));

			List resultSet = dao.executeQuery(query, null, columnValueBeanList);

			for (Object object : resultSet)
			{
				ArrayList particpantIdList = (ArrayList) object;
				if (particpantIdList != null && !particpantIdList.isEmpty())
				{
					particpantIdColl.add(Long.valueOf(particpantIdList.get(0).toString()));
				}
			}
		}
		finally
		{
			dao.closeSession();
		}
		return particpantIdColl;
	}

	/**
	 *
	 * @param name
	 * @return
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

}
