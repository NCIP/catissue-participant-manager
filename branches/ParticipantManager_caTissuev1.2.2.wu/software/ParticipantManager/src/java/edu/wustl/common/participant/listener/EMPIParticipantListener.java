
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
import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.participant.bizlogic.CommonParticipantBizlogic;
import edu.wustl.common.participant.bizlogic.EMPIParticipantRegistrationBizLogic;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.IRace;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.domain.IUser;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerException;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.query.generator.ColumnValueBean;
import edu.wustl.patientLookUp.util.PatientLookupException;
import edu.wustl.patientLookUp.util.PropertyHandler;

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
	private static final Logger LOGGER = Logger.getCommonLogger(EMPIParticipantListener.class);

	/** The document. */
	private Document document;

	/** The parti med id coll. */
	private Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> partiMedIdColl;

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
	public Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> getPartiMedIdColl()
	{
		return partiMedIdColl;
	}

	/**
	 * Sets the parti med id coll.
	 *
	 * @param partiMedIdColl the new parti med id coll
	 */
	public void setPartiMedIdColl(
			final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> partiMedIdColl)
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
	public void setParticipantId(final String participantId)
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
	public void setDocument(final Document document)
	{
		this.document = document;
	}

	/* (non-Javadoc)
	 * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
	 */
	public void onMessage(final Message message)
	{
		String personDemoGraphics = null;
		try
		{
			if (message instanceof TextMessage)
			{
				personDemoGraphics = ((TextMessage) message).getText();
				LOGGER.info(" Received demographics message \n \n");
				LOGGER.info(personDemoGraphics);
				updateParticipantWithEMPIDetails(personDemoGraphics);
			}
		}
		catch (Exception e)
		{
			LOGGER.info(e.getCause());
			LOGGER.info(e.getMessage());
		}
	}

	/**
	 * Process domographic xml.
	 *
	 * @param personDemoGraphics the person demo graphics
	 * @throws ParticipantManagerException
	 * @throws BizLogicException
	 *
	 * @throws Exception the exception
	 */
	public void updateParticipantWithEMPIDetails(final String personDemoGraphics)
	throws ApplicationException, ParticipantManagerException
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

		sourceObjectName = edu.wustl.common.participant.utility.PropertyHandler
		.getValue(Constants.PARTICIPANT_CLASS);

		try
		{
			document = getDocument(personDemoGraphics);

			docEle = document.getDocumentElement();
			parseDomographicXML(docEle);
			clinPortalId = getParticipantId();
			final String permanentId = getPermanentId(clinPortalId);
			oldParticipantId = clinPortalId;
			if (permanentId != null && !"".equals(permanentId))
			{
				isGenerateMgrMessage = true;
				clinPortalId = permanentId;
			}
			final DefaultBizLogic bizlogic = new DefaultBizLogic();
			partcipantObj = (IParticipant) bizlogic.retrieve(sourceObjectName, Long.valueOf(Long
					.parseLong(clinPortalId)));
			oldEMPIID = getOldEmpiId(clinPortalId);

			loginName = XMLPropertyHandler.getValue(Constants.HL7_LISTENER_ADMIN_USER);
			//loginName = Constants.CLINPORTAL_EMPI_ADMIN_LOGIN_ID;
			validUser = getUser(loginName, Constants.ACTIVITY_STATUS_ACTIVE);

			if (validUser != null)
			{
				sessionData = getSessionDataBean(validUser);
				updateParticipant(docEle, partcipantObj, sessionData);
				if (isGenerateMgrMessage)
				{
					final EMPIParticipantRegistrationBizLogic eMPIPartiReg = new EMPIParticipantRegistrationBizLogic();
					eMPIPartiReg.sendMergeMessage(partcipantObj, oldParticipantId, oldEMPIID);
				}
			}
			else
			{
				checkUserAccount(loginName);
			}

		}
		catch (PatientLookupException e)
		{
			// TODO Auto-generated catch block
			throw new ApplicationException(null, e, e.getMessage());
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			throw new ApplicationException(null, e, e.getMessage());
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
	private String getPermanentId(final String clinPortalId) throws DAOException, SQLException
	{
		String permanentId = null;

		JDBCDAO jdbcdao = null;
		ResultSet result = null;
		try
		{
			jdbcdao = ParticipantManagerUtility.getJDBCDAO();
			final LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
			columnValueBeanList.add(new ColumnValueBean(clinPortalId));
			final String query = "SELECT PERMANENT_PARTICIPANT_ID FROM PARTICIPANT_EMPI_ID_MAPPING WHERE TEMPARARY_PARTICIPANT_ID=?";

			result = jdbcdao.getResultSet(query, columnValueBeanList, null);
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
	 * @throws PatientLookupException
	 *
	 * @throws PatientLookupException the patient lookup exception
	 * @throws BizLogicException
	 * @throws BizLogicException the biz logic exception
	 * @throws DAOException the DAO exception
	 */
	private void updateParticipant(final Element docEle, final IParticipant partcipantObj,
			final SessionDataBean sessionData) throws PatientLookupException, BizLogicException
			{
		IParticipant participant = null;
		String gender = null;
		Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> partiMedIdColl = null;
		if (partcipantObj != null)
		{
			final String personUpi = docEle.getElementsByTagName("personUpi").item(0)
			.getFirstChild().getNodeValue();
			final NodeList childNodeList = docEle.getElementsByTagName("demographics").item(0)
			.getChildNodes();
			for (int i = 0; i < childNodeList.getLength(); i++)
			{
				if ((Constants.UNSPECIFIED.equals(partcipantObj.getGender()) || Constants.UNKNOWN
						.equals(partcipantObj.getGender()))
						&& Constants.EMPI_DEMOGRAPHIC_XML_GENDER.equals(childNodeList.item(i)
								.getNodeName()))
				{
					final Element ele = (Element) childNodeList.item(i);
					final String value = getNodeValue(ele, "id");
					gender = PropertyHandler.getValue(value);
					partcipantObj.setGender(gender);
				}
				if (Constants.EMPI_DEMOGRAPHIC_XML_RACE_COLLECTION.equals(childNodeList.item(i)
						.getNodeName()))
				{
					setRaceCollection(partcipantObj, childNodeList.item(i));
				}
			}
			partiMedIdColl = getPartiMedIdColl();
			processPartiMedIdColl(partiMedIdColl, partcipantObj);
			participant = partcipantObj;
			partcipantObj.setEmpiId(personUpi);
			partcipantObj.setEmpiIdStatus(Constants.EMPI_ID_CREATED);
			partcipantObj.setParticipantMedicalIdentifierCollection(partiMedIdColl);
			final CommonParticipantBizlogic bizlogic = new CommonParticipantBizlogic();
			bizlogic.update(partcipantObj, participant, sessionData);

			LOGGER.info("\n\n\n\n\nPARTIICPANT SUCCESSFULLY UPDATED WITH  EMPI \n\n\n\n\n");
		}
			}

	/**
	 * Sets the race collection.
	 *
	 * @param partcipantObj the partcipant obj
	 * @param childNode the child node
	 */
	private void setRaceCollection(final IParticipant partcipantObj, final Node childNode)
	{
		label0 :
		{
		Collection<IRace<IParticipant>> raceCollection = null;
		if (partcipantObj.getRaceCollection() == null
				|| partcipantObj.getRaceCollection().isEmpty())
		{
			break label0;
		}
		final Iterator itr = partcipantObj.getRaceCollection().iterator();
		IRace<IParticipant> race;
		do
		{
			if (!itr.hasNext())
			{
				break label0;
			}
			race = (IRace<IParticipant>) itr.next();
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
	private Document getDocument(final String hl7MessageFromQueue)
	throws ParserConfigurationException, SAXException, IOException
	{
		Document document = null;
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		final DocumentBuilder docBuilder = dbf.newDocumentBuilder();
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
	public String getNodeValue(final Element ele, final String nodeName)
	{
		final NodeList list1 = ele.getElementsByTagName(nodeName);
		final Element element1 = (Element) list1.item(0);
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
	private Collection<IRace<IParticipant>> getRaceCollection(final Node childNode,
			final IParticipant partcipantObj)
			{
		final NodeList subChildNodes = childNode.getChildNodes();
		final Collection<IRace<IParticipant>> raceCollection = new LinkedHashSet<IRace<IParticipant>>();
		try
		{
			for (int p = 0; p < subChildNodes.getLength(); p++)
			{
				if (subChildNodes.item(p).getNodeName().equals("race"))
				{
					final Element ele = (Element) subChildNodes.item(p);
					final String raceId = getNodeValue(ele, "id");
					final String raceName = PropertyHandler.getValue(raceId);
					final Object raceInstance = ParticipantManagerUtility.getRaceInstance();
					final IRace<IParticipant> race = (IRace<IParticipant>) raceInstance;
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
	private void processPartiMedIdColl(
			final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> partiMedIdColl,
			final IParticipant partcipantObj)
	{
		IParticipantMedicalIdentifier<IParticipant, ISite> partMedIdOld = null;
		Iterator itreratorNew = null;
		boolean MRNNotFound = false;
		int count = 0;
		final ArrayList<Long> oldMrnIdList = new ArrayList<Long>();
		if (partiMedIdColl != null && !partiMedIdColl.isEmpty())
		{
			final Collection partiMedIdCollOld = partcipantObj
			.getParticipantMedicalIdentifierCollection();
			if (partiMedIdCollOld != null && !partiMedIdCollOld.isEmpty())
			{
				final Iterator itreratorOld = partiMedIdCollOld.iterator();
				do
				{
					if (!itreratorOld.hasNext())
					{
						break;
					}
					MRNNotFound = false;
					partMedIdOld = (IParticipantMedicalIdentifier<IParticipant, ISite>) itreratorOld
					.next();
					if (partMedIdOld.getMedicalRecordNumber() != null
							&& partMedIdOld.getSite() != null)
					{
						final String oldMRN = partMedIdOld.getMedicalRecordNumber();
						final ISite site = partMedIdOld.getSite();
						final Long oldSiteID = site.getId();
						itreratorNew = partiMedIdColl.iterator();
						do
						{
							if (!itreratorNew.hasNext())
							{
								break;
							}
							final IParticipantMedicalIdentifier<IParticipant, ISite> partiMedIdNew = (IParticipantMedicalIdentifier<IParticipant, ISite>) itreratorNew
							.next();
							final Long oldSiteIDNew = site.getId();
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
						oldMrnIdList.add(count, Long.valueOf(partMedIdOld.getId().longValue()));
						count++;
					}
				}
				while (true);
			}
		}
		count = 0;
		if (partiMedIdColl != null)
		{
			final Iterator iterator1 = partiMedIdColl.iterator();
			do
			{
				if (!iterator1.hasNext())
				{
					break;
				}
				final IParticipantMedicalIdentifier<IParticipant, ISite> partiMediIdNew = (IParticipantMedicalIdentifier<IParticipant, ISite>) iterator1
				.next();
				if (partiMediIdNew.getId() == null)
				{
					if (count < oldMrnIdList.size())
					{
						partiMediIdNew.setId(Long.valueOf((oldMrnIdList.get(count)).longValue()));
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
	 * @throws BizLogicException
	 * @throws ParticipantManagerException
	 * @throws PatientLookupException
	 *
	 * @throws Exception the exception
	 */
	private void parseDomographicXML(final Element docEle) throws BizLogicException,
	ParticipantManagerException, PatientLookupException
	{
		IParticipantMedicalIdentifier<IParticipant, ISite> participantMedicalIdentifier = null;
		partiMedIdColl = new LinkedHashSet<IParticipantMedicalIdentifier<IParticipant, ISite>>();
		String facilityId = "";
		String clinPortalId = null;
		String mrn = "";
		final NodeList attributeGroup = docEle.getElementsByTagName("attributeGroup");
		for (int h = 0; h < attributeGroup.getLength(); h++)
		{
			final NodeList subChildNodes = attributeGroup.item(h).getChildNodes();
			for (int p = 0; p < subChildNodes.getLength(); p++)
			{
				if (!"attribute".equals(subChildNodes.item(p).getNodeName()))
				{
					continue;
				}
				final Element ele = (Element) subChildNodes.item(p);
				final String element = getNodeValue(ele, "element");
				final String value = getNodeValue(ele, "value");
				if ("FacilityID".equals(element))
				{
					facilityId = value;
				}
				if ("MRN".equals(element))
				{
					mrn = value;
				}
			}
			//			changes by amol
			//			will get the value of facilityId from participantManager.properties file
			final String facilityIdValue;
			if(PropertyHandler.getValue(Constants.Facility_ID)!=null){
				facilityIdValue = PropertyHandler.getValue(Constants.Facility_ID);
			}else{
				facilityIdValue = Constants.CLINPORTAL_FACILITY_ID ;
			}
			//			if (Constants.CLINPORTAL_FACILITY_ID.equals(facilityId))
			if (facilityIdValue.equals(facilityId))
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
	private SessionDataBean getSessionDataBean(final IUser validUser)
	{
		final SessionDataBean sessionData = new SessionDataBean();
		sessionData.setAdmin(isAdminUser(validUser.getRoleId()));
		sessionData.setUserName(validUser.getLoginName());
		sessionData.setUserId(validUser.getId());
		sessionData.setFirstName(validUser.getFirstName());
		sessionData.setLastName(validUser.getLastName());
		sessionData.setCsmUserId(validUser.getCsmUserId().toString());
		return sessionData;
	}

	private boolean isAdminUser(final String userRole)
	{
		boolean adminUser;
		if (userRole.equalsIgnoreCase(Constants.ADMIN_USER))
		{
			adminUser = true;
		}
		else
		{
			adminUser = false;
		}
		return adminUser;
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
	private void checkUserAccount(final String loginName) throws BizLogicException, Exception
	{
		if (getUser(loginName, Constants.ACTIVITY_STATUS_CLOSED) != null)
		{
			throw new Exception(loginName + " Closed user. Sending back to the login Page");
		}
		else
		{
			throw new Exception(loginName + "Invalid user. Sending back to the login Page");
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
	 * @throws ParticipantManagerException
	 */
	private IUser getUser(final String loginName, final String activityStatus)
	throws BizLogicException, ParticipantManagerException
	{
		IUser validUser = null;
		String userClassName = edu.wustl.common.participant.utility.PropertyHandler
		.getValue(Constants.USER_CLASS);
		final String getActiveUser = "from " + userClassName + " user where user.activityStatus= '"
		+ activityStatus + "' and user.loginName =" + "'" + loginName + "'";
		final DefaultBizLogic bizlogic = new DefaultBizLogic();
		final List users = bizlogic.executeQuery(getActiveUser);
		if (users != null && !users.isEmpty())
		{
			validUser = (IUser) users.get(0);
		}
		return validUser;
	}

	/**
	 * Gets the old empi id.
	 *
	 * @param clinPortalId the clin portal id
	 *
	 * @return the permanent id
	 *
	 * @throws DAOException the DAO exception
	 * @throws SQLException the SQL exception
	 */
	private String getOldEmpiId(final String clinPortalId) throws DAOException, SQLException
	{
		String oldEmpiID = null;
		JDBCDAO jdbcdao = null;
		try
		{
			jdbcdao = ParticipantManagerUtility.getJDBCDAO();
			final LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
			columnValueBeanList.add(new ColumnValueBean(clinPortalId));
			final String query = "SELECT * FROM PARTICIPANT_EMPI_ID_MAPPING WHERE PERMANENT_PARTICIPANT_ID=? ORDER BY TEMPMRNDATE DESC";

			List<Object> idList = jdbcdao.executeQuery(query, null, columnValueBeanList);
			if (null != idList && idList.size() > 0)
			{

				if (null != idList.get(0))
				{
					Object obj = idList.get(0);
					oldEmpiID = ((ArrayList) obj).get(2).toString();

				}
			}

		}
		finally
		{

			jdbcdao.closeSession();
		}
		return oldEmpiID;
	}

}
