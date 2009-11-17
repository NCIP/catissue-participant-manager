package edu.wustl.common.participant.actionForm;

// TODO: Auto-generated Javadoc
/**
 * @author geeta_jaggal
 * The Interface IParticipantForm.
 */
public interface IParticipantForm {

	/**
	 * Gets the last name.
	 *
	 * @return the last name
	 */
	public abstract String getLastName();

	/**
	 * Sets the last name.
	 *
	 * @param s the new last name
	 */
	public abstract void setLastName(String s);

	/**
	 * Gets the first name.
	 *
	 * @return the first name
	 */
	public abstract String getFirstName();

	/**
	 * Sets the first name.
	 *
	 * @param s the new first name
	 */
	public abstract void setFirstName(String s);

	/**
	 * Gets the birth date.
	 *
	 * @return the birth date
	 */
	public abstract String getBirthDate();

	/**
	 * Sets the birth date.
	 *
	 * @param s the new birth date
	 */
	public abstract void setBirthDate(String s);

	/**
	 * Sets the operation.
	 *
	 * @param s the new operation
	 */
	public abstract void setOperation(String s);

	/**
	 * Gets the operation.
	 *
	 * @return the operation
	 */
	public abstract String getOperation();

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public long getId();

	/**
	 * Sets the id.
	 *
	 * @param identifier the new id
	 */
	public void setId(long identifier);

	/**
	 * Gets the empi id status.
	 *
	 * @return the empi id status
	 */
	public String getEmpiIdStatus();

	/**
	 * Sets the empi id status.
	 *
	 * @param empiIdStatus the new empi id status
	 */
	public void setEmpiIdStatus(String empiIdStatus);

	/**
	 * Gets the empi id.
	 *
	 * @return the empi id
	 */
	public String getEmpiId();

	/**
	 * Sets the empi id.
	 *
	 * @param empiId the new empi id
	 */
	public void setEmpiId(String empiId);

}
