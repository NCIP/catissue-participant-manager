/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue_participant_manager/LICENSE.txt for details.
 */

package edu.wustl.patientLookUp.lookUpServiceBizLogic;

import java.util.List;

import edu.wustl.patientLookUp.domain.PatientInformation;
import edu.wustl.patientLookUp.queryExecutor.IQueryExecutor;
import edu.wustl.patientLookUp.util.PatientLookupException;

/**
 * This Interface is used to declare method for participant matching algorithm.
 * @author geeta_jaggal
 */
public interface IPatientLookUp
{

	/**
	 * @param patinetInformaton :object which contains user entered patient information.
	 * @param threshold : cutoff points value.
	 * @param maxNoOfRecords : max no of records to be returned by algorithm.
	 * @return List of all matched participants.
	 * @throws edu.wustl.patientLookUp.util.PatientLookupException - throws PatientLookupException.
	 */
	List<PatientInformation> searchMatchingParticipant(PatientInformation patinetInformaton,
			int threshold, int maxNoOfRecords) throws PatientLookupException;

	/**
	 * @param queryExecutor - query generator object.
	 * @throws PatientLookupException - throws PatientLookupException.
	 */
	public void setQueryExecutor(IQueryExecutor queryExecutor)throws PatientLookupException;


	//List<PatientInformation> perFormMatch(PatientInformation patinetInformaton,IQueryExecutor queryExecutor,int threshold, int maxNoOfRecords) throws PatientLookupException;

}
