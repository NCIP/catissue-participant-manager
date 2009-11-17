
package edu.wustl.common.participant.bizlogic;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.codec.language.Metaphone;

import edu.wustl.catissuecore.domain.ParticipantMedicalIdentifier;
import edu.wustl.catissuecore.domain.Site;
import edu.wustl.common.audit.AuditManager;
import edu.wustl.common.cde.CDEManager;
import edu.wustl.common.domain.AbstractDomainObject;
import edu.wustl.common.exception.AuditException;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.IRace;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.util.Utility;
import edu.wustl.common.util.global.ApplicationProperties;
import edu.wustl.common.util.global.CommonServiceLocator;
import edu.wustl.common.util.global.Status;
import edu.wustl.common.util.global.Validator;
import edu.wustl.dao.DAO;
import edu.wustl.dao.exception.DAOException;

// TODO: Auto-generated Javadoc
/**
 * The Class CommonParticipantBizlogic.
 */
public class CommonParticipantBizlogic
{

	/**
	 * Insert.
	 *
	 * @param obj the obj
	 * @param dao the dao
	 * @param auditManager the audit manager
	 * @param pmi the pmi
	 *
	 * @return the i participant
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws DAOException the DAO exception
	 * @throws AuditException the audit exception
	 */
	public static IParticipant insert(Object obj, DAO dao, AuditManager auditManager,
			IParticipantMedicalIdentifier pmi) throws BizLogicException, DAOException,
			AuditException
	{
		final IParticipant participant = (IParticipant) obj;
		// update metaPhoneInformartion
		final Metaphone metaPhoneObj = new Metaphone();
		final String lNameMetaPhone = metaPhoneObj.metaphone(participant.getLastName());
		participant.setMetaPhoneCode(lNameMetaPhone);
		dao.insert(participant);
		auditManager.insertAudit(dao, participant);
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
			auditManager.insertAudit(dao, pmIdentifier);
		}
		return participant;
	}

	/**
	 * Updates the persistent object in the database.
	 *
	 * @param dao - DAO object
	 * @param participant the participant
	 * @param oldParticipant the old participant
	 * @param auditManager the audit manager
	 *
	 * @throws BizLogicException throws BizLogicException
	 * @throws DAOException the DAO exception
	 * @throws AuditException the audit exception
	 */
	public static void update(DAO dao, IParticipant participant, IParticipant oldParticipant,
			AuditManager auditManager) throws BizLogicException, DAOException, AuditException
	{
		final Metaphone metaPhoneObj = new Metaphone();
		final String lNameMetaPhone = metaPhoneObj.metaphone(participant.getLastName());
		participant.setMetaPhoneCode(lNameMetaPhone);
		dao.update(participant);
		auditManager.updateAudit(dao, participant, oldParticipant);
	}

	/**
	 * Update pmi.
	 *
	 * @param dao the dao
	 * @param auditManager the audit manager
	 * @param oldParticipantMedicalIdentifierCollection the old participant medical identifier collection
	 * @param pmIdentifier the pm identifier
	 *
	 * @throws DAOException the DAO exception
	 * @throws AuditException the audit exception
	 */
	public static void updatePMI(DAO dao, final AuditManager auditManager,
			final Collection oldParticipantMedicalIdentifierCollection,
			final IParticipantMedicalIdentifier pmIdentifier) throws DAOException, AuditException
	{
		if (pmIdentifier.getId() != null)
		{
			dao.update(pmIdentifier);
		}
		else if (pmIdentifier.getId() == null || pmIdentifier.getId().equals(""))
		{
			dao.insert(pmIdentifier);
			auditManager.insertAudit(dao, pmIdentifier);
		}

		// Audit of ParticipantMedicalIdentifier.
		final IParticipantMedicalIdentifier oldPmIdentifier = (IParticipantMedicalIdentifier) getCorrespondingOldObject(
				oldParticipantMedicalIdentifierCollection, pmIdentifier.getId());

		auditManager.updateAudit(dao, pmIdentifier, oldPmIdentifier);
	}

	/**
	 * Gets the corresponding old object.
	 *
	 * @param objectCollection the object collection
	 * @param id the id
	 *
	 * @return the corresponding old object
	 */
	private static Object getCorrespondingOldObject(Collection objectCollection, Long id)
	{
		Iterator iterator = objectCollection.iterator();
		while (iterator.hasNext())
		{
			AbstractDomainObject abstractDomainObject = (AbstractDomainObject) iterator.next();

			if (id != null && id.equals(abstractDomainObject.getId()))
			{
				return abstractDomainObject;
			}
		}
		return null;
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
				final ParticipantMedicalIdentifier participantIdentifier = (ParticipantMedicalIdentifier) itr
						.next();
				final Site site = participantIdentifier.getSite();
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

}