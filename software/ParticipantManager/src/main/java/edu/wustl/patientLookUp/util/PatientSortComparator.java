/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue_participant_manager/LICENSE.txt for details.
 */

package edu.wustl.patientLookUp.util;

import java.util.Comparator;

import edu.wustl.patientLookUp.domain.PatientInformation;

/**
 * This class will compare the PatientInformation object based on their id.
 * @author geeta_jaggal
 */
public class PatientSortComparator implements Comparator<PatientInformation>
{

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(PatientInformation patient1, PatientInformation patient2)
	{
		int returnValue = 0;
		if (patient1.getMatchingScore() == patient2.getMatchingScore())
		{
			returnValue = 0;
		}
		else if (patient2.getMatchingScore() > patient1.getMatchingScore())
		{
			returnValue = 1;
		}
		else
		{
			returnValue = -1;
		}
		return returnValue;
	}
}
