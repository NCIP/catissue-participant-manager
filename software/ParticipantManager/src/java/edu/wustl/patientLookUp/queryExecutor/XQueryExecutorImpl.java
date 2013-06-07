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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.patientLookUp.domain.PatientInformation;
import edu.wustl.patientLookUp.util.Constants;
import edu.wustl.patientLookUp.util.Logger;
import edu.wustl.patientLookUp.util.PatientLookupException;
import edu.wustl.patientLookUp.util.PropertyHandler;
import edu.wustl.patientLookUp.util.Utility;

/**
 * @author geeta_jaggal
 * This class will provide methods to fetch teh patients based on MRN,SSN and lastName
 */
public class XQueryExecutorImpl extends AbstractQueryExecutor
{

	PreparedStatement statement = null;
	ResultSet rs = null;
	LinkedHashSet<String> lastNameMatchUPIList = null;

	/**
	 * constructor.
	 * @throws PatientLookupException : PatientLookupException
	 */
	public XQueryExecutorImpl() throws PatientLookupException
	{

	}

	/* (non-Javadoc)
	 * @see edu.wustl.patientLookUp.queryExecutor.AbstractQueryExecutor#executeQueryForMRN(java.lang.String)
	 */
	public List<PatientInformation> executeQueryForMRN(String mrn, String siteId,String facilityId,
			Set<Long> protocolIdSet, String pmiObjName) throws PatientLookupException
	{
		List<PatientInformation> patientInformationList = new ArrayList<PatientInformation>();
		try
		{
			String query = QueryGenerator.getMRNQuery();
			statement = getConnection().prepareStatement(query);
			statement.setString(1, mrn);
			statement.setString(2, facilityId);
			rs = statement.executeQuery();
			if (rs != null)
			{
				patientInformationList = populatePatientInfo(rs);
			}

			rs.close();
			statement.close();
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			Logger.out.info(e.getMessage(), e);
			Logger.out.info("Error while retriving matched patients based on MRN");
			throw new PatientLookupException(e.getMessage(), e);
		}
		finally
		{
			closeConnection();
		}
		return patientInformationList;
	}

	/* (non-Javadoc)
	 * @see edu.wustl.patientLookUp.queryExecutor.AbstractQueryExecutor#executeQueryForName(java.lang.String)
	 */
	public List<PatientInformation> executeQueryForName(String lastName, Set<Long> protocolIdSet,
			String participantObjName) throws PatientLookupException
	{
		List<PatientInformation> patientInformationList = new ArrayList<PatientInformation>();
		lastNameMatchUPIList = new LinkedHashSet<String>();
		lastName = lastName.toUpperCase();
		try
		{
			String queryByName = QueryGenerator.getNameQuery();

			statement = getConnection().prepareStatement(queryByName);
			statement.setString(1, lastName);
			statement.setString(2, lastName + "ZZ");
			rs = statement.executeQuery();
			if (rs != null)
			{
				patientInformationList = populatePatientInfo(rs);
			}

			rs.close();
			statement.close();
		}
		catch (SQLException e)
		{
			Logger.out.info("Error while retriving the matched patients based on name\n");
			throw new PatientLookupException(e.getMessage(), e);
		}
		finally
		{
			closeConnection();
		}

		return patientInformationList;
	}

	/* (non-Javadoc)
	 * @see edu.wustl.patientLookUp.queryExecutor.AbstractQueryExecutor#executetQueryForPhonetic(java.lang.String)
	 */
	public List<PatientInformation> executetQueryForPhonetic(String lMetaPhone,
			Set<Long> protocolIdSet, String participantObjName) throws PatientLookupException
	{
		List<PatientInformation> patientInformationList = new ArrayList<PatientInformation>();
		try
		{
			String queryByMetaPhone = QueryGenerator.getMetaPhoneQuery();
			statement = getConnection().prepareStatement(queryByMetaPhone);
			statement.setString(1, lMetaPhone);
			rs = statement.executeQuery();
			if (rs != null)
			{
				patientInformationList = populatePatientInfo(rs);
			}

			rs.close();
			statement.close();
		}
		catch (SQLException e)
		{
			Logger.out.debug(e.getMessage(), e);
			throw new PatientLookupException(e.getMessage(), e);
		}
		finally
		{
			closeConnection();
		}

		return patientInformationList;
	}

	/* (non-Javadoc)
	 * @see edu.wustl.patientLookUp.queryExecutor.AbstractQueryExecutor#executetQueryForSSN(java.lang.String)
	 */
	public List<PatientInformation> executetQueryForSSN(String ssn, Set<Long> protocolIdSet,
			String participantObjName) throws PatientLookupException
	{
		List<PatientInformation> patientInformationList = new ArrayList<PatientInformation>();
		try
		{
			String queryBySSN = QueryGenerator.getSSNQuery();
			statement = getConnection().prepareStatement(queryBySSN);
			statement.setString(1, ssn);
			rs = statement.executeQuery();
			if (rs != null)
			{
				patientInformationList = populatePatientInfo(rs);
			}

			rs.close();
			statement.close();
		}
		catch (SQLException e)
		{
			Logger.out.info(e.getMessage(), e);
			Logger.out.info("Error while retriving the matched patients based on SSN\n");
			throw new PatientLookupException(e.getMessage(), e);
		}
		finally
		{
			closeConnection();
		}
		return patientInformationList;
	}

	/**
	 * @param rs : Result set object
	 * @return PatientInformation object populated with patient value
	 * @throws PatientLookupException
	 */
	private List<PatientInformation> populatePatientInfo(ResultSet rs)
			throws PatientLookupException
	{

		PatientInformation patientInfo = null;
		List<PatientInformation> patientInfoList = new LinkedList<PatientInformation>();
		Map<String, PatientInformation> patientDataMap = new LinkedHashMap<String, PatientInformation>();
		Collection<String> raceCollection = null;
		String raceValue = null;
		try
		{
			while (rs.next())
			{
				raceCollection = null;
				patientInfo = (PatientInformation) patientDataMap.get((rs.getString("upi").trim()));
				if (patientInfo == null)
				{
					patientInfo = new PatientInformation();

					//patientInfo.setId(Long.valueOf((rs.getString("upi").trim())));
					patientInfo.setUpi((rs.getString("upi").trim()));
					patientInfo.setLastName(rs.getString("lastname"));
					if (rs.getString("firstname") != null)
					{
						patientInfo.setFirstName(rs.getString("firstname"));
					}
					if (rs.getString("middlename") != null)
					{
						patientInfo.setMiddleName(rs.getString("middlename"));
					}
					if (rs.getString("ssn") != null)
					{
						patientInfo.setSsn(rs.getString("ssn"));
					}
					if (rs.getString("dob") != null)
					{
						patientInfo.setDob(Utility.parse(rs.getString("dob"),
								Constants.DATE_FORMAT_YYYY_MM_DD));
					}
					patientInfo.setIsFromEMPI("YES");

					if (rs.getString("gender") != null)
					{
						patientInfo.setGender(PropertyHandler.getValue((String) rs
								.getString("gender")));
					}
					if (rs.getString("raceID") != null)
					{
						//setRaceCollection(rs.getString("raceID"), patientInfo);
						raceValue = getRaceCollection(rs.getString("raceID"));
						if (raceValue != null && !"".equals(raceValue))
						{
							raceCollection = new LinkedHashSet<String>();
							raceCollection.add(raceValue);
						}
						patientInfo.setRaceCollection(raceCollection);
					}
					patientDataMap.put((rs.getString("upi").trim()), patientInfo);
					patientInfoList.add(patientInfo);
				}
				else
				{
					raceValue = getRaceCollection(rs.getString("raceID"));

					if (raceValue != null && !"".equals(raceValue))
					{
						raceCollection = patientInfo.getRaceCollection();
						if (raceCollection == null)
						{
							raceCollection = new LinkedHashSet<String>();
						}
						raceCollection.add(raceValue);
						patientInfo.setRaceCollection(raceCollection);
					}
				}
			}
		}
		catch (SQLException e)
		{
			Logger.out.info(e.getMessage(), e);
			Logger.out.info(e.getMessage(), e);
			throw new PatientLookupException(e.getMessage(), e);
		}
		return patientInfoList;
	}

	/* (non-Javadoc)
	 * @see edu.wustl.patientLookUp.queryExecutor.AbstractQueryExecutor#
	 * fetchRegDateFacilityAndMRNOfPatient(java.util.List)
	 */
	public void fetchRegDateFacilityAndMRNOfPatient(List<PatientInformation> matchedPatientsList)
			throws PatientLookupException
	{
		try
		{
			String query = QueryGenerator.getQuery();
			String medicalRecNumber = null;
			String facilityId = null;
			ResultSet result = null;
			Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> newPmiColl = null;
			Collection<String> patientMedicalIdentifierColl = null;

			statement = getConnection().prepareStatement(query);

			for (int i = 0; i < matchedPatientsList.size(); i++)
			{
				PatientInformation patientInfo = (PatientInformation) matchedPatientsList.get(i);

				newPmiColl = new LinkedList<IParticipantMedicalIdentifier<IParticipant, ISite>>();
				patientMedicalIdentifierColl = new LinkedList<String>();
				Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> pmiColl = patientInfo
						.getPmiCollection();

				for (IParticipantMedicalIdentifier<IParticipant, ISite> iParticipantMedicalIdentifier : pmiColl)
				{
					String facilityID = iParticipantMedicalIdentifier.getSite().getFacilityId();
					statement.setString(1, patientInfo.getUpi());
					statement.setString(2, facilityID);
					result = statement.executeQuery();

					if (result != null)
					{
						while (result.next())
						{
							//patientInfo.setDateVisited(Utility.parse(result.getString("regDate"),Constants.DATE_FORMAT_YYYY_MM_DD));
							//patientInfo.setFacilityVisited(PropertyHandler.getValue((String) result.getString("facility")));
							medicalRecNumber = (String) result.getString("mrn");
							facilityId = (String) result.getString("facility_id");
							String facilityName = (String) result.getString("print_name");

							if (!patientMedicalIdentifierColl.contains(medicalRecNumber))
							{
								patientMedicalIdentifierColl.add(medicalRecNumber);
								patientMedicalIdentifierColl.add(facilityId);
								patientMedicalIdentifierColl.add(facilityName);

								IParticipantMedicalIdentifier<IParticipant, ISite> pmiObj = ParticipantManagerUtility
										.getPMIInstance();
								pmiObj.setMedicalRecordNumber(medicalRecNumber);
								ISite site = (ISite) ParticipantManagerUtility.getSiteInstance();
								site.setId(Long.valueOf(facilityId));
								site.setName(facilityName);
								pmiObj.setSite(site);
								newPmiColl.add(pmiObj);
							}
						}
						//					patientInfo
						//							.setParticipantMedicalIdentifierCollection(patientMedicalIdentifierColl);
						result.close();
					}
				}
				patientInfo.setPmiCollection(newPmiColl);
			}
			statement.close();
		}
		catch (SQLException e)
		{
			Logger.out.info(e.getMessage(), e);
			throw new PatientLookupException(e.getMessage(), e);

		}
		catch (Exception e)
		{
			Logger.out.info(e.getMessage(), e);
			throw new PatientLookupException(e.getMessage(), e);
		}

		finally
		{
			closeConnection();
		}
	}

	/**
	 * @param race : race value
	 * @param patientInfo : PatientInformation object
	 * @throws PatientLookupException : PatientLookupException
	 */
	private String getRaceCollection(String raceId) throws PatientLookupException
	{
		String raceValue = null;
		if (raceId != null)
		{
			raceValue = PropertyHandler.getValue(raceId);
			if (raceValue == null || raceValue == "")
			{
				raceValue = Constants.UNSPECIFIED;
			}
		}
		return raceValue;
	}

}
