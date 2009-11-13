package edu.wustl.common.participant.listener;

import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.bizlogic.DefaultBizLogic;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.participant.bizlogic.EMPIParticipantRegistrationBizLogic;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.IRace;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.domain.IUser;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.common.util.global.CommonServiceLocator;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.daofactory.DAOConfigFactory;
import edu.wustl.dao.daofactory.IDAOFactory;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.patientLookUp.util.PatientLookupException;
import edu.wustl.patientLookUp.util.PropertyHandler;
import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class EMPIParticipantListener
    implements MessageListener
{

    private Document document;
    private Collection partiMedIdColl;
    public String participantId;

    public EMPIParticipantListener()
    {
        document = null;
    }

    public Collection getPartiMedIdColl()
    {
        return partiMedIdColl;
    }

    public void setPartiMedIdColl(Collection partiMedIdColl)
    {
        this.partiMedIdColl = partiMedIdColl;
    }

    public String getParticipantId()
    {
        return participantId;
    }

    public void setParticipantId(String participantId)
    {
        this.participantId = participantId;
    }

    public Document getDocument()
    {
        return document;
    }

    public void setDocument(Document document)
    {
        this.document = document;
    }

    public void onMessage(Message message)
    {
        try
        {
            SessionDataBean sessionData = null;
            String clinPortalId = null;
            String sourceObjectName = null;
            Element docEle = null;
            IUser validUser = null;
            String loginName = null;
            IParticipant partcipantObj = null;
            String personDemoGraphics = null;
            String oldParticipantId = null;
            String oldEMPIID = null;
            boolean isGenerateMgrMessage = false;
            if(message instanceof TextMessage)
            {
                personDemoGraphics = ((TextMessage)message).getText();
                loginName = XMLPropertyHandler.getValue("catissueAdminLoginId");
                validUser = getUser(loginName, "Active");
                sourceObjectName = IParticipant.class.getName();
                document = getDocument(personDemoGraphics);
                docEle = document.getDocumentElement();
                processDomographicXML(docEle);
                clinPortalId = getParticipantId();
                String permanentId = getPermanentId(clinPortalId);
                oldParticipantId = clinPortalId;
                if(permanentId != null && permanentId != "")
                {
                    isGenerateMgrMessage = true;
                    clinPortalId = permanentId;
                }
                DefaultBizLogic bizlogic = new DefaultBizLogic();
                partcipantObj = (IParticipant)bizlogic.retrieve(sourceObjectName, Long.valueOf(Long.parseLong(clinPortalId)));
                oldEMPIID = String.valueOf(partcipantObj.getEmpiId());
                if(validUser != null)
                {
                    sessionData = getSessionDataBean(validUser, loginName);
                    updateParticipant(docEle, partcipantObj, sessionData);
                    if(isGenerateMgrMessage)
                    {
                        EMPIParticipantRegistrationBizLogic eMPIPartiReg = new EMPIParticipantRegistrationBizLogic();
                        eMPIPartiReg.sendMergeMessage(partcipantObj, oldParticipantId, oldEMPIID);
                    }
                } else
                {
                    checkUserAccount(loginName);
                }
            }
        }
        catch(ParserConfigurationException pce)
        {
            Logger.out.error(pce.getMessage());
        }
        catch(SAXException se)
        {
            Logger.out.error(se.getMessage());
        }
        catch(IOException ioe)
        {
            Logger.out.error(ioe.getMessage());
        }
        catch(BizLogicException e)
        {
            Logger.out.error(e.getMessage());
        }
        catch(JMSException e)
        {
            Logger.out.error(e.getMessage());
        }
        catch(Exception e)
        {
            Logger.out.error(e.getMessage());
        }
    }

    private String getPermanentId(String clinPortalId)
        throws DAOException, SQLException
    {
        String permanentId = null;
        String appName = CommonServiceLocator.getInstance().getAppName();
        IDAOFactory daoFactory = DAOConfigFactory.getInstance().getDAOFactory(appName);
        JDBCDAO jdbcdao = null;
        jdbcdao = daoFactory.getJDBCDAO();
        jdbcdao.openSession(null);
        ResultSet rs = jdbcdao.getQueryResultSet((new StringBuilder()).append("select permanent_participant_id from catissue_empi_parti_id_mapping where tempar" +
"ary_participant_id='"
).append(clinPortalId).append("'").toString());
        if(rs != null)
        {
            while(rs.next())
            {
                permanentId = rs.getString("permanent_participant_id");
            }
        }
        jdbcdao.closeSession();
        return permanentId;
    }

    private void checkUserAccount(String loginName)
        throws BizLogicException, Exception
    {
        if(getUser(loginName, "Closed") != null)
        {
            throw new Exception((new StringBuilder()).append(loginName).append(" Closed user. Sending back to the login Page").toString());
        } else
        {
            throw new Exception((new StringBuilder()).append(loginName).append("Invalid user. Sending back to the login Page").toString());
        }
    }

    private void updateParticipant(Element docEle, IParticipant partcipantObj, SessionDataBean sessionData)
        throws PatientLookupException, BizLogicException
    {
        IParticipant participant = null;
        String gender = null;
        Collection partiMedicalIdColl = null;
        if(partcipantObj != null)
        {
            String personUpi = docEle.getElementsByTagName("personUpi").item(0).getFirstChild().getNodeValue();
            NodeList childNodeList = docEle.getElementsByTagName("demographics").item(0).getChildNodes();
            for(int i = 0; i < childNodeList.getLength(); i++)
            {
                if(("Unspecified".equals(partcipantObj.getGender()) || "Unknown".equals(partcipantObj.getGender())) && "gender".equals(childNodeList.item(i).getNodeName()))
                {
                    Element ele = (Element)childNodeList.item(i);
                    String value = getNodeValue(ele, "id");
                    gender = PropertyHandler.getValue(value);
                    partcipantObj.setGender(gender);
                }
                if("raceCollection".equals(childNodeList.item(i).getNodeName()))
                {
                    setRaceCollection(partcipantObj, childNodeList.item(i));
                }
            }

            partiMedicalIdColl = getPartiMedIdColl();
            processPartiMedIdColl(partiMedicalIdColl, partcipantObj);
            participant = partcipantObj;
            partcipantObj.setEmpiId(personUpi);
            partcipantObj.setEmpiIdStatus("CREATED");
            partcipantObj.setParticipantMedicalIdentifierCollection(partiMedicalIdColl);
            DefaultBizLogic bizlogic = new DefaultBizLogic();
            bizlogic.update(partcipantObj, participant, sessionData);
        }
    }

    private void setRaceCollection(IParticipant partcipantObj, Node childNode)
    {
label0:
        {
            Collection raceCollection = null;
            if(partcipantObj.getRaceCollection() == null || partcipantObj.getRaceCollection().isEmpty())
            {
                break label0;
            }
            Iterator itr = partcipantObj.getRaceCollection().iterator();
            IRace race;
            do
            {
                if(!itr.hasNext())
                {
                    break label0;
                }
                race = (IRace)itr.next();
            } while(!"Unknown".equalsIgnoreCase(race.getRaceName()) && !"Not Reported".equals(race.getRaceName()) && !"Not Specified".equals(race.getRaceName()));
            raceCollection = getRaceCollection(childNode, partcipantObj);
            partcipantObj.setRaceCollection(raceCollection);
        }
    }

    private Document getDocument(String hl7MessageFromQueue)
        throws ParserConfigurationException, SAXException, IOException
    {
        Document document = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        document = docBuilder.parse(new InputSource(new StringReader(hl7MessageFromQueue)));
        setDocument(document);
        return document;
    }

    public String getNodeValue(Element ele, String nodeName)
    {
        NodeList list1 = ele.getElementsByTagName(nodeName);
        Element element1 = (Element)list1.item(0);
        return element1.getFirstChild().getNodeValue();
    }

    private Collection getRaceCollection(Node childNode, IParticipant partcipantObj)
    {
        NodeList subChildNodes = childNode.getChildNodes();
        Collection raceCollection = new LinkedHashSet();
        try
        {
            for(int p = 0; p < subChildNodes.getLength(); p++)
            {
                if(subChildNodes.item(p).getNodeName().equals("race"))
                {
                    Element ele = (Element)subChildNodes.item(p);
                    String raceId = getNodeValue(ele, "id");
                    String raceName = PropertyHandler.getValue(raceId);
                    Object raceInstance = ParticipantManagerUtility.getRaceInstance();
                    IRace race = (IRace)raceInstance;
                    race.setRaceName(raceName);
                    race.setParticipant(partcipantObj);
                    raceCollection.add(race);
                }
            }

        }
        catch(Exception e)
        {
            Logger.out.info(e.getMessage());
        }
        return raceCollection;
    }

    private void processPartiMedIdColl(Collection partiMedIdColl, IParticipant partcipantObj)
    {
        IParticipantMedicalIdentifier partMedIdOld = null;
        Iterator itreratorNew = null;
        boolean MRNNotFound = false;
        int i = 0;
        ArrayList oldMrnIdList = new ArrayList();
        if(partiMedIdColl != null && !partiMedIdColl.isEmpty())
        {
            Collection partiMedIdCollOld = partcipantObj.getParticipantMedicalIdentifierCollection();
            if(partiMedIdCollOld != null && !partiMedIdCollOld.isEmpty())
            {
                Iterator itreratorOld = partiMedIdCollOld.iterator();
                do
                {
                    if(!itreratorOld.hasNext())
                    {
                        break;
                    }
                    MRNNotFound = false;
                    partMedIdOld = (IParticipantMedicalIdentifier)itreratorOld.next();
                    if(partMedIdOld.getMedicalRecordNumber() != null && partMedIdOld.getSite() != null)
                    {
                        String oldMRN = partMedIdOld.getMedicalRecordNumber();
                        ISite site = (ISite)partMedIdOld.getSite();
                        Long oldSiteID = site.getId();
                        itreratorNew = partiMedIdColl.iterator();
                        do
                        {
                            if(!itreratorNew.hasNext())
                            {
                                break;
                            }
                            IParticipantMedicalIdentifier partiMedIdNew = (IParticipantMedicalIdentifier)itreratorNew.next();
                            ISite siteNew = (ISite)partiMedIdNew.getSite();
                            Long oldSiteIDNew = site.getId();
                            if(oldMRN.equals(partiMedIdNew.getMedicalRecordNumber()) && oldSiteID.equals(oldSiteIDNew))
                            {
                                partiMedIdNew.setId(partMedIdOld.getId());
                                MRNNotFound = true;
                            }
                        } while(true);
                    }
                    if(!MRNNotFound)
                    {
                        oldMrnIdList.add(i, new Long(partMedIdOld.getId().longValue()));
                        i++;
                    }
                } while(true);
            }
        }
        i = 0;
        if(partiMedIdColl != null)
        {
            Iterator iterator1 = partiMedIdColl.iterator();
            do
            {
                if(!iterator1.hasNext())
                {
                    break;
                }
                IParticipantMedicalIdentifier participantMedicalIdentifierNew = (IParticipantMedicalIdentifier)iterator1.next();
                if(participantMedicalIdentifierNew.getId() == null)
                {
                    if(i < oldMrnIdList.size())
                    {
                        participantMedicalIdentifierNew.setId(Long.valueOf(((Long)oldMrnIdList.get(i)).longValue()));
                    }
                    i++;
                }
            } while(true);
        }
    }

    private void processDomographicXML(Element docEle)
        throws BizLogicException
    {
        IParticipantMedicalIdentifier participantMedicalIdentifier = null;
        partiMedIdColl = new LinkedHashSet();
        String facilityId = "";
        String clinPortalId = null;
        String mrn = "";
        NodeList attributeGroup = docEle.getElementsByTagName("attributeGroup");
        for(int h = 0; h < attributeGroup.getLength(); h++)
        {
            NodeList subChildNodes = attributeGroup.item(h).getChildNodes();
            for(int p = 0; p < subChildNodes.getLength(); p++)
            {
                if(!"attribute".equals(subChildNodes.item(p).getNodeName()))
                {
                    continue;
                }
                Element ele = (Element)subChildNodes.item(p);
                String element = getNodeValue(ele, "element");
                String value = getNodeValue(ele, "value");
                if("FacilityID".equals(element))
                {
                    facilityId = value;
                }
                if("MRN".equals(element))
                {
                    mrn = value;
                }
            }

            if("6B".equals(facilityId))
            {
                clinPortalId = mrn;
            } else
            {
                participantMedicalIdentifier = ParticipantManagerUtility.getParticipantMedicalIdentifierObj(mrn, facilityId);
                partiMedIdColl.add(participantMedicalIdentifier);
            }
            facilityId = "";
            mrn = "";
        }

        setPartiMedIdColl(partiMedIdColl);
        setParticipantId(clinPortalId);
    }

    private SessionDataBean getSessionDataBean(IUser validUser, String loginName)
    {
        SessionDataBean sessionData = new SessionDataBean();
        Long userId = validUser.getId();
        boolean adminUser = false;
        if("1".equalsIgnoreCase(validUser.getRoleId()))
        {
            adminUser = true;
        }
        sessionData.setAdmin(adminUser);
        sessionData.setUserName(loginName);
        sessionData.setUserId(userId);
        sessionData.setFirstName(validUser.getFirstName());
        sessionData.setLastName(validUser.getLastName());
        sessionData.setCsmUserId(validUser.getCsmUserId().toString());
        return sessionData;
    }

    private IUser getUser(String loginName, String activityStatus)
        throws BizLogicException
    {
        String getActiveUser = (new StringBuilder()).append("from edu.wustl.clinportal.domain.User user where user.activityStatus= '").append(activityStatus).append("' and user.loginName =").append("'").append(loginName).append("'").toString();
        DefaultBizLogic bizlogic = new DefaultBizLogic();
        List users = bizlogic.executeQuery(getActiveUser);
        if(users != null && !users.isEmpty())
        {
            IUser validUser = (IUser)users.get(0);
            return validUser;
        } else
        {
            return null;
        }
    }
}
