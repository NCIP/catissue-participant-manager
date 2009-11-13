package edu.wustl.common.participant.domain;


public interface IUser
{
	public Long getId();
	public void setId(Long identifier);
	public String getRoleId();
	public String getFirstName();
	public void setFirstName(String firstName);
	public String getLastName();
	public void setLastName(String lastName);
	public Long getCsmUserId();
	public void setCsmUserId(Long csmUserId);
}
