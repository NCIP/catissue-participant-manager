
package edu.wustl.common.participant.listener;

import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

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

import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.bizlogic.DefaultBizLogic;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.participant.bizlogic.CommonParticipantBizlogic;
import edu.wustl.common.participant.bizlogic.EMPIParticipantRegistrationBizLogic;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.IRace;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.domain.IUser;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.query.generator.ColumnValueBean;
import edu.wustl.patientLookUp.util.PatientLookupException;
import edu.wustl.patientLookUp.util.PropertyHandler;

// TODO: Auto-generated Javadoc
/**
 * * @author geeta_jaggal
 *
 * The listener interface for receiving EMPIParticipant events.
 * The class that is interested in processing a EMPIParticipant
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addEMPIParticipantListener<code> method. When
 * the EMPIParticipant event occurs, that object's appropriate
 * method is invoked.
 *
 * @see EMPIParticipantEvent
 */
public class EMPIParticipantListener implements MessageListener
{

	/** The logger. */
	private static final Logger logger = Logger.getCommonLogger(EMPIParticipantListener.class);

	/** The document. */
	private Document document;

	/** The parti med id coll. */
	private Collection partiMedIdColl;

	/** The participant id. */
	public String participantId;

	/**
	 * Instantiates a new eMPI participant listener.
	 */
	public EMPIParticipantListener()
	{
		document = null;
	}

	/**
	 * Gets the parti med id coll.
	 *
	 * @return the parti med id coll
	 */
	public Collection getPartiMedIdColl()
	{
		return partiMedIdColl;
	}

	/**
	 * Sets the parti med id coll.
	 *
	 * @param partiMedIdColl the new parti med id coll
	 */
	public void setPartiMedIdColl(Collection partiMedIdColl)
	{
		this.partiMedIdColl = partiMedIdColl;
	}

	/**
	 * Gets the participant id.
	 *
	 * @return the participant id
	 */
	public String getParticipantId()
	{
		return participantId;
	}

	/**
	 * Sets the participant id.
	 *
	 * @param participantId the new participant id
	 */
	public void setParticipantId(String participantId)
	{
		this.participantId = participantId;
	}

	/**
	 * Gets the document.
	 *
	 * @return the document
	 */
	public Document getDocument()
	{
		return document;
	}

	/**
	 * Sets the document.
	 *
	 * @param document the new document
	 */
	public void setDocument(Document document)
	{
		this.document = document;
	}

	/* (non-Javadoc)
	 * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
	 */
	public void onMessage(Message message)
	{
		String personDemoGraphics = null;
		try
		{
			if (message instanceof TextMessage)
			{
				personDemoGraphics = ((TextMessage) message).getText();
				logger.info(" Received demographics message \n \n");
				logger.info(personDemoGraphics);
				updateParticipantWithEMPIDetails(personDemoGraphics);

			}
		}
		catch (Exception e)
		{
			logger.info(e.getCause());
			logger.info(e.getMessage());
		}
	}

	/**
	 * Process domographic xml.
	 *
	 * @param personDemoGraphics the person demo graphics
	 *
	 * @throws Exception the exception
	 */
	public void updateParticipantWithEMPIDetails(String personDemoGraphics) throws Exception
	{

		String clinPortalId = null;
		String sourceObjectName = null;
		Element docEle = null;
		IUser validUser = null;
		String loginName = null;
		IParticipant partcipantObj = null;

		String oldParticipantId = null;
		String oldEMPIID = null;
		SessionDataBean sessionData = null;
		boolean isGenerateMgrMessage = false;
		if ("clinportal".equals(ParticipantManagerUtility.applicationType()))
		{
			sourceObjectName = "edu.wustl.clinportal.domain.Participant";
		}
		else
		{
			sourceObjectName = "edu.wustl.catissue.domain.Participant";
		}

		document = getDocument(personDemoGraphics);
		docEle = document.getDocumentElement();
		parseDomographicXML(docEle);
		clinPortalId = getParticipantId();
		String permanentId = getPermanentId(clinPortalId);
		oldParticipantId = clinPortalId;
		if (permanentId != null && permanentId != "")
		{
			isGenerateMgrMessage = true;
			clinPortalId = permanentId;
		}
		DefaultBizLogic bizlogic = new DefaultBizLogic();
		partcipantObj = (IParticipant) bizlogic.retrieve(sourceObjectName, Long.valueOf(Long
				.parseLong(clinPortalId)));
		oldEMPIID = String.valueOf(partcipantObj.getEmpiId());

		loginName = XMLPropertyHandler.getValue(Constants.HL7_LISTENER_ADMIN_USER);
		//loginName = Constants.CLINPORTAL_EMPI_ADMIN_LOGIN_ID;
		validUser = getUser(loginName, Constants.ACTIVITY_STATUS_ACTIVE);

		if (validUser != null)
		{
			sessionData = getSessionDataBean(validUser);
			updateParticipant(docEle, partcipantObj, sessionData);
			if (isGenerateMgrMessage)
			{
				EMPIParticipantRegistrationBizLogic eMPIPartiReg = new EMPIParticipantRegistrationBizLogic();
				eMPIPartiReg.sendMergeMessage(partcipantObj, oldParticipantId, oldEMPIID);
			}
		}
		else
		{
			checkUserAccount(loginName);
		}
	}

	/**
	 * Gets the permanent id.
	 *
	 * @param clinPortalId the clin portal id
	 *
	 * @return the permanent id
	 *
	 * @throws DAOException the DAO exception
	 * @throws SQLException the SQL exception
	 */
	private String getPermanentId(String clinPortalId) throws DAOException, SQLException
	{
		String permanentId = null;

		JDBCDAO jdbcdao = null;
		ResultSet result = null;
		try
		{
			jdbcdao = ParticipantManagerUtility.getJDBCDAO();
			LinkedList columnValueBeanList = columnValueBeanList = new LinkedList();
			columnValueBeanList.add(new ColumnValueBean(clinPortalId));
			String query = (new StringBuilder()).append(
			"SELECT PERMANENT_PARTICIPANT_ID FROM PARTICIPANT_EMPI_ID_MAPPING WHERE TEMPARARY_PARTICIPANT_ID=?")
			.toString();

			result = jdbcdao
					.getResultSet(query, columnValueBeanList, null);
			if (result != null)
			{
				while (result.next())
				{
					permanentId = result.getString("permanent_participant_id");
				}
			}

		}
		finally
		{
			result.close();
			jdbcdao.closeSession();
		}
		return permanentId;
	}

	/**
	 * Update participant.
	 *
	 * @param docEle the doc ele
	 * @param partcipantObj the partcipant obj
	 * @param sessionData the session data
	 *
	 * @throws PatientLookupException the patient lookup exception
	 * @throws BizLogicException the biz logic exception
	 * @throws DAOException the DAO exception
	 */
	private void updateParticipant(Element docEle, IParticipant partcipantObj,
			SessionDataBean sessionData) throws PatientLookupException, BizLogicException,
			DAOException
	{
		IParticipant participant = null;
		String gender = null;
		Collection partiMedicalIdColl = null;
		if (partcipantObj != null)
		{
			String personUpi = docEle.getElementsByTagName("personUpi").item(0).getFirstChild()
					.getNodeValue();
			NodeList childNodeList = docEle.getElementsByTagName("demographics").item(0)
					.getChildNodes();
			for (int i = 0; i < childNodeList.getLength(); i++)
			{
				if ((Constants.UNSPECIFIED.equals(partcipantObj.getGender()) || Constants.UNKNOWN
						.equals(partcipantObj.getGender()))
						&& Constants.EMPI_DEMOGRAPHIC_XML_GENDER.equals(childNodeList.item(i)
								.getNodeName()))
				{
					Element ele = (Element) childNodeList.item(i);
					String value = getNodeValue(ele, "id");
					gender = PropertyHandler.getValue(value);
					partcipantObj.setGender(gender);
				}
				if (Constants.EMPI_DEMOGRAPHIC_XML_RACE_COLLECTION.equals(childNodeList.item(i)
						.getNodeName()))
				{
					setRaceCollection(partcipantObj, childNodeList.item(i));
				}
			}
			partiMedicalIdColl = getPartiMedIdColl();
			processPartiMedIdColl(partiMedicalIdColl, partcipantObj);
			participant = partcipantObj;
			partcipantObj.setEmpiId(personUpi);
			partcipantObj.setEmpiIdStatus(Constants.EMPI_ID_CREATED);
			partcipantObj.setParticipantMedicalIdentifierCollection(partiMedicalIdColl);
			CommonParticipantBizlogic bizlogic = new CommonParticipantBizlogic();
			bizlogic.update(partcipantObj, participant, sessionData);

			logger.info("\n\n\n\n\nPARTIICPANT SUCCESSFULLY UPDATED WITH  EMPI \n\n\n\n\n");
		}
	}

	/**
	 * Sets the race collection.
	 *
	 * @param partcipantObj the partcipant obj
	 * @param childNode the child node
	 */
	private void setRaceCollection(IParticipant partcipantObj, Node childNode)
	{
		label0 :
		{
			Collection raceCollection = null;
			if (partcipantObj.getRaceCollection() == null
					|| partcipantObj.getRaceCollection().isEmpty())
			{
				break label0;
			}
			Iterator itr = partcipantObj.getRaceCollection().iterator();
			IRace race;
			do
			{
				if (!itr.hasNext())
				{
					break label0;
				}
				race = (IRace) itr.next();
			}
			while (!Constants.UNKNOWN.equalsIgnoreCase(race.getRaceName())
					&& !Constants.NOT_REPORTED.equals(race.getRaceName())
					&& !Constants.NOTSPECIFIED.equals(race.getRaceName()));
			raceCollection = getRaceCollection(childNode, partcipantObj);
			partcipantObj.setRaceCollection(raceCollection);
		}
	}

	/**
	 * Gets the document.
	 *
	 * @param hl7MessageFromQueue the hl7 message from queue
	 *
	 * @return the document
	 *
	 * @throws ParserConfigurationException the parser configuration exception
	 * @throws SAXException the SAX exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private Document getDocument(String hl7MessageFromQueue) throws ParserConfigurationException,
			SAXException, IOException
	{
		Document document = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbf.newDocumentBuilder();
		document = docBuilder.parse(new InputSource(new StringReader(hl7MessageFromQueue)));
		setDocument(document);
		return document;
	}

	/**
	 * Gets the node value.
	 *
	 * @param ele the ele
	 * @param nodeName the node name
	 *
	 * @return the node value
	 */
	public String getNodeValue(Element ele, String nodeName)
	{
		NodeList list1 = ele.getElementsByTagName(nodeName);
		Element element1 = (Element) list1.item(0);
		return element1.getFirstChild().getNodeValue();
	}

	/**
	 * Gets the race collection.
	 *
	 * @param childNode the child node
	 * @param partcipantObj the partcipant obj
	 *
	 * @return the race collection
	 */
	private Collection getRaceCollection(Node childNode, IParticipant partcipantObj)
	{
		NodeList subChildNodes = childNode.getChildNodes();
		Collection raceCollection = new LinkedHashSet();
		try
		{
			for (int p = 0; p < subChildNodes.getLength(); p++)
			{
				if (subChildNodes.item(p).getNodeName().equals("race"))
				{
					Element ele = (Element) subChildNodes.item(p);
					String raceId = getNodeValue(ele, "id");
					String raceName = PropertyHandler.getValue(raceId);
					Object raceInstance = ParticipantManagerUtility.getRaceInstance();
					IRace race = (IRace) raceInstance;
					race.setRaceName(raceName);
					race.setParticipant(partcipantObj);
					raceCollection.add(race);
				}
			}

		}
		catch (Exception e)
		{
			Logger.out.info(e.getMessage());
		}
		return raceCollection;
	}

	/**
	 * Process parti med id coll.
	 *
	 * @param partiMedIdColl the parti med id coll
	 * @param partcipantObj the partcipant obj
	 */
	private void processPartiMedIdColl(Collection partiMedIdColl, IParticipant partcipantObj)
	{
		IParticipantMedicalIdentifier partMedIdOld = null;
		Iterator itreratorNew = null;
		boolean MRNNotFound = false;
		int count = 0;
		ArrayList oldMrnIdList = new ArrayList();
		if (partiMedIdColl != null && !partiMedIdColl.isEmpty())
		{
			Collection partiMedIdCollOld = partcipantObj
					.getParticipantMedicalIdentifierCollection();
			if (partiMedIdCollOld != null && !partiMedIdCollOld.isEmpty())
			{
				Iterator itreratorOld = partiMedIdCollOld.iterator();
				do
				{
					if (!itreratorOld.hasNext())
					{
						break;
					}
					MRNNotFound = false;
					partMedIdOld = (IParticipantMedicalIdentifier) itreratorOld.next();
					if (partMedIdOld.getMedicalRecordNumber() != null
							&& partMedIdOld.getSite() != null)
					{
						String oldMRN = partMedIdOld.getMedicalRecordNumber();
						ISite site = (ISite) partMedIdOld.getSite();
						Long oldSiteID = site.getId();
						itreratorNew = partiMedIdColl.iterator();
						do
						{
							if (!itreratorNew.hasNext())
							{
								break;
							}
							IParticipantMedicalIdentifier partiMedIdNew = (IParticipantMedicalIdentifier) itreratorNew
									.next();
							ISite siteNew = (ISite) partiMedIdNew.getSite();
							Long oldSiteIDNew = site.getId();
							if (oldMRN.equals(partiMedIdNew.getMedicalRecordNumber())
									&& oldSiteID.equals(oldSiteIDNew))
							{
								partiMedIdNew.setId(partMedIdOld.getId());
								MRNNotFound = true;
							}
						}
						while (true);
					}
					if (!MRNNotFound)
					{
						oldMrnIdList.add(count, new Long(partMedIdOld.getId().longValue()));
						count++;
					}
				}
				while (true);
			}
		}
		count = 0;
		if (partiMedIdColl != null)
		{
			Iterator iterator1 = partiMedIdColl.iterator();
			do
			{
				if (!iterator1.hasNext())
				{
					break;
				}
				IParticipantMedicalIdentifier partiMediIdNew = (IParticipantMedicalIdentifier) iterator1
						.next();
				if (partiMediIdNew.getId() == null)
				{
					if (count < oldMrnIdList.size())
					{
						partiMediIdNew.setId(Long.valueOf(((Long) oldMrnIdList
								.get(count)).longValue()));
					}
					count++;
				}
			}
			while (true);
		}
	}

	/**
	 * Process domographic xml.
	 *
	 * @param docEle the doc ele
	 *
	 * @throws Exception the exception
	 */
	private void parseDomographicXML(Element docEle) throws Exception
	{
		IParticipantMedicalIdentifier participantMedicalIdentifier = null;
		partiMedIdColl = new LinkedHashSet();
		String facilityId = "";
		String clinPortalId = null;
		String mrn = "";
		NodeList attributeGroup = docEle.getElementsByTagName("attributeGroup");
		for (int h = 0; h < attributeGroup.getLength(); h++)
		{
			NodeList subChildNodes = attributeGroup.item(h).getChildNodes();
			for (int p = 0; p < subChildNodes.getLength(); p++)
			{
				if (!"attribute".equals(subChildNodes.item(p).getNodeName()))
				{
					continue;
				}
				Element ele = (Element) subChildNodes.item(p);
				String element = getNodeValue(ele, "element");
				String value = getNodeValue(ele, "value");
				if ("FacilityID".equals(element))
				{
					facilityId = value;
				}
				if ("MRN".equals(element))
				{
					mrn = value;
				}
			}

			if (Constants.CLINPORTAL_FACILITY_ID.equals(facilityId))
			{
				clinPortalId = mrn;
			}
			else
			{
				participantMedicalIdentifier = ParticipantManagerUtility
						.getParticipantMedicalIdentifierObj(mrn, facilityId);
				if (participantMedicalIdentifier != null)
				{
					partiMedIdColl.add(participantMedicalIdentifier);
				}
			}
			facilityId = "";
			mrn = "";
		}

		setPartiMedIdColl(partiMedIdColl);
		setParticipantId(clinPortalId);
	}

	/**
	 * Gets the session data bean.
	 *
	 * @param validUser the valid user
	 *
	 * @return the session data bean
	 */
	private SessionDataBean getSessionDataBean(IUser validUser)
	{
		SessionDataBean sessionData = new SessionDataBean();
		sessionData.setAdmin(validUser.getAdminuser());
		sessionData.setUserName(validUser.getLoginName());
		sessionData.setUserId(validUser.getId());
		sessionData.setFirstName(validUser.getFirstName());
		sessionData.setLastName(validUser.getLastName());
		sessionData.setCsmUserId(validUser.getCsmUserId().toString());
		return sessionData;
	}

	/**
	 * Check user account.
	 *
	 * @param loginName the login name
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws Exception the exception
	 */
	private void checkUserAccount(String loginName) throws BizLogicException, Exception
	{
		if (getUser(loginName, Constants.ACTIVITY_STATUS_CLOSED) != null)
		{
			throw new Exception((new StringBuilder()).append(loginName).append(
					" Closed user. Sending back to the login Page").toString());
		}
		else
		{
			throw new Exception((new StringBuilder()).append(loginName).append(
					"Invalid user. Sending back to the login Page").toString());
		}
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
	 */
	private IUser getUser(String loginName, String activityStatus) throws BizLogicException
	{
		String getActiveUser = (new StringBuilder()).append(
				"from edu.wustl.clinportal.domain.User user where user.activityStatus= '").append(
				activityStatus).append("' and user.loginName =").append("'").append(loginName)
				.append("'").toString();
		DefaultBizLogic bizlogic = new DefaultBizLogic();
		List users = bizlogic.executeQuery(getActiveUser);
		if (users != null && !users.isEmpty())
		{
			IUser validUser = (IUser) users.get(0);
			return validUser;
		}
		else
		{
			return null;
		}
	}

}
