/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue-participant-manager/LICENSE.txt for details.
 */

package edu.wustl.common.participant.bizlogic;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.IRace;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerException;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.Utility;
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

		final Collection paticipantMedCol = participant
				.getParticipantMedicalIdentifierCollection();
		// Created a new PMI collection for bulk operation functionality.
		final Collection newPMICollection = new LinkedHashSet();
		if (paticipantMedCol != null && !paticipantMedCol.isEmpty()) {
			final Iterator itr = paticipantMedCol.iterator();
			while (itr.hasNext()) {
				final IParticipantMedicalIdentifier<IParticipant, ISite> partiMedobj = (IParticipantMedicalIdentifier<IParticipant, ISite>) itr
						.next();
				final ISite site = partiMedobj.getSite();
				final String medicalRecordNo = partiMedobj
						.getMedicalRecordNumber();
				if (validator.isEmpty(medicalRecordNo) || site == null
						|| site.getId() == null) {
					if (partiMedobj.getId() == null) {
						throw new BizLogicException(null, null,
								"errors.participant.extiden.missing", "");
					}
				} else {
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

		if (!validator.isEmpty(participant.getEthnicity())) {
			final List ethnicityList = CDEManager
					.getCDEManager()
					.getPermissibleValueList(Constants.CDE_NAME_ETHNICITY, null);
			if (!Validator.isEnumeratedOrNullValue(ethnicityList, participant
					.getEthnicity())) {
				throw new BizLogicException(null, null,
						"participant.ethnicity.errMsg", "");
			}
		}

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
		final IParticipant participant = (IParticipant) obj;
		final IParticipant oldParticipant = (IParticipant) oldObj;
		final String oldEMPIStatus = oldParticipant.getEmpiIdStatus();

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
			throws BizLogicException, DAOException {

		JDBCDAO jdbcdao = null;
		String query = null;
		String queryForStatusUpdate = "";
		String temporaryParticipantId = "";
		final IParticipant oldParticipant = (IParticipant) oldObj;
		final IParticipant participant = (IParticipant) currentObj;

		try {
			jdbcdao = getJDBCDAO();
			if (ParticipantManagerUtility.isEMPIEnable(participant.getId())
					&& ParticipantManagerUtility.isParticipantEdited(
							oldParticipant, participant)) {

				// if user resolved match by selecting a record from the grid, then insert entry into PARTICIPANT_EMPI_ID_MAPPING table so that HL7 msg is sent with tmpMRN Id
				if(null!=participant.getGridValueSelected()&&participant.getGridValueSelected().equals(Constants.YES)&&null!=oldParticipant.getEmpiId()){
						temporaryParticipantId = participant.getId() + "T";
						final LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
						final LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
						columnValueBeanList.add(new ColumnValueBean("PERMANENT_PARTICIPANT_ID",
								participant.getId(), DBTypes.VARCHAR));
						columnValueBeanList.add(new ColumnValueBean("TEMPARARY_PARTICIPANT_ID",
								temporaryParticipantId, DBTypes.VARCHAR));
						columnValueBeanList.add(new ColumnValueBean("OLD_EMPI_ID",
								oldParticipant.getEmpiId(), DBTypes.VARCHAR));
						columnValueBeanList.add(new ColumnValueBean("TEMPMRNDATE",new Timestamp(new Date().getTime())
								, DBTypes.TIMESTAMP));
						final String sql = "INSERT INTO PARTICIPANT_EMPI_ID_MAPPING VALUES(?,?,?,?)";
						columnValueBeans.add(columnValueBeanList);
						jdbcdao.executeUpdate(sql, columnValueBeans);

						queryForStatusUpdate = "UPDATE CATISSUE_PARTICIPANT SET EMPI_ID_STATUS = 'PENDING' WHERE IDENTIFIER = "
								+ participant.getId();
						jdbcdao.executeUpdate(queryForStatusUpdate);
						jdbcdao.commit();
				}

					// to send for participant matching when status is CREATED
					if (edu.wustl.common.participant.utility.Constants.EMPI_ID_CREATED
							.equals(oldParticipant.getEmpiIdStatus())) {
						regNewPatientToEMPI(participant, userIdSet);
					}
			}



		}

		catch (DAOException e) {
			jdbcdao.rollback();
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}


		catch (Exception e) {
			logger
					.info("ERROR WHILE REGISTERING NEW PATIENT TO EMPI  ##############  \n");
			throw new BizLogicException(null, e, e.getMessage());
		}

		finally {
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
	private static void regNewPatientToEMPI(IParticipant participant,
			LinkedHashSet<Long> userIdSet) throws BizLogicException, Exception {

		String permanentPartiId = null;
		String query="";
		JDBCDAO jdbcdao = null;
		String oldeMPIId = null;
		String empIdStatus="";
		oldeMPIId = participant.getEmpiId();
		// tempararyPartiId = participant.getId() + "T";
		String mrn = ParticipantManagerUtility.getMrnValue(participant
				.getParticipantMedicalIdentifierCollection());

		try {
			jdbcdao = getJDBCDAO();
			//get current empi Id status of the participant
			query = "SELECT EMPI_ID_STATUS FROM CATISSUE_PARTICIPANT WHERE IDENTIFIER=?";
			LinkedList<ColumnValueBean> colValueBeanList = new LinkedList<ColumnValueBean>();
			colValueBeanList.add(new ColumnValueBean(
					"IDENTIFIER", participant.getId(),
					DBTypes.LONG));
			List<Object> idList = jdbcdao.executeQuery(query, null,colValueBeanList);
			if(null!=idList &&idList.size()>0){
				if (null != idList.get(0)) {
					Object obj = idList.get(0);
					empIdStatus = ((ArrayList) obj).get(0)
							.toString();
				}
			}

		if (ParticipantManagerUtility.isParticipantValidForEMPI(participant
				.getLastName(), participant.getFirstName(), participant
				.getBirthDate(), participant.getSocialSecurityNumber(), mrn)) {

			// go for participant matching only when status is 'CREATED'
			if (empIdStatus.equals(Constants.EMPI_ID_CREATED)) {

				participant.setEmpiId("");

				// Process participant for CIDER participant matching.
				ParticipantManagerUtility.addParticipantToProcessMessageQueue(
						userIdSet, participant.getId());
				// sendHL7RegMes(participant, tempararyPartiId);
			}
		}
		}catch (DAOException e) {
			jdbcdao.rollback();
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}

		finally {
			jdbcdao.closeSession();
		}
	}

	/**
	 * Map participant id.
	 *
	 * @param oldeMPIId
	 *            the olde mpi id
	 * @param permanentPartiId
	 *            the permanent parti id
	 * @param tempararyPartiId
	 *            the temparary parti id
	 *
	 * @throws DAOException
	 *             the DAO exception
	 */
	private static void mapParticipantId(String oldeMPIId,
			String permanentPartiId, String tempararyPartiId)
			throws DAOException {
		JDBCDAO jdbcDao = null;
		try {
			final LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
			final LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
			columnValueBeanList.add(new ColumnValueBean(permanentPartiId));
			columnValueBeanList.add(new ColumnValueBean(tempararyPartiId));
			columnValueBeanList.add(new ColumnValueBean(oldeMPIId));
			jdbcDao = ParticipantManagerUtility.getJDBCDAO();
			final String sql = "INSERT INTO PARTICIPANT_EMPI_ID_MAPPING VALUES(?,?,?)";

			columnValueBeans.add(columnValueBeanList);
			jdbcDao.executeUpdate(sql, columnValueBeans);
			jdbcDao.commit();
		} catch (DAOException e) {
			logger.info("ERROE WHILE UPDATING THE PARTICIPANT EMPI STATUS");
			throw new DAOException(e.getErrorKey(), e, e.getMsgValues());
		} finally {
			jdbcDao.closeSession();
		}
	}

	/**
	 * Send h l7 reg mes.
	 *
	 * @param participant
	 *            the participant
	 * @param tempararyPartiId
	 *            the temparary parti id
	 *
	 * @throws BizLogicException
	 *             the biz logic exception
	 * @throws Exception
	 *             the exception
	 */
	private static void sendHL7RegMes(IParticipant participant,
			String tempararyPartiId) throws BizLogicException, Exception {

		// IParticipant participant = (IParticipant)
		// ParticipantManagerUtility.getParticipantInstance();
		// ((AbstractDomainObject)
		// participant).setAllValues((AbstractActionForm) participantForm);
		// participant.setId(participantForm.getId());

		final EMPIParticipantRegistrationBizLogic eMPIPartiReg = new EMPIParticipantRegistrationBizLogic();
		eMPIPartiReg.setTempMrnId(tempararyPartiId);
		eMPIPartiReg.registerPatientToeMPI(participant);
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


}