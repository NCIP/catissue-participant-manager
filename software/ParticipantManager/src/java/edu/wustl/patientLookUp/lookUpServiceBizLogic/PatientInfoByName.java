/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue-participant-manager/LICENSE.txt for details.
 */

package edu.wustl.patientLookUp.lookUpServiceBizLogic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.language.Metaphone;

import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.patientLookUp.domain.PatientInformation;
import edu.wustl.patientLookUp.queryExecutor.IQueryExecutor;
import edu.wustl.patientLookUp.util.PatientLookupException;
import edu.wustl.patientLookUp.util.Utility;

/**
 * This class will searches the patients based on the LastName  value
 * and returns all the lastName matched patients.
 * @author geeta_jaggal
 */
public class PatientInfoByName
{

	private Map<String, PatientInformation> patientDataMap = new LinkedHashMap<String, PatientInformation>();
	edu.wustl.common.util.logger.Logger log = edu.wustl.common.util.logger.Logger.getCommonLogger(PatientInfoByName.class);

	/**
	 * This method will perform the patient match on lastName and also perform the
	 * phonetic match on lastName and returns the lastName matched patients.
	 * @param patientInformation : object which contains user entered patient information.
	 * @param queryExecutor : object of query executor class.
	 * @param threshold :cutoff points value.
	 * @param maxNoOfRecords :max no of records to be returned by algorithm.
	 * @return List of all lastName matched patients.
	 * @throws PatientLookupException
	 */
	public List<PatientInformation> performMatchOnName(PatientInformation patientInformation,
			IQueryExecutor queryExecutor, int threshold, int maxNoOfRecords)
			throws PatientLookupException
	{

		List<PatientInformation> matchedPatientsByName = new ArrayList<PatientInformation>();
		List<PatientInformation> matchedPatientsByMetaPhone = null;
		List<PatientInformation> matchedParticipantList = new ArrayList<PatientInformation>();
		try
		{
			//added a empty check for fixing bug :22880
			if(patientInformation.getLastName() !=null && !"".equals(patientInformation.getLastName()))
			{
				String lastName = Utility.compressLastName(patientInformation.getLastName());
				matchedPatientsByName.addAll(queryExecutor.executeQueryForName(lastName, patientInformation
						.getProtocolIdSet(), patientInformation.getParticipantObjName()));
			}
			// get meta phone value
			Metaphone metaPhoneObj = new Metaphone();
			String metaPhone = metaPhoneObj.metaphone(patientInformation.getLastName());
			// Search for metaPhoen matched records
			if (metaPhone != null && metaPhone != "")
			{
				matchedPatientsByMetaPhone = queryExecutor.executetQueryForPhonetic(metaPhone,
						patientInformation.getProtocolIdSet(), patientInformation
								.getParticipantObjName());
				matchedPatientsByName.addAll(matchedPatientsByMetaPhone);
			}
			log.debug("***** After executetQueryForPhonetic ****" );
			Long time1=System.currentTimeMillis();
			List<Long> facilityIdList= ParticipantManagerUtility.getFacilityIds(patientInformation);
			log.debug("@@@@@@@@@ Score calculation started from mactches based on NAME @@@@@@@@@@@@@@@@@@@@@");
			log.debug("no of participants on Mathches On NAME ="+matchedPatientsByName.size());

			Utility.calculateScore(matchedPatientsByName, patientInformation);
			Long time2=System.currentTimeMillis();
			log.debug("Time taken for calculateScore::"+(time2-time1));
			Utility.sortListByScore(matchedPatientsByName);
			Long time3=System.currentTimeMillis();
			log.debug("Time taken for sortListByScore::"+(time3-time2));
			matchedPatientsByName = Utility.processMatchingListForFilteration(
					matchedPatientsByName, threshold, maxNoOfRecords);
			log.debug("no of patients on NAme matches after filteration based on score "+matchedPatientsByName.size());
			log.debug("@@@@@@@@@ Score calculation ends from mactches based on NAME @@@@@@@@@@@@@@@@@@@@@");
			Utility.populatePatientDataMap(matchedPatientsByName, patientDataMap);
			Long time4=System.currentTimeMillis();
			log.debug("Time taken for processMatchingListForFilteration::"+(time4-time3));
			matchedParticipantList.addAll(patientDataMap.values());
			queryExecutor.fetchRegDateFacilityAndMRNOfPatient(matchedPatientsByName,facilityIdList);
			return matchedParticipantList;
		}
		catch (Exception e)
		{
			throw new PatientLookupException(e.getMessage(), e);
		}
	}
}
