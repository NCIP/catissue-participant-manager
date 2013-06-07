/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue-participant-manager/LICENSE.txt for details.
 */

package edu.wustl.patientLookUp.queryExecutor;

import java.util.List;
import java.util.Set;

import edu.wustl.patientLookUp.domain.PatientInformation;
import edu.wustl.patientLookUp.util.PatientLookupException;

/**
 * This interface will declare the methods for the patient matching algorithm.
 * @author geeta_jaggal
 *
 */
public interface IQueryExecutor
{

	/**
	 * This method will set the database related configuration value.
	 * @param dbURL : database url
	 * @param dbUser :database username
	 * @param dbPassword : database password
	 * @param dbDriver : database driver name
	 * @param dbSchema : database scheman name
	 * @throws PatientLookupException : PatientLookupException
	 */
	void setDBParameteres(String dbURL, String dbUser, String dbPassword, String dbDriver,
			String dbSchema) throws PatientLookupException;

	List<PatientInformation> executeQueryForMRN(String mrn, String siteId,String facilityId,
			Set<Long> protocolIdSet, String pmiObjName) throws PatientLookupException;

	List<PatientInformation> executetQueryForSSN(String ssn, Set<Long> protocolIdSet,
			String participantObjName) throws PatientLookupException;;

	List<PatientInformation> executeQueryForName(String name, Set<Long> protocolIdSet,
			String participantObjName) throws PatientLookupException;;

	List<PatientInformation> executetQueryForPhonetic(String metaPhone, Set<Long> protocolIdSet,
			String participantObjName) throws PatientLookupException;

	/**
	 * This method will fetch the mrn,facility visited and dataVisited values of each patients.
	 * in the matched patient list
	 * @param patientMatchingList : list of matched patients
	 * @param facilityIdList : list of facility ids to search for patients
	 * @throws PatientLookupException : PatientLookupException
	 */
	void fetchRegDateFacilityAndMRNOfPatient(List<PatientInformation> patientMatchingList, List<Long> facilityIdList)
			throws PatientLookupException;;

}
