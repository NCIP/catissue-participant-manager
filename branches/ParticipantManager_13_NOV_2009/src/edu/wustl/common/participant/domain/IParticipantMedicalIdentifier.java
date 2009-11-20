
package edu.wustl.common.participant.domain;


/**
 * The Interface IParticipantMedicalIdentifier.
 */
public interface IParticipantMedicalIdentifier<T, S>
{

	/**
	 * Sets the id.
	 *
	 * @param identifier the new id
	 */
	public void setId(Long identifier);

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public Long getId();

	/**
	 * Gets the medical record number.
	 *
	 * @return the medical record number
	 */
	public String getMedicalRecordNumber();

	/**
	 * Sets the medical record number.
	 *
	 * @param medicalRecordNumber the new medical record number
	 */
	public void setMedicalRecordNumber(String medicalRecordNumber);

	/**
	 * Sets the participant.
	 *
	 * @param participant the new participant
	 */
	public void setParticipant(T participant);


	/**
	 * Gets the site.
	 *
	 * @return the site
	 */
	public S getSite();

	/**
	 * Sets the site.
	 *
	 * @param site the new site
	 */

	public void setSite(S site);
}
