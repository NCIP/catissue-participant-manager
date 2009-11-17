
package edu.wustl.common.participant.domain;

import java.util.Collection;
import java.util.Date;

// TODO: Auto-generated Javadoc
/**
 * The Interface IParticipant.
 */
public interface IParticipant
{

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public Long getId();

	/**
	 * Sets the id.
	 *
	 * @param identifier the new id
	 */
	public void setId(Long identifier);

	/**
	 * Gets the last name.
	 *
	 * @return the last name
	 */
	public String getLastName();

	/**
	 * Sets the last name.
	 *
	 * @param lastName the new last name
	 */
	public void setLastName(String lastName);

	/**
	 * Gets the first name.
	 *
	 * @return the first name
	 */
	public String getFirstName();

	/**
	 * Sets the first name.
	 *
	 * @param firstName the new first name
	 */
	public void setFirstName(String firstName);

	/**
	 * Gets the middle name.
	 *
	 * @return the middle name
	 */
	public String getMiddleName();

	/**
	 * Sets the middle name.
	 *
	 * @param middleName the new middle name
	 */
	public void setMiddleName(String middleName);

	/**
	 * Gets the birth date.
	 *
	 * @return the birth date
	 */
	public Date getBirthDate();

	/**
	 * Sets the birth date.
	 *
	 * @param birthDate the new birth date
	 */
	public void setBirthDate(Date birthDate);

	/**
	 * Gets the gender.
	 *
	 * @return the gender
	 */
	public String getGender();

	/**
	 * Sets the gender.
	 *
	 * @param gender the new gender
	 */
	public void setGender(String gender);

	/**
	 * Gets the sex genotype.
	 *
	 * @return the sex genotype
	 */
	public String getSexGenotype();

	/**
	 * Sets the sex genotype.
	 *
	 * @param sexGenotype the new sex genotype
	 */
	public void setSexGenotype(String sexGenotype);

	/**
	 * Gets the race collection.
	 *
	 * @return the race collection
	 */
	public Collection getRaceCollection();

	/**
	 * Sets the race collection.
	 *
	 * @param raceCollection the new race collection
	 */
	public void setRaceCollection(Collection raceCollection);

	/**
	 * Gets the ethnicity.
	 *
	 * @return the ethnicity
	 */
	public String getEthnicity();

	/**
	 * Sets the ethnicity.
	 *
	 * @param ethnicity the new ethnicity
	 */
	public void setEthnicity(String ethnicity);

	/**
	 * Gets the social security number.
	 *
	 * @return the social security number
	 */
	public String getSocialSecurityNumber();

	/**
	 * Sets the social security number.
	 *
	 * @param socialSecurityNumber the new social security number
	 */
	public void setSocialSecurityNumber(String socialSecurityNumber);

	/**
	 * Gets the activity status.
	 *
	 * @return the activity status
	 */
	public String getActivityStatus();

	/**
	 * Sets the activity status.
	 *
	 * @param activityStatus the new activity status
	 */
	public void setActivityStatus(String activityStatus);

	/**
	 * Gets the death date.
	 *
	 * @return the death date
	 */
	public Date getDeathDate();

	/**
	 * Sets the death date.
	 *
	 * @param deathDate the new death date
	 */
	public void setDeathDate(Date deathDate);

	/**
	 * Gets the vital status.
	 *
	 * @return the vital status
	 */
	public String getVitalStatus();

	/**
	 * Sets the vital status.
	 *
	 * @param vitalStatus the new vital status
	 */
	public void setVitalStatus(String vitalStatus);

	/**
	 * Gets the participant medical identifier collection.
	 *
	 * @return the participant medical identifier collection
	 */
	public Collection getParticipantMedicalIdentifierCollection();

	/**
	 * Sets the participant medical identifier collection.
	 *
	 * @param participantMedicalIdentifierCollection the new participant medical identifier collection
	 */
	public void setParticipantMedicalIdentifierCollection(
			Collection participantMedicalIdentifierCollection);

	/**
	 * Gets the meta phone code.
	 *
	 * @return the meta phone code
	 */
	public String getMetaPhoneCode();

	/**
	 * Sets the meta phone code.
	 *
	 * @param metaPhoneCode the new meta phone code
	 */
	public void setMetaPhoneCode(String metaPhoneCode);

	/**
	 * Gets the empi id.
	 *
	 * @return the empi id
	 */
	public String getEmpiId();

	/**
	 * Gets the empi id status.
	 *
	 * @return the empi id status
	 */
	public String getEmpiIdStatus();

	/**
	 * Sets the empi id.
	 *
	 * @param empiId the new empi id
	 */
	public void setEmpiId(String empiId);

	/**
	 * Sets the empi id status.
	 *
	 * @param empiIdStatus the new empi id status
	 */
	public void setEmpiIdStatus(String empiIdStatus);

	/**
	 * Gets the checks if is from empi.
	 *
	 * @return the checks if is from empi
	 */
	public String getIsFromEMPI();

	/**
	 * Sets the checks if is from empi.
	 *
	 * @param isFromEMPI the new checks if is from empi
	 */
	public void setIsFromEMPI(String isFromEMPI);

}
