package edu.wustl.common.participant.test.domain;

import edu.wustl.common.participant.domain.ISite;


public class SiteTestObject implements ISite
{
	private Long id;
	private String name;
	private String facilityId;
	
	@Override
	public String getFacilityId()
	{
		return facilityId;
	}

	@Override
	public Long getId()
	{

		return id;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public void setFacilityId(String facilityId)
	{
		this.facilityId = facilityId;

	}

	@Override
	public void setId(Long identifier)
	{
		this.id=identifier;
	}

	@Override
	public void setName(String name)
	{
		this.name=name;

	}

}
