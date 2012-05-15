package edu.wustl.common.participant.bizlogic;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.language.Metaphone;

import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.cde.CDEManager;
import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.exception.ErrorKey;
import edu.wustl.common.lookup.DefaultLookupResult;
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
import edu.wustl.patientLookUp.domain.PatientInformation;

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
				if (site != null)
				{
					boolean checkDuplicate = siteIdset.add(site.getId());
					if (!checkDuplicate)
					{
						//duplicate site present in collection , so find old one delete that one as well.
						throw new BizLogicException(null, null,
							"errors.participant.mediden.duplicate", "");
					}
				}
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
		final IUser validUser = ParticipantManagerUtility.getUser(loginName, Constants.ACTIVITY_STATUS_ACTIVE);
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
		String siteClassName = edu.wustl.common.participant.utility.PropertyHandler
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
		JDBCDAO dao = null;
		List<DefaultLookupResult> matchPartpantLst = null;
		try
		{
			dao = ParticipantManagerUtility.getJDBCDAO();

			final String query = "SELECT * FROM CATISSUE_MATCHED_PARTICIPANT WHERE SEARCHED_PARTICIPANT_ID=? order by ORDER_NO";

			final LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
			columnValueBeanList.add(new ColumnValueBean("SEARCHED_PARTICIPANT_ID", participantId,
					DBTypes.LONG));
			final List matchPartpantLstTemp = dao.executeQuery(query, null, columnValueBeanList);
			matchPartpantLst = populateParticipantList(matchPartpantLstTemp);
		}
		catch (DAOException e)
		{
			//LOGGER.info(e.getMessage());
			throw new DAOException(e.getErrorKey(), e, e.getMsgValues());
		}
		finally
		{
			dao.closeSession();
		}
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
			final List participantValueList = (List) matchPartpantLstTmp.get(i);
			if (!participantValueList.isEmpty() && participantValueList.get(0) != null
					&& !"".equals(participantValueList.get(0)))
			{
				final IParticipant participant = getParticipantObj(participantValueList);
				final DefaultLookupResult result = new DefaultLookupResult();
				result.setObject(participant);
				matchPartpantLst.add(result);
			}
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
	private IParticipant getParticipantObj(final List participantValueList)
			throws BizLogicException, ParseException, ParticipantManagerException
	{
		final IParticipant participant = (IParticipant) ParticipantManagerUtility
				.getParticipantInstance();

		participant.setId(Long.valueOf((String) participantValueList.get(0)));
		participant.setEmpiId((String) participantValueList.get(1));

		final String lastName = (String) participantValueList.get(2);
		final String firstName = (String) participantValueList.get(3);
		final String middleName = (String) participantValueList.get(4);

		participant.setLastName(ParticipantManagerUtility.modifyNameWithProperCase(lastName));
		participant.setFirstName(ParticipantManagerUtility.modifyNameWithProperCase(firstName));
		participant.setMiddleName(ParticipantManagerUtility.modifyNameWithProperCase(middleName));
		if (participantValueList.get(5) != null && !"".equals(participantValueList.get(5)))
		{
			final String dateStr = (String) participantValueList.get(5);
			final Date date = Utility.parseDate(dateStr, Constants.DATE_FORMAT);
			participant.setBirthDate(date);
		}
		participant.setGender((String) participantValueList.get(6));
		participant.setSocialSecurityNumber((String) participantValueList.get(7));
		participant.setActivityStatus((String) participantValueList.get(8));
		if (participantValueList.get(9) != null && !("".equals(participantValueList.get(9))))
		{
			final String dateStr = (String) participantValueList.get(9);
			final Date date = Utility.parseDate(dateStr, Constants.DATE_FORMAT);
			participant.setDeathDate(date);
		}
		participant.setVitalStatus((String) participantValueList.get(10));
		final String mrnString = (String) participantValueList.get(11);
		if (mrnString != null && !"".equals(mrnString))
		{
			final Collection partiMediIdColn = getPartiMediIdColnCollection(mrnString);
			participant.setParticipantMedicalIdentifierCollection(partiMediIdColn);
		}
		final String raceString = (String) participantValueList.get(12);
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
	public static  IParticipant getParticipantObj(PatientInformation patientInfo)
			throws BizLogicException, ParseException, ParticipantManagerException
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
	public void updateParticipantFromEmpi(final IParticipant participant, final IParticipant participantEMPI)
	{
		final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>>  medIdColTemp = new LinkedHashSet<IParticipantMedicalIdentifier<IParticipant, ISite>>();
		final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>>  medIdColLocal = participant.getParticipantMedicalIdentifierCollection();
		if (medIdColLocal != null && !medIdColLocal.isEmpty())
		{
			 final Iterator<IParticipantMedicalIdentifier<IParticipant, ISite>> iterator = medIdColLocal.iterator();
			while (iterator.hasNext())
			{
				Long localSiteID = null;

				 final IParticipantMedicalIdentifier<IParticipant, ISite> partMedIdLocal = iterator.next();
				final String localMRN = partMedIdLocal.getMedicalRecordNumber();
				if (partMedIdLocal.getSite() != null)
				{
					localSiteID = partMedIdLocal.getSite().getId();
				}
				if ((localMRN != null && !"".equals(localMRN))
						&& (localSiteID != null && localSiteID != -1))
				{
					final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>>  medIdColEMPI = participantEMPI
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
		removeAllDuplicateMRNS(participantEMPI.getParticipantMedicalIdentifierCollection());
		participant.setParticipantMedicalIdentifierCollection(participantEMPI.getParticipantMedicalIdentifierCollection());
		participant.setEmpiId(participantEMPI.getEmpiId());
		participant.setEmpiIdStatus(participantEMPI.getEmpiIdStatus());

	}

	private void removeAllDuplicateMRNS(
			Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> participantMedicalIdentifierCollection)
	{
		Set<Long> duplicateSiteId = new HashSet<Long>();
		Set<Long> siteIdSet = new HashSet<Long>();
		for( IParticipantMedicalIdentifier<IParticipant, ISite> partMedId : participantMedicalIdentifierCollection)
		{
			if (partMedId.getSite() != null)
			{
				if(!siteIdSet.add(partMedId.getSite().getId()))
				{
					duplicateSiteId.add(partMedId.getSite().getId());
				}
			}
		}
		final Iterator newPMIiterator = participantMedicalIdentifierCollection.iterator();
		while (newPMIiterator.hasNext())
		{
			final IParticipantMedicalIdentifier<IParticipant, ISite> partiMedobj = (IParticipantMedicalIdentifier<IParticipant, ISite>) newPMIiterator
					.next();
			if (duplicateSiteId.contains(partiMedobj.getSite().getId()))
			{
				newPMIiterator.remove();
			}
		}

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
	private void removeDuplicatesMRN(final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> medIdColEMPI,
			final IParticipantMedicalIdentifier partMedIdLocal, final String localMRN,
			final Long localSiteID, final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> medIdColTemp)
	{
		boolean MRNNotFound = false;
		final Iterator<IParticipantMedicalIdentifier<IParticipant, ISite>> itrEMPI = medIdColEMPI.iterator();
		while (itrEMPI.hasNext())
		{
			MRNNotFound = false;
			Long empiSite = null;
			final IParticipantMedicalIdentifier<IParticipant, ISite> partiMedIdEMPI = itrEMPI.next();
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