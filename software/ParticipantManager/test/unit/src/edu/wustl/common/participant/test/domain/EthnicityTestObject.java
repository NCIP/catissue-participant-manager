/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue-participant-manager/LICENSE.txt for details.
 */

package edu.wustl.common.participant.test.domain;

import edu.wustl.common.participant.domain.IEthnicity;


public class EthnicityTestObject implements IEthnicity<ParticipantTestObject>
{
	private Long id;
	private String name;
	private ParticipantTestObject participant;
	
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
	public void setId(Long identifier)
	{
		id=identifier;
		
	}

	@Override
	public void setName(String name)
	{
		this.name=name;
		
	}

	@Override
	public void setParticipant(ParticipantTestObject participant)
	{
		this.participant=participant;
		
	}

}