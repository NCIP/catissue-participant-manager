package edu.wustl.common.participant.bizlogic;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.participant.client.IParticipantManager;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IRace;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.MQMessageWriter;
import edu.wustl.common.participant.utility.ParticipantManagerException;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.participant.utility.RaceGenderCodesProperyHandler;
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

/**
 * The Class EMPIParticipantRegistrationBizLogic.
 *
 * @author geeta_jaggal. The Class EMPIParticipantRegistrationBizLogic.
 */
public class EMPIParticipantRegistrationBizLogic {

	/** The logger. */
	private static final Logger LOGGER = Logger
			.getCommonLogger(EMPIParticipantRegistrationBizLogic.class);

	/** The blank literal. */
	private final transient String blankLiteral;

	/** The temp mrn id. */
	protected String tempMrnId;

	/** The msg control id. */
	protected String msgControlId;

	/** The date time. */
	protected String dateTime;

	/**
	 * Instantiates a new eMPI participant registration biz logic.
	 */
	public EMPIParticipantRegistrationBizLogic() {
		blankLiteral = "\"\"";
		tempMrnId = null;
	}

	/**
	 * Gets the temp mrn id.
	 *
	 * @return the temp mrn id
	 */
	public String getTempMrnId() {
		return tempMrnId;
	}

	/**
	 * Sets the temp mrn id.
	 *
	 * @param tempMrnId
	 *            the new temp mrn id
	 */
	public void setTempMrnId(final String tempMrnId) {
		this.tempMrnId = tempMrnId;
	}

	/**
	 * Gets the msg control id.
	 *
	 * @return the msg control id
	 */
	public String getMsgControlId() {
		return msgControlId;
	}

	/**
	 * Sets the msg control id.
	 *
	 * @param msgControlId
	 *            the new msg control id
	 */
	public void setMsgControlId(final String msgControlId) {
		this.msgControlId = msgControlId;
	}

	/**
	 * Gets the date time.
	 *
	 * @return the date time
	 */
	public String getDateTime() {
		return dateTime;
	}

	/**
	 * Sets the date time.
	 *
	 * @param dateTime
	 *            the new date time
	 */
	public void setDateTime(final String dateTime) {
		this.dateTime = dateTime;
	}

	/**
	 * Register patient to empi.
	 *
	 * @param participant
	 *            the participant
	 *
	 * @throws Exception
	 *             the exception
	 * @throws ApplicationException
	 *             the application exception
	 */
	public void registerPatientToeMPI(final IParticipant participant) throws ApplicationException
	{
		JDBCDAO jdbcdao = null;

		try
		{
			jdbcdao = getJDBCDAO();

			//update status to 'CREATED' before sending HL7 message, because user selected a record and resolved match, so new empiId was created for user
			/*queryForStatusUpdate = "UPDATE CATISSUE_PARTICIPANT SET EMPI_ID_STATUS = 'CREATED' WHERE IDENTIFIER = "
				+ participant.getId();
			jdbcdao.executeUpdate(queryForStatusUpdate);
			jdbcdao.commit();*/

			//for update flow,HL7 message needs to be sent with temporary participant Id,
			// hence queries the temp Participant Id from PARTICIPANT_EMPI_ID_MAPPING table
			String query = "SELECT * FROM PARTICIPANT_EMPI_ID_MAPPING WHERE PERMANENT_PARTICIPANT_ID=?";
			LinkedList<ColumnValueBean> colValueBeanList = new LinkedList<ColumnValueBean>();
			colValueBeanList.add(new ColumnValueBean("PERMANENT_PARTICIPANT_ID", participant
					.getId(), DBTypes.LONG));
			List<Object> idList = jdbcdao.executeQuery(query, null, colValueBeanList);
			if (null != idList && idList.size() > 0)
			{
				String temporaryParticipantId = "";
				if (null != idList.get(0))
				{
					Object obj = idList.get(0);
					temporaryParticipantId = ((ArrayList) obj).get(1).toString();
				}
				//send HL7 message with temp participant Id queried
				this.setTempMrnId(temporaryParticipantId);
			}

			// for new case, send HL7 directly
			String hl7Message = getRegHL7Message(participant);
			sendHLMessage(hl7Message);
		}
		catch (DAOException e)
		{
			jdbcdao.rollback();
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}
		catch (Exception e)
		{
			LOGGER.info("Error while sending HL7 message to EMPI ");
			LOGGER.info(e.getMessage());
			throw new ApplicationException(null, e, e.getMessage());
		}
		finally
		{
			jdbcdao.closeSession();
		}
	}

	/**
	 * Gets the jDBCDAO.
	 *
	 * @return the jDBCDAO
	 *
	 * @throws DAOException
	 *             the DAO exception
	 */
	public static JDBCDAO getJDBCDAO() throws DAOException {
		String appName = CommonServiceLocator.getInstance().getAppName();
		IDAOFactory daoFactory = DAOConfigFactory.getInstance().getDAOFactory(
				appName);
		JDBCDAO jdbcdao = null;
		jdbcdao = daoFactory.getJDBCDAO();
		jdbcdao.openSession(null);
		return jdbcdao;
	}

	/**
	 * Gets the reg h l7 message.
	 *
	 * @param participant
	 *            the participant
	 *
	 * @return the reg h l7 message
	 *
	 * @throws Exception
	 *             the exception
	 */
	public String getRegHL7Message(final IParticipant participant)
			throws ApplicationException {
		String hl7Message = "";
		final String eventTypeCode = Constants.HL7_REG_EVENT_TYPE_A04;
		LOGGER.info("\n\nHL7 Message \n \n \n\n\n");

		final String commonHL7Segments = getMSHEVNPIDSengment(participant,
				eventTypeCode);
		String pvSegment;
		try {
			pvSegment = getHL7PVSegment(participant, dateTime);
			hl7Message = commonHL7Segments + "\r" + pvSegment + "\n";
		} catch (ParticipantManagerException e) {
			// TODO Auto-generated catch block
			throw new ApplicationException(null, e, e.getMessage());
		}
		return hl7Message;
	}

	/**
	 * Send merge message.
	 *
	 * @param participant
	 *            the participant
	 * @param oldParticipantId
	 *            the old participant id
	 * @param oldEMPIID
	 *            the old empiid
	 *
	 * @throws Exception
	 *             the exception
	 */
	public void sendMergeMessage(final IParticipant participant,
			final String oldParticipantId, final String oldEMPIID)
			throws ApplicationException {
		if (!participant.getEmpiId().equals(oldEMPIID)) {
			sendEMPIMIdMergeMgs(participant, oldEMPIID);
		}
		sendMRNMergeMgs(participant, oldParticipantId);
	}

	/**
	 * Gets the zero appended empi id.
	 *
	 * @param empiId
	 *            the empi id
	 *
	 * @return the zero appended empi id
	 */
	private String getZeroAppendedEMPIId(final String empiId) {
		final StringBuffer eMPIIDZeroApp = new StringBuffer(empiId);
		if (empiId != null && !"".equals(empiId)) {
			for (; eMPIIDZeroApp.length() < 24; eMPIIDZeroApp.insert(0, 0)) {
			}
		}
		return eMPIIDZeroApp.toString();
	}

	/**
	 * Send mrn merge mgs.
	 *
	 * @param participant
	 *            the participant
	 * @param oldParticipantId
	 *            the old participant id
	 *
	 * @throws Exception
	 *             the exception
	 */
	private void sendMRNMergeMgs(final IParticipant participant,
			final String oldParticipantId) throws ApplicationException {
		String hl7Message = "";
		hl7Message = getMRNMergeMgs(participant, oldParticipantId);
		sendHLMessage(hl7Message);
	}

	/**
	 * Gets the mRN merge mgs.
	 *
	 * @param participant
	 *            the participant
	 * @param oldParticipantId
	 *            the old participant id
	 *
	 * @return the mRN merge mgs
	 *
	 * @throws Exception
	 *             the exception
	 */
	public String getMRNMergeMgs(final IParticipant participant,
			final String oldParticipantId) throws ApplicationException {
		LOGGER.info("\n\n  MRN Merge HL7 Message \n \n \n\n\n");
		String hl7Message = "";
		final String eventTypeCode = Constants.HL7_MERGE_EVENT_TYPE_A34;
		final String commonHL7Segments = getMSHEVNPIDSengment(participant,
				eventTypeCode);
		final String mrgSegment = getHL7MgrSegment(participant.getEmpiId(),
				oldParticipantId);
		hl7Message = commonHL7Segments + "\r" + mrgSegment + "\n";
		LOGGER.info(mrgSegment + "\n");
		return hl7Message;
	}

	/**
	 * Send empim id merge mgs.
	 *
	 * @param participant
	 *            the participant
	 * @param oldEMPIID
	 *            the old empiid
	 *
	 * @throws Exception
	 *             the exception
	 */
	private void sendEMPIMIdMergeMgs(final IParticipant participant,
			final String oldEMPIID) throws ApplicationException {
		String hl7Message = "";
		hl7Message = getEMPIMIdMergeMgs(participant, oldEMPIID);
		sendHLMessage(hl7Message);
	}

	/**
	 * Gets the eMPIM id merge mgs.
	 *
	 * @param participant
	 *            the participant
	 * @param oldEMPIID
	 *            the old empiid
	 *
	 * @return the eMPIM id merge mgs
	 *
	 * @throws Exception
	 *             the exception
	 */
	public String getEMPIMIdMergeMgs(final IParticipant participant,
			final String oldEMPIID) throws ApplicationException {
		String hl7Message = "";
		LOGGER.info("\n\n  EMPI Merge HL7 Message \n \n \n\n\n");
		final String eventTypeCode = Constants.HL7_MERGE_EVENT_TYPE_A34;

		final String commonHL7Segments = getMSHEVNPIDSengment(participant,
				eventTypeCode);
		final String mgrSegment = getHL7MgrSegment(oldEMPIID, String
				.valueOf(participant.getId()));
		hl7Message = commonHL7Segments + "\r" + mgrSegment + "\n";

		LOGGER.info(mgrSegment + "\n");
		return hl7Message;
	}

	/**
	 * Gets the mSHEVNPID sengment.
	 *
	 * @param participant
	 *            the participant
	 * @param eventTypeCode
	 *            the event type code
	 *
	 * @return the mSHEVNPID sengment
	 *
	 * @throws Exception
	 *             the exception
	 */
	private String getMSHEVNPIDSengment(final IParticipant participant,
			final String eventTypeCode) throws ApplicationException {
		String hl7Segment = "";
		setCurrentDateTime();
		final String msgControlId = getMsgControlId();
		final String dateTime = getDateTime();
		final String msgSegment = getHL7MSHSegment(msgControlId, dateTime,
				eventTypeCode);
		final String evnSegment = getHL7EVNSegment(dateTime, eventTypeCode);
		final String pid = getHL7PIDSegment(participant, eventTypeCode);
		hl7Segment = msgSegment + "\r" + evnSegment + "\r" + pid;
		LOGGER.info(msgSegment + "\n");
		LOGGER.info(evnSegment + "\n");
		LOGGER.info(pid + "\n");
		return hl7Segment;
	}

	/**
	 * Gets the h l7 mgr segment.
	 *
	 * @param eMPI
	 *            the e mpi
	 * @param particiapntId
	 *            the particiapnt id
	 *
	 * @return the h l7 mgr segment
	 */
	private String getHL7MgrSegment(final String eMPI,
			final String particiapntId) {
		final String empiIdZeroAppnd = getZeroAppendedEMPIId(eMPI);
		final String eMPIID = empiIdZeroAppnd + "^^^64";
		final String mrn = particiapntId + "^^^"
				+ Constants.CLINPORTAL_FACILITY_ID + "^U";
		final String mgrSegment = "MRG|" + mrn + "|||" + eMPIID + "||||^^^&&";
		return mgrSegment;
	}

	/**
	 * Sets the current date time.
	 */
	private void setCurrentDateTime() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				Constants.DATE_FORMAT, Locale.US);
		final Calendar calendar = Calendar.getInstance();
		final String dateStr[] = dateFormat.format(calendar.getTime()).split(
				"-");
		final String month = dateStr[0];
		final String date = dateStr[1];
		final String year = dateStr[2];
		dateFormat = new SimpleDateFormat("HH:mm:ss:SSS", Locale.US);
		final String time[] = dateFormat.format(calendar.getTime()).split(":");
		final String hour = time[0];
		final String minute = time[1];
		final String second = time[2];
		final String milisecond = time[3];
		final String msgControlId = milisecond + "FAC" + year + month + date
				+ hour + minute + second;
		final String dateTime = year + month + date + hour + minute + second
				+ "-0500^S";
		setMsgControlId(msgControlId);
		setDateTime(dateTime);
	}

	/**
	 * Gets the h l7 msh segment.
	 *
	 * @param msgControlId
	 *            the msg control id
	 * @param dateTime
	 *            the date time
	 * @param eventTypeCode
	 *            the event type code
	 *
	 * @return the h l7 msh segment
	 */
	private String getHL7MSHSegment(final String msgControlId,
			final String dateTime, final String eventTypeCode) {
		String msgSegment = null;
		if (Constants.HL7_REG_EVENT_TYPE_A04.equals(eventTypeCode)) {
			msgSegment = "MSH|^~\\&|CLINPORTAL|CLINPORTAL|ADMISSION|ADT1|"
					+ dateTime + "||ADT^" + eventTypeCode + "|" + msgControlId
					+ "|P|2.1";
		} else if (Constants.HL7_MERGE_EVENT_TYPE_A34.equals(eventTypeCode)) {
			msgSegment = "MSH|^~\\&|CLINPORTAL|CLINPORTAL|CDR__S|BJC_SYSTEM|"
					+ dateTime + "||ADT^" + eventTypeCode + "|" + msgControlId
					+ "|P|2.1";
		}
		return msgSegment;
	}

	/**
	 * Gets the h l7 evn segment.
	 *
	 * @param dateTime
	 *            the date time
	 * @param eventTypeCode
	 *            the event type code
	 *
	 * @return the h l7 evn segment
	 */
	private String getHL7EVNSegment(final String dateTime,
			final String eventTypeCode) {
		final String evnSegment = "EVN|" + eventTypeCode + "|" + dateTime;
		return evnSegment;
	}

	/**
	 * Gets the h l7 pid segment.
	 *
	 * @param participant
	 *            the participant
	 *
	 * @return the h l7 pid segment
	 *
	 * @throws Exception
	 *             the exception
	 */
	private String getHL7PIDSegment(final IParticipant participant,
			final String eventTypeCode) throws ApplicationException {
		String pid = null;
		try {
			final String facilityId = Constants.CLINPORTAL_FACILITY_ID;
			final String lastName = participant.getLastName();
			final String firstName = participant.getFirstName();
			final String middleName = getMiddleName(participant.getMiddleName());
			final String socialSecurityNumber = getSSN(participant
					.getSocialSecurityNumber());
			final String gender = getGenderCode(participant.getGender());
			final String raceCode = getRaceCode(participant.getRaceCollection());
			final String dateOfBirth = getBirthDate(participant.getBirthDate());
			String empiIdInPID2 = "";
			final String mrn = getMRN(participant.getId());
			final String pan = getPAN(participant.getId());
			String pidFirstField = "";
			if (participant.getEmpiId() != null
					&& !"".equals(participant.getEmpiId())) {
				final String empiIdZeroAppnd = getZeroAppendedEMPIId(participant
						.getEmpiId());
				empiIdInPID2 = empiIdZeroAppnd + "^^^64";
			}
			if (Constants.HL7_MERGE_EVENT_TYPE_A34.equals(eventTypeCode)) {
				// for merge messages PID.1 field should have value :1
				pidFirstField = "1";
			}
			if (Constants.HL7_REG_EVENT_TYPE_A04.equals(eventTypeCode)) {
				pidFirstField = mrn;
			}
			pid = "PID|" + pidFirstField + "|" + empiIdInPID2 + "|" + mrn
					+ "^^^" + facilityId + "^U||"
					+ lastName.toUpperCase(Locale.US) + "^"
					+ firstName.toUpperCase(Locale.US) + "^"
					+ middleName.toUpperCase(Locale.US) + "||" + dateOfBirth
					+ "|" + gender + "||" + raceCode + "||||||||" + pan + "^^^"
					+ facilityId + "|" + socialSecurityNumber + "|||";
		} catch (Exception e) {
			throw new ApplicationException(null, e, e.getMessage());
		}
		return pid;
	}

	/**
	 * Gets the h l7 pv segment.
	 *
	 * @param participant
	 *            the participant
	 * @param dateTime
	 *            the date time
	 *
	 * @return the h l7 pv segment
	 *
	 * @throws DAOException
	 *             the DAO exception
	 * @throws ParticipantManagerException
	 * @throws BizLogicException
	 */
	private String getHL7PVSegment(final IParticipant participant,
			final String dateTime) throws DAOException, BizLogicException,
			ParticipantManagerException {
		String pvSegment;
		String csPILastName = null;
		String csPIFirstName = null;
		DAO dao = null;
		pvSegment = null;
		try {

			// dao = ParticipantManagerUtility.getJDBCDAO();
			final String appName = CommonServiceLocator.getInstance()
					.getAppName();
			final IDAOFactory daoFactory = DAOConfigFactory.getInstance()
					.getDAOFactory(appName);
			dao = daoFactory.getDAO();
			dao.openSession(null);

			// final String hql = getQuery(participant.getId().longValue());
			final String hql = getQueryForPICordinators();
			List<ColumnValueBean> columnValueBeans = new ArrayList<ColumnValueBean>();
			columnValueBeans.add(new ColumnValueBean(participant.getId()
					.longValue()));

			// final List csPINameColl = dao.executeQuery(hql);
			final List csPINameColl = dao.executeQuery(hql, columnValueBeans);
			if (csPINameColl != null && !csPINameColl.isEmpty()) {
				final Object names[] = (Object[]) csPINameColl.get(0);
				csPIFirstName = (String) names[0];
				csPILastName = (String) names[1];
			}
			if (csPILastName == null) {
				csPILastName = blankLiteral;
			}
			if (csPIFirstName == null) {
				csPIFirstName = blankLiteral;
			}
			pvSegment = "PV1|1|T|||||" + csPILastName.toUpperCase(Locale.US)
					+ "^" + csPIFirstName.toUpperCase(Locale.US)
					+ "|||||||||||||||||||||||||||||||||||||" + dateTime
					+ "||||||";

			LOGGER.info(pvSegment + "\n");
		} catch (DAOException e) {
			LOGGER.info("Error while sending HL7 message to EMPI ");
			LOGGER.info(e.getMessage());
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		} finally {
			dao.closeSession();
		}
		return pvSegment;
	}

	/**
	 * Gets the query.
	 *
	 * @param csId
	 *            the cs id
	 *
	 * @return the query
	 *
	 * @throws DAOException
	 *             the DAO exception
	 * @throws ParticipantManagerException
	 * @throws BizLogicException
	 */
	private String getQueryForPICordinators() throws DAOException,
			ParticipantManagerException, BizLogicException {

		String PartiManagerImplClassName = (String) edu.wustl.common.participant.utility.PropertyHandler
				.getValue(Constants.PARTICIPANT_MANAGER_IMPL_CLASS);

		IParticipantManager participantManagerImplObj = (IParticipantManager) ParticipantManagerUtility
				.getObject(PartiManagerImplClassName);

		return participantManagerImplObj.getPICordinatorsofProtocol();

		/*
		 * String application = null; try { application =
		 * PropertyHandler.getValue("application"); } catch (Exception e) {
		 * LOGGER.info(e.getMessage()); throw new DAOException(null, e,
		 * "Error while get value from PatientInfoLookUpService.properties"); }
		 * final StringBuffer hql = new StringBuffer(); if
		 * (Constants.CLINPORTAL_APPLICATION_NAME.equals(application)) { hql
		 * .append(
		 * "select CSReg.clinicalStudy.principalInvestigator.firstName,CSReg.clinicalStudy.p"
		 * +
		 * "rincipalInvestigator.lastName from edu.wustl.clinportal.domain.ClinicalStudyRegistration "
		 * + " CSReg where CSReg.participant.id= ? "); } else { hql.append(
		 * "select CSReg.clinicalStudy.principalInvestigator.firstName,CSReg.clinicalStudy.p"
		 * + "rincipalInvestigator.lastName from " +
		 * " edu.wustl.catissuecore.domain.ClinicalStudyRegistration" +
		 * " CSReg where CSReg.participant.id= ?"); } return hql.toString();
		 */
	}

	/**
	 * Send hl message.
	 *
	 * @param hl7Message
	 *            the hl7 message
	 */
	private void sendHLMessage(final String hl7Message) {

		final String hostName = XMLPropertyHandler
				.getValue(Constants.WMQ_SERVER_NAME);
		final String qmgName = XMLPropertyHandler
				.getValue(Constants.WMQ_QMG_NAME);
		final String channelName = XMLPropertyHandler
				.getValue(Constants.WMQ_CHANNEL);
		final int port = Integer.parseInt(XMLPropertyHandler
				.getValue(Constants.WMQ_PORT));
		final String outBoundQueueName = XMLPropertyHandler
				.getValue(Constants.OUT_BOUND_QUEUE_NAME);

		final MQMessageWriter messageWriter = new MQMessageWriter();
		messageWriter.setHostName(hostName);
		messageWriter.setPort(port);
		messageWriter.setQManagerName(qmgName);
		messageWriter.setChannelName(channelName);
		messageWriter.setQName(outBoundQueueName);
		messageWriter.sendTextMessage(hl7Message);
	}

	/**
	 * Gets the mRN.
	 *
	 * @param participantId
	 *            the participant id
	 *
	 * @return the mRN
	 */
	private String getMRN(final Long participantId) {
		String mrn = null;
		if (tempMrnId == null) {
			mrn = String.valueOf(participantId);
			if (mrn == null) {
				mrn = blankLiteral;
			}
		} else {
			mrn = tempMrnId;
		}
		return mrn;
	}

	/**
	 * Gets the pAN.
	 *
	 * @param participantId
	 *            the participant id
	 *
	 * @return the pAN
	 */
	private String getPAN(final Long participantId) {
		String pan = null;
		if (tempMrnId == null) {
			pan = participantId + "1";
			if (pan == null) {
				pan = blankLiteral;
			}
		} else {
			pan = tempMrnId + "1";
		}
		return pan;
	}

	/**
	 * Gets the middle name.
	 *
	 * @param middleName
	 *            the middle name
	 *
	 * @return the middle name
	 */
	private String getMiddleName(final String middleName) {
		String middleNameTemp = middleName;
		if (middleNameTemp == null) {
			middleNameTemp = blankLiteral;
		}
		return middleNameTemp;
	}

	/**
	 * Gets the sSN.
	 *
	 * @param socialSecurityNumber
	 *            the social security number
	 *
	 * @return the sSN
	 */
	private String getSSN(final String socialSecurityNumber) {
		String ssnTemp = socialSecurityNumber;
		if (ssnTemp == null) {
			ssnTemp = blankLiteral;
		}
		return ssnTemp;
	}

	/**
	 * Gets the birth date.
	 *
	 * @param dob
	 *            the dob
	 *
	 * @return the birth date
	 */
	private String getBirthDate(final Date dob) {
		String dateOfBirth = null;
		if (dob != null) {
			dateOfBirth = Utility.parseDateToString(dob,
					Constants.DATE_PATTERN_YYYY_MM_DD);
			if (dateOfBirth != null && !"".equals(dateOfBirth)) {
				String dobStr[] = dateOfBirth.split("-");
				if (dobStr.length >= 3) {
					dateOfBirth = dobStr[0] + dobStr[1] + dobStr[2];
				} else {
					dobStr = dateOfBirth.split("/");
					dateOfBirth = dobStr[0] + dobStr[1] + dobStr[2];
				}
			}
		} else {
			dateOfBirth = blankLiteral;
		}
		return dateOfBirth;
	}

	/**
	 * Gets the race code.
	 *
	 * @param participantRaceCollection
	 *            the participant race collection
	 *
	 * @return the race code
	 *
	 * @throws Exception
	 *             the exception
	 */
	private String getRaceCode(
			final Collection<IRace<IParticipant>> participantRaceCollection)
			throws ApplicationException {
		String raceName = null;
		String raceCode = null;
		raceName = getRaceName(participantRaceCollection);
		if (raceName == null) {
			raceCode = blankLiteral;
		} else {
			raceCode = getRaceCode(raceName);
		}
		return raceCode;
	}

	/**
	 * Gets the race code.
	 *
	 * @param raceName
	 *            the race name
	 *
	 * @return the race code
	 *
	 * @throws Exception
	 *             the exception
	 */
	private String getRaceCode(final String raceName)
			throws ApplicationException {
		String raceCode = "";
		raceCode = RaceGenderCodesProperyHandler.getValue(raceName);
		if (raceCode == null || "".equals(raceCode)) {
			raceCode = blankLiteral;
		}
		return raceCode;
	}

	/**
	 * Gets the race name.
	 *
	 * @param participantRaceCollection
	 *            the participant race collection
	 *
	 * @return the race name
	 */
	private String getRaceName(
			final Collection<IRace<IParticipant>> participantRaceCollection) {
		IRace<IParticipant> race = null;
		String raceName = null;
		if (participantRaceCollection != null
				&& !participantRaceCollection.isEmpty()) {
			final Iterator<IRace<IParticipant>> itr = participantRaceCollection
					.iterator();
			while (itr.hasNext()) {
				race = itr.next();
				if (race != null) {
					raceName = race.getRaceName();
				}
			}
		}
		return raceName;
	}

	/**
	 * Gets the gender code.
	 *
	 * @param gender
	 *            the gender
	 *
	 * @return the gender code
	 *
	 * @throws Exception
	 *             the exception
	 */
	private String getGenderCode(final String gender)
			throws ApplicationException {
		String gendercode = null;
		gendercode = RaceGenderCodesProperyHandler.getValue(gender);
		if (gendercode == null || "".equals(gendercode)) {
			gendercode = blankLiteral;
		}
		return gendercode;
	}


	/**
	 * Registers patient to eMPI again when empI Id was already generated once and
	 * user came back and edited participant
	 * and now on message board user selects "Ignore all and Create New" .
	 *
	 * @param participant the participant
	 * @throws ApplicationException the application exception
	 */
	public void ignoreAndCreateNewFlow(final IParticipant participant) throws ApplicationException
	{
		JDBCDAO jdbcdao = null;

		try
		{
			jdbcdao = getJDBCDAO();

			// if block to insert tempMRN entry in case eMPI ID was earlier generated
			if (null != participant.getEmpiId() && !("").equals(participant.getEmpiId()))
			{
				// Update PARTICIPANT_EMPI_ID_MAPPING table with tempMRN
				ParticipantManagerUtility utility = new ParticipantManagerUtility();
				utility.updateOldEMPIDetails(participant.getId(), participant.getEmpiId(), jdbcdao);
				participant.setEmpiId("");
				this.setTempMrnId(participant.getId() + "T");
			}

			// for new eMPIId generation, directly send HL7 message and
			// before sending HL7 message, reset empiId to blank and empiIdstatus to PENDING
			String queryForStatusUpdate = "UPDATE CATISSUE_PARTICIPANT SET EMPI_ID = '', "
					+ "EMPI_ID_STATUS='PENDING' WHERE IDENTIFIER = " + participant.getId();
			jdbcdao.executeUpdate(queryForStatusUpdate);
			jdbcdao.commit();

			String hl7Message = getRegHL7Message(participant);
			sendHLMessage(hl7Message);
		}
		catch (DAOException e)
		{
			jdbcdao.rollback();
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}
		catch (Exception e)
		{
			LOGGER.info("Error while sending HL7 message to EMPI ");
			LOGGER.info(e.getMessage());
			throw new ApplicationException(null, e, e.getMessage());
		}
		finally
		{
			jdbcdao.closeSession();
		}
	}

}
