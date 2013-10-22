package edu.wustl.common.participant.validator;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import edu.wustl.common.cde.CDEManager;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.participant.domain.IEthnicity;
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


public class CommonParticipantValidator
{
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
	public boolean validate(final IParticipant participant,final String operation, final Validator validator)
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
			if (!Status.ACTIVITY_STATUS_ACTIVE.toString().equals(participant.getActivityStatus()) && !Constants.ACTIVITY_STATUS_DRAFT.equals(participant.getActivityStatus()) )
			{
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
    private void validateEthnicity(IParticipant participant) throws BizLogicException
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

}
