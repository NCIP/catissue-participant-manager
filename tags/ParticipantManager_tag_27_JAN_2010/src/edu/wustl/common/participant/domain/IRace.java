package edu.wustl.common.participant.domain;

public interface IRace <T>
{
	public Long getId();
	public void setId(Long identifier);
	public String getRaceName();
	public void setRaceName(String raceName);
	public void setParticipant(T participant);
}
