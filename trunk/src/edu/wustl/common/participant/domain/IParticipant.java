package edu.wustl.common.participant.domain;

import java.util.Collection;
import java.util.Date;


public interface IParticipant
{
	public Long getId();
	public void setId(Long identifier);
	public String getLastName();
	public void setLastName(String lastName);
	public String getFirstName();
	public void setFirstName(String firstName);
	public String getMiddleName();
	public void setMiddleName(String middleName);
	public Date getBirthDate();
	public void setBirthDate(Date birthDate);
	public String getGender();
	public void setGender(String gender);
	public String getSexGenotype();
	public void setSexGenotype(String sexGenotype);
	public Collection getRaceCollection();
	public void setRaceCollection(Collection raceCollection);
	public String getEthnicity();
	public void setEthnicity(String ethnicity);
	public String getSocialSecurityNumber();
	public void setSocialSecurityNumber(String socialSecurityNumber);
	public String getActivityStatus();
	public void setActivityStatus(String activityStatus);
	public Date getDeathDate();
	public void setDeathDate(Date deathDate);
	public String getVitalStatus();
	public void setVitalStatus(String vitalStatus);
	public Collection getParticipantMedicalIdentifierCollection();
	public void setParticipantMedicalIdentifierCollection(Collection participantMedicalIdentifierCollection);
	public String getMetaPhoneCode();
	public void setMetaPhoneCode(String metaPhoneCode);
	public String getEmpiId();
	public String getEmpiIdStatus();
	public void setEmpiId(String empiId);
	public void setEmpiIdStatus(String empiIdStatus);
	public String getIsFromEMPI();
	public void setIsFromEMPI(String isFromEMPI);

}
