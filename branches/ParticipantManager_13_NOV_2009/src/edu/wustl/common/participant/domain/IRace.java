
package edu.wustl.common.participant.domain;

// TODO: Auto-generated Javadoc
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
