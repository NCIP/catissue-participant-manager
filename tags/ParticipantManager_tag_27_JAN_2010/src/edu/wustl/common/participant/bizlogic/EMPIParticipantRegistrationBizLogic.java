
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
 * The Class EMPIParticipantRegistrationBizLogic.
 *
 * @author geeta_jaggal
 */
public class EMPIParticipantRegistrationBizLogic
{

	private static Logger logger = Logger.getCommonLogger(EMPIParticipantRegistrationBizLogic.class);
    private transient String blankLiteral;
    protected String tempMrnId;
    protected String msgControlId;
    protected String dateTime;

    public EMPIParticipantRegistrationBizLogic()
    {
        blankLiteral = "\"\"";
        tempMrnId = null;
    }

    public String getTempMrnId()
    {
        return tempMrnId;
    }

    public void setTempMrnId(String tempMrnId)
    {
        this.tempMrnId = tempMrnId;
    }

    public String getMsgControlId()
    {
        return msgControlId;
    }

    public void setMsgControlId(String msgControlId)
    {
        this.msgControlId = msgControlId;
    }

    public String getDateTime()
    {
        return dateTime;
    }

    public void setDateTime(String dateTime)
    {
        this.dateTime = dateTime;
    }

    public void registerPatientToeMPI(IParticipant participant)
        throws Exception
    {
        String hl7Message = "";
        String eventTypeCode = "A04";
        try
        {
            String commonHL7Segments = getMSHEVNPIDSengment(participant, eventTypeCode);
            String pvSegment = getHL7PVSegment(participant, dateTime);
            logger.info((new StringBuilder()).append(pvSegment).append("\n").toString());
            hl7Message = (new StringBuilder()).append(commonHL7Segments).append("\r").append(pvSegment).append("\n").toString();
            logger.info((new StringBuilder()).append("\n\nHL7 Message \n \n \n\n\n").append(hl7Message).toString());
            sendHLMessage(hl7Message);
        }
        catch(Exception e)
        {
            logger.info("Error while sending HL7 message to EMPI ");
            logger.info(e.getMessage());
            throw new Exception(e.getMessage());
        }
    }

    public void sendMergeMessage(IParticipant participant, String oldParticipantId, String oldEMPIID)
        throws Exception
    {
        sendMRNMergeMgs(participant, oldParticipantId);
        if(!participant.getEmpiId().equals(oldEMPIID))
        {
            sendEMPIMIdMergeMgs(participant, oldEMPIID);
        }
    }

    private String getZeroAppendedEMPIId(String empiId)
    {
        StringBuffer eMPIIDZeroApp = new StringBuffer(empiId);
        if(empiId != null && empiId != "")
        {
            for(; eMPIIDZeroApp.length() < 24; eMPIIDZeroApp.insert(0, 0)) { }
        }
        return eMPIIDZeroApp.toString();
    }

    private void sendMRNMergeMgs(IParticipant participant, String oldParticipantId)
        throws Exception
    {
        logger.info("\n\n  MRN Merge HL7 Message \n \n \n\n\n");
        String hl7Message = "";
        String eventTypeCode = "A34";
        String commonHL7Segments = getMSHEVNPIDSengment(participant, eventTypeCode);
        String mgrSegment = getHL7MgrSegment(participant.getEmpiId(), oldParticipantId);
        hl7Message = (new StringBuilder()).append(commonHL7Segments).append("\r").append(mgrSegment).append("\n").toString();
        logger.info((new StringBuilder()).append(mgrSegment).append("\n").toString());
        sendHLMessage(hl7Message);
    }

    private void sendEMPIMIdMergeMgs(IParticipant participant, String oldEMPIID)
        throws Exception
    {
        String hl7Message = "";
        logger.info("\n\n  EMPI Merge HL7 Message \n \n \n\n\n");
        String eventTypeCode = "A34";
        String commonHL7Segments = getMSHEVNPIDSengment(participant, eventTypeCode);
        String mgrSegment = getHL7MgrSegment(oldEMPIID, String.valueOf(participant.getId()));
        hl7Message = (new StringBuilder()).append(commonHL7Segments).append("\r").append(mgrSegment).append("\n").toString();
        logger.info((new StringBuilder()).append(mgrSegment).append("\n").toString());
        sendHLMessage(hl7Message);
    }

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
        hl7Segment = (new StringBuilder()).append(msgSegment).append("\r").append(evnSegment).append("\r").append(pid).toString();
        logger.info((new StringBuilder()).append(msgSegment).append("\n").toString());
        logger.info((new StringBuilder()).append(evnSegment).append("\n").toString());
        logger.info((new StringBuilder()).append(pid).append("\n").toString());
        return hl7Segment;
    }

    private String getHL7MgrSegment(String eMPI, String particiapntId)
    {
        String empiIdZeroAppnd = getZeroAppendedEMPIId(eMPI);
        String eMPIID = (new StringBuilder()).append(empiIdZeroAppnd).append("^^^64").toString();
        String mrn = (new StringBuilder()).append(particiapntId).append("^^^").append("6B").append("^U").toString();
        String mgrSegment = (new StringBuilder()).append("MGR|").append(mrn).append("||").append(eMPIID).toString();
        return mgrSegment;
    }

    private void setCurrentDateTime()
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
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
        String msgControlId = (new StringBuilder()).append(milisecond).append("FAC").append(year).append(month).append(date).append(hour).append(minute).append(second).toString();
        String dateTime = (new StringBuilder()).append(year).append(month).append(date).append(hour).append(minute).append(second).append("-0500^S").toString();
        setMsgControlId(msgControlId);
        setDateTime(dateTime);
    }

    private String getHL7MSHSegment(String msgControlId, String dateTime, String eventTypeCode)
    {
        String msgSegment = (new StringBuilder()).append("MSH|^~\\&|CLINPORTAL|CLINPORTAL|ADMISSION|ADT1|").append(dateTime).append("||ADT^").append(eventTypeCode).append("|").append(msgControlId).append("|P|2.1").toString();
        return msgSegment;
    }

    private String getHL7EVNSegment(String dateTime, String eventTypeCode)
    {
        String evnSegment = (new StringBuilder()).append("EVN|").append(eventTypeCode).append("|").append(dateTime).toString();
        return evnSegment;
    }

    private String getHL7PIDSegment(IParticipant participant)
        throws Exception
    {
        String facilityId = "6B";
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
        if(participant.getEmpiId() != null && participant.getEmpiId() != "")
        {
            String empiIdZeroAppnd = getZeroAppendedEMPIId(participant.getEmpiId());
            empiIdInPID2 = (new StringBuilder()).append(empiIdZeroAppnd).append("^^^64").toString();
        }
        String pid = (new StringBuilder()).append("PID|").append(mrn).append("|").append(empiIdInPID2).append("|").append(mrn).append("^^^").append(facilityId).append("^U||").append(lastName.toUpperCase(Locale.US)).append("^").append(firstName.toUpperCase(Locale.US)).append("^").append(middleName.toUpperCase(Locale.US)).append("||").append(dateOfBirth).append("|").append(gender).append("||").append(raceCode).append("||||||||").append(pan).append("^^^").append(facilityId).append("|").append(socialSecurityNumber).append("|||").toString();
        return pid;
    }

    private String getHL7PVSegment(IParticipant participant, String dateTime)
        throws DAOException
    {
        String pvSegment;
        Exception exception;
        String csPILastName = null;
        String csPIFirstName = null;
        DAO dao = null;
        pvSegment = null;
        try
        {
            String appName = CommonServiceLocator.getInstance().getAppName();
            IDAOFactory daoFactory = DAOConfigFactory.getInstance().getDAOFactory(appName);
            dao = daoFactory.getDAO();
            dao.openSession(null);
            String hql = getQuery(participant.getId().longValue());
            List csPINameColl = dao.executeQuery(hql);
            if(csPINameColl != null && !csPINameColl.isEmpty())
            {
                Object names[] = (Object[])(Object[])csPINameColl.get(0);
                csPIFirstName = (String)names[0];
                csPILastName = (String)names[1];
            }
            if(csPILastName == null)
            {
                csPILastName = blankLiteral;
            }
            if(csPIFirstName == null)
            {
                csPIFirstName = blankLiteral;
            }
            pvSegment = (new StringBuilder()).append("PV1|1|T|||||").append(csPILastName.toUpperCase(Locale.US)).append("^").append(csPIFirstName.toUpperCase(Locale.US)).append("|||||||||||||||||||||||||||||||||||||").append(dateTime).append("||||||").toString();
        }
        catch(DAOException e)
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

    private String getQuery(long csId)
        throws DAOException
    {
        String application = null;
        try
        {
            application = PropertyHandler.getValue("application");
        }
        catch(Exception e)
        {
            e.printStackTrace();
            throw new DAOException(null, e, "Error while get value from PatientInfoLookUpService.properties");
        }
        StringBuffer hql = new StringBuffer();
        if("clinportal".equals(application))
        {
            hql.append("select CSReg.clinicalStudy.principalInvestigator.firstName,CSReg.clinicalStudy.p" +
"rincipalInvestigator.lastName from "
);
            hql.append("edu.wustl.clinportal.domain.ClinicalStudyRegistration ");
            hql.append(" CSReg where CSReg.participant.id=");
            hql.append(csId);
        } else
        {
            hql.append("select CSReg.clinicalStudy.principalInvestigator.firstName,CSReg.clinicalStudy.p" +
"rincipalInvestigator.lastName from "
);
            hql.append("edu.wustl.catissuecore.domain.ClinicalStudyRegistration");
            hql.append(" CSReg where CSReg.participant.id=");
            hql.append(csId);
        }
        return hql.toString();
    }

    private void sendHLMessage(String hl7Message)
    {
        String hostName = XMLPropertyHandler.getValue("WMQServerName");
        String qmgName = XMLPropertyHandler.getValue("WMQMGRName");
        String channelName = XMLPropertyHandler.getValue("WMQChannel");
        int port = Integer.parseInt(XMLPropertyHandler.getValue("WMQPort"));
        String inBoundQueueName = XMLPropertyHandler.getValue("InBoundQueue");
        MQMessageWriter messageWriter = new MQMessageWriter();
        messageWriter.setHostName(hostName);
        messageWriter.setPort(port);
        messageWriter.setQManagerName(qmgName);
        messageWriter.setChannelName(channelName);
        messageWriter.setQName(inBoundQueueName);
        messageWriter.sendTextMessage(hl7Message);
    }

    private String getMRN(Long participantId)
    {
        String mrn = null;
        if(tempMrnId == null)
        {
            mrn = String.valueOf(participantId);
            if(mrn == null)
            {
                mrn = blankLiteral;
            }
        } else
        {
            mrn = tempMrnId;
        }
        return mrn;
    }

    private String getPAN(Long participantId)
    {
        String pan = null;
        if(tempMrnId == null)
        {
            pan = (new StringBuilder()).append(String.valueOf(participantId)).append("1").toString();
            if(pan == null)
            {
                pan = blankLiteral;
            }
        } else
        {
            pan = (new StringBuilder()).append(tempMrnId).append("1").toString();
        }
        return pan;
    }

    private String getMiddleName(String middleName)
    {
        String middleNameTemp = middleName;
        if(middleNameTemp == null)
        {
            middleNameTemp = blankLiteral;
        }
        return middleNameTemp;
    }

    private String getSSN(String socialSecurityNumber)
    {
        String ssnTemp = socialSecurityNumber;
        if(ssnTemp == null)
        {
            ssnTemp = blankLiteral;
        }
        return ssnTemp;
    }

    private String getBirthDate(Date dob)
    {
        String dateOfBirth = null;
        if(dob != null)
        {
            dateOfBirth = Utility.parseDateToString(dob, "yyyy-MM-dd");
            if(dateOfBirth != null && dateOfBirth != "")
            {
                String dobStr[] = dateOfBirth.split("-");
                if(dobStr.length >= 3)
                {
                    dateOfBirth = (new StringBuilder()).append(dobStr[0]).append(dobStr[1]).append(dobStr[2]).toString();
                } else
                {
                    dobStr = dateOfBirth.split("/");
                    dateOfBirth = (new StringBuilder()).append(dobStr[0]).append(dobStr[1]).append(dobStr[2]).toString();
                }
            }
        } else
        {
            dateOfBirth = blankLiteral;
        }
        return dateOfBirth;
    }

    private String getRaceCode(Collection participantRaceCollection)
        throws Exception
    {
        String raceName = null;
        String raceCode = null;
        raceName = getRaceName(participantRaceCollection);
        if(raceName == null)
        {
            raceCode = blankLiteral;
        } else
        {
            raceCode = getRaceCode(raceName);
        }
        return raceCode;
    }

    private String getRaceCode(String raceName)
        throws Exception
    {
        String raceCode = "";
        raceCode = RaceGenderCodesProperyHandler.getValue(raceName);
        if(raceCode == null || raceCode == "")
        {
            raceCode = blankLiteral;
        }
        return raceCode;
    }

    private String getRaceName(Collection participantRaceCollection)
    {
        IRace race = null;
        String raceName = null;
        if(participantRaceCollection != null && !participantRaceCollection.isEmpty())
        {
            Iterator itr = participantRaceCollection.iterator();
            do
            {
                if(!itr.hasNext())
                {
                    break;
                }
                race = (IRace)itr.next();
                if(race != null)
                {
                    raceName = race.getRaceName();
                }
            } while(true);
        }
        return raceName;
    }

    private String getGenderCode(String gender)
        throws Exception
    {
        String gendercode = null;
        gendercode = RaceGenderCodesProperyHandler.getValue(gender);
        if(gendercode == null || gendercode == "")
        {
            gendercode = blankLiteral;
        }
        return gendercode;
    }
}
