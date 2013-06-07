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

import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;


public class ParticipantMedicalIdentifierTestObject implements IParticipantMedicalIdentifier<ParticipantTestObject, SiteTestObject>
{
	private Long id;
	private String medicalRec;
	private SiteTestObject site;
	private ParticipantTestObject participant;
	
	@Override
	public Long getId()
	{
		return id;
	}

	@Override
	public String getMedicalRecordNumber()
	{
		return medicalRec;
	}

	@Override
	public SiteTestObject getSite()
	{
		return site;
	}

	@Override
	public void setId(Long identifier)
	{
		id = identifier;
		
	}

	@Override
	public void setMedicalRecordNumber(String medicalRecordNumber)
	{
		medicalRec = medicalRecordNumber;
		
	}

	@Override
	public void setParticipant(ParticipantTestObject participant)
	{
		this.participant=participant;
		
	}

	@Override
	public void setSite(SiteTestObject site)
	{
		this.site = site;
		
	}

}
