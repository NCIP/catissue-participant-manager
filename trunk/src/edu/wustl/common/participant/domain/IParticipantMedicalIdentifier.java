package edu.wustl.common.participant.domain;

public interface IParticipantMedicalIdentifier <T, S>
{
	public void setId(Long identifier);
	public Long getId();
	public String getMedicalRecordNumber();
	public void setMedicalRecordNumber(String medicalRecordNumber);
	public void setParticipant(T participant);
	public void setSite(S site);
	public S getSite();
}
