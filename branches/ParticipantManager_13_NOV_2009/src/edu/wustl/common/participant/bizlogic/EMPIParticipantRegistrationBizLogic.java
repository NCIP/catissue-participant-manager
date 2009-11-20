
package edu.wustl.common.participant.bizlogic;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IRace;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.MQMessageWriter;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.participant.utility.RaceGenderCodesProperyHandler;
import edu.wustl.common.util.Utility;
import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.common.util.global.CommonServiceLocator;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.DAO;
import edu.wustl.dao.daofactory.DAOConfigFactory;
import edu.wustl.dao.daofactory.IDAOFactory;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.patientLookUp.util.PropertyHandler;

// TODO: Auto-generated Javadoc
/**
 * @author geeta_jaggal.
 * The Class EMPIParticipantRegistrationBizLogic.
 */
public class EMPIParticipantRegistrationBizLogic
{

	/** The logger. */
	private static final Logger logger = Logger
			.getCommonLogger(EMPIParticipantRegistrationBizLogic.class);

	/** The blank literal. */
	private transient String blankLiteral;

	/** The temp mrn id. */
	protected String tempMrnId;

	/** The msg control id. */
	protected String msgControlId;

	/** The date time. */
	protected String dateTime;

	/**
	 * Instantiates a new eMPI participant registration biz logic.
	 */
	public EMPIParticipantRegistrationBizLogic()
	{
		blankLiteral = "\"\"";
		tempMrnId = null;
	}

	/**
	 * Gets the temp mrn id.
	 *
	 * @return the temp mrn id
	 */
	public String getTempMrnId()
	{
		return tempMrnId;
	}

	/**
	 * Sets the temp mrn id.
	 *
	 * @param tempMrnId the new temp mrn id
	 */
	public void setTempMrnId(String tempMrnId)
	{
		this.tempMrnId = tempMrnId;
	}

	/**
	 * Gets the msg control id.
	 *
	 * @return the msg control id
	 */
	public String getMsgControlId()
	{
		return msgControlId;
	}

	/**
	 * Sets the msg control id.
	 *
	 * @param msgControlId the new msg control id
	 */
	public void setMsgControlId(String msgControlId)
	{
		this.msgControlId = msgControlId;
	}

	/**
	 * Gets the date time.
	 *
	 * @return the date time
	 */
	public String getDateTime()
	{
		return dateTime;
	}

	/**
	 * Sets the date time.
	 *
	 * @param dateTime the new date time
	 */
	public void setDateTime(String dateTime)
	{
		this.dateTime = dateTime;
	}

	/**
	 * Register patient toe mpi.
	 *
	 * @param participant the participant
	 *
	 * @throws Exception the exception
	 */
	public void registerPatientToeMPI(IParticipant participant) throws Exception
	{
		String hl7Message = "";
		String eventTypeCode = Constants.HL7_REG_EVENT_TYPE;
		try
		{
			String commonHL7Segments = getMSHEVNPIDSengment(participant, eventTypeCode);
			String pvSegment = getHL7PVSegment(participant, dateTime);
			logger.info((new StringBuilder()).append(pvSegment).append("\n").toString());
			hl7Message = (new StringBuilder()).append(commonHL7Segments).append("\r").append(
					pvSegment).append("\n").toString();
			logger.info((new StringBuilder()).append("\n\nHL7 Message \n \n \n\n\n").append(
					hl7Message).toString());
			sendHLMessage(hl7Message);
		}
		catch (Exception e)
		{
			logger.info("Error while sending HL7 message to EMPI ");
			logger.info(e.getMessage());
			throw new Exception(e.getMessage());
		}
	}

	/**
	 * Send merge message.
	 *
	 * @param participant the participant
	 * @param oldParticipantId the old participant id
	 * @param oldEMPIID the old empiid
	 *
	 * @throws Exception the exception
	 */
	public void sendMergeMessage(IParticipant participant, String oldParticipantId, String oldEMPIID)
			throws Exception
	{
		sendMRNMergeMgs(participant, oldParticipantId);
		if (!participant.getEmpiId().equals(oldEMPIID))
		{
			sendEMPIMIdMergeMgs(participant, oldEMPIID);
		}
	}

	/**
	 * Gets the zero appended empi id.
	 *
	 * @param empiId the empi id
	 *
	 * @return the zero appended empi id
	 */
	private String getZeroAppendedEMPIId(String empiId)
	{
		StringBuffer eMPIIDZeroApp = new StringBuffer(empiId);
		if (empiId != null && empiId != "")
		{
			for (; eMPIIDZeroApp.length() < 24; eMPIIDZeroApp.insert(0, 0))
			{
			}
		}
		return eMPIIDZeroApp.toString();
	}

	/**
	 * Send mrn merge mgs.
	 *
	 * @param participant the participant
	 * @param oldParticipantId the old participant id
	 *
	 * @throws Exception the exception
	 */
	private void sendMRNMergeMgs(IParticipant participant, String oldParticipantId)
			throws Exception
	{
		logger.info("\n\n  MRN Merge HL7 Message \n \n \n\n\n");
		String hl7Message = "";
		String eventTypeCode = Constants.HL7_MERGE_EVENT_TYPE_A34;
		String commonHL7Segments = getMSHEVNPIDSengment(participant, eventTypeCode);
		String mgrSegment = getHL7MgrSegment(participant.getEmpiId(), oldParticipantId);
		hl7Message = (new StringBuilder()).append(commonHL7Segments).append("\r")
				.append(mgrSegment).append("\n").toString();
		logger.info((new StringBuilder()).append(mgrSegment).append("\n").toString());
		sendHLMessage(hl7Message);
	}

	/**
	 * Send empim id merge mgs.
	 *
	 * @param participant the participant
	 * @param oldEMPIID the old empiid
	 *
	 * @throws Exception the exception
	 */
	private void sendEMPIMIdMergeMgs(IParticipant participant, String oldEMPIID) throws Exception
	{
		String hl7Message = "";
		logger.info("\n\n  EMPI Merge HL7 Message \n \n \n\n\n");
		String eventTypeCode = Constants.HL7_MERGE_EVENT_TYPE_A34;
		String commonHL7Segments = getMSHEVNPIDSengment(participant, eventTypeCode);
		String mgrSegment = getHL7MgrSegment(oldEMPIID, String.valueOf(participant.getId()));
		hl7Message = (new StringBuilder()).append(commonHL7Segments).append("\r")
				.append(mgrSegment).append("\n").toString();
		logger.info((new StringBuilder()).append(mgrSegment).append("\n").toString());
		sendHLMessage(hl7Message);
	}

	/**
	 * Gets the mSHEVNPID sengment.
	 *
	 * @param participant the participant
	 * @param eventTypeCode the event type code
	 *
	 * @return the mSHEVNPID sengment
	 *
	 * @throws Exception the exception
	 */
	private String getMSHEVNPIDSengment(IParticipant participant, String eventTypeCode)
			throws Exception
	{
		String hl7Segment = "";
		setCurrentDateTime();
		String msgControlId = getMsgControlId();
		String dateTime = getDateTime();
		String msgSegment = getHL7MSHSegment(msgControlId, dateTime, eventTypeCode);
		String evnSegment = getHL7EVNSegment(dateTime, eventTypeCode);
		String pid = getHL7PIDSegment(participant);
		hl7Segment = (new StringBuilder()).append(msgSegment).append("\r").append(evnSegment)
				.append("\r").append(pid).toString();
		logger.info((new StringBuilder()).append(msgSegment).append("\n").toString());
		logger.info((new StringBuilder()).append(evnSegment).append("\n").toString());
		logger.info((new StringBuilder()).append(pid).append("\n").toString());
		return hl7Segment;
	}

	/**
	 * Gets the h l7 mgr segment.
	 *
	 * @param eMPI the e mpi
	 * @param particiapntId the particiapnt id
	 *
	 * @return the h l7 mgr segment
	 */
	private String getHL7MgrSegment(String eMPI, String particiapntId)
	{
		String empiIdZeroAppnd = getZeroAppendedEMPIId(eMPI);
		String eMPIID = (new StringBuilder()).append(empiIdZeroAppnd).append("^^^64").toString();
		String mrn = (new StringBuilder()).append(particiapntId).append("^^^").append(
				Constants.CLINPORTAL_FACILITY_ID).append("^U").toString();
		String mgrSegment = (new StringBuilder()).append("MGR|").append(mrn).append("||").append(
				eMPIID).toString();
		return mgrSegment;
	}

	/**
	 * Sets the current date time.
	 */
	private void setCurrentDateTime()
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US);
		Calendar calendar = Calendar.getInstance();
		String dateStr[] = dateFormat.format(calendar.getTime()).split("-");
		String month = dateStr[0];
		String date = dateStr[1];
		String year = dateStr[2];
		dateFormat = new SimpleDateFormat("HH:mm:ss:SSS", Locale.US);
		String time[] = dateFormat.format(calendar.getTime()).split(":");
		String hour = time[0];
		String minute = time[1];
		String second = time[2];
		String milisecond = time[3];
		String msgControlId = (new StringBuilder()).append(milisecond).append("FAC").append(year)
				.append(month).append(date).append(hour).append(minute).append(second).toString();
		String dateTime = (new StringBuilder()).append(year).append(month).append(date)
				.append(hour).append(minute).append(second).append("-0500^S").toString();
		setMsgControlId(msgControlId);
		setDateTime(dateTime);
	}

	/**
	 * Gets the h l7 msh segment.
	 *
	 * @param msgControlId the msg control id
	 * @param dateTime the date time
	 * @param eventTypeCode the event type code
	 *
	 * @return the h l7 msh segment
	 */
	private String getHL7MSHSegment(String msgControlId, String dateTime, String eventTypeCode)
	{
		String msgSegment = (new StringBuilder()).append(
				"MSH|^~\\&|CLINPORTAL|CLINPORTAL|ADMISSION|ADT1|").append(dateTime)
				.append("||ADT^").append(eventTypeCode).append("|").append(msgControlId).append(
						"|P|2.1").toString();
		return msgSegment;
	}

	/**
	 * Gets the h l7 evn segment.
	 *
	 * @param dateTime the date time
	 * @param eventTypeCode the event type code
	 *
	 * @return the h l7 evn segment
	 */
	private String getHL7EVNSegment(String dateTime, String eventTypeCode)
	{
		String evnSegment = (new StringBuilder()).append("EVN|").append(eventTypeCode).append("|")
				.append(dateTime).toString();
		return evnSegment;
	}

	/**
	 * Gets the h l7 pid segment.
	 *
	 * @param participant the participant
	 *
	 * @return the h l7 pid segment
	 *
	 * @throws Exception the exception
	 */
	private String getHL7PIDSegment(IParticipant participant) throws Exception
	{
		String facilityId = Constants.CLINPORTAL_FACILITY_ID;
		String lastName = participant.getLastName();
		String firstName = participant.getFirstName();
		String middleName = getMiddleName(participant.getMiddleName());
		String socialSecurityNumber = getSSN(participant.getSocialSecurityNumber());
		String gender = getGenderCode(participant.getGender());
		String raceCode = getRaceCode(participant.getRaceCollection());
		String dateOfBirth = getBirthDate(participant.getBirthDate());
		String empiIdInPID2 = "";
		String mrn = getMRN(participant.getId());
		String pan = getPAN(participant.getId());
		if (participant.getEmpiId() != null && participant.getEmpiId() != "")
		{
			String empiIdZeroAppnd = getZeroAppendedEMPIId(participant.getEmpiId());
			empiIdInPID2 = (new StringBuilder()).append(empiIdZeroAppnd).append("^^^64").toString();
		}
		String pid = (new StringBuilder()).append("PID|").append(mrn).append("|").append(
				empiIdInPID2).append("|").append(mrn).append("^^^").append(facilityId).append(
				"^U||").append(lastName.toUpperCase(Locale.US)).append("^").append(
				firstName.toUpperCase(Locale.US)).append("^").append(
				middleName.toUpperCase(Locale.US)).append("||").append(dateOfBirth).append("|")
				.append(gender).append("||").append(raceCode).append("||||||||").append(pan)
				.append("^^^").append(facilityId).append("|").append(socialSecurityNumber).append(
						"|||").toString();
		return pid;
	}

	/**
	 * Gets the h l7 pv segment.
	 *
	 * @param participant the participant
	 * @param dateTime the date time
	 *
	 * @return the h l7 pv segment
	 *
	 * @throws DAOException the DAO exception
	 */
	private String getHL7PVSegment(IParticipant participant, String dateTime) throws DAOException
	{
		String pvSegment;
		Exception exception;
		String csPILastName = null;
		String csPIFirstName = null;
		DAO dao = null;
		pvSegment = null;
		try
		{

			//dao = ParticipantManagerUtility.getJDBCDAO();
			String appName = CommonServiceLocator.getInstance().getAppName();
			IDAOFactory daoFactory = DAOConfigFactory.getInstance().getDAOFactory(appName);
			dao = daoFactory.getDAO();
			dao.openSession(null);

			String hql = getQuery(participant.getId().longValue());
			List csPINameColl = dao.executeQuery(hql);
			if (csPINameColl != null && !csPINameColl.isEmpty())
			{
				Object names[] = (Object[]) (Object[]) csPINameColl.get(0);
				csPIFirstName = (String) names[0];
				csPILastName = (String) names[1];
			}
			if (csPILastName == null)
			{
				csPILastName = blankLiteral;
			}
			if (csPIFirstName == null)
			{
				csPIFirstName = blankLiteral;
			}
			pvSegment = (new StringBuilder()).append("PV1|1|T|||||").append(
					csPILastName.toUpperCase(Locale.US)).append("^").append(
					csPIFirstName.toUpperCase(Locale.US)).append(
					"|||||||||||||||||||||||||||||||||||||").append(dateTime).append("||||||")
					.toString();
		}
		catch (DAOException e)
		{
			logger.info("Error while sending HL7 message to EMPI ");
			logger.info(e.getMessage());
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}
		finally
		{
			dao.closeSession();
		}
		return pvSegment;
	}

	/**
	 * Gets the query.
	 *
	 * @param csId the cs id
	 *
	 * @return the query
	 *
	 * @throws DAOException the DAO exception
	 */
	private String getQuery(long csId) throws DAOException
	{
		String application = null;
		try
		{
			application = PropertyHandler.getValue("application");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new DAOException(null, e,
					"Error while get value from PatientInfoLookUpService.properties");
		}
		StringBuffer hql = new StringBuffer();
		if ("clinportal".equals(application))
		{
			hql
					.append("select CSReg.clinicalStudy.principalInvestigator.firstName,CSReg.clinicalStudy.p"
							+ "rincipalInvestigator.lastName from ");
			hql.append("edu.wustl.clinportal.domain.ClinicalStudyRegistration ");
			hql.append(" CSReg where CSReg.participant.id=");
			hql.append(csId);
		}
		else
		{
			hql
					.append("select CSReg.clinicalStudy.principalInvestigator.firstName,CSReg.clinicalStudy.p"
							+ "rincipalInvestigator.lastName from ");
			hql.append("edu.wustl.catissuecore.domain.ClinicalStudyRegistration");
			hql.append(" CSReg where CSReg.participant.id=");
			hql.append(csId);
		}
		return hql.toString();
	}

	/**
	 * Send hl message.
	 *
	 * @param hl7Message the hl7 message
	 */
	private void sendHLMessage(String hl7Message)
	{

		String hostName = XMLPropertyHandler.getValue(Constants.WMQ_SERVER_NAME);
		String qmgName = XMLPropertyHandler.getValue(Constants.WMQ_QMG_NAME);
		String channelName = XMLPropertyHandler.getValue(Constants.WMQ_CHANNEL);
		int port = Integer.parseInt(XMLPropertyHandler.getValue(Constants.WMQ_PORT));
		String inBoundQueueName = XMLPropertyHandler.getValue(Constants.IN_BOUND_QUEUE_NAME);

		MQMessageWriter messageWriter = new MQMessageWriter();
		messageWriter.setHostName(hostName);
		messageWriter.setPort(port);
		messageWriter.setQManagerName(qmgName);
		messageWriter.setChannelName(channelName);
		messageWriter.setQName(inBoundQueueName);
		messageWriter.sendTextMessage(hl7Message);
	}

	/**
	 * Gets the mRN.
	 *
	 * @param participantId the participant id
	 *
	 * @return the mRN
	 */
	private String getMRN(Long participantId)
	{
		String mrn = null;
		if (tempMrnId == null)
		{
			mrn = String.valueOf(participantId);
			if (mrn == null)
			{
				mrn = blankLiteral;
			}
		}
		else
		{
			mrn = tempMrnId;
		}
		return mrn;
	}

	/**
	 * Gets the pAN.
	 *
	 * @param participantId the participant id
	 *
	 * @return the pAN
	 */
	private String getPAN(Long participantId)
	{
		String pan = null;
		if (tempMrnId == null)
		{
			pan = (new StringBuilder()).append(String.valueOf(participantId)).append("1")
					.toString();
			if (pan == null)
			{
				pan = blankLiteral;
			}
		}
		else
		{
			pan = (new StringBuilder()).append(tempMrnId).append("1").toString();
		}
		return pan;
	}

	/**
	 * Gets the middle name.
	 *
	 * @param middleName the middle name
	 *
	 * @return the middle name
	 */
	private String getMiddleName(String middleName)
	{
		String middleNameTemp = middleName;
		if (middleNameTemp == null)
		{
			middleNameTemp = blankLiteral;
		}
		return middleNameTemp;
	}

	/**
	 * Gets the sSN.
	 *
	 * @param socialSecurityNumber the social security number
	 *
	 * @return the sSN
	 */
	private String getSSN(String socialSecurityNumber)
	{
		String ssnTemp = socialSecurityNumber;
		if (ssnTemp == null)
		{
			ssnTemp = blankLiteral;
		}
		return ssnTemp;
	}

	/**
	 * Gets the birth date.
	 *
	 * @param dob the dob
	 *
	 * @return the birth date
	 */
	private String getBirthDate(Date dob)
	{
		String dateOfBirth = null;
		if (dob != null)
		{
			dateOfBirth = Utility.parseDateToString(dob, Constants.DATE_PATTERN_YYYY_MM_DD);
			if (dateOfBirth != null && dateOfBirth != "")
			{
				String dobStr[] = dateOfBirth.split("-");
				if (dobStr.length >= 3)
				{
					dateOfBirth = (new StringBuilder()).append(dobStr[0]).append(dobStr[1]).append(
							dobStr[2]).toString();
				}
				else
				{
					dobStr = dateOfBirth.split("/");
					dateOfBirth = (new StringBuilder()).append(dobStr[0]).append(dobStr[1]).append(
							dobStr[2]).toString();
				}
			}
		}
		else
		{
			dateOfBirth = blankLiteral;
		}
		return dateOfBirth;
	}

	/**
	 * Gets the race code.
	 *
	 * @param participantRaceCollection the participant race collection
	 *
	 * @return the race code
	 *
	 * @throws Exception the exception
	 */
	private String getRaceCode(Collection participantRaceCollection) throws Exception
	{
		String raceName = null;
		String raceCode = null;
		raceName = getRaceName(participantRaceCollection);
		if (raceName == null)
		{
			raceCode = blankLiteral;
		}
		else
		{
			raceCode = getRaceCode(raceName);
		}
		return raceCode;
	}

	/**
	 * Gets the race code.
	 *
	 * @param raceName the race name
	 *
	 * @return the race code
	 *
	 * @throws Exception the exception
	 */
	private String getRaceCode(String raceName) throws Exception
	{
		String raceCode = "";
		raceCode = RaceGenderCodesProperyHandler.getValue(raceName);
		if (raceCode == null || raceCode == "")
		{
			raceCode = blankLiteral;
		}
		return raceCode;
	}

	/**
	 * Gets the race name.
	 *
	 * @param participantRaceCollection the participant race collection
	 *
	 * @return the race name
	 */
	private String getRaceName(Collection participantRaceCollection)
	{
		IRace race = null;
		String raceName = null;
		if (participantRaceCollection != null && !participantRaceCollection.isEmpty())
		{
			Iterator itr = participantRaceCollection.iterator();
			do
			{
				if (!itr.hasNext())
				{
					break;
				}
				race = (IRace) itr.next();
				if (race != null)
				{
					raceName = race.getRaceName();
				}
			}
			while (true);
		}
		return raceName;
	}

	/**
	 * Gets the gender code.
	 *
	 * @param gender the gender
	 *
	 * @return the gender code
	 *
	 * @throws Exception the exception
	 */
	private String getGenderCode(String gender) throws Exception
	{
		String gendercode = null;
		gendercode = RaceGenderCodesProperyHandler.getValue(gender);
		if (gendercode == null || gendercode == "")
		{
			gendercode = blankLiteral;
		}
		return gendercode;
	}
}
