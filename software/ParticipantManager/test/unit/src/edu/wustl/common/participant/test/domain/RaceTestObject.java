package edu.wustl.common.participant.test.domain;

import edu.wustl.common.participant.domain.IRace;


public class RaceTestObject implements IRace<ParticipantTestObject>
{
	private Long id;
	private String raceName;
	private ParticipantTestObject participant;
	
	@Override
	public Long getId()
	{
		return id;
	}

	@Override
	public String getRaceName()
	{
		return null;
	}

	@Override
	public void setId(Long identifier)
	{
		id=identifier;
	}

	@Override
	public void setParticipant(ParticipantTestObject participant)
	{
		this.participant=participant;
	}

	@Override
	public void setRaceName(String raceName)
	{
		this.raceName=raceName;
		
	}

}
