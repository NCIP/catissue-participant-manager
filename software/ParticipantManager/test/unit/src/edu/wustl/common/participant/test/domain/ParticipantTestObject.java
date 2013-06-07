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

import java.util.Collection;
import java.util.Date;

import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.ISite;


public class ParticipantTestObject implements IParticipant
{
	
	private String activityStatus;
	private Date birthDate;
	private Integer birthYear;
	private Date deathDate;
	private String empiId;
	private String empiIdStatus;
	private Collection ethnicityCollection;
	private String firstName;
	private String gender;
	private String gridValueSelected;
	private Long id;
	private String lastName;
	private String metaPhoneCode;
	private String middleName;
	private String participantCode;
	private Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> participantMedicalIdentifierCollection;
	private Collection raceCollection;
	private String sexGenotype;
	private String socialSecurityNumber;
	private String vitalStatus;
	
	public IParticipant clone()
	{
		return this;
	}

	
	public String getActivityStatus()
	{
		return activityStatus;
	}

	
	public void setActivityStatus(String activityStatus)
	{
		this.activityStatus = activityStatus;
	}

	
	public Date getBirthDate()
	{
		return birthDate;
	}

	
	public void setBirthDate(Date birthDate)
	{
		this.birthDate = birthDate;
	}

	
	public Integer getBirthYear()
	{
		return birthYear;
	}

	
	public void setBirthYear(Integer birthYear)
	{
		this.birthYear = birthYear;
	}

	
	public Date getDeathDate()
	{
		return deathDate;
	}

	
	public void setDeathDate(Date deathDate)
	{
		this.deathDate = deathDate;
	}

	
	public String getEmpiId()
	{
		return empiId;
	}

	
	public void setEmpiId(String empiId)
	{
		this.empiId = empiId;
	}

	
	public String getEmpiIdStatus()
	{
		return empiIdStatus;
	}

	
	public void setEmpiIdStatus(String empiIdStatus)
	{
		this.empiIdStatus = empiIdStatus;
	}

	
	public Collection getEthnicityCollection()
	{
		return ethnicityCollection;
	}

	
	public void setEthnicityCollection(Collection ethnicityCollection)
	{
		this.ethnicityCollection = ethnicityCollection;
	}

	
	public String getFirstName()
	{
		return firstName;
	}

	
	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}

	
	public String getGender()
	{
		return gender;
	}

	
	public void setGender(String gender)
	{
		this.gender = gender;
	}

	
	public String getGridValueSelected()
	{
		return gridValueSelected;
	}

	
	public void setGridValueSelected(String gridValueSelected)
	{
		this.gridValueSelected = gridValueSelected;
	}

	
	public Long getId()
	{
		return id;
	}

	
	public void setId(Long id)
	{
		this.id = id;
	}

	
	public String getLastName()
	{
		return lastName;
	}

	
	public void setLastName(String lastName)
	{
		this.lastName = lastName;
	}

	
	public String getMetaPhoneCode()
	{
		return metaPhoneCode;
	}

	
	public void setMetaPhoneCode(String metaPhoneCode)
	{
		this.metaPhoneCode = metaPhoneCode;
	}

	
	public String getMiddleName()
	{
		return middleName;
	}

	
	public void setMiddleName(String middleName)
	{
		this.middleName = middleName;
	}

	
	public String getParticipantCode()
	{
		return participantCode;
	}

	
	public void setParticipantCode(String participantCode)
	{
		this.participantCode = participantCode;
	}

	
	public Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> getParticipantMedicalIdentifierCollection()
	{
		return participantMedicalIdentifierCollection;
	}

	
	public void setParticipantMedicalIdentifierCollection(
			Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> participantMedicalIdentifierCollection)
	{
		this.participantMedicalIdentifierCollection = participantMedicalIdentifierCollection;
	}

	
	public Collection getRaceCollection()
	{
		return raceCollection;
	}

	
	public void setRaceCollection(Collection raceCollection)
	{
		this.raceCollection = raceCollection;
	}

	
	public String getSexGenotype()
	{
		return sexGenotype;
	}

	
	public void setSexGenotype(String sexGenotype)
	{
		this.sexGenotype = sexGenotype;
	}

	
	public String getSocialSecurityNumber()
	{
		return socialSecurityNumber;
	}

	
	public void setSocialSecurityNumber(String socialSecurityNumber)
	{
		this.socialSecurityNumber = socialSecurityNumber;
	}

	
	public String getVitalStatus()
	{
		return vitalStatus;
	}

	
	public void setVitalStatus(String vitalStatus)
	{
		this.vitalStatus = vitalStatus;
	}

}
