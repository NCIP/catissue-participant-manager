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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.participant.client.IParticipantManager;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.domain.IUser;


public class ParticipantManagerTest implements IParticipantManager
{

	@Override
	public List<String> getClinicalStudyNamesQuery(Long participantId) throws ApplicationException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List getPICordinatorsofProtocol(Long participantId) throws ApplicationException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IParticipant getParticipantById(Long identifier) throws ApplicationException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LinkedHashSet<Long> getParticipantPICordinators(long participantId)
			throws ApplicationException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<IParticipant> getParticipantsByLastName(Set<Long> protocolIdSet, String lastName)
			throws ApplicationException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<IParticipant> getParticipantsByMRN(Set<Long> protocolIdSet,
			String medicalRecordNumber, Long siteId) throws ApplicationException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<IParticipant> getParticipantsByMetaPhoneCode(Set<Long> protocolIdSet,
			String metaPhoneCode) throws ApplicationException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<IParticipant> getParticipantsByParticipantCode(Set<Long> protocolIdSet,
			String participantCode) throws ApplicationException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<IParticipant> getParticipantsBySSN(Set<Long> protocolIdSet,
			String socialSecurityNumber) throws ApplicationException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IParticipant getParticpantByEmpiId(String empiId) throws ApplicationException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProcessedMatchedParticipantQuery(Long userId)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Long> getProtocolIdLstForMICSEnabledForMatching(Long protocolId)
			throws ApplicationException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getSiteIdByName(String siteName) throws ApplicationException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List getSiteObject(String facilityId) throws ApplicationException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List isEmpiEnabled(Long participantId) throws ApplicationException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ISite getSiteByName(String siteName) throws ApplicationException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IUser getUserByLoginNameAndActivityStatus(String loginName, String activityStatus)
			throws ApplicationException
	{
		// TODO Auto-generated method stub
		return null;
	}

}
