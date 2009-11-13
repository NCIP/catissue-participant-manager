package edu.wustl.common.participant.actionForm;

public interface IParticipantForm {

	public abstract String getLastName();

	public abstract void setLastName(String s);

	public abstract String getFirstName();

	public abstract void setFirstName(String s);

	public abstract String getBirthDate();

	public abstract void setBirthDate(String s);

	public abstract void setOperation(String s);

	public abstract String getOperation();

	public long getId();

	public void setId(long identifier);

	public String getEmpiIdStatus();

	public void setEmpiIdStatus(String empiIdStatus);

	public String getEmpiId();

	public void setEmpiId(String empiId);

}
