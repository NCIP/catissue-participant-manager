
package edu.wustl.common.participant.client;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.domain.IUser;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.query.generator.ColumnValueBean;
import edu.wustl.patientLookUp.domain.PatientInformation;

// TODO: Auto-generated Javadoc
/**
 * The Interface IParticipantManager.
 *
 * @author geeta_jaggal
 */
public interface IParticipantManager
{

	public Long getSiteIdByName(String siteName) throws ApplicationException;
	
	public ISite getSiteByName(String siteName) throws ApplicationException;
	
	public IUser getUserByLoginNameAndActivityStatus(String loginName,String activityStatus) throws ApplicationException;
	
	public IParticipant getParticipantById(Long identifier) throws ApplicationException;
	
	public List getSiteObject(final String facilityId) throws ApplicationException;
	
	public IParticipant getParticpantByEmpiId(String empiId) throws ApplicationException;
	
	/**
	 * Gets the pI cordinators first name and last name.
	 *
	 * @return the pI cordinatorsof protocol
	 */
	public List getPICordinatorsofProtocol(Long participantId) throws ApplicationException;

	/**
	 * This method will return all the protocol ids
	 * associated with MISC id.
	 *
	 * This method first fetch the MICS id to which the protocol id is associated with.
	 * And then fetch all the protocol ids associated with the MICS id.
	 *
	 * Suppose  protocolId associated with MICSId and MICSId enabled for participant
	 * match withing mics then this method will return all the protocol ids associated with the MICSid.
	 *
	 * @param cpId the cp id
	 *
	 * @return the associted mutli inst protocol id list
	 *
	 * @throws ApplicationException the application exception
	 */
	public Set<Long> getProtocolIdLstForMICSEnabledForMatching(Long protocolId)
			throws ApplicationException;

	/**
	 * Gets the last name query.
	 *
	 * @param protocolIdSet the protocol id set
	 *
	 * @return the last name query
	 */
	public List<IParticipant> getParticipantsByLastName(Set<Long> protocolIdSet,String lastName) throws ApplicationException;

	/**
	 * Gets the meta phone code query.
	 *
	 * @param protocolIdSet the protocol id set
	 *
	 * @return the meta phone code query
	 */
	public List<IParticipant> getParticipantsByMetaPhoneCode(Set<Long> protocolIdSet,String metaPhoneCode) throws ApplicationException;

	/**
	 * Gets the mRN query.
	 *
	 * @param protocolIdSet the protocol id set
	 *
	 * @return the mRN query
	 */
	public List<IParticipant> getParticipantsByMRN(Set<Long> protocolIdSet,String medicalRecordNumber,Long siteId) throws ApplicationException;

	/**
	 * Gets the sSN query.
	 *
	 * @param protocolIdSet the protocol id set
	 *
	 * @return the sSN query
	 */
	public List<IParticipant> getParticipantsBySSN(Set<Long> protocolIdSet,String socialSecurityNumber) throws ApplicationException;

	/**
	 * Fetch the PI and co-ordinators ids.
	 * @param participantId
	 * @return
	 * @throws ApplicationException
	 */
	public LinkedHashSet<Long> getParticipantPICordinators(long participantId)
			throws ApplicationException;

	/**
	 * @param participantId
	 * @return
	 */
	public List isEmpiEnabled(Long participantId) throws ApplicationException;

	public List<IParticipant> getParticipantsByParticipantCode(Set<Long> protocolIdSet,String participantCode) throws ApplicationException;

	public List<String> getClinicalStudyNamesQuery(Long participantId) throws ApplicationException;

	public String getProcessedMatchedParticipantQuery(Long userId) ;
}
