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
	edu.wustl.common.util.logger.Logger log = edu.wustl.common.util.logger.Logger.getCommonLogger(XQueryExecutorImpl.class);
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
		if (Utility.isNumeric(facilityId))
		{
			try
			{
				Long time1= System.currentTimeMillis();
				String query = QueryGenerator.getMRNQuery();
				statement = getConnection().prepareStatement(query);
				statement.setString(1, mrn);
				statement.setLong(2, Long.valueOf(facilityId));
				log.debug("MRN query::"+query);
				log.debug("MRN::"+mrn+"::facilityId::"+facilityId);
				rs = statement.executeQuery();
				Long time2= System.currentTimeMillis();
				log.debug("time for execution for MRN query:: " + (time2-time1));
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
			Long time1= System.currentTimeMillis();
			statement = getConnection().prepareStatement(queryByName);
			statement.setString(1, lastName);
			statement.setString(2, lastName + "ZZ");
			log.debug("query By Name::"+queryByName);
			log.debug("lastName::"+lastName);
			log.debug("lastName::"+lastName+"ZZ");
			rs = statement.executeQuery();
			Long time2= System.currentTimeMillis();
			log.debug("time for execution for Name query:: " + (time2-time1));
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
			Long time1= System.currentTimeMillis();
			String queryByMetaPhone = QueryGenerator.getMetaPhoneQuery();
			log.debug("query By MetaPhone::"+queryByMetaPhone);
			log.debug("MetaPhone value::"+lMetaPhone);
			statement = getConnection().prepareStatement(queryByMetaPhone);
			statement.setString(1, lMetaPhone);
			rs = statement.executeQuery();
			Long time2= System.currentTimeMillis();
			log.debug("time for execution for MetaPhone query:: " + (time2-time1));
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
			Long time1= System.currentTimeMillis();
			statement = getConnection().prepareStatement(queryBySSN);
			statement.setString(1, ssn);
			log.debug("query By SSN::"+queryBySSN);
			log.debug("ssn value::"+ssn);
			rs = statement.executeQuery();
			Long time2= System.currentTimeMillis();
			log.debug("time for execution for SSN query:: " + (time2-time1));
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
				patientInfo = patientDataMap.get((rs.getString("upi").trim()));
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
						patientInfo.setGender(PropertyHandler.getValue(rs
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
	 * fetch MRN, facility ids (which is required to send in HL7) based on facility ids from the application
	 */
	public void fetchRegDateFacilityAndMRNOfPatient(List<PatientInformation> matchedPatientsList, List<Long> facilityIdList)
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
			Long time1 = System.currentTimeMillis();
			//Statement stmt = getConnection().createStatement();
			statement = getConnection().prepareStatement(query);

			for (int i = 0; i < matchedPatientsList.size(); i++)
			{
				PatientInformation patientInfo = matchedPatientsList.get(i);

				newPmiColl = new LinkedList<IParticipantMedicalIdentifier<IParticipant, ISite>>();
				patientMedicalIdentifierColl = new LinkedList<String>();

			//	Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> pmiColl = patientInfo
			//			.getPmiCollection();

			//	for (IParticipantMedicalIdentifier<IParticipant, ISite> iParticipantMedicalIdentifier : pmiColl)
				{
					//String facilityID = iParticipantMedicalIdentifier.getSite().getFacilityId();
					//String upi="'"+patientInfo.getUpi()+"'";
					//query=query.replace("@@", patientInfo.getUpi());

					statement.setString(1,patientInfo.getUpi() );
					log.debug("fetchRegDateFacilityAndMRNOfPatient :: query::" + query
						+ ":: UPI ::" + patientInfo.getUpi());
					//statement.setString(2, facilityID);
					result = statement.executeQuery();

					if (result != null)
					{
						Long time2= System.currentTimeMillis();
						log.debug(" time for execution= "+ (time2-time1) );
						while (result.next())
						{
							medicalRecNumber = result.getString("mrn");
							facilityId = result.getString("facility");
							String facilityName = result.getString("print_name");
							log.debug("fetchRegDateFacilityAndMRNOfPatient from CIDER :: medicalRecNumber::: "
											+ medicalRecNumber
											+ ":: facilityId :: "
											+ facilityId
											+ ":: facilityName" + facilityName);
							if (!patientMedicalIdentifierColl.contains(medicalRecNumber))
							{
								patientMedicalIdentifierColl.add(medicalRecNumber);
								patientMedicalIdentifierColl.add(facilityId);
								patientMedicalIdentifierColl.add(facilityName);

								IParticipantMedicalIdentifier<IParticipant, ISite> pmiObj = ParticipantManagerUtility
										.getPMIInstance();
								pmiObj.setMedicalRecordNumber(medicalRecNumber);
								ISite site = (ISite) ParticipantManagerUtility.getSiteInstance();
							//	site.setId(Long.valueOf(facilityId));
								site.setFacilityId(facilityId);
								site.setName(facilityName);
								pmiObj.setSite(site);
								newPmiColl.add(pmiObj);
							}
						}
						result.close();
					}
				}
				patientInfo.setPmiCollection(newPmiColl);
				log.debug("fetchRegDateFacilityAndMRNOfPatient after CIDER matches::  pmiColl size is::"
								+ newPmiColl.size());
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

	/* (non-Javadoc)
	 * @see edu.wustl.patientLookUp.queryExecutor.IQueryExecutor#getPatientByUpi(java.lang.String, edu.wustl.patientLookUp.domain.PatientInformation)
	 */
	public List<PatientInformation> getPatientByUpi(String upi,
			PatientInformation patientInformation) throws PatientLookupException
	{
		List<Long> facilityIdList = ParticipantManagerUtility.getFacilityIds(patientInformation);
		List<PatientInformation> patientInformationList = new ArrayList<PatientInformation>();
		String upiQuery = QueryGenerator.getUPIQuery();
		try
		{
			Long time1 = System.currentTimeMillis();
			statement = getConnection().prepareStatement(upiQuery);
			statement.setString(1, upi);

			log.debug("query By UPI::" + upiQuery);
			log.debug("UPI value::" + upi);
			rs = statement.executeQuery();
			Long time2 = System.currentTimeMillis();
			log.debug("time for execution for UPI query:: " + (time2 - time1));
			if (rs != null)
			{
				patientInformationList.addAll(populatePatientInfo(rs));
			}

			rs.close();
			statement.close();
			fetchRegDateFacilityAndMRNOfPatient(patientInformationList, facilityIdList);
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

}
