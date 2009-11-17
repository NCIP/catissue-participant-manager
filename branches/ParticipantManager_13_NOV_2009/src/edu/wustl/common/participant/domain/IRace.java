
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
	public Long getId();

	/**
	 * Sets the id.
	 *
	 * @param identifier the new id
	 */
	public void setId(Long identifier);

	/**
	 * Gets the race name.
	 *
	 * @return the race name
	 */
	public String getRaceName();

	/**
	 * Sets the race name.
	 *
	 * @param raceName the new race name
	 */
	public void setRaceName(String raceName);

	/**
	 * Sets the participant.
	 *
	 * @param participant the new participant
	 */
	public void setParticipant(T participant);
}
