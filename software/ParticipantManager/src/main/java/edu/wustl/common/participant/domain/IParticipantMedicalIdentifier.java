/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue-participant-manager/LICENSE.txt for details.
 */

package edu.wustl.common.participant.domain;

/**
 * The Interface IParticipantMedicalIdentifier.
 */
public interface IParticipantMedicalIdentifier<P extends IParticipant, S extends ISite>
{

	/**
	 * Sets the id.
	 *
	 * @param identifier the new id
	 */
	void setId(Long identifier);

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	Long getId();

	/**
	 * Gets the medical record number.
	 *
	 * @return the medical record number
	 */
	String getMedicalRecordNumber();

	/**
	 * Sets the medical record number.
	 *
	 * @param medicalRecordNumber the new medical record number
	 */
	void setMedicalRecordNumber(String medicalRecordNumber);

	/**
	 * Sets the participant.
	 *
	 * @param participant the new participant
	 */
	void setParticipant(P participant);

	/**
	 * Gets the site.
	 *
	 * @return the site
	 */
	ISite getSite();

	/**
	 * Sets the site.
	 *
	 * @param site the new site
	 */
	void setSite(S site);
}
