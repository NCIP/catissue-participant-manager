
package edu.wustl.common.participant.domain;

// TODO: Auto-generated Javadoc
/**
 * The Interface IUser.
 */
public interface IUser
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
	 * Gets the role id.
	 *
	 * @return the role id
	 */
	public String getRoleId();

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
	 * Gets the csm user id.
	 *
	 * @return the csm user id
	 */
	public Long getCsmUserId();

	/**
	 * Sets the csm user id.
	 *
	 * @param csmUserId the new csm user id
	 */
	public void setCsmUserId(Long csmUserId);
}
