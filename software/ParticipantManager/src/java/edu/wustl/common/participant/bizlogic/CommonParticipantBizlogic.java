
package edu.wustl.common.participant.bizlogic;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.codec.language.Metaphone;

import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.exception.ErrorKey;
import edu.wustl.common.lookup.DefaultLookupResult;
import edu.wustl.common.participant.dao.CommonParticipantDAO;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.IRace;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.domain.IUser;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerException;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.common.util.global.CommonServiceLocator;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.patientLookUp.domain.PatientInformation;

/**
 * The Class CommonParticipantBizlogic.
 */
public class CommonParticipantBizlogic extends CommonDefaultBizLogic
{

	/** The Constant logger. */
	private static final Logger logger = Logger.getCommonLogger(CommonParticipantBizlogic.class);

	private CommonParticipantDAO participantDao = new CommonParticipantDAO(CommonServiceLocator
			.getInstance().getAppName(), null);;

	public CommonParticipantBizlogic()
	{
	}

	public CommonParticipantBizlogic(SessionDataBean sessionDataBean)
	{
		participantDao.setSessionDataBean(sessionDataBean);
	}

	/**
	 * Insert.
	 *
	 * @param obj
	 *            Participant Object
	 * @param dao
	 *            DAo Object
	 * @param ParticipantMedicalIdentifier
	 *            Object
	 *
	 * @return the i participant
	 *
	 * @throws BizLogicException
	 *             the biz logic exception
	 * @throws DAOException
	 *             the DAO exception
	 */
	public IParticipant insert(final IParticipant participant,
			final IParticipantMedicalIdentifier pmi) throws BizLogicException
	{
		//final IParticipant participant = (IParticipant) obj;
		try
		{
			setMetaPhoneCode(participant);
			Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> pmiCollection = participant
					.getParticipantMedicalIdentifierCollection();
			if (pmiCollection == null)
			{
				pmiCollection = new LinkedHashSet<IParticipantMedicalIdentifier<IParticipant, ISite>>();
			}
			if (pmiCollection.isEmpty())
			{
				pmi.setMedicalRecordNumber(null);
				pmi.setSite(null);
				pmiCollection.add(pmi);
			}
			checkForSiteIdentifierInPMI(pmiCollection);
			final Iterator<IParticipantMedicalIdentifier<IParticipant, ISite>> iterator = pmiCollection
					.iterator();
			while (iterator.hasNext())
			{
				final IParticipantMedicalIdentifier<IParticipant, ISite> pmIdentifier = iterator
						.next();
				pmIdentifier.setParticipant(participant);
			}
			participantDao.insert(participant);
		}
		catch (DAOException daoExp)
		{
			logger.error(daoExp.getMessage(), daoExp);
			throw new BizLogicException(daoExp);
		}
		return participant;
	}

	/**
	 * For Bulk Operations: retrieving site_id from site_name. Check For Site
	 * Identifier In PMI.
	 *
	 * @param dao
	 *            DAO
	 * @param pmiCollection
	 *            Collection of ParticipantMedicalIdentifier
	 * @throws DAOException
	 *             DAOException
	 * @throws BizLogicException
	 *             BizLogicException
	 */
	private void checkForSiteIdentifierInPMI(
			final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> pmiCollection)
			throws DAOException, BizLogicException
	{
		final Iterator<IParticipantMedicalIdentifier<IParticipant, ISite>> pmiIterator = pmiCollection
				.iterator();
		while (pmiIterator.hasNext())
		{
			final IParticipantMedicalIdentifier<IParticipant, ISite> pmIdentifier = pmiIterator
					.next();
			if (pmIdentifier.getSite() != null && pmIdentifier.getSite().getId() == null
					&& pmIdentifier.getSite().getName() != null)
			{
				final ISite site = pmIdentifier.getSite();
				Long siteId = null;
				try
				{
					siteId = ParticipantManagerUtility.getParticipantMgrImplObj().getSiteIdByName(site.getName());
				}
				catch(Exception appExp)
				{
					logger.error("Exception occurred in checkForSiteIdentifierInPMI" +appExp.getMessage());
					throw new BizLogicException(null,appExp,appExp.getMessage());
				}

				if (siteId != null)
				{
					site.setId(siteId);
				}
				else
				{
					throw new BizLogicException(ErrorKey.getErrorKey("invalid.site.name"), null,
							site.getName());
				}
			}
		}
	}

	/**
	 * Updates the persistent object in the database.
	 *
	 * @param dao
	 *            - DAO object
	 * @param participant
	 *            the participant
	 * @param oldParticipant
	 *            the old participant
	 *
	 * @throws BizLogicException
	 *             throws BizLogicException
	 * @throws DAOException
	 *             the DAO exception
	 */
	public void update(final IParticipant participant, final IParticipant oldParticipant)
			throws BizLogicException
	{
		setMetaPhoneCode(participant);
		try
		{
			participantDao.update(participant, oldParticipant);
		}
		catch (DAOException daoExp)
		{
			logger.error(daoExp.getMessage(), daoExp);
			throw new BizLogicException(daoExp);
		}
	}

	/**
	 * Sets the meta phone code.
	 *
	 * @param participant
	 *            the new meta phone code
	 */
	private void setMetaPhoneCode(final IParticipant participant)
	{
		final Metaphone metaPhoneObj = new Metaphone();
		final String lNameMetaPhone = metaPhoneObj.metaphone(participant.getLastName());
		participant.setMetaPhoneCode(lNameMetaPhone);
	}

	//	/**
	//	 * Update ParticipantMedicalIdentifier.
	//	 *
	//	 * @param dao
	//	 *            DAo Object
	//	 * @param pmIdentifier
	//	 *            ParticipantMedicalIdentifier Identifier
	//	 * @throws DAOException
	//	 *             the DAO exception
	//	 */
	//	public static void updatePMI(
	//			final DAO dao,
	//			final IParticipantMedicalIdentifier<IParticipant, ISite> pmIdentifier)
	//			throws DAOException {
	//		if (pmIdentifier.getId() != null) {
	//			dao.update(pmIdentifier);
	//		} else if (pmIdentifier.getId() == null
	//				|| pmIdentifier.getId().equals("")) {
	//			dao.insert(pmIdentifier);
	//		}
	//	}

	//	/**
	//	 * Modify participant object.
	//	 *
	//	 * @param dao
	//	 *            the dao
	//	 * @param sessionDataBean
	//	 *            the session data bean
	//	 * @param participant
	//	 *            the participant
	//	 *
	//	 * @throws BizLogicException
	//	 *             the biz logic exception
	//	 */
	//	public void modifyParticipantObject(final DAO dao,
	//			final SessionDataBean sessionDataBean,
	//			final IParticipant participant, final IParticipant oldParticipant)
	//			throws BizLogicException {
	//		try {
	//			updateParticipant(dao, sessionDataBean, participant, oldParticipant);
	//		} catch (DAOException e) {
	//			throw new BizLogicException(e);
	//		} catch (BizLogicException biz) {
	//			logger.debug(biz.getMessage(), biz);
	//			throw getBizLogicException(biz, biz.getErrorKeyName(), biz
	//					.getMsgValues());
	//
	//		} catch (Exception exception) {
	//			throw getBizLogicException(exception,
	//					"Error while updating object", "");
	//		}
	//	}

	//	/**
	//	 * This method will update Participant Object.
	//	 *
	//	 * @param participant
	//	 *            Participant object
	//	 * @param oldParticipant
	//	 *            Persistent participant object
	//	 * @param dao
	//	 *            DAO Object
	//	 * @param sessionDataBean
	//	 *            SessionDataBean Object
	//	 *
	//	 * @return AuditManager
	//	 *
	//	 * @throws BizLogicException
	//	 *             BizLogicException Exception
	//	 * @throws DAOException
	//	 *             DAOException Exception
	//	 * @throws AuditException
	//	 *             AuditException Exception
	//	 */
	//	private void updateParticipant(final DAO dao,
	//			final SessionDataBean sessionDataBean,
	//			final IParticipant participant, final IParticipant oldParticipant)
	//			throws BizLogicException, DAOException {
	//		update(dao, participant, oldParticipant);
	//	}

	/**
	 * check not null.
	 *
	 * @param object
	 *            object
	 *
	 * @return boolean
	 */

	public boolean isNullobject(Object object)
	{
		boolean result = true;
		if (object != null)
		{
			result = false;
		}
		return result;
	}

	/**
	 * Sets the participant medical identifier default.
	 *
	 * @param partMedIdentifier
	 *            the part med identifier
	 *
	 * @throws BizLogicException
	 *             the biz logic exception
	 * @throws ParticipantManagerException
	 */
	public void setParticipantMedicalIdentifierDefault(
			IParticipantMedicalIdentifier<IParticipant, ISite> partMedIdentifier)
			throws BizLogicException, ParticipantManagerException
	{
		if (isNullobject(partMedIdentifier.getSite()))
		{
			final ISite site = (ISite) ParticipantManagerUtility.getSiteInstance();
			partMedIdentifier.setSite(site);
		}
	}

	/**
	 * Post insert.
	 *
	 * @param obj
	 *            the obj
	 * @param sessionDataBean
	 *            the session data bean
	 * @throws ApplicationException
	 */
	public void postInsert(final Object obj, LinkedHashSet<Long> userIdSet)
			throws BizLogicException
	{
		final IParticipant participant = (IParticipant) obj;
		// if for CS eMPI is enable then set the eMPI status as pending if its
		// eligible
		if (ParticipantManagerUtility.isEMPIEnable(participant.getId()))
		{
			insertParticipantToProcessingQue(participant, userIdSet);
		}

	}

	/**
	 * Insert participant to processing que.
	 *
	 * @param participant
	 *            the participant
	 * @param userIdSet
	 *            the user id set
	 * @throws BizLogicException
	 *
	 * @throws DAOException
	 *             the DAO exception
	 */
	private void insertParticipantToProcessingQue(final IParticipant participant,
			LinkedHashSet<Long> userIdSet) throws BizLogicException
	{
		String mrn = null;
		mrn = ParticipantManagerUtility.getMrnValue(participant
				.getParticipantMedicalIdentifierCollection());
		try
		{
			if (ParticipantManagerUtility.isParticipantValidForEMPI(participant.getLastName(),
					participant.getFirstName(), participant.getBirthDate(), participant
							.getSocialSecurityNumber(), mrn))
			{
				// Process participant for CIDER participant matching.
				ParticipantManagerUtility.addParticipantToProcessMessageQueue(userIdSet,
						participant.getId());
			}
		}
		catch (DAOException daoException)
		{
			throw new BizLogicException(daoException);
		}
	}

	/**
	 * Pre update.
	 *
	 * @param obj
	 *            the obj
	 * @param sessionDataBean
	 *            the session data bean
	 * @throws BizLogicException
	 * @throws ApplicationException
	 */
	public void preUpdate(Object oldObj, Object obj) throws BizLogicException
	{
		/*final IParticipant participant = (IParticipant) obj;
		final IParticipant oldParticipant = (IParticipant) oldObj;
		final String oldEMPIStatus = oldParticipant.getEmpiIdStatus();*/

		/*
		 * if (ParticipantManagerUtility.isEMPIEnable(participant.getId()) &&
		 * ParticipantManagerUtility.isParticipantEdited(oldParticipant,
		 * participant)) { if (oldEMPIStatus != null &&
		 * !("".equals(oldEMPIStatus))) { if
		 * (Constants.EMPI_ID_CREATED.equals(participant.getEmpiIdStatus())) {
		 * participant.setEmpiIdStatus(Constants.EMPI_ID_PENDING); } } }
		 */

	}

	/**
	 * Post update.
	 *
	 * @param obj
	 *            the obj
	 * @param sessionDataBean
	 *            the session data bean
	 *
	 * @throws BizLogicException
	 *             the biz logic exception
	 * @throws DAOException
	 */
	public void postUpdate(Object oldObj, Object currentObj, LinkedHashSet<Long> userIdSet)
			throws BizLogicException
	{
		final IParticipant oldParticipant = (IParticipant) oldObj;
		final IParticipant participant = (IParticipant) currentObj;

		try
		{
			if (ParticipantManagerUtility.isEMPIEnable(participant.getId())
					&& ParticipantManagerUtility.isParticipantEdited(oldParticipant, participant))
			{
				// if user resolved match by selecting a record from the grid,
				// then insert entry into PARTICIPANT_EMPI_ID_MAPPING table
				// so that HL7 msg is sent with tmpMRN Id
				if (null != participant.getGridValueSelected()
						&& participant.getGridValueSelected().equals(Constants.YES)
						&& null != oldParticipant.getEmpiId())
				{
					// Update PARTICIPANT_EMPI_ID_MAPPING table with tempMRN
					ParticipantManagerUtility utility = new ParticipantManagerUtility();
					utility.updateOldEMPIDetails(participant.getId(), oldParticipant.getEmpiId());

					utility.setEMPIIdStatus(participant.getId(), Constants.EMPI_ID_PENDING);
				}

				// to send for participant matching when status is CREATED -- this check if
				// done in the method, so removing from here
				/*if (edu.wustl.common.participant.utility.Constants.EMPI_ID_CREATED
						.equals(oldParticipant.getEmpiIdStatus())) {*/

				// in case of normal edit flow the method must be executed and
				// not when a match is selected
				if (!(null != participant.getGridValueSelected() && participant
						.getGridValueSelected().equals(Constants.YES)))
				{
					regNewPatientToEMPI(participant, userIdSet);
				}
				//}
			}
		}
		catch (Exception e)
		{
			logger.info("ERROR WHILE REGISTERING NEW PATIENT TO EMPI  ##############  \n");
			throw new BizLogicException(null, e, e.getMessage());
		}
	}

	/**
	 * Reg new patient to empi.
	 *
	 * @param participant
	 *            the participant
	 *
	 * @throws BizLogicException
	 *             the biz logic exception
	 * @throws Exception
	 *             the exception
	 */
	private void regNewPatientToEMPI(IParticipant participant, LinkedHashSet<Long> userIdSet)
			throws BizLogicException
	{
		//JDBCDAO jdbcdao = null;

		try
		{
			String mrn = ParticipantManagerUtility.getMrnValue(participant
					.getParticipantMedicalIdentifierCollection());
			if (ParticipantManagerUtility.isParticipantValidForEMPI(participant.getLastName(),
					participant.getFirstName(), participant.getBirthDate(), participant
							.getSocialSecurityNumber(), mrn))
			{
				String empIdStatus = participantDao.getPartiEMPIStatus(participant.getId());
				if (empIdStatus == null)
				{
					empIdStatus = "";
				}
				//				jdbcdao = getJDBCDAO();
				//				//get current empi Id status of the participant
				//				String query = "SELECT EMPI_ID_STATUS FROM CATISSUE_PARTICIPANT WHERE IDENTIFIER=?";
				//				LinkedList<ColumnValueBean> colValueBeanList = new LinkedList<ColumnValueBean>();
				//				colValueBeanList.add(new ColumnValueBean("IDENTIFIER", participant.getId(),
				//						DBTypes.LONG));
				//				List<Object> idList = jdbcdao.executeQuery(query, null, colValueBeanList);
				//				if (null != idList && !idList.isEmpty())
				//				{
				//					if (null != idList.get(0))
				//					{
				//						Object obj = idList.get(0);
				//						empIdStatus = ((ArrayList) obj).get(0).toString();
				//					}
				//				}

				// Check if participant matches from CIDER are already received before
				// for old participant before the edit happened -- Bug fixed 18824
				//				LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
				//				columnValueBeanList.add(new ColumnValueBean(participant.getId()));
				//				query = "SELECT SEARCHED_PARTICIPANT_ID FROM MATCHED_PARTICIPANT_MAPPING "
				//						+ "WHERE SEARCHED_PARTICIPANT_ID=?";
				//				List<Object> partIdList = jdbcdao.executeQuery(query, null, columnValueBeanList);
				//				boolean isOldMatchesPresent = false;
				//				if (partIdList != null && !partIdList.isEmpty() && partIdList.get(0) != null
				//						&& !((List) partIdList.get(0)).isEmpty())
				//				{
				//					isOldMatchesPresent = true;
				//				}
				boolean isOldMatchesPresent = participantDao.isOldMatchesPresent(participant
						.getId());
				// go for participant matching only when status is 'CREATED'
				if (empIdStatus.equals(Constants.EMPI_ID_CREATED) || isOldMatchesPresent)
				{
					participant.setEmpiId("");

					// Process participant for CIDER participant matching.
					ParticipantManagerUtility.addParticipantToProcessMessageQueue(userIdSet,
							participant.getId());
					// sendHL7RegMes(participant, tempararyPartiId);
				}
			}
		}
		catch (DAOException e)
		{
			//jdbcdao.rollback();
			throw new BizLogicException(e);
		}
		//		finally
		//		{
		//			jdbcdao.closeSession();
		//		}
	}

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

	/**
	 * Gets the participant.
	 *
	 * @param empiId the empi id
	 *
	 * @return the participant
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws DAOException
	 */
	public IParticipant getParticipant(String empiId) throws BizLogicException, ApplicationException,ParticipantManagerException
	{
		IParticipant participant = ParticipantManagerUtility.getParticipantMgrImplObj().getParticpantByEmpiId(empiId);
//		final String sourceObjectName = IParticipant.class.getName();
//		final String[] selectColumnName = {"id"};
//		final QueryWhereClause queryWhereClause = new QueryWhereClause(sourceObjectName);
//		queryWhereClause.addCondition(new EqualClause(Constants.PARTICIPANT_EMPIID, '?'));
//		List<ColumnValueBean> columnValueBeans = new ArrayList<ColumnValueBean>();
//		columnValueBeans.add(new ColumnValueBean(empiId));
//		/*
//		 * final List list = dao .retrieve(sourceObjectName,
//		 * selectColumnName, queryWhereClause);
//		 */
//		final List list = this.retrieve(sourceObjectName, null, queryWhereClause, columnValueBeans);
//		if (!list.isEmpty())
//		{
//			participant = (IParticipant) list.get(0);
//		}
		return participant;
	}

	/**
	 * Update participant in database.
	 *
	 * @param newpartcipantObj the new participant.
	 * @param oldPartcipantObj the old participant.
	 *
	 * @return the status
	 */
	public void updateParticipant(final IParticipant newpartcipantObj,
			final IParticipant oldPartcipantObj) throws BizLogicException,
			ParticipantManagerException
	{
		final String loginName = XMLPropertyHandler.getValue(Constants.HL7_LISTENER_ADMIN_USER);
		final IUser validUser = ParticipantManagerUtility.getUser(loginName,
				Constants.ACTIVITY_STATUS_ACTIVE);
		if (validUser != null)
		{
			SessionDataBean sessionData = ParticipantManagerUtility.getSessionDataBean(validUser);
			update(newpartcipantObj, oldPartcipantObj, sessionData);
		}
	}

	/**
	 * @param siteName
	 * @return
	 * @throws ParticipantManagerException
	 * @throws BizLogicException
	 */
	public ISite getSite(final String siteName) throws ParticipantManagerException,
			BizLogicException
	{
		ISite site = null;
		String siteClassName = edu.wustl.common.participant.utility.PropertyHandler
				.getValue(Constants.SITE_CLASS);
		final String getSite = "from " + siteClassName + " site where site.name= '" + siteName
				+ "'";
		final List sites = this.executeQuery(getSite);
		if (sites != null && !sites.isEmpty())
		{
			site = (ISite) sites.get(0);
		}
		else
		{
			throw new BizLogicException(null, null, null);
		}
		return site;
	}

	/**
	 * This method will retrieve the matched participant stored in DB for given id
	 * @param participantId for which to search maching patients.
	 * @return list of matched patients
	 * @throws DAOException exception
	 * @throws BizLogicException exception
	 * @throws ParseException exception
	 * @throws ParticipantManagerException exception
	 */
	public List<DefaultLookupResult> retrieveMatchedParticipantList(final long participantId)
			throws DAOException, BizLogicException, ParseException, ParticipantManagerException
	{
		//JDBCDAO dao = null;
		List<DefaultLookupResult> matchPartpantLst = null;
		try
		{
//			dao = ParticipantManagerUtility.getJDBCDAO();
//
//			final String query = "SELECT * FROM CATISSUE_MATCHED_PARTICIPANT WHERE SEARCHED_PARTICIPANT_ID=? order by ORDER_NO";
//
//			final LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//			columnValueBeanList.add(new ColumnValueBean("SEARCHED_PARTICIPANT_ID", participantId,
//					DBTypes.LONG));
			final List matchPartpantLstTemp =  participantDao.retrieveMatchedParticipantList(participantId);//dao.executeQuery(query, null, columnValueBeanList);
			matchPartpantLst = populateParticipantList(matchPartpantLstTemp);
		}
		catch (DAOException e)
		{
			//LOGGER.info(e.getMessage());
			throw new DAOException(e.getErrorKey(), e, e.getMsgValues());
		}
//		finally
//		{
//			dao.closeSession();
//		}
		return matchPartpantLst;
	}

	//	/**
	//	 * This method will search for the
	//	 * @param participantId
	//	 * @param upi
	//	 * @return
	//	 * @throws DAOException
	//	 * @throws BizLogicException
	//	 * @throws ParseException
	//	 * @throws ParticipantManagerException
	//	 */
	//	public List<DefaultLookupResult> getMatchedParticipantByEmpi(final long participantId,String upi)
	//			throws DAOException, BizLogicException, ParseException, ParticipantManagerException
	//	{
	//		JDBCDAO dao = null;
	//		List<DefaultLookupResult> matchPartpantLst = null;
	//		try
	//		{
	//			dao = ParticipantManagerUtility.getJDBCDAO();
	//
	//			final String query = "SELECT * FROM CATISSUE_MATCHED_PARTICIPANT WHERE SEARCHED_PARTICIPANT_ID=? and EMPI_ID = ? order by ORDER_NO";
	//
	//			final LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
	//			columnValueBeanList.add(new ColumnValueBean("SEARCHED_PARTICIPANT_ID", participantId,
	//					DBTypes.LONG));
	//			columnValueBeanList.add(new ColumnValueBean("EMPI_ID", upi));
	//			final List matchPartpantLstTemp = dao.executeQuery(query, null, columnValueBeanList);
	//			if(matchPartpantLstTemp.isEmpty())
	//			{
	//				throw new BizLogicException(ErrorKey.getErrorKey(""), null, "No participant found");
	//			}
	//			matchPartpantLst = populateParticipantList(matchPartpantLstTemp);
	//		}
	//		catch (DAOException e)
	//		{
	//			//LOGGER.info(e.getMessage());
	//			throw new DAOException(e.getErrorKey(), e, e.getMsgValues());
	//		}
	//		finally
	//		{
	//			dao.closeSession();
	//		}
	//		return matchPartpantLst;
	//	}

	/**
	 * Populate participant list.
	 *
	 * @param matchPartpantLstTmp the match partpant lst tmp
	 *
	 * @return the list
	 *
	 * @throws ParseException the parse exception
	 * @throws BizLogicException the biz logic exception
	 * @throws ParticipantManagerException
	 * @throws Exception the exception
	 */
	private List<DefaultLookupResult> populateParticipantList(final List matchPartpantLstTmp)
			throws BizLogicException, ParseException, ParticipantManagerException

	{
		final List<DefaultLookupResult> matchPartpantLst = new ArrayList<DefaultLookupResult>();
		for (int i = 0; i < matchPartpantLstTmp.size(); i++)
		{
			final Object[] participantValueList = (Object[]) matchPartpantLstTmp.get(i);
			if (participantValueList[0] != null)
			{
				final IParticipant participant = getParticipantObj(participantValueList);
				final DefaultLookupResult result = new DefaultLookupResult();
				result.setObject(participant);
				matchPartpantLst.add(result);
			}
//			final List participantValueList = (List) matchPartpantLstTmp.get(i);
//			if (!participantValueList.isEmpty() && participantValueList.get(0) != null
//					&& !"".equals(participantValueList.get(0)))
//			{
//				final IParticipant participant = getParticipantObj(participantValueList);
//				final DefaultLookupResult result = new DefaultLookupResult();
//				result.setObject(participant);
//				matchPartpantLst.add(result);
//			}
		}
		return matchPartpantLst;
	}

	/**
	 * Gets the participant obj.
	 *
	 * @param participantValueList the participant value list
	 *
	 * @return the participant obj
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws ParseException the parse exception
	 * @throws ParticipantManagerException
	 * @throws Exception the exception
	 */
	private IParticipant getParticipantObj(final Object[] participantValueList)
			throws BizLogicException, ParseException, ParticipantManagerException
	{
		final IParticipant participant = (IParticipant) ParticipantManagerUtility
				.getParticipantInstance();

		participant.setId((Long)participantValueList[0]);
		participant.setEmpiId((String) participantValueList[1]);

		final String lastName = (String) participantValueList[2];
		final String firstName = (String) participantValueList[3];
		final String middleName = (String) participantValueList[4];

		participant.setLastName(ParticipantManagerUtility.modifyNameWithProperCase(lastName));
		participant.setFirstName(ParticipantManagerUtility.modifyNameWithProperCase(firstName));
		participant.setMiddleName(ParticipantManagerUtility.modifyNameWithProperCase(middleName));
		if (participantValueList[5] != null)
		{
//			final String dateStr = (String) participantValueList[5];
//			final Date date = Utility.parseDate(dateStr, Constants.DATE_FORMAT);
			participant.setBirthDate((Date)participantValueList[5]);
		}
		participant.setGender((String) participantValueList[6]);
		participant.setSocialSecurityNumber((String) participantValueList[7]);
		participant.setActivityStatus((String) participantValueList[8]);
		if (participantValueList[9] != null)
		{
//			final String dateStr = (String) participantValueList[9];
//			final Date date = Utility.parseDate(dateStr, Constants.DATE_FORMAT);
			participant.setDeathDate((Date)participantValueList[9]);
		}
		participant.setVitalStatus((String) participantValueList[10]);
		final String mrnString = (String) participantValueList[11];
		if (mrnString != null && !"".equals(mrnString))
		{
			final Collection partiMediIdColn = getPartiMediIdColnCollection(mrnString);
			participant.setParticipantMedicalIdentifierCollection(partiMediIdColn);
		}
		final String raceString = (String) participantValueList[12];
		if (raceString != null && !"".equals(raceString))
		{
			final Collection raceCollection = getRaceCollection(raceString);
			participant.setRaceCollection(raceCollection);
		}
		return participant;
	}

	/**
	 * Gets the participant obj.
	 *
	 * @param participantValueList the participant value list
	 *
	 * @return the participant obj
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws ParseException the parse exception
	 * @throws ParticipantManagerException
	 * @throws Exception the exception
	 */
	public IParticipant getParticipantObj(PatientInformation patientInfo) throws BizLogicException,
			ParseException, ParticipantManagerException
	{
		final IParticipant participant = (IParticipant) ParticipantManagerUtility
				.getParticipantInstance();

		participant.setId(patientInfo.getId());
		participant.setEmpiId(patientInfo.getUpi());

		final String lastName = patientInfo.getLastName();
		final String firstName = patientInfo.getFirstName();
		final String middleName = patientInfo.getMiddleName();

		participant.setLastName(ParticipantManagerUtility.modifyNameWithProperCase(lastName));
		participant.setFirstName(ParticipantManagerUtility.modifyNameWithProperCase(firstName));
		participant.setMiddleName(ParticipantManagerUtility.modifyNameWithProperCase(middleName));

		participant.setBirthDate(patientInfo.getDob());

		participant.setGender(patientInfo.getGender());
		participant.setSocialSecurityNumber(patientInfo.getSsn());
		participant.setActivityStatus(patientInfo.getActivityStatus());
		participant.setDeathDate(patientInfo.getDeathDate());

		participant.setVitalStatus(patientInfo.getVitalStatus());
		participant.setParticipantMedicalIdentifierCollection(patientInfo.getPmiCollection());
		participant.setRaceCollection(patientInfo.getRaceCollection());

		return participant;
	}

	/**
	 * Gets the race collection.
	 *
	 * @param raceString the race string
	 *
	 * @return the race collection
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws ParticipantManagerException
	 */
	private Collection getRaceCollection(final String raceString) throws BizLogicException,
			ParticipantManagerException
	{
		final Collection<IRace> raceCollection = new LinkedHashSet<IRace>();
		final String racevalues[] = raceString.split(",");
		for (int i = 0; i < racevalues.length; i++)
		{
			final IRace race = (IRace) ParticipantManagerUtility.getRaceInstance();
			final String raceName = racevalues[i];
			race.setRaceName(raceName);
			raceCollection.add(race);
		}
		return raceCollection;
	}

	/**
	 * Gets the parti medi id coln collection.
	 *
	 * @param mrnString the mrn string
	 *
	 * @return the parti medi id coln collection
	 * mrnId + ":" + facilityId + ":" + siteName;
	 * @throws BizLogicException the biz logic exception
	 * @throws ParticipantManagerException
	 * @throws Exception the exception
	 */
	private Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> getPartiMediIdColnCollection(
			final String mrnString) throws BizLogicException, ParticipantManagerException
	{
		final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> partiMediIdColn = new LinkedHashSet<IParticipantMedicalIdentifier<IParticipant, ISite>>();
		final String values[] = mrnString.split(",");
		for (int i = 0; i < values.length; i++)
		{
			final String value = values[i];
			final IParticipantMedicalIdentifier<IParticipant, ISite> participantMedicalIdentifier = getParticipantMedicalIdentifierObj(value);
			if (participantMedicalIdentifier != null)
			{
				partiMediIdColn.add(participantMedicalIdentifier);
			}
		}

		return partiMediIdColn;
	}

	/**
	 * Gets the participant medical identifier obj.
	 *
	 * @param value the value
	 *
	 * @return the participant medical identifier obj
	 * mrnId + ":" + facilityId + ":" + siteName;
	 * @throws BizLogicException the biz logic exception
	 * @throws ParticipantManagerException
	 * @throws Exception the exception
	 */
	private IParticipantMedicalIdentifier<IParticipant, ISite> getParticipantMedicalIdentifierObj(
			final String value) throws BizLogicException, ParticipantManagerException

	{
		IParticipantMedicalIdentifier<IParticipant, ISite> participantMedicalIdentifier = null;
		final String values[] = value.split(":");
		final String mrn = values[0];
		final String facilityId = values[1];
		final ISite siteObj = ParticipantManagerUtility.getSiteObject(facilityId);
		if (siteObj != null)
		{
			participantMedicalIdentifier = ParticipantManagerUtility.getPMIInstance();
			participantMedicalIdentifier.setMedicalRecordNumber(mrn);
			siteObj.setFacilityId(facilityId);
			participantMedicalIdentifier.setSite(siteObj);
		}
		return participantMedicalIdentifier;
	}

	/**
	 * Updating the old participant with new mrn value.
	 *
	 * @param participant : participant .
	 * @param participantEMPI : participantEMPI
	 */
	public void updateParticipantFromEmpi(final IParticipant participant,
			final IParticipant participantEMPI)
	{
		final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> medIdColTemp = new LinkedHashSet<IParticipantMedicalIdentifier<IParticipant, ISite>>();
		final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> medIdColLocal = participant
				.getParticipantMedicalIdentifierCollection();
		if (medIdColLocal != null && !medIdColLocal.isEmpty())
		{
			final Iterator<IParticipantMedicalIdentifier<IParticipant, ISite>> iterator = medIdColLocal
					.iterator();
			while (iterator.hasNext())
			{
				Long localSiteID = null;

				final IParticipantMedicalIdentifier<IParticipant, ISite> partMedIdLocal = iterator
						.next();
				final String localMRN = partMedIdLocal.getMedicalRecordNumber();
				if (partMedIdLocal.getSite() != null)
				{
					localSiteID = partMedIdLocal.getSite().getId();
				}
				if ((localMRN != null && !"".equals(localMRN))
						&& (localSiteID != null && localSiteID != -1))
				{
					final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> medIdColEMPI = participantEMPI
							.getParticipantMedicalIdentifierCollection();

					if (medIdColEMPI == null)
					{
						medIdColTemp.add(partMedIdLocal);
					}
					else if (!medIdColEMPI.isEmpty())
					{
						//check weather this empi site and local site matches
						//if yes then override local mrn with empi mrn and pmi id
						// if no then add empi pmi to medIdColTemp
						/**
						 * empi object pmi update - >
						 * if local and empi object has same site then don't add else add.
						 */

						removeDuplicatesMRN(medIdColEMPI, partMedIdLocal, localMRN, localSiteID,
								medIdColTemp);
					}
				}
			}

			if (!medIdColTemp.isEmpty())
			{
				participantEMPI.getParticipantMedicalIdentifierCollection().addAll(medIdColTemp);
			}
		}
		participant.setParticipantMedicalIdentifierCollection(participantEMPI
				.getParticipantMedicalIdentifierCollection());
		participant.setEmpiId(participantEMPI.getEmpiId());
		participant.setEmpiIdStatus(participantEMPI.getEmpiIdStatus());

	}

	/**
	 * Removes the duplicates mrn.
	 *
	 * @param medIdColEMPI the med id col empi
	 * @param partMedIdLocal the part med id local
	 * @param localMRN the local mrn
	 * @param localSiteID the local site id
	 * @param medIdColTemp the med id col temp
	 */
	private void removeDuplicatesMRN(
			final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> medIdColEMPI,
			final IParticipantMedicalIdentifier partMedIdLocal, final String localMRN,
			final Long localSiteID,
			final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> medIdColTemp)
	{
		boolean MRNNotFound = false;
		final Iterator<IParticipantMedicalIdentifier<IParticipant, ISite>> itrEMPI = medIdColEMPI
				.iterator();
		while (itrEMPI.hasNext())
		{
			MRNNotFound = false;
			Long empiSite = null;
			final IParticipantMedicalIdentifier<IParticipant, ISite> partiMedIdEMPI = itrEMPI
					.next();
			final String empiMRN = partiMedIdEMPI.getMedicalRecordNumber();
			if (partiMedIdEMPI.getSite() != null)
			{
				empiSite = partiMedIdEMPI.getSite().getId();
			}
			if ((empiMRN.equals(localMRN)) && (empiSite.equals(localSiteID)))
			{
				partiMedIdEMPI.setId(partMedIdLocal.getId());
				MRNNotFound = true;
				break;
			}
			if ((!empiMRN.equals(localMRN)) && (empiSite.equals(localSiteID)))
			{
				partiMedIdEMPI.setId(partMedIdLocal.getId());
				MRNNotFound = true;
				break;
			}
		}
		if (!MRNNotFound)
		{
			medIdColTemp.add(partMedIdLocal);
		}
	}

}