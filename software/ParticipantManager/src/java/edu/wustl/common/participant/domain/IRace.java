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
 * The Interface IRace.
 */
public interface IRace<T>
{

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	Long getId();

	/**
	 * Sets the id.
	 *
	 * @param identifier the new id
	 */
	void setId(Long identifier);

	/**
	 * Gets the race name.
	 *
	 * @return the race name
	 */
	String getRaceName();

	/**
	 * Sets the race name.
	 *
	 * @param raceName the new race name
	 */
	void setRaceName(String raceName);

	/**
	 * Sets the participant.
	 *
	 * @param participant the new participant
	 */
	void setParticipant(T participant);
}
