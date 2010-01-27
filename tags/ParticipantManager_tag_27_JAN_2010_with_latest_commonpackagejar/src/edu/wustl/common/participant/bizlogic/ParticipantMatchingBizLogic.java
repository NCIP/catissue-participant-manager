
package edu.wustl.common.participant.bizlogic;

import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.query.generator.ColumnValueBean;
import edu.wustl.patientLookUp.domain.PatientInformation;

// TODO: Auto-generated Javadoc
/**
 * The Class ParticipantMatchingBizLogic.
 */
public class ParticipantMatchingBizLogic
{

	/** The Constant logger. */
	private static final Logger logger = Logger.getCommonLogger(ParticipantMatchingBizLogic.class);


	/**
	 * Per form participant match.
	 *
	 * @param ParticipantIdLst the participant id lst
	 *
	 * @throws ApplicationException the application exception
	 */
	public void perFormParticipantMatch(List ParticipantIdLst) throws ApplicationException
	{
		try
		{
			EMPIParticipantRegistrationBizLogic bizLogic = new EMPIParticipantRegistrationBizLogic();
			for (int i = 0; i < ParticipantIdLst.size(); i++)
			{
				List idsList = (List) ParticipantIdLst.get(i);
				if (!idsList.isEmpty() && idsList.get(0) != "")
				{
					Long identifier = Long.valueOf((String) idsList.get(0));
					IParticipant participant = ParticipantManagerUtility
							.getParticipantById(identifier);
					boolean isCallToLkupLgic = ParticipantManagerUtility
							.isCallToLookupLogicNeeded(participant);
					if (isCallToLkupLgic)
					{
						List matchPartpantLst = ParticipantManagerUtility
								.getListOfMatchingParticipants(participant, null,
										Constants.PARTICIPANT_LOOKUP_ALGO_EMPI,null);
						if (matchPartpantLst.size() == 0
								&& ParticipantManagerUtility.isParticipantValidForEMPI(participant
										.getLastName(), participant.getFirstName(), participant
										.getBirthDate()))
						{
							ParticipantManagerUtility.setEMPIIdStatus(participant.getId(),
									Constants.EMPI_ID_PENDING);
							bizLogic.registerPatientToeMPI(participant);
						}
						storeMatchedParticipant(participant, matchPartpantLst);
					}
				}
			}

		}
		catch (Exception e)
		{
			logger.info("Error while performing the EMPI participant match");
			throw new ApplicationException(null, e, e.getMessage());
		}
	}

	/**
	 * Store matched participant.
	 *
	 * @param participant the participant
	 * @param matchPartpantLst the match partpant lst
	 *
	 * @throws DAOException the DAO exception
	 */
	private void storeMatchedParticipant(IParticipant participant, List matchPartpantLst)
			throws DAOException
	{

		JDBCDAO dao = null;
		try
		{
			dao = ParticipantManagerUtility.getJDBCDAO();
			String query = "";
			String raceValues = null;
			String mrnValue = "";
			PatientInformation patientInformation = null;
			LinkedList columnValueBeanList = null;
			LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
 			for (int i = 0; i < matchPartpantLst.size(); i++)
			{
				patientInformation = (PatientInformation) matchPartpantLst.get(i);
				raceValues = getRaceValues(patientInformation.getRaceCollection());
				mrnValue = getMRNValues(patientInformation
						.getParticipantMedicalIdentifierCollection());
				columnValueBeanList = new LinkedList();
				columnValueBeanList.add(new ColumnValueBean("PARTICIPANT_ID", patientInformation
						.getId(), 22));
				columnValueBeanList.add(new ColumnValueBean("EMPI_ID", patientInformation.getUpi(),
						11));
				columnValueBeanList.add(new ColumnValueBean("LAST_NAME", patientInformation
						.getLastName(), 11));
				columnValueBeanList.add(new ColumnValueBean("FIRST_NAME", patientInformation
						.getFirstName(), 11));
				columnValueBeanList.add(new ColumnValueBean("MIDDLE_NAME", patientInformation
						.getMiddleName(), 11));
				columnValueBeanList.add(new ColumnValueBean("GENDER", patientInformation
						.getGender(), 11));
				columnValueBeanList.add(new ColumnValueBean("SOCIAL_SECURITY_NUMBER",
						patientInformation.getSsn(), 11));
				columnValueBeanList.add(new ColumnValueBean("ACTIVITY_STATUS", patientInformation
						.getActivityStatus(), 11));
				columnValueBeanList.add(new ColumnValueBean("VITAL_STATUS", patientInformation
						.getVitalStatus(), 11));
				columnValueBeanList.add(new ColumnValueBean("PARTICIPANT_MRN", mrnValue, 11));
				columnValueBeanList.add(new ColumnValueBean("PARTICIPANT_RACE", raceValues, 11));
				//columnValueBeanList.add(new ColumnValueBean("IS_FROM_EMPI", patientInformation.getIsFromEMPI(), 11));
				columnValueBeanList.add(new ColumnValueBean("SEARCHED_PARTICIPANT_ID", participant
						.getId(), 22));
				if (patientInformation.getDob() == null
						&& patientInformation.getDeathDate() == null)
				{
					query = "INSERT INTO CATISSUE_MATCHED_PARTICIPANT(PARTICIPANT_ID,EMPI_ID,LAST_NAME,FIRST_"
							+ "NAME,MIDDLE_NAME,GENDER,SOCIAL_SECURITY_NUMBER,ACTIVITY_STATUS,VITAL_STATUS,PART"
							+ "ICIPANT_MRN,PARTICIPANT_RACE,SEARCHED_PARTICIPANT_ID) VALUES(?,?,?,"
							+ "?,?,?,?,?,?,?,?,?)";
				}
				else if (patientInformation.getDob() == null
						&& patientInformation.getDeathDate() != null)
				{
					query = "INSERT INTO CATISSUE_MATCHED_PARTICIPANT(PARTICIPANT_ID,EMPI_ID,LAST_NAME,FIRST_"
							+ "NAME,MIDDLE_NAME,GENDER,SOCIAL_SECURITY_NUMBER,ACTIVITY_STATUS,VITAL_STATUS,PART"
							+ "ICIPANT_MRN,PARTICIPANT_RACE,SEARCHED_PARTICIPANT_ID,DEATH_DATE) VA"
							+ "LUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
					columnValueBeanList.add(new ColumnValueBean("DEATH_DATE", patientInformation
							.getDeathDate(), 13));
				}
				else if (patientInformation.getDob() != null
						&& patientInformation.getDeathDate() == null)
				{
					query = "INSERT INTO CATISSUE_MATCHED_PARTICIPANT(PARTICIPANT_ID,EMPI_ID,LAST_NAME,FIRST_"
							+ "NAME,MIDDLE_NAME,GENDER,SOCIAL_SECURITY_NUMBER,ACTIVITY_STATUS,VITAL_STATUS,PART"
							+ "ICIPANT_MRN,PARTICIPANT_RACE,SEARCHED_PARTICIPANT_ID,BIRTH_DATE) VA"
							+ "LUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
					columnValueBeanList.add(new ColumnValueBean("BIRTH_DATE", patientInformation
							.getDob(), 13));
				}
				else
				{
					query = "INSERT INTO CATISSUE_MATCHED_PARTICIPANT(PARTICIPANT_ID,EMPI_ID,LAST_NAME,FIRST_"
							+ "NAME,MIDDLE_NAME,GENDER,SOCIAL_SECURITY_NUMBER,ACTIVITY_STATUS,VITAL_STATUS,PART"
							+ "ICIPANT_MRN,PARTICIPANT_RACE,SEARCHED_PARTICIPANT_ID,BIRTH_DATE,DEA"
							+ "TH_DATE) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					columnValueBeanList.add(new ColumnValueBean("BIRTH_DATE", patientInformation
							.getDob(), 13));
					columnValueBeanList.add(new ColumnValueBean("DEATH_DATE", patientInformation
							.getDeathDate(), 13));
				}
				columnValueBeans.add(columnValueBeanList);
				dao.executeUpdate(query, columnValueBeans);
			}

			updateMatchedPartiMapping(dao, participant.getId().longValue(), matchPartpantLst.size());
			dao.commit();
		}
		catch (DAOException e)
		{
			dao.rollback();
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}
		finally
		{
			dao.closeSession();
		}
	}

	/**
	 * Update matched parti mapping.
	 *
	 * @param jdbcdao the jdbcdao
	 * @param searchPartiId the search parti id
	 * @param noOfMathcedPaticipants the no of mathced paticipants
	 *
	 * @throws DAOException the DAO exception
	 */
	private void updateMatchedPartiMapping(JDBCDAO jdbcdao, long searchPartiId,
			int noOfMathcedPaticipants) throws DAOException
	{
		Calendar cal = Calendar.getInstance();
		java.util.Date date = cal.getTime();
		LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
		LinkedList columnValueBeanList = new LinkedList();
		columnValueBeanList.add(new ColumnValueBean(noOfMathcedPaticipants));
		columnValueBeanList.add(new ColumnValueBean(searchPartiId));
		columnValueBeans.add(columnValueBeanList);
		String query = (new StringBuilder()).append(
				"UPDATE MATCHED_PARTICIPANT_MAPPING SET NO_OF_MATCHED_PARTICIPANTS = ?").append(" WHERE SEARCHED_PARTICIPANT_ID=?").toString();
		try
		{
			jdbcdao.executeUpdate(query, columnValueBeans);
			jdbcdao.commit();
		}
		catch (DAOException e)
		{
			jdbcdao.rollback();
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}
	}

	/**
	 * Gets the race values.
	 *
	 * @param raceCollection the race collection
	 *
	 * @return the race values
	 */
	private String getRaceValues(Collection raceCollection)
	{
		StringBuffer raceValues = new StringBuffer();
		if (raceCollection != null && raceCollection.size() > 0)
		{
			for (Iterator iterator = raceCollection.iterator(); iterator.hasNext();)
			{
				if (raceValues.length() == 0)
				{
					raceValues.append((String) iterator.next());
				}
				else
				{
					raceValues.append((new StringBuilder()).append(",").append(
							(String) iterator.next()).toString());
				}
			}

		}
		return raceValues.toString();
	}

	/**
	 * Gets the mRN values.
	 *
	 * @param patientMedicalIdentifierColl the patient medical identifier coll
	 *
	 * @return the mRN values
	 */
	private String getMRNValues(Collection patientMedicalIdentifierColl)
	{
		StringBuffer mrnValue = new StringBuffer();
		String mrnId = "";
		String siteId = "";
		String siteName = "";
		String mrn = "";
		if (patientMedicalIdentifierColl != null)
		{
			for (Iterator iterator = patientMedicalIdentifierColl.iterator(); iterator.hasNext();)
			{
				mrnId = (String) iterator.next();
				siteId = (String) iterator.next();
				siteName = (String) iterator.next();
				mrn = mrnId+":"+siteId+":"+siteName;
				if (mrnValue.length() == 0)
				{
					mrnValue.append(mrn);
				}
				else
				{
					mrnValue.append(",").append(mrn).toString();
				}
			}
		}
		return mrnValue.toString();
	}

	/**
	 * Populate list with cs name.
	 *
	 * @param list the list
	 * @param dao the dao
	 *
	 * @throws DAOException the DAO exception
	 */
	private void populateListWithCSName(List list, JDBCDAO dao) throws DAOException
	{
		if (list != null && !list.isEmpty())
		{
			for (int i = 0; i < list.size(); i++)
			{
				List values = (List) list.get(i);
				if (!values.isEmpty() && values.get(0) != "")
				{
					Long partiId = Long.valueOf((String) values.get(0));
					String clinstdyNames = getClinicalStudyNames(partiId, dao);
					values.add(0, Integer.valueOf(0));
					values.add(values.size(), clinstdyNames);
				}
			}

		}
	}

	/**
	 * Gets the clinical study names.
	 *
	 * @param participantId the participant id
	 * @param dao the dao
	 *
	 * @return the clinical study names
	 *
	 * @throws DAOException the DAO exception
	 */
	private String getClinicalStudyNames(Long participantId, JDBCDAO dao) throws DAOException
	{
		String query = (new StringBuilder())
				.append(
						"SELECT SHORT_TITLE FROM CATISSUE_CLINICAL_STUDY_REG CSR JOIN CATISSUE_SPECIMEN_P"
								+ "ROTOCOL CSP ON CSR.CLINICAL_STUDY_ID=CSP.IDENTIFIER WHERE PARTICIPANT_ID='")
				.append(participantId).append("'").toString();
		List list = dao.executeQuery(query);
		StringBuffer csNames = new StringBuffer();
		if (!list.isEmpty())
		{
			for (int i = 0; i < list.size(); i++)
			{
				List clinStuNameLst = (List) list.get(0);
				if (clinStuNameLst.isEmpty() || clinStuNameLst.get(0) == "")
				{
					continue;
				}
				if (csNames.length() == 0)
				{
					csNames.append(clinStuNameLst.get(0));
				}
				else
				{
					csNames.append((new StringBuilder()).append(",").append(clinStuNameLst.get(0))
							.toString());
				}
			}

		}
		return csNames.toString();
	}

	/**
	 * Gets the processed matched participants.
	 *
	 * @param userId the user id
	 *
	 * @return the processed matched participants
	 *
	 * @throws DAOException the DAO exception
	 */
	public List getProcessedMatchedParticipants(Long userId) throws DAOException
	{

		JDBCDAO dao = null;
		List list = null;
		try
		{
			dao = ParticipantManagerUtility.getJDBCDAO();
			String query = "SELECT SEARCHED_PARTICIPANT_ID,LAST_NAME,FIRST_NAME,CREATION_DATE,NO_OF_MATCHED_"
					+ "PARTICIPANTS FROM MATCHED_PARTICIPANT_MAPPING  PARTIMAPPING JOIN CATISSUE_PARTIC"
					+ "IPANT PARTI ON PARTI.IDENTIFIER=PARTIMAPPING.SEARCHED_PARTICIPANT_ID WHERE PARTI"
					+ "MAPPING.USER_ID='"
					+ userId
					+ "' AND PARTIMAPPING.NO_OF_MATCHED_PARTICIPANTS!='-1'";
			list = dao.executeQuery(query);
			populateListWithCSName(list, dao);
		}
		finally
		{
			dao.closeSession();
		}
		return list;
	}
}
