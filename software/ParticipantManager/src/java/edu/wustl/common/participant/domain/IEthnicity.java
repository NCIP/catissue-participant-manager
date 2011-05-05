
package edu.wustl.common.participant.domain;


/**
 * The Interface IRace.
 */
public interface IEthnicity<T>
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
	 */
	String getName();

	/**
	 */
	void setName(String name);

	/**
	 * Sets the participant.
	 *
	 * @param participant the new participant
	 */
	void setParticipant(T participant);
}
