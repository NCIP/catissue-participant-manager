/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue-participant-manager/LICENSE.txt for details.
 */

package edu.wustl.patientLookUp.util;

import edu.wustl.patientLookUp.lookUpServiceBizLogic.IPatientLookUp;
import edu.wustl.patientLookUp.queryExecutor.IQueryExecutor;

/**
 * @author geeta_jaggal
 * This is the factory class to retrieve singleton instance of IPatientLookUp class.
 */
public class PatientLookUpFactory
{

	/**
	 * @return object of class which implements the interface IPatientLookUp
	 * @throws Exception :Exception
	 */
	public static IPatientLookUp getPatientLookupServiceImpl() throws Exception
	{
		final String className = PropertyHandler.getValue(Constants.PATIENT_LOOKUP_SERVICE);

		return (IPatientLookUp) Class.forName(className).newInstance();
	}

	/**
	 * @param dbURL :database url
	 * @param dbUser : database username
	 * @param dbPassword :database password
	 * @param dbDriver : database driver name
	 * @param dbSchema : database scheman name
	 * @return object of the query executor class which implements the interface IQueryExecutor
	 * @throws Exception
	 */
	public static IQueryExecutor getQueryExecutorImpl(String dbURL, String dbUser,
			String dbPassword, String dbDriver, String dbSchema) throws Exception
	{
		final String className = PropertyHandler.getValue(Constants.EMPI_QUERY_EXECUTOR);
		IQueryExecutor queryExecutor = (IQueryExecutor) Class.forName(className).newInstance();
		queryExecutor.setDBParameteres(dbURL, dbUser, dbPassword, dbDriver, dbSchema);
		return queryExecutor;
	}

}
