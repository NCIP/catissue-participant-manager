package edu.wustl.common.participant.bizlogic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.language.Metaphone;

import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.cde.CDEManager;
import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.exception.ErrorKey;
import edu.wustl.common.participant.domain.IEthnicity;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.IRace;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.domain.IUser;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerException;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.Utility;
import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.common.util.global.ApplicationProperties;
import edu.wustl.common.util.global.CommonServiceLocator;
import edu.wustl.common.util.global.Status;
import edu.wustl.common.util.global.Validator;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.DAO;
import edu.wustl.dao.HibernateDAO;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.QueryWhereClause;
import edu.wustl.dao.condition.EqualClause;
import edu.wustl.dao.daofactory.DAOConfigFactory;
import edu.wustl.dao.daofactory.IDAOFactory;
import edu.wustl.dao.exception.AuditException;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.query.generator.ColumnValueBean;
import edu.wustl.dao.query.generator.DBTypes;

/**
 * The Class CommonParticipantBizlogic.
 */
public class CommonParticipantBizlogic extends CommonDefaultBizLogic {

	/** The Constant logger. */
	private static final Logger logger = Logger
			.getCommonLogger(CommonParticipantBizlogic.class);

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
	public static IParticipant insert(final Object obj, final DAO dao,
			final IParticipantMedicalIdentifier pmi) throws BizLogicException,
			DAOException {
		final IParticipant participant = (IParticipant) obj;
		setMetaPhoneCode(participant);
		Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> pmiCollection = participant
				.getParticipantMedicalIdentifierCollection();
		if (pmiCollection == null) {
			pmiCollection = new LinkedHashSet<IParticipantMedicalIdentifier<IParticipant, ISite>>();
		}
		if (pmiCollection.isEmpty()) {
			pmi.setMedicalRecordNumber(null);
			pmi.setSite(null);
			pmiCollection.add(pmi);
		}
		checkForSiteIdentifierInPMI(dao, pmiCollection);
		final Iterator<IParticipantMedicalIdentifier<IParticipant, ISite>> iterator = pmiCollection
				.iterator();
		while (iterator.hasNext()) {
			final IParticipantMedicalIdentifier<IParticipant, ISite> pmIdentifier = iterator
					.next();
			pmIdentifier.setParticipant(participant);
		}
		dao.insert(participant);
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
	private static void checkForSiteIdentifierInPMI(
			final DAO dao,
			final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> pmiCollection)
			throws DAOException, BizLogicException {
		final Iterator<IParticipantMedicalIdentifier<IParticipant, ISite>> pmiIterator = pmiCollection
				.iterator();
		while (pmiIterator.hasNext()) {
			final IParticipantMedicalIdentifier<IParticipant, ISite> pmIdentifier = pmiIterator
					.next();
			if (pmIdentifier.getSite() != null
					&& pmIdentifier.getSite().getId() == null
					&& pmIdentifier.getSite().getName() != null) {
				final ISite site = pmIdentifier.getSite();
				final String sourceObjectName = ISite.class.getName();
				final String[] selectColumnName = { "id" };
				final QueryWhereClause queryWhereClause = new QueryWhereClause(
						sourceObjectName);
				// queryWhereClause.addCondition(new EqualClause("name",
				// site.getName()));
				queryWhereClause.addCondition(new EqualClause("name", '?'));
				List<ColumnValueBean> columnValueBeans = new ArrayList<ColumnValueBean>();
				columnValueBeans.add(new ColumnValueBean(site.getName()));
				/*
				 * final List list = dao .retrieve(sourceObjectName,
				 * selectColumnName, queryWhereClause);
				 */
				final List list = ((HibernateDAO) dao).retrieve(
						sourceObjectName, selectColumnName, queryWhereClause,
						columnValueBeans);

				if (!list.isEmpty()) {
					site.setId((Long) list.get(0));
					pmIdentifier.setSite(site);
				} else {
					throw new BizLogicException(ErrorKey
							.getErrorKey("invalid.site.name"), null, site
							.getName());
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
	public static void update(final DAO dao, final IParticipant participant,
			final IParticipant oldParticipant) throws BizLogicException,
			DAOException {

		setMetaPhoneCode(participant);
		dao.update(participant, oldParticipant);
	}

	/**
	 * Sets the meta phone code.
	 *
	 * @param participant
	 *            the new meta phone code
	 */
	private static void setMetaPhoneCode(final IParticipant participant) {
		final Metaphone metaPhoneObj = new Metaphone();
		final String lNameMetaPhone = metaPhoneObj.metaphone(participant
				.getLastName());
		participant.setMetaPhoneCode(lNameMetaPhone);
	}

	/**
	 * Update ParticipantMedicalIdentifier.
	 *
	 * @param dao
	 *            DAo Object
	 * @param pmIdentifier
	 *            ParticipantMedicalIdentifier Identifier
	 * @throws DAOException
	 *             the DAO exception
	 */
	public static void updatePMI(
			final DAO dao,
			final IParticipantMedicalIdentifier<IParticipant, ISite> pmIdentifier)
			throws DAOException {
		if (pmIdentifier.getId() != null) {
			dao.update(pmIdentifier);
		} else if (pmIdentifier.getId() == null
				|| pmIdentifier.getId().equals("")) {
			dao.insert(pmIdentifier);
		}
	}

	/**
	 * Validate.
	 *
	 * @param dao
	 *            : DAO object. Overriding the parent class's method to validate
	 *            the enumerated attribute values.
	 * @param participant
	 *            the participant
	 * @param operation
	 *            the operation
	 * @param validator
	 *            the validator
	 *
	 * @return true, if validate
	 *
	 * @throws BizLogicException
	 *             the biz logic exception
	 */
	public static boolean validate(final IParticipant participant,
			final DAO dao, final String operation, final Validator validator)
			throws BizLogicException {
		String message = "";
		if (participant == null) {

			throw new BizLogicException(null, null,
					"domain.object.null.err.msg", "Participant");
		}

		String errorKeyForBirthDate = "";
		String errorKeyForDeathDate = "";

		final String birthDate = Utility.parseDateToString(participant
				.getBirthDate(), CommonServiceLocator.getInstance()
				.getDatePattern());
		if (!validator.isEmpty(birthDate)) {
			errorKeyForBirthDate = validator.validateDate(birthDate, true);
			if (errorKeyForBirthDate.trim().length() > 0) {
				message = ApplicationProperties
						.getValue("participant.birthDate");
				throw new BizLogicException(null, null, errorKeyForBirthDate,
						message);
			}
		}

		final String deathDate = Utility.parseDateToString(participant
				.getDeathDate(), CommonServiceLocator.getInstance()
				.getDatePattern());
		if (!validator.isEmpty(deathDate)) {
			errorKeyForDeathDate = validator.validateDate(deathDate, true);
			if (errorKeyForDeathDate.trim().length() > 0) {
				message = ApplicationProperties
						.getValue("participant.deathDate");
				throw new BizLogicException(null, null, errorKeyForDeathDate,
						message);
			}
		}

		if (participant.getVitalStatus() == null
				|| !participant.getVitalStatus().equals("Dead")) {
			if (!validator.isEmpty(deathDate)) {
				throw new BizLogicException(null, null,
						"participant.invalid.enddate", "");
			}
		}
		if ((!validator.isEmpty(birthDate) && !validator.isEmpty(deathDate))
				&& (errorKeyForDeathDate.trim().length() == 0 && errorKeyForBirthDate
						.trim().length() == 0)) {
			final boolean errorKey1 = validator.compareDates(
					Utility
							.parseDateToString(participant.getBirthDate(),
									CommonServiceLocator.getInstance()
											.getDatePattern()), Utility
							.parseDateToString(participant.getDeathDate(),
									CommonServiceLocator.getInstance()
											.getDatePattern()));

			if (!errorKey1) {

				throw new BizLogicException(null, null,
						"participant.invaliddate", "");
			}
		}

		if (!validator.isEmpty(participant.getSocialSecurityNumber())) {
			if (!validator.isValidSSN(participant.getSocialSecurityNumber())) {
				message = ApplicationProperties
						.getValue("participant.socialSecurityNumber");
				throw new BizLogicException(null, null, "errors.invalid",
						message);
			}
		}

		if (!validator.isEmpty(participant.getVitalStatus())) {
			final List vitalStatusList = CDEManager.getCDEManager()
					.getPermissibleValueList(Constants.CDE_VITAL_STATUS, null);
			if (!Validator.isEnumeratedOrNullValue(vitalStatusList, participant
					.getVitalStatus())) {
				throw new BizLogicException(null, null,
						"participant.vitalstatus.errMsg", "");
			}
		}

		if (!validator.isEmpty(participant.getGender())) {
			final List genderList = CDEManager.getCDEManager()
					.getPermissibleValueList(Constants.CDE_NAME_GENDER, null);

			if (!Validator.isEnumeratedOrNullValue(genderList, participant
					.getGender())) {
				throw new BizLogicException(null, null,
						"participant.gender.errMsg", "");
			}
		}

		if (!validator.isEmpty(participant.getSexGenotype())) {
			final List genotypeList = CDEManager.getCDEManager()
					.getPermissibleValueList(Constants.CDE_NAME_GENOTYPE, null);
			if (!Validator.isEnumeratedOrNullValue(genotypeList, participant
					.getSexGenotype())) {
				throw new BizLogicException(null, null,
						"participant.genotype.errMsg", "");
			}
		}

		final Collection paticipantMedCol = participant.getParticipantMedicalIdentifierCollection();
		// Created a new PMI collection for bulk operation functionality.
		final Collection newPMICollection = new LinkedHashSet();
		if (paticipantMedCol != null && !paticipantMedCol.isEmpty())
		{
			final Iterator itr = paticipantMedCol.iterator();
			java.util.HashSet<Long> siteIdset = new java.util.HashSet<Long>();
			while (itr.hasNext())
			{
				final IParticipantMedicalIdentifier<IParticipant, ISite> partiMedobj = (IParticipantMedicalIdentifier<IParticipant, ISite>) itr
						.next();
				final ISite site = partiMedobj.getSite();
				final String medicalRecordNo = partiMedobj.getMedicalRecordNumber();
				if (validator.isEmpty(medicalRecordNo) || site == null || site.getId() == null)
				{
					if (partiMedobj.getId() == null)
					{
						throw new BizLogicException(null, null, "errors.participant.extiden.missing", "");
					}
				}
				else
				{
					newPMICollection.add(partiMedobj);
				}
				if (site != null)
				{
					boolean checkDuplicate = siteIdset.add(site.getId());
					if (!checkDuplicate)
					{
						throw new BizLogicException(null, null,
								"errors.participant.mediden.duplicate", "");
					}
				}
			}
		}
		participant.setParticipantMedicalIdentifierCollection(newPMICollection);

		final Collection raceCollection = participant.getRaceCollection();
		if (raceCollection != null && !raceCollection.isEmpty()) {
			final List raceList = CDEManager.getCDEManager()
					.getPermissibleValueList(Constants.CDE_NAME_RACE, null);
			final Iterator itr = raceCollection.iterator();
			while (itr.hasNext()) {
				final IRace race = (IRace) itr.next();
				if (race != null) {
					final String raceName = race.getRaceName();
					if (!validator.isEmpty(raceName)
							&& !Validator.isEnumeratedOrNullValue(raceList,
									raceName)) {
						throw new BizLogicException(null, null,
								"participant.race.errMsg", "");
					}
				}
			}
		}
        validateEthnicity(participant);

		if (operation.equals(Constants.ADD)) {
			if (!Status.ACTIVITY_STATUS_ACTIVE.toString().equals(
					participant.getActivityStatus())) {
				throw new BizLogicException(null, null,
						"activityStatus.active.errMsg", "");
			}
		} else {
			if (!Validator.isEnumeratedValue(Constants.ACTIVITY_STATUS_VALUES,
					participant.getActivityStatus())) {
				throw new BizLogicException(null, null,
						"activityStatus.errMsg", "");
			}
		}
		return true;
	}

    /**
     * @param participant
     * @throws BizLogicException
     */
    private static void validateEthnicity(IParticipant participant) throws BizLogicException
    {
        final Collection ethnicityColl = participant.getEthnicityCollection();
        if (ethnicityColl != null && !ethnicityColl.isEmpty()) {
            final List ethnicityList = CDEManager.getCDEManager()
                    .getPermissibleValueList(Constants.CDE_NAME_ETHNICITY, null);
            final Iterator itr = ethnicityColl.iterator();
            while (itr.hasNext()) {
                final IEthnicity etnicity = (IEthnicity) itr.next();
                if (etnicity != null) {
                    final String name = etnicity.getName();
                    if (!Validator.isEmpty(name)
                            && !Validator.isEnumeratedOrNullValue(ethnicityList,
                                    name)) {
                        throw new BizLogicException(null, null,
                                "participant.ethnicity.errMsg", "");
                    }
                }
            }
        }
    }

	/**
	 * Modify participant object.
	 *
	 * @param dao
	 *            the dao
	 * @param sessionDataBean
	 *            the session data bean
	 * @param participant
	 *            the participant
	 *
	 * @throws BizLogicException
	 *             the biz logic exception
	 */
	public void modifyParticipantObject(final DAO dao,
			final SessionDataBean sessionDataBean,
			final IParticipant participant, final IParticipant oldParticipant)
			throws BizLogicException {
		try {
			updateParticipant(dao, sessionDataBean, participant, oldParticipant);
		} catch (DAOException e) {
			throw new BizLogicException(e);
		} catch (BizLogicException biz) {
			logger.debug(biz.getMessage(), biz);
			throw getBizLogicException(biz, biz.getErrorKeyName(), biz
					.getMsgValues());

		} catch (Exception exception) {
			throw getBizLogicException(exception,
					"Error while updating object", "");
		}
	}

	/**
	 * This method will update Participant Object.
	 *
	 * @param participant
	 *            Participant object
	 * @param oldParticipant
	 *            Persistent participant object
	 * @param dao
	 *            DAO Object
	 * @param sessionDataBean
	 *            SessionDataBean Object
	 *
	 * @return AuditManager
	 *
	 * @throws BizLogicException
	 *             BizLogicException Exception
	 * @throws DAOException
	 *             DAOException Exception
	 * @throws AuditException
	 *             AuditException Exception
	 */
	private void updateParticipant(final DAO dao,
			final SessionDataBean sessionDataBean,
			final IParticipant participant, final IParticipant oldParticipant)
			throws BizLogicException, DAOException {
		update(dao, participant, oldParticipant);
	}

	/**
	 * check not null.
	 *
	 * @param object
	 *            object
	 *
	 * @return boolean
	 */

	public static boolean isNullobject(Object object) {
		boolean result = true;
		if (object != null) {
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
	public static void setParticipantMedicalIdentifierDefault(
			IParticipantMedicalIdentifier<IParticipant, ISite> partMedIdentifier)
			throws BizLogicException, ParticipantManagerException {
		if (isNullobject(partMedIdentifier.getSite())) {
			final ISite site = (ISite) ParticipantManagerUtility
					.getSiteInstance();
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
	public static void postInsert(final Object obj,
			LinkedHashSet<Long> userIdSet) throws BizLogicException {
		final IParticipant participant = (IParticipant) obj;
		// if for CS eMPI is enable then set the eMPI status as pending if its
		// eligible
		if (ParticipantManagerUtility.isEMPIEnable(participant.getId())) {
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
	private static void insertParticipantToProcessingQue(
			final IParticipant participant, LinkedHashSet<Long> userIdSet)
			throws BizLogicException {
		String mrn = null;
		mrn = ParticipantManagerUtility.getMrnValue(participant
				.getParticipantMedicalIdentifierCollection());
		try {
			if (ParticipantManagerUtility
					.isParticipantValidForEMPI(participant.getLastName(),
							participant.getFirstName(), participant
									.getBirthDate(), participant
									.getSocialSecurityNumber(), mrn)) {
				// Process participant for CIDER participant matching.
				ParticipantManagerUtility.addParticipantToProcessMessageQueue(
						userIdSet, participant.getId());
			}
		} catch (DAOException daoException) {
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
	public static void preUpdate(Object oldObj, Object obj,
			SessionDataBean sessionDataBean) throws BizLogicException {
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
	public static void postUpdate(Object oldObj, Object currentObj,
			SessionDataBean sessionDataBean, LinkedHashSet<Long> userIdSet)
			throws BizLogicException, DAOException
	{
		JDBCDAO jdbcdao = null;
		final IParticipant oldParticipant = (IParticipant) oldObj;
		final IParticipant participant = (IParticipant) currentObj;

		try
		{
			jdbcdao = getJDBCDAO();
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
					utility.updateOldEMPIDetails(participant.getId(), oldParticipant.getEmpiId(),
							jdbcdao);

					utility.setEMPIIdStatus(participant.getId(), Constants.EMPI_ID_PENDING, jdbcdao);
					jdbcdao.commit();
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
		catch (DAOException e)
		{
			jdbcdao.rollback();
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}
		catch (Exception e)
		{
			logger.info("ERROR WHILE REGISTERING NEW PATIENT TO EMPI  ##############  \n");
			throw new BizLogicException(null, e, e.getMessage());
		}
		finally
		{
			jdbcdao.closeSession();
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
	private static void regNewPatientToEMPI(IParticipant participant, LinkedHashSet<Long> userIdSet)
			throws BizLogicException, Exception
	{
		JDBCDAO jdbcdao = null;

		try
		{
			String mrn = ParticipantManagerUtility.getMrnValue(participant
					.getParticipantMedicalIdentifierCollection());
			if (ParticipantManagerUtility.isParticipantValidForEMPI(participant.getLastName(),
					participant.getFirstName(), participant.getBirthDate(), participant
							.getSocialSecurityNumber(), mrn))
			{
				String empIdStatus = "";
				jdbcdao = getJDBCDAO();
				//get current empi Id status of the participant
				String query = "SELECT EMPI_ID_STATUS FROM CATISSUE_PARTICIPANT WHERE IDENTIFIER=?";
				LinkedList<ColumnValueBean> colValueBeanList = new LinkedList<ColumnValueBean>();
				colValueBeanList.add(new ColumnValueBean("IDENTIFIER", participant.getId(),
						DBTypes.LONG));
				List<Object> idList = jdbcdao.executeQuery(query, null, colValueBeanList);
				if (null != idList && !idList.isEmpty())
				{
					if (null != idList.get(0))
					{
						Object obj = idList.get(0);
						empIdStatus = ((ArrayList) obj).get(0).toString();
					}
				}

				// Check if participant matches from CIDER are already received before
				// for old participant before the edit happened -- Bug fixed 18824
				LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
				columnValueBeanList.add(new ColumnValueBean(participant.getId()));
				query = "SELECT SEARCHED_PARTICIPANT_ID FROM MATCHED_PARTICIPANT_MAPPING "
						+ "WHERE SEARCHED_PARTICIPANT_ID=?";
				List<Object> partIdList = jdbcdao.executeQuery(query, null, columnValueBeanList);
				boolean isOldMatchesPresent = false;
				if (partIdList != null && !partIdList.isEmpty() && partIdList.get(0) != null
						&& !((List) partIdList.get(0)).isEmpty())
				{
					isOldMatchesPresent = true;
				}

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
			jdbcdao.rollback();
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
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
	 * Gets the participant.
	 *
	 * @param empiId the empi id
	 *
	 * @return the participant
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws DAOException
	 */
	public IParticipant getParticipant(String empiId) throws BizLogicException, DAOException
	{
		IParticipant participant = null;
		final String sourceObjectName = IParticipant.class.getName();
		final String[] selectColumnName = { "id" };
		final QueryWhereClause queryWhereClause = new QueryWhereClause(
				sourceObjectName);
		queryWhereClause.addCondition(new EqualClause(Constants.PARTICIPANT_EMPIID, '?'));
		List<ColumnValueBean> columnValueBeans = new ArrayList<ColumnValueBean>();
		columnValueBeans.add(new ColumnValueBean(empiId));
		/*
		 * final List list = dao .retrieve(sourceObjectName,
		 * selectColumnName, queryWhereClause);
		 */
		final List list = this.retrieve(sourceObjectName, null, queryWhereClause, columnValueBeans);
		if(!list.isEmpty()){
			participant = (IParticipant) list.get(0);
		}
		return participant;
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
	public IUser getUser(final String loginName, final String activityStatus)
			throws BizLogicException, ParticipantManagerException
	{
		IUser validUser = null;
		String userClassName = (String) edu.wustl.common.participant.utility.PropertyHandler
				.getValue(Constants.USER_CLASS);
		final String getActiveUser = "from " + userClassName + " user where user.activityStatus= '"
				+ activityStatus + "' and user.loginName =" + "'" + loginName + "'";
		final List users = this.executeQuery(getActiveUser);
		if (users != null && !users.isEmpty())
		{
			validUser = (IUser) users.get(0);
		}
		return validUser;
	}
	/**
	 * Update participant in database.
	 *
	 * @param newpartcipantObj the new participant.
	 * @param oldPartcipantObj the old participant.
	 *
	 * @return the status
	 */
	public void updateParticipant(final IParticipant newpartcipantObj,final IParticipant oldPartcipantObj)
			throws BizLogicException, ParticipantManagerException
	{
		final String loginName = XMLPropertyHandler.getValue(Constants.HL7_LISTENER_ADMIN_USER);
		final IUser validUser = getUser(loginName, Constants.ACTIVITY_STATUS_ACTIVE);
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
	public ISite getSite(final String siteName) throws ParticipantManagerException, BizLogicException
	{
		ISite site = null;
		String siteClassName = (String) edu.wustl.common.participant.utility.PropertyHandler
				.getValue(Constants.SITE_CLASS);
		final String getSite = "from " + siteClassName + " site where site.name= '"
				+ siteName + "'";
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

}