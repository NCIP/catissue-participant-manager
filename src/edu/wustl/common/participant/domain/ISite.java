
package edu.wustl.common.participant.domain;

// TODO: Auto-generated Javadoc
/**
 * The Interface ISite.
 */
public interface ISite
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
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName();

	/**
	 * Sets the name.
	 *
	 * @param name the new name
	 */
	public void setName(String name);

	public String getFacilityId();

	public void setFacilityId(String facilityId);
}
