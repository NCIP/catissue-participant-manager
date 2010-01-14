
package edu.wustl.common.participant.bizlogic;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.language.Metaphone;

import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.cde.CDEManager;
import edu.wustl.common.domain.AbstractDomainObject;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.IRace;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.Utility;
import edu.wustl.common.util.global.ApplicationProperties;
import edu.wustl.common.util.global.CommonServiceLocator;
import edu.wustl.common.util.global.Status;
import edu.wustl.common.util.global.Validator;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.DAO;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.exception.AuditException;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.query.generator.ColumnValueBean;

// TODO: Auto-generated Javadoc
/**
 * The Class CommonParticipantBizlogic.
 */
public class CommonParticipantBizlogic extends CommonDefaultBizLogic
{

	/** The Constant logger. */
	private static final Logger logger = Logger.getCommonLogger(CommonParticipantBizlogic.class);

	/**
	 * Insert.
	 *
	 * @param obj the obj
	 * @param dao the dao
	 * @param pmi the pmi
	 *
	 * @return the i participant
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws DAOException the DAO exception
	 */
	public static IParticipant insert(Object obj, DAO dao, IParticipantMedicalIdentifier pmi)
			throws BizLogicException, DAOException
	{
		final IParticipant participant = (IParticipant) obj;
		// update metaPhoneInformartion
		setMetaPhoneCode(participant);
		dao.insert(participant);
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
		// Inserting medical identifiers in the database after setting the
		// participant associated.
		final Iterator<IParticipantMedicalIdentifier<IParticipant, ISite>> iterator = pmiCollection
				.iterator();
		while (iterator.hasNext())
		{
			final IParticipantMedicalIdentifier pmIdentifier = (IParticipantMedicalIdentifier) iterator
					.next();
			pmIdentifier.setParticipant(participant);
			dao.insert(pmIdentifier);
		}
		return participant;
	}

	/**
	 * Updates the persistent object in the database.
	 *
	 * @param dao - DAO object
	 * @param participant the participant
	 * @param oldParticipant the old participant
	 *
	 * @throws BizLogicException throws BizLogicException
	 * @throws DAOException the DAO exception
	 */
	public static void update(DAO dao, IParticipant participant, IParticipant oldParticipant)
			throws BizLogicException, DAOException
	{

		setMetaPhoneCode(participant);
		dao.update(participant, oldParticipant);
	}

	/**
	 * Sets the meta phone code.
	 *
	 * @param participant the new meta phone code
	 */
	private static void setMetaPhoneCode(IParticipant participant)
	{
		final Metaphone metaPhoneObj = new Metaphone();
		final String lNameMetaPhone = metaPhoneObj.metaphone(participant.getLastName());
		participant.setMetaPhoneCode(lNameMetaPhone);
	}

	/**
	 * Update pmi.
	 *
	 * @param dao the dao
	 * @param pmIdentifier the pm identifier
	 * @param oldPartiMedIdCollection the old parti med id collection
	 *
	 * @throws DAOException the DAO exception
	 */
	public static void updatePMI(DAO dao, final Collection oldPartiMedIdCollection,
			final IParticipantMedicalIdentifier pmIdentifier) throws DAOException
	{
		if (pmIdentifier.getId() != null)
		{
			final IParticipantMedicalIdentifier oldPmIdentifier = (IParticipantMedicalIdentifier) getCorrespondingOldObj(
					oldPartiMedIdCollection, pmIdentifier.getId());
			dao.update(pmIdentifier, oldPmIdentifier);
		}
		else if (pmIdentifier.getId() == null || pmIdentifier.getId().equals(""))
		{
			dao.insert(pmIdentifier);
		}
	}

	/**
	 * This method gets Corresponding Old Object.
	 *
	 * @param objectCollection object Collection
	 * @param identifier id.
	 *
	 * @return Object.
	 */
	public static Object getCorrespondingOldObj(Collection objectCollection, Long identifier)
	{
		Iterator iterator = objectCollection.iterator();
		AbstractDomainObject abstractDomainObject = null;
		while (iterator.hasNext())
		{
			AbstractDomainObject abstractDomainObj = (AbstractDomainObject) iterator.next();

			if (identifier != null && identifier.equals(abstractDomainObj.getId()))
			{
				abstractDomainObject = abstractDomainObj;
				break;
			}
		}
		return abstractDomainObject;
	}

	/**
	 * Validate.
	 *
	 * @param dao : DAO object. Overriding the parent class's method to validate
	 * the enumerated attribute values.
	 * @param participant the participant
	 * @param operation the operation
	 * @param validator the validator
	 *
	 * @return true, if validate
	 *
	 * @throws BizLogicException the biz logic exception
	 */
	public static boolean validate(IParticipant participant, DAO dao, String operation,
			final Validator validator) throws BizLogicException
	{
		String message = "";
		if (participant == null)
		{

			throw new BizLogicException(null, null, "domain.object.null.err.msg", "Participant");
		}

		String errorKeyForBirthDate = "";
		String errorKeyForDeathDate = "";

		final String birthDate = Utility.parseDateToString(participant.getBirthDate(),
				CommonServiceLocator.getInstance().getDatePattern());
		if (!validator.isEmpty(birthDate))
		{
			errorKeyForBirthDate = validator.validateDate(birthDate, true);
			if (errorKeyForBirthDate.trim().length() > 0)
			{
				message = ApplicationProperties.getValue("participant.birthDate");
				throw new BizLogicException(null, null, errorKeyForBirthDate, message);
			}
		}

		final String deathDate = Utility.parseDateToString(participant.getDeathDate(),
				CommonServiceLocator.getInstance().getDatePattern());
		if (!validator.isEmpty(deathDate))
		{
			errorKeyForDeathDate = validator.validateDate(deathDate, true);
			if (errorKeyForDeathDate.trim().length() > 0)
			{
				message = ApplicationProperties.getValue("participant.deathDate");
				throw new BizLogicException(null, null, errorKeyForDeathDate, message);
			}
		}

		if (participant.getVitalStatus() == null || !participant.getVitalStatus().equals("Dead"))
		{
			if (!validator.isEmpty(deathDate))
			{
				throw new BizLogicException(null, null, "participant.invalid.enddate", "");
			}
		}
		if ((!validator.isEmpty(birthDate) && !validator.isEmpty(deathDate))
				&& (errorKeyForDeathDate.trim().length() == 0 && errorKeyForBirthDate.trim()
						.length() == 0))
		{
			final boolean errorKey1 = validator.compareDates(Utility.parseDateToString(participant
					.getBirthDate(), CommonServiceLocator.getInstance().getDatePattern()), Utility
					.parseDateToString(participant.getDeathDate(), CommonServiceLocator
							.getInstance().getDatePattern()));

			if (!errorKey1)
			{

				throw new BizLogicException(null, null, "participant.invaliddate", "");
			}
		}

		if (!validator.isEmpty(participant.getSocialSecurityNumber()))
		{
			if (!validator.isValidSSN(participant.getSocialSecurityNumber()))
			{
				message = ApplicationProperties.getValue("participant.socialSecurityNumber");
				throw new BizLogicException(null, null, "errors.invalid", message);
			}
		}

		if (!validator.isEmpty(participant.getVitalStatus()))
		{
			final List vitalStatusList = CDEManager.getCDEManager().getPermissibleValueList(
					Constants.CDE_VITAL_STATUS, null);
			if (!Validator.isEnumeratedOrNullValue(vitalStatusList, participant.getVitalStatus()))
			{
				throw new BizLogicException(null, null, "participant.gender.errMsg", "");
			}
		}

		if (!validator.isEmpty(participant.getGender()))
		{
			final List genderList = CDEManager.getCDEManager().getPermissibleValueList(
					Constants.CDE_NAME_GENDER, null);

			if (!Validator.isEnumeratedOrNullValue(genderList, participant.getGender()))
			{
				throw new BizLogicException(null, null, "participant.gender.errMsg", "");
			}
		}

		if (!validator.isEmpty(participant.getSexGenotype()))
		{
			final List genotypeList = CDEManager.getCDEManager().getPermissibleValueList(
					Constants.CDE_NAME_GENOTYPE, null);
			if (!Validator.isEnumeratedOrNullValue(genotypeList, participant.getSexGenotype()))
			{
				throw new BizLogicException(null, null, "participant.genotype.errMsg", "");
			}
		}

		final Collection paticipantMedicicalCollection = participant
				.getParticipantMedicalIdentifierCollection();
		if (paticipantMedicicalCollection != null && !paticipantMedicicalCollection.isEmpty())
		{
			final Iterator itr = paticipantMedicicalCollection.iterator();
			while (itr.hasNext())
			{
				final IParticipantMedicalIdentifier participantIdentifier = (IParticipantMedicalIdentifier) itr
						.next();
				final ISite site = (ISite) participantIdentifier.getSite();
				final String medicalRecordNo = participantIdentifier.getMedicalRecordNumber();
				if (validator.isEmpty(medicalRecordNo) || site == null || site.getId() == null)
				{
					throw new BizLogicException(null, null, "errors.participant.extiden.missing",
							"");
				}
			}
		}

		final Collection raceCollection = participant.getRaceCollection();
		if (raceCollection != null && !raceCollection.isEmpty())
		{
			final List raceList = CDEManager.getCDEManager().getPermissibleValueList(
					Constants.CDE_NAME_RACE, null);
			final Iterator itr = raceCollection.iterator();
			while (itr.hasNext())
			{
				final IRace race = (IRace) itr.next();
				if (race != null)
				{
					final String raceName = (String) race.getRaceName();
					if (!validator.isEmpty(raceName)
							&& !Validator.isEnumeratedOrNullValue(raceList, raceName))
					{
						throw new BizLogicException(null, null, "participant.race.errMsg", "");
					}
				}
			}
		}

		if (!validator.isEmpty(participant.getEthnicity()))
		{
			final List ethnicityList = CDEManager.getCDEManager().getPermissibleValueList(
					Constants.CDE_NAME_ETHNICITY, null);
			if (!Validator.isEnumeratedOrNullValue(ethnicityList, participant.getEthnicity()))
			{
				throw new BizLogicException(null, null, "participant.ethnicity.errMsg", "");
			}
		}

		if (operation.equals(Constants.ADD))
		{
			if (!Status.ACTIVITY_STATUS_ACTIVE.toString().equals(participant.getActivityStatus()))
			{
				throw new BizLogicException(null, null, "activityStatus.active.errMsg", "");
			}
		}
		else
		{
			if (!Validator.isEnumeratedValue(Constants.ACTIVITY_STATUS_VALUES, participant
					.getActivityStatus()))
			{
				throw new BizLogicException(null, null, "activityStatus.errMsg", "");
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see edu.wustl.common.bizlogic.AbstractBizLogic#update(edu.wustl.dao.DAO, java.lang.Object, java.lang.Object, edu.wustl.common.beans.SessionDataBean)
	 */
	@Override
	protected void update(DAO dao, Object obj, Object oldObj, SessionDataBean sessionDataBean)
			throws BizLogicException
	{
		final IParticipant participant = (IParticipant) obj;
		modifyParticipantObject(dao, sessionDataBean, participant);

	}

	/**
	 * Modify participant object.
	 *
	 * @param dao the dao
	 * @param sessionDataBean the session data bean
	 * @param participant the participant
	 *
	 * @throws BizLogicException the biz logic exception
	 */
	public void modifyParticipantObject(DAO dao, SessionDataBean sessionDataBean,
			IParticipant participant) throws BizLogicException
	{
		IParticipant oldParticipant;
		DAO cleanDAO = null;
		try
		{
			cleanDAO = ParticipantManagerUtility.getDAO();
			oldParticipant = ParticipantManagerUtility.getOldParticipant(cleanDAO, participant
					.getId());
			updateParticipant(dao, sessionDataBean, participant, oldParticipant);
			pmiUpdate(dao, participant, oldParticipant);
		}
		catch (DAOException e)
		{
			throw new BizLogicException(e);
		}
		catch (BizLogicException biz)
		{
			logger.debug(biz.getMessage(), biz);
			throw getBizLogicException(biz, biz.getErrorKeyName(), biz.getMsgValues());

		}
		catch (Exception exception)
		{
			throw getBizLogicException(exception, "Error while updating object", "");
		}
		finally
		{
			try
			{
				if (cleanDAO != null)
				{
					cleanDAO.closeSession();
				}
			}
			catch (DAOException daoExp)
			{
				logger.debug(daoExp.getMessage(), daoExp);
				throw getBizLogicException(daoExp, daoExp.getErrorKeyName(), daoExp.getMsgValues());
			}
		}
	}

	/**
	 * This method will update Participant Object.
	 *
	 * @param participant Participant object
	 * @param oldParticipant Persistent participant object
	 * @param dao DAO Object
	 * @param sessionDataBean SessionDataBean Object
	 *
	 * @return AuditManager
	 *
	 * @throws BizLogicException BizLogicException Exception
	 * @throws DAOException DAOException Exception
	 * @throws AuditException AuditException Exception
	 */
	private void updateParticipant(DAO dao, SessionDataBean sessionDataBean,
			final IParticipant participant, final IParticipant oldParticipant)
			throws BizLogicException, DAOException
	{
		update(dao, participant, oldParticipant);
	}

	/**
	 * Pmi update.
	 *
	 * @param dao the dao
	 * @param participant the participant
	 * @param oldParticipant the old participant
	 *
	 * @throws DAOException the DAO exception
	 * @throws BizLogicException the biz logic exception
	 */
	private void pmiUpdate(DAO dao, IParticipant participant, IParticipant oldParticipant)
			throws DAOException, BizLogicException
	{
		Collection oldPartMedIdColln = (Collection) oldParticipant
				.getParticipantMedicalIdentifierCollection();
		Collection partiMedIdColln = participant.getParticipantMedicalIdentifierCollection();
		Iterator iterator = partiMedIdColln.iterator();
		while (iterator.hasNext())
		{
			IParticipantMedicalIdentifier pmIdentifier = (IParticipantMedicalIdentifier) iterator
					.next();
			setParticipantMedicalIdentifierDefault(pmIdentifier);
			pmIdentifier.setParticipant(participant);
			updatePMI(dao, oldPartMedIdColln, pmIdentifier);
		}
	}

	/**
	 * check not null.
	 *
	 * @param object object
	 *
	 * @return boolean
	 */

	public static boolean isNullobject(Object object)
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
	 * @param partMedIdentifier the part med identifier
	 *
	 * @throws BizLogicException the biz logic exception
	 */
	public static void setParticipantMedicalIdentifierDefault(
			IParticipantMedicalIdentifier partMedIdentifier) throws BizLogicException
	{
		if (isNullobject(partMedIdentifier.getSite()))
		{

			partMedIdentifier.setSite(ParticipantManagerUtility.getSiteInstance());
		}
	}

	//	/**
	//	 * This method will be called to return the Audit manager.
	//	 *
	//	 * @param sessionDataBean the session data bean
	//	 *
	//	 * @return the audit manager
	//	 */
	//	public AuditManager getAuditManager(SessionDataBean sessionDataBean)
	//	{
	//
	//		AuditManager auditManager = new AuditManager();
	//		if (sessionDataBean == null)
	//		{
	//			auditManager.setUserId(null);
	//		}
	//		else
	//		{
	//			auditManager.setUserId(sessionDataBean.getUserId());
	//			auditManager.setIpAddress(sessionDataBean.getIpAddress());
	//		}
	//		return auditManager;
	//
	//	}

	/**
	 * Post insert.
	 *
	 * @param obj the obj
	 * @param sessionDataBean the session data bean
	 *
	 * @throws DAOException the DAO exception
	 */
	public static void postInsert(Object obj, SessionDataBean sessionDataBean) throws DAOException
	{
		final IParticipant participant = (IParticipant) obj;
		// if for CS eMPI is enable then set the eMPI status as pending if its eligible
		if (ParticipantManagerUtility.isEMPIEnable(participant.getId()))
		{
			if (ParticipantManagerUtility.isParticipantValidForEMPI(participant.getLastName(),
					participant.getFirstName(), participant.getBirthDate()))
			{
				// Process participant for CIDER participant matching.
				ParticipantManagerUtility.addParticipantToProcessMessageQueue(sessionDataBean
						.getUserId(), participant.getId());
			}
		}

	}

	/**
	 * Pre update.
	 *
	 * @param obj the obj
	 * @param sessionDataBean the session data bean
	 *
	 * @throws BizLogicException the biz logic exception
	 */
	public static void preUpdate(Object oldObj, Object obj, SessionDataBean sessionDataBean)
			throws BizLogicException
	{
		final IParticipant participant = (IParticipant) obj;
		IParticipant oldParticipant = (IParticipant) oldObj;
		String oldEMPIStatus = oldParticipant.getEmpiIdStatus();
		try
		{
			if (ParticipantManagerUtility.isEMPIEnable(participant.getId()))
			{
				// if the
				if (oldEMPIStatus != null && !("".equals(oldEMPIStatus)))
				{
					if (Constants.EMPI_ID_CREATED.equals(participant.getEmpiIdStatus()))
					{
						participant.setEmpiIdStatus(Constants.EMPI_ID_PENDING);
					}
				}
			}
		}
		catch (DAOException e)
		{
			throw new BizLogicException(e);
		}
	}

	/**
	 * Post update.
	 *
	 * @param obj the obj
	 * @param sessionDataBean the session data bean
	 *
	 * @throws BizLogicException the biz logic exception
	 */
	public static void postUpdate(Object obj, SessionDataBean sessionDataBean)
			throws BizLogicException
	{
		final IParticipant participant = (IParticipant) obj;
		try
		{
			if (ParticipantManagerUtility.isEMPIEnable(participant.getId()))
			{
				regNewPatientToEMPI(participant);
			}
		}
		catch (Exception e)
		{
			logger.info("ERROR WHILE REGISTERING NEW PATIENT TO EMPI  ##############  \n");
			throw new BizLogicException(null,e,e.getMessage());
		}
	}

	/**
	 * Reg new patient to empi.
	 *
	 * @param participant the participant
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws Exception the exception
	 */
	private static void regNewPatientToEMPI(IParticipant participant) throws BizLogicException,
			Exception
	{
		String permanentPartiId = null;
		String tempararyPartiId = null;
		String oldeMPIId = null;
		oldeMPIId = participant.getEmpiId();
		tempararyPartiId = participant.getId() + "T";
		if (ParticipantManagerUtility.isParticipantValidForEMPI(participant.getLastName(),
				participant.getFirstName(), participant.getBirthDate()))
		{
			if (oldeMPIId != null && oldeMPIId != "")
			{
				permanentPartiId = String.valueOf(participant.getId());
				mapParticipantId(oldeMPIId, permanentPartiId, tempararyPartiId);
			}
			if (!participant.getEmpiIdStatus().equals(Constants.EMPI_ID_CREATED))
			{
				participant.setEmpiId("");
				sendHL7RegMes(participant, tempararyPartiId);
			}
		}
	}

	/**
	 * Map participant id.
	 *
	 * @param oldeMPIId the olde mpi id
	 * @param permanentPartiId the permanent parti id
	 * @param tempararyPartiId the temparary parti id
	 *
	 * @throws DAOException the DAO exception
	 */
	private static void mapParticipantId(String oldeMPIId, String permanentPartiId,
			String tempararyPartiId) throws DAOException
	{
		JDBCDAO jdbcDao = null;
		try
		{
			LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
			LinkedList columnValueBeanList = new LinkedList();
			columnValueBeanList.add(new ColumnValueBean(permanentPartiId));
			columnValueBeanList.add(new ColumnValueBean(tempararyPartiId));
			columnValueBeanList.add(new ColumnValueBean(oldeMPIId));
			jdbcDao = ParticipantManagerUtility.getJDBCDAO();
			String sql = "INSERT INTO PARTICIPANT_EMPI_ID_MAPPING VALUES(?,?,?)";

			columnValueBeans.add(columnValueBeanList);
			jdbcDao.executeUpdate(sql, columnValueBeans);
			jdbcDao.commit();
		}
		catch (DAOException e)
		{
			logger.info("ERROE WHILE UPDATING THE PARTICIPANT EMPI STATUS");
			throw new DAOException(e.getErrorKey(), e, e.getMsgValues());
		}
		finally
		{
			jdbcDao.closeSession();
		}
	}

	/**
	 * Send h l7 reg mes.
	 *
	 * @param participant the participant
	 * @param tempararyPartiId the temparary parti id
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws Exception the exception
	 */
	private static void sendHL7RegMes(IParticipant participant, String tempararyPartiId)
			throws BizLogicException, Exception
	{

		//IParticipant participant = (IParticipant) ParticipantManagerUtility.getParticipantInstance();
		//((AbstractDomainObject) participant).setAllValues((AbstractActionForm) participantForm);
		//participant.setId(participantForm.getId());

		EMPIParticipantRegistrationBizLogic eMPIPartiReg = new EMPIParticipantRegistrationBizLogic();
		eMPIPartiReg.setTempMrnId(tempararyPartiId);
		eMPIPartiReg.registerPatientToeMPI(participant);
	}

}