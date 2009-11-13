
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
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;

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
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.daofactory.DAOConfigFactory;
import edu.wustl.dao.daofactory.IDAOFactory;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.query.generator.ColumnValueBean;
import edu.wustl.patientLookUp.domain.PatientInformation;
import edu.wustl.patientLookUp.util.PropertyHandler;

public class ParticipantManagerUtility {

	private static Logger logger = Logger
			.getCommonLogger(ParticipantManagerUtility.class);

	public ParticipantManagerUtility() {
	}

	public static void registerWMQListener() throws JMSException {
		String hostName = null;
		String qmgName = null;
		String channel = null;
		String outBoundQueueName = null;
		int port = 0;
		try {
			hostName = XMLPropertyHandler.getValue("WMQServerName");
			qmgName = XMLPropertyHandler.getValue("WMQMGRName");
			channel = XMLPropertyHandler.getValue("WMQChannel");
			port = Integer.parseInt(XMLPropertyHandler.getValue("WMQPort"));
			MQQueueConnectionFactory factory = new MQQueueConnectionFactory();
			factory.setTransportType(1);
			factory.setQueueManager(qmgName);
			factory.setHostName(hostName);
			factory.setChannel(channel);
			factory.setPort(port);
			QueueConnection connection = factory.createQueueConnection();
			connection.start();
			QueueSession session = connection.createQueueSession(false, 1);
			outBoundQueueName = XMLPropertyHandler.getValue("OutBoundQueue");
			javax.jms.Queue outBoundQueue = session
					.createQueue((new StringBuilder()).append("queue:///")
							.append(outBoundQueueName).toString());
			QueueReceiver queueReceiver = session.createReceiver(outBoundQueue);
			EMPIParticipantListener listener = new EMPIParticipantListener();
			queueReceiver.setMessageListener(listener);
			javax.jms.Queue mrgMessageQueue = session
					.createQueue("CP.CLINPORTAL.MERGES");
			queueReceiver = session.createReceiver(mrgMessageQueue);
			EMPIParticipantMergeMessageListener mrgMesListener = new EMPIParticipantMergeMessageListener();
			queueReceiver.setMessageListener(mrgMesListener);
		} catch (JMSException e) {
			e.printStackTrace();
			throw new JMSException(e.getMessage());
		}
	}

	public static void initialiseParticiapntMatchScheduler() {
		ParticipantMatchingTimerTask timerTask = new ParticipantMatchingTimerTask();
		Timer scheduleTime = new Timer();
		String delay = "120000";
		scheduleTime.schedule(timerTask, 0x1d4c0L, Long.parseLong(delay));
	}

	public static IParticipantMedicalIdentifier getParticipantMedicalIdentifierObj(
			String mrn, String facilityId) throws BizLogicException {
		String application = applicationType();
		ISite site = null;
		String sourceObjectName = ISite.class.getName();
		String selectColumnNames[] = { "id", "name" };
		String whereColumnName[] = { "facilityId" };
		String whColCondn[] = { "=" };
		Object whereColumnValue[] = { facilityId };
		DefaultBizLogic siteBizLogic = new DefaultBizLogic();
		List siteObject = siteBizLogic.retrieve(sourceObjectName,
				selectColumnNames, whereColumnName, whColCondn,
				whereColumnValue, null);
		if (siteObject != null && siteObject.size() > 0) {
			Object siteList[] = (Object[]) (Object[]) siteObject.get(0);
			Long siteId = (Long) siteList[0];
			String siteName = (String) siteList[1];
			site = (ISite) getSiteInstance();
			site.setId(siteId);
			site.setName(siteName);
		}
		IParticipantMedicalIdentifier participantMedicalIdentifier = (IParticipantMedicalIdentifier) getPMIInstance();
		participantMedicalIdentifier.setMedicalRecordNumber(mrn);
		participantMedicalIdentifier.setSite(site);
		return participantMedicalIdentifier;
	}

	public static Object getObject(String bizLogicFactoryName)
			throws BizLogicException {
		try {
			Class className = Class.forName(bizLogicFactoryName);
			Object newobj = className.newInstance();
			return newobj;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new BizLogicException(null, null, "IllegalAccessException",
					"IllegalAccessException");
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new BizLogicException(null, null, "InstantiationException",
					"InstantiationException");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new BizLogicException(null, null, "ClassNotFoundException",
					"ClassNotFoundException");
		}

	}

	/**
	 * This function takes identifier as parameter and returns corresponding
	 * Participant
	 *
	 * @param identifier
	 * @return - Participant object
	 * @throws BizLogicException
	 * @throws Exception
	 */
	public static IParticipant getParticipantById(Long identifier)
			throws BizLogicException {
		// Initializing instance of IBizLogic
		IFactory factory = AbstractFactoryConfig.getInstance()
				.getBizLogicFactory();

		IBizLogic bizLogic = factory.getBizLogic(Constants.DEFAULT_BIZ_LOGIC);
		String sourceObjectName = IParticipant.class.getName();
		// getting all the participants from the database
		List participantList = bizLogic.retrieve(sourceObjectName, "id",
				identifier);
		return (IParticipant) participantList.get(0);

	}

	public static boolean isParticipantValidForEMPI(String LName, String FName,
			Date dob) {
		boolean isValid = true;
		if (LName == null || LName == "") {
			isValid = false;
		} else if (FName == null || FName == "") {
			isValid = false;
		} else if (dob == null) {
			isValid = false;
		}
		return isValid;
	}

	public static List getListOfMatchingParticipants(IParticipant participant,
			SessionDataBean sessionDataBean, String lookupAlgorithm)
			throws Exception {
		List matchParticipantList = null;
		LookupLogic partLookupLgic = null;
		if (lookupAlgorithm == null) {
			partLookupLgic = (LookupLogic) Utility.getObject(XMLPropertyHandler
					.getValue(Constants.PARTICIPANT_LOOKUP_ALGO));
		} else {
			partLookupLgic = (LookupLogic) Utility.getObject(XMLPropertyHandler
					.getValue(lookupAlgorithm));
		}
		DefaultLookupParameters params = new DefaultLookupParameters();
		params.setObject(participant);
		matchParticipantList = partLookupLgic.lookup(params);
		return matchParticipantList;
	}

	public static Object getRaceInstance() throws BizLogicException {
		String application = applicationType();
		Object raceInstance = null;
		if ("clinportal".equals(application)) {
			raceInstance = getObject("edu.wustl.clinportal.domain.Race");
		} else {
			raceInstance = getObject("edu.wustl.catissuecore.domain.Race");
		}
		return raceInstance;
	}

	public static Object getSiteInstance() throws BizLogicException {
		String application = applicationType();
		Object siteInstance = null;
		if ("clinportal".equals(application)) {
			siteInstance = getObject("edu.wustl.clinportal.domain.Site");
		} else {
			siteInstance = getObject("edu.wustl.catissuecore.domain.Site");
		}
		return siteInstance;
	}

	public static Object getParticipantInstance() throws BizLogicException {
		String application = applicationType();
		Object partiicpantInstance = null;
		if ("clinportal".equals(application)) {
			partiicpantInstance = getObject("edu.wustl.clinportal.domain.Participant");
		} else {
			partiicpantInstance = getObject("edu.wustl.catissuecore.domain.Participant");
		}
		return partiicpantInstance;
	}

	public static Object getPMIInstance() throws BizLogicException {
		String application = applicationType();
		Object PMIInstance = null;
		if ("clinportal".equals(application)) {
			PMIInstance = getObject("edu.wustl.clinportal.domain.ParticipantMedicalIdentifier");
		} else {
			PMIInstance = getObject("edu.wustl.catissuecore.domain.ParticipantMedicalIdentifier");
		}
		return PMIInstance;
	}

	private static String applicationType() throws BizLogicException {
		String application = null;
		try {
			application = PropertyHandler.getValue("application");
		} catch (Exception e) {
			logger.info(e.getStackTrace());
			throw new BizLogicException(null, e,
					"Error while get value from PatientInfoLookUpService.properties");
		}
		return application;
	}



	public static void setEMPIIdStatus(Long participantId, String status)
			throws DAOException {
		Exception exception;
		String appName = CommonServiceLocator.getInstance().getAppName();
		IDAOFactory daoFactory = DAOConfigFactory.getInstance().getDAOFactory(
				appName);
		JDBCDAO jdbcDao = null;
		try {
			jdbcDao = daoFactory.getJDBCDAO();
			jdbcDao.openSession(null);
			String sql = (new StringBuilder()).append(
					"UPDATE CATISSUE_PARTICIPANT SET EMPI_ID_STATUS='").append(
					status).append("' WHERE IDENTIFIER='")
					.append(participantId).append("'").toString();
			jdbcDao.executeUpdate(sql);
			jdbcDao.commit();
		} catch (DAOException e) {
			logger.info("ERROE WHILE UPDATING THE PARTICIPANT EMPI STATUS");
			throw new DAOException(e.getErrorKey(), e, e.getMsgValues());
		} finally {
			jdbcDao.closeSession();
		}
	}

	public static String getSSN(String ssn) {
		String ssnA = "";
		String ssnB = "";
		String ssnC = "";
		boolean result = false;
		Pattern pattern = Pattern.compile("[0-9]{3}-[0-9]{2}-[0-9]{4}", 2);
		Matcher mat = pattern.matcher(ssn);
		result = mat.matches();
		if (result) {
			return ssn;
		}
		if (ssn.length() >= 9) {
			ssnA = ssn.substring(0, 3);
			ssnB = ssn.substring(3, 5);
			ssnC = ssn.substring(5, 9);
		} else if (ssn.length() >= 4) {
			ssnC = ssn.substring(0, 3);
		} else {
			return ssn;
		}
		ssn = (new StringBuilder()).append(ssnA).append("-").append(ssnB)
				.append("-").append(ssnC).toString();
		return ssn;
	}

	public static List getColumnList(List columnList) throws DAOException {
		List displayList = new ArrayList();
		String appName = CommonServiceLocator.getInstance().getAppName();
		IDAOFactory daoFactory = DAOConfigFactory.getInstance().getDAOFactory(
				appName);
		JDBCDAO jdbcDao = daoFactory.getJDBCDAO();
		jdbcDao.openSession(null);
		String sql = "SELECT  columnData.COLUMN_NAME,displayData.DISPLAY_NAME FROM CATISSUE_INTERFACE_"
				+ "COLUMN_DATA columnData,CATISSUE_TABLE_RELATION relationData,CATISSUE_QUERY_TABLE"
				+ "_DATA tableData,CATISSUE_SEARCH_DISPLAY_DATA displayData where relationData.CHIL"
				+ "D_TABLE_ID = columnData.TABLE_ID and relationData.PARENT_TABLE_ID = tableData.TA"
				+ "BLE_ID and relationData.RELATIONSHIP_ID = displayData.RELATIONSHIP_ID and column"
				+ "Data.IDENTIFIER = displayData.COL_ID and tableData.ALIAS_NAME = 'Participant'";
		logger.debug((new StringBuilder()).append("DATA ELEMENT SQL : ")
				.append(sql).toString());
		List list = jdbcDao.executeQuery(sql);
		for (Iterator iterator1 = columnList.iterator(); iterator1.hasNext();) {
			String colName1 = (String) iterator1.next();
			Logger.out.debug((new StringBuilder()).append(
					"colName1------------------------").append(colName1)
					.toString());
			Iterator iterator2 = list.iterator();
			while (iterator2.hasNext()) {
				List rowList = (List) iterator2.next();
				String colName2 = (String) rowList.get(0);
				Logger.out.debug((new StringBuilder()).append(
						"colName2------------------------").append(colName2)
						.toString());
				if (colName1.equals(colName2)) {
					displayList.add((String) rowList.get(1));
				}
			}
		}

		jdbcDao.closeSession();
		return displayList;
	}

	public static boolean csEMPIStatus(long participantId) throws DAOException {
		boolean status;
		IDAOFactory daoFactory;
		JDBCDAO dao;
		status = false;
		String appName = CommonServiceLocator.getInstance().getAppName();
		daoFactory = DAOConfigFactory.getInstance().getDAOFactory(appName);
		dao = null;
		dao = daoFactory.getJDBCDAO();
		dao.openSession(null);
		String query = " SELECT SP.IS_EMPI_ENABLE FROM  CATISSUE_SPECIMEN_PROTOCOL SP JOIN  CATISSUE_CLINICAL_STUDY_REG CSR  "
				+ " ON SP.IDENTIFIER = CSR.CLINICAL_STUDY_ID  WHERE CSR.PARTICIPANT_id='"
				+ participantId + "'";
		List statusList = dao.executeQuery(query);
		if (!statusList.isEmpty() && statusList.get(0) != "") {
			List idList = (List) statusList.get(0);
			if (!idList.isEmpty() && ((String) idList.get(0)).equals("1")) {
				status = true;
			}
		}
		dao.closeSession();
		return status;
	}

	public static void updatePartiEMPIId(long participantId, String lastName,
			String firstName, Date dob) throws ParseException, DAOException {
		if (isParticipantValidForEMPI(lastName, firstName, dob)) {
			String appName = CommonServiceLocator.getInstance().getAppName();
			IDAOFactory daoFactory = DAOConfigFactory.getInstance()
					.getDAOFactory(appName);
			JDBCDAO dao = null;
			try {
				dao = daoFactory.getJDBCDAO();
				dao.openSession(null);
				String query = (new StringBuilder())
						.append(
								"UPDATE CATISSUE_PARTICIPANT SET EMPI_ID_STATUS='PENDING' WHERE IDENTIFIER='")
						.append(participantId).append("'").toString();
				dao.executeUpdate(query);
				dao.commit();
				dao.closeSession();
			} catch (DAOException exp) {
				logger.info("ERROR WHILE UPDATING THE EMPI STATUS");
				throw new DAOException(exp.getErrorKey(), exp, exp
						.getMsgValues());
			}
		}
	}

	public static String getPartiEMPIStatus(long participantId)
			throws DAOException {
		String appName = CommonServiceLocator.getInstance().getAppName();
		IDAOFactory daoFactory = DAOConfigFactory.getInstance().getDAOFactory(
				appName);
		JDBCDAO dao = null;
		String eMPIStatus = "";
		try {
			dao = daoFactory.getJDBCDAO();
			dao.openSession(null);
			String query = (new StringBuilder())
					.append(
							"SELECT EMPI_ID_STATUS FROM CATISSUE_PARTICIPANT  WHERE IDENTIFIER='")
					.append(participantId).append("'").toString();
			List list = dao.executeQuery(query);
			if (!list.isEmpty() && list.get(0) != "") {
				List statusList = (List) list.get(0);
				if (!statusList.isEmpty()) {
					eMPIStatus = (String) statusList.get(0);
				}
			}
			dao.commit();
			dao.closeSession();
		} catch (DAOException exp) {
			logger.info("ERROR WHILE GETTING THE EMPI STATUS");
			throw new DAOException(exp.getErrorKey(), exp, exp.getMsgValues());
		}
		return eMPIStatus;
	}

	public static boolean isCallToLookupLogicNeeded(IParticipant participant) {
		boolean flag = true;
		if ((participant.getFirstName() == null || participant.getFirstName()
				.length() == 0)
				&& (participant.getMiddleName() == null || participant
						.getMiddleName().length() == 0)
				&& (participant.getLastName() == null || participant
						.getLastName().length() == 0)
				&& (participant.getSocialSecurityNumber() == null || participant
						.getSocialSecurityNumber().length() == 0)
				&& participant.getBirthDate() == null
				&& (participant.getParticipantMedicalIdentifierCollection() == null || participant
						.getParticipantMedicalIdentifierCollection().size() == 0)) {
			flag = false;
		}
		return flag;
	}

	public static List getColumnHeadingList() throws DAOException {
		String columnHeaderList[] = { "LAST_NAME", "FIRST_NAME", "MIDDLE_NAME",
				"BIRTH_DATE", "DEATH_DATE", "VITAL_STATUS", "GENDER",
				"SOCIAL_SECURITY_NUMBER", "MEDICAL_RECORD_NUMBER" };
		List columnList = Arrays.asList(columnHeaderList);
		Logger.out.info((new StringBuilder()).append("column List size ;")
				.append(columnList.size()).toString());
		List displayList = new ArrayList();
		displayList.add("EMPI");
		displayList.add("EMPIID");
		List displayListTemp = getColumnList(columnList);
		displayList.addAll(displayListTemp);
		return displayList;
	}

	public static List getParticipantDisplayList(List participantList) {
		List pcpantDisplaylst = new ArrayList();
		Iterator itr = participantList.iterator();
		String medicalRecordNo = "";
		List participantInfo;
		for (; itr.hasNext(); pcpantDisplaylst.add(participantInfo)) {
			DefaultLookupResult result = (DefaultLookupResult) itr.next();
			IParticipant participant = (IParticipant) result.getObject();
			participantInfo = new ArrayList();
			participantInfo.add(participant.getIsFromEMPI());
			participantInfo.add(participant.getEmpiId());
			participantInfo.add(Utility.toString(participant.getLastName()));
			participantInfo.add(Utility.toString(participant.getFirstName()));
			participantInfo.add(Utility.toString(participant.getMiddleName()));
			participantInfo.add(Utility.toString(participant.getBirthDate()));
			participantInfo.add(Utility.toString(participant.getDeathDate()));
			participantInfo.add(Utility.toString(participant.getVitalStatus()));
			participantInfo.add(Utility.toString(participant.getGender()));
			participantInfo.add(Utility.toString(participant
					.getSocialSecurityNumber()));
			if (participant.getParticipantMedicalIdentifierCollection() != null) {
				medicalRecordNo = getParticipantMrnDisplay(participant);
			}
			participantInfo.add(Utility.toString(medicalRecordNo));
			participantInfo.add(participant.getId());
		}

		return pcpantDisplaylst;
	}

	private static String getParticipantMrnDisplay(IParticipant participant) {
		StringBuffer mrn = new StringBuffer();
		if (participant.getParticipantMedicalIdentifierCollection() != null) {
			Iterator pmiItr = participant
					.getParticipantMedicalIdentifierCollection().iterator();
			do {
				if (!pmiItr.hasNext()) {
					break;
				}
				IParticipantMedicalIdentifier participantMedicalIdentifier = (IParticipantMedicalIdentifier) pmiItr
						.next();
				if (participantMedicalIdentifier.getSite() != null
						&& ((ISite) participantMedicalIdentifier.getSite())
								.getId() != null) {
					String siteName = ((ISite) participantMedicalIdentifier
							.getSite()).getName();
					mrn.append(participantMedicalIdentifier
							.getMedicalRecordNumber());
					mrn.append(':');
					mrn.append(siteName);
					mrn.append("\n<br>");
				}
			} while (true);
		}
		return mrn.toString();
	}

	public static void addParticipantToProcessMessageQueue(Long userId,
			Long participantId) throws DAOException {
		String appName = CommonServiceLocator.getInstance().getAppName();
		IDAOFactory daoFactory = DAOConfigFactory.getInstance().getDAOFactory(
				appName);
		JDBCDAO jdbcdao = null;
		String query = null;
		try {
			jdbcdao = daoFactory.getJDBCDAO();
			jdbcdao.openSession(null);
			List idList = jdbcdao
					.executeQuery((new StringBuilder())
							.append(
									"SELECT SEARCHED_PARTICIPANT_ID FROM MATCHED_PARTICIPANT_MAPPING WHERE SEARCHED_P"
											+ "ARTICIPANT_ID='").append(
									participantId).append("'").toString());
			if (!idList.isEmpty() && idList.get(0) != "") {
				query = (new StringBuilder())
						.append(
								"UPDATE MATCHED_PARTICIPANT_MAPPING SET SEARCHED_PARTICIPANT_ID=?,NO_OF_MATCHED_P"
										+ "ARTICIPANTS=?,USER_ID=?,CREATION_DATE=? WHERE SEARCHED_PARTICIPANT_ID ='")
						.append(participantId).append("'").toString();
			} else {
				query = "INSERT INTO MATCHED_PARTICIPANT_MAPPING VALUES(?,?,?,?)";
			}
			Calendar cal = Calendar.getInstance();
			Date date = cal.getTime();
			LinkedList columnValueBeanList = new LinkedList();
			columnValueBeanList.add(new ColumnValueBean(
					"SEARCHED_PARTICIPANT_ID", participantId, 3));
			columnValueBeanList.add(new ColumnValueBean(
					"NO_OF_MATCHED_PARTICIPANTS", Integer.valueOf(-1), 3));
			columnValueBeanList.add(new ColumnValueBean("USER_ID", userId, 3));
			columnValueBeanList.add(new ColumnValueBean("CREATION_DATE", date,
					13));
			jdbcdao.executeUpdate(query, columnValueBeanList);
			jdbcdao.commit();
			jdbcdao.closeSession();
		} catch (DAOException e) {
			jdbcdao.rollback();
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}
	}

	public static PatientInformation populatePatientObject(
			IParticipant participant) {
		PatientInformation patientInformation = new PatientInformation();
		patientInformation.setLastName(participant.getLastName());
		patientInformation.setFirstName(participant.getFirstName());
		patientInformation.setMiddleName(participant.getMiddleName());
		String ssn = participant.getSocialSecurityNumber();
		if (ssn != null) {
			String ssnValue[] = ssn.split("-");
			ssn = ssnValue[0];
			ssn = (new StringBuilder()).append(ssn).append(ssnValue[1])
					.toString();
			ssn = (new StringBuilder()).append(ssn).append(ssnValue[2])
					.toString();
		}
		patientInformation.setSsn(ssn);
		if (participant.getBirthDate() != null) {
			patientInformation.setDob(participant.getBirthDate());
		}
		patientInformation.setGender(participant.getGender());
		Collection participantInfoMedicalIdentifierCollection = new LinkedList();
		Collection participantMedicalIdentifierCollection = participant
				.getParticipantMedicalIdentifierCollection();
		if (participantMedicalIdentifierCollection != null) {
			Iterator itr = participantMedicalIdentifierCollection.iterator();
			do {
				if (!itr.hasNext()) {
					break;
				}
				IParticipantMedicalIdentifier participantMedicalIdentifier = (IParticipantMedicalIdentifier) itr
						.next();
				if (participantMedicalIdentifier.getMedicalRecordNumber() != null) {
					participantInfoMedicalIdentifierCollection
							.add(participantMedicalIdentifier
									.getMedicalRecordNumber());
					participantInfoMedicalIdentifierCollection.add(String
							.valueOf(((ISite) participantMedicalIdentifier
									.getSite()).getId()));
					participantInfoMedicalIdentifierCollection
							.add(((ISite) participantMedicalIdentifier
									.getSite()).getName());
				}
			} while (true);
		}
		patientInformation
				.setParticipantMedicalIdentifierCollection(participantInfoMedicalIdentifierCollection);
		Collection participantInfoRaceCollection = new HashSet();
		Collection participantRaceCollection = participant.getRaceCollection();
		if (participantRaceCollection != null) {
			Iterator itr = participantRaceCollection.iterator();
			do {
				if (!itr.hasNext()) {
					break;
				}
				IRace race = (IRace) itr.next();
				if (race != null) {
					participantInfoRaceCollection.add(race.getRaceName());
				}
			} while (true);
		}
		patientInformation.setRaceCollection(participantInfoRaceCollection);
		return patientInformation;
	}

	public static boolean deleteProcessedParticipant(Long id)
			throws DAOException {
		boolean status;
		Exception exception;
		status = false;
		String appName = CommonServiceLocator.getInstance().getAppName();
		IDAOFactory daoFactory = DAOConfigFactory.getInstance().getDAOFactory(
				appName);
		JDBCDAO jdbcdao = null;
		try {
			jdbcdao = daoFactory.getJDBCDAO();
			jdbcdao.openSession(null);
			jdbcdao
					.executeUpdate((new StringBuilder())
							.append(
									"DELETE FROM MATCHED_PARTICIPANT_MAPPING WHERE SEARCHED_PARTICIPANT_ID='")
							.append(id).append("'").toString());
			jdbcdao.commit();
		} catch (DAOException e) {
			jdbcdao.rollback();
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		} finally {
			jdbcdao.closeSession();
		}
		jdbcdao.closeSession();
		return status;
	}

}
