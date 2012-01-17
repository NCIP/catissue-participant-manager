
package edu.wustl.common.participant.bizlogic;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.Utility;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.query.generator.ColumnValueBean;
import edu.wustl.dao.query.generator.DBTypes;
import edu.wustl.patientLookUp.domain.PatientInformation;
import edu.wustl.patientLookUp.util.PatientLookupException;

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
		EMPIParticipantRegistrationBizLogic bizLogic = new EMPIParticipantRegistrationBizLogic();
		for (int i = 0; i < ParticipantIdLst.size(); i++)
		{
			List idsList = (List) ParticipantIdLst.get(i);
			if (!idsList.isEmpty() && idsList.get(0) != "")
			{
				Long identifier = Long.valueOf((String) idsList.get(0));
				logger.info("Fetching matches for participant : " + identifier);
				try
				{
					IParticipant participant = ParticipantManagerUtility.getParticipantById(identifier);
					boolean isCallToLkupLgic = ParticipantManagerUtility.isCallToLookupLogicNeeded(participant);
					if (isCallToLkupLgic)
					{
						List matchPartpantLst = ParticipantManagerUtility.getListOfMatchingParticipants(participant,
								Constants.PARTICIPANT_LOOKUP_ALGO_EMPI, null,null);
						if (matchPartpantLst.size() == 0
								&& (participant.getBirthDate() != null || (participant.getSocialSecurityNumber() != null && !""
										.equals(participant.getSocialSecurityNumber()))))
						{
							ParticipantManagerUtility.setEMPIIdStatus(participant.getId(), Constants.EMPI_ID_PENDING);
							bizLogic.registerPatientToeMPI(participant);
							//							ParticipantManagerUtility.deleteProcessedParticipant(participant
							//									.getId());
							// count of matched patients updated to 0 when no matches found
							ParticipantManagerUtility.updateProcessedParticipant(participant.getId());
						}
						else
						{
							storeMatchedParticipant(participant, matchPartpantLst);
						}
					}
				}
				catch (Exception e)
				{
					logger.error("Error while performing the EMPI participant match for participant with id "
							+ identifier, e);
				}
				logger.info("Matches fetched for participant : " + identifier);
			}
		}
	}



	/**
	 * Method which will search for matched for given participant id with the given threshhold.
	 * @param ParticipantId participant Id
	 * @param threshHold threshhold for matching algorithm
	 * @return List of matched patientInformation objects.
	 * @throws ApplicationException exception
	 * @throws PatientLookupException exception
	 */
	public List getMatchedParticipantList(Long ParticipantId, Integer threshHold)
			throws ApplicationException, PatientLookupException
	{
		List matchPartpantLst = new ArrayList();
		logger.info("Fetching matches for participant : " + ParticipantId);
		IParticipant participant = ParticipantManagerUtility.getParticipantById(ParticipantId);
		boolean isCallToLkupLgic = ParticipantManagerUtility.isCallToLookupLogicNeeded(participant);
		if (isCallToLkupLgic)
		{
			matchPartpantLst.addAll(ParticipantManagerUtility.getListOfMatchingParticipants(
					participant, Constants.PARTICIPANT_LOOKUP_ALGO_EMPI, null, threshHold));
		}
		return matchPartpantLst;
	}

	/**
	 * Store matched participant.
	 *
	 * @param participant the participant
	 * @param matchPartpantLst the match partpant lst
	 *
	 * @throws DAOException the DAO exception
	 */
	private void storeMatchedParticipant(IParticipant participant, List matchPartpantLst) throws DAOException
	{

		JDBCDAO dao = null;
		try
		{
			dao = ParticipantManagerUtility.getJDBCDAO();
			String query = "";
			String raceValues = null;
			String mrnValue = "";
			PatientInformation patientInformation = null;
			LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = null;
			LinkedList<ColumnValueBean> columnValueBeanList = null;

			for (int i = 0; i < matchPartpantLst.size(); i++)
			{
				patientInformation = (PatientInformation) matchPartpantLst.get(i);
				raceValues = getRaceValues(patientInformation.getRaceCollection());
				mrnValue = getMRNValues(patientInformation.getPmiCollection());
				columnValueBeanList = new LinkedList<ColumnValueBean>();
				columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
				if (null != patientInformation.getId())
				{
					columnValueBeanList.add(new ColumnValueBean("PARTICIPANT_ID", patientInformation.getId(),
							DBTypes.LONG));
				}
				columnValueBeanList.add(new ColumnValueBean("EMPI_ID", patientInformation.getUpi(), DBTypes.VARCHAR));
				columnValueBeanList.add(new ColumnValueBean("LAST_NAME", patientInformation.getLastName(),
						DBTypes.VARCHAR));
				columnValueBeanList.add(new ColumnValueBean("FIRST_NAME", patientInformation.getFirstName(),
						DBTypes.VARCHAR));
				columnValueBeanList.add(new ColumnValueBean("MIDDLE_NAME", patientInformation.getMiddleName(),
						DBTypes.VARCHAR));
				columnValueBeanList.add(new ColumnValueBean("GENDER", patientInformation.getGender(), DBTypes.VARCHAR));
				columnValueBeanList.add(new ColumnValueBean("SOCIAL_SECURITY_NUMBER", patientInformation.getSsn(),
						DBTypes.VARCHAR));
				columnValueBeanList.add(new ColumnValueBean("ACTIVITY_STATUS", patientInformation.getActivityStatus(),
						DBTypes.VARCHAR));
				columnValueBeanList.add(new ColumnValueBean("VITAL_STATUS", patientInformation.getVitalStatus(),
						DBTypes.VARCHAR));
				columnValueBeanList.add(new ColumnValueBean("PARTICIPANT_MRN", mrnValue, DBTypes.VARCHAR));
				columnValueBeanList.add(new ColumnValueBean("PARTICIPANT_RACE", raceValues, DBTypes.VARCHAR));
				//columnValueBeanList.add(new ColumnValueBean("IS_FROM_EMPI", patientInformation.getIsFromEMPI(), 11));
				columnValueBeanList.add(new ColumnValueBean("SEARCHED_PARTICIPANT_ID", participant.getId(),
						DBTypes.LONG));
				columnValueBeanList.add(new ColumnValueBean("ORDER_NO", i+1,
						DBTypes.LONG));
				if (patientInformation.getDob() == null && patientInformation.getDeathDate() == null)
				{
					query = "INSERT INTO CATISSUE_MATCHED_PARTICIPANT(PARTICIPANT_ID,EMPI_ID,LAST_NAME,FIRST_"
							+ "NAME,MIDDLE_NAME,GENDER,SOCIAL_SECURITY_NUMBER,ACTIVITY_STATUS,VITAL_STATUS,PART"
							+ "ICIPANT_MRN,PARTICIPANT_RACE,SEARCHED_PARTICIPANT_ID,ORDER_NO) VALUES(?,?,?,"
							+ "?,?,?,?,?,?,?,?,?,?)";
				}
				else if (patientInformation.getDob() == null && patientInformation.getDeathDate() != null)
				{
					query = "INSERT INTO CATISSUE_MATCHED_PARTICIPANT(PARTICIPANT_ID,EMPI_ID,LAST_NAME,FIRST_"
							+ "NAME,MIDDLE_NAME,GENDER,SOCIAL_SECURITY_NUMBER,ACTIVITY_STATUS,VITAL_STATUS,PART"
							+ "ICIPANT_MRN,PARTICIPANT_RACE,SEARCHED_PARTICIPANT_ID,ORDER_NO,DEATH_DATE) VA"
							+ "LUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					columnValueBeanList.add(new ColumnValueBean("DEATH_DATE", patientInformation.getDeathDate(),
							DBTypes.DATE));
				}
				else if (patientInformation.getDob() != null && patientInformation.getDeathDate() == null)
				{
					query = "INSERT INTO CATISSUE_MATCHED_PARTICIPANT(PARTICIPANT_ID,EMPI_ID,LAST_NAME,FIRST_"
							+ "NAME,MIDDLE_NAME,GENDER,SOCIAL_SECURITY_NUMBER,ACTIVITY_STATUS,VITAL_STATUS,PART"
							+ "ICIPANT_MRN,PARTICIPANT_RACE,SEARCHED_PARTICIPANT_ID,ORDER_NO,BIRTH_DATE) VA"
							+ "LUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					columnValueBeanList
							.add(new ColumnValueBean("BIRTH_DATE", patientInformation.getDob(), DBTypes.DATE));
				}
				else
				{
					query = "INSERT INTO CATISSUE_MATCHED_PARTICIPANT(PARTICIPANT_ID,EMPI_ID,LAST_NAME,FIRST_"
							+ "NAME,MIDDLE_NAME,GENDER,SOCIAL_SECURITY_NUMBER,ACTIVITY_STATUS,VITAL_STATUS,PART"
							+ "ICIPANT_MRN,PARTICIPANT_RACE,SEARCHED_PARTICIPANT_ID,ORDER_NO,BIRTH_DATE,DEA"
							+ "TH_DATE) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					columnValueBeanList
							.add(new ColumnValueBean("BIRTH_DATE", patientInformation.getDob(), DBTypes.DATE));
					columnValueBeanList.add(new ColumnValueBean("DEATH_DATE", patientInformation.getDeathDate(),
							DBTypes.DATE));
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
	private void updateMatchedPartiMapping(JDBCDAO jdbcdao, long searchPartiId, int noOfMathcedPaticipants)
			throws DAOException
	{
		Calendar.getInstance();
		LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean(noOfMathcedPaticipants));
		columnValueBeanList.add(new ColumnValueBean(searchPartiId));
		columnValueBeans.add(columnValueBeanList);
		String query = (new StringBuilder()).append(
				"UPDATE MATCHED_PARTICIPANT_MAPPING SET NO_OF_MATCHED_PARTICIPANTS = ?").append(
				" WHERE SEARCHED_PARTICIPANT_ID=?").toString();
		try
		{
			jdbcdao.executeUpdate(query, columnValueBeans);
			//jdbcdao.commit();
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
					raceValues.append((new StringBuilder()).append(",").append((String) iterator.next()).toString());
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
		String facilityId = "";
		String siteName = "";
		String mrn = "";
		if (patientMedicalIdentifierColl != null)
		{
			for (Iterator<IParticipantMedicalIdentifier<IParticipant, ISite>> iterator = patientMedicalIdentifierColl.iterator(); iterator.hasNext();)
			{
				IParticipantMedicalIdentifier<IParticipant, ISite> pmi =iterator.next();
				mrnId=pmi.getMedicalRecordNumber();
				//siteId= pmi.getSite().getId().toString();
				facilityId=  pmi.getSite().getFacilityId();
				siteName=pmi.getSite().getName();
				mrn = mrnId + ":" + facilityId + ":" + siteName;
				mrn=mrn.trim();
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
	 * @throws DAOException the dAO exception
	 */
	public void populateListWithCSName(List list) throws DAOException
	{
		JDBCDAO dao = null;
		try
		{
			dao = ParticipantManagerUtility.getJDBCDAO();
			populateListWithCSName(list, dao);
		}
		finally
		{
			dao.closeSession();
		}
	}

	/**
	 * Populate list with cs name.
	 *
	 * @param list the list
	 * @param dao the dao
	 *
	 * @throws DAOException the DAO exception
	 */
	@SuppressWarnings("unchecked")
	private void populateListWithCSName(List list, JDBCDAO dao) throws DAOException
	{
		if (list != null && !list.isEmpty())
		{
			for (int i = 0; i < list.size(); i++)
			{
				List values = (List) list.get(i);

				if (!values.isEmpty())
				{

					String lastName = ParticipantManagerUtility.modifyNameWithProperCase((String) values.get(1));
					String firstName = ParticipantManagerUtility.modifyNameWithProperCase((String) values.get(2));
					values.set(1, lastName);
					values.set(2, firstName);

					String dt = (String) values.get(3);
					try
					{
						Date date = Utility.parseDate(dt, Constants.TIMESTAMP_PATTERN);
						dt = Utility.parseDateToString(date, Constants.DATE_FORMAT);
						values.set(3, dt);
					}
					catch (java.text.ParseException e)
					{
						logger.error("Error while parsing date", e);
					}

					if (values.get(0) != "")
					{
						Long partiId = Long.valueOf((String) values.get(0));
						String clinstdyNames = getClinicalStudyNames(partiId, dao);
						values.add(0, Integer.valueOf(0));
						values.add(values.size(), clinstdyNames);
					}
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
		String query = "SELECT SHORT_TITLE FROM CATISSUE_CLINICAL_STUDY_REG CSR JOIN CATISSUE_SPECIMEN_PROTOCOL CSP ON CSR.CLINICAL_STUDY_ID=CSP.IDENTIFIER WHERE PARTICIPANT_ID=?";
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean("PARTICIPANT_ID", participantId, DBTypes.LONG));
		List list = dao.executeQuery(query, null, columnValueBeanList);
		StringBuffer csNames = new StringBuffer();
		if (!list.isEmpty())
		{
			for (int i = 0; i < list.size(); i++)
			{
				List clinStuNameLst = (List) list.get(i);
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
					csNames.append(",");
					csNames.append(clinStuNameLst.get(0));
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
	public List getProcessedMatchedParticipants(Long userId, int recordsPerPage) throws DAOException
	{
		JDBCDAO dao = null;
		List list = null;
		String query = null;
		try
		{
			dao = ParticipantManagerUtility.getJDBCDAO();
			/*
						String query = "SELECT SEARCHED_PARTICIPANT_ID,LAST_NAME,FIRST_NAME,CREATION_DATE,NO_OF_MATCHED_"
								+ "PARTICIPANTS FROM MATCHED_PARTICIPANT_MAPPING  PARTIMAPPING JOIN CATISSUE_PARTIC"
								+ "IPANT PARTI ON PARTI.IDENTIFIER=PARTIMAPPING.SEARCHED_PARTICIPANT_ID WHERE PARTI"
								+ "MAPPING.USER_ID='"
								+ userId
								+ "' AND PARTIMAPPING.NO_OF_MATCHED_PARTICIPANTS!='-1'";

			*/
			/*query = "SELECT SEARCHED_PARTICIPANT_ID,LAST_NAME,FIRST_NAME,CREATION_DATE,NO_OF_MATCHED_PARTICIPANTS FROM "
					+ " MATCHED_PARTICIPANT_MAPPING PARTIMAPPING JOIN CATISSUE_PARTICIPANT PARTI ON PARTI.IDENTIFIER=PARTIMAPPING.SEARCHED_PARTICIPANT_ID "
					+ " JOIN EMPI_PARTICIPANT_USER_MAPPING ON PARTIMAPPING.SEARCHED_PARTICIPANT_ID=EMPI_PARTICIPANT_USER_MAPPING.PARTICIPANT_ID"
					+ " WHERE EMPI_PARTICIPANT_USER_MAPPING.USER_ID=? AND PARTIMAPPING.NO_OF_MATCHED_PARTICIPANTS!=? ORDER BY CREATION_DATE";
			*/
			query = getQuery(userId);
			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
			columnValueBeanList.add(new ColumnValueBean("USER_ID", userId, DBTypes.LONG));
			columnValueBeanList.add(new ColumnValueBean("NO_OF_MATCHED_PARTICIPANTS", "-1", DBTypes.INTEGER));
			list = dao.executeQuery(query, recordsPerPage, null);
			//populateListWithCSName(list, dao);
		}
		finally
		{
			dao.closeSession();
		}
		return list;
	}

	public String getQuery(Long userId)
	{
		String query = " select temp, SEARCHED_PARTICIPANT_ID, INITCAP(LAST_NAME),INITCAP(FIRST_NAME), CREATION_DATE, NO_OF_MATCHED_PARTICIPANTS," +
				" substr(SYS_CONNECT_BY_PATH(SHORT_TITLE, ';'),2) as SHORT_TITLE from "
				+ " (SELECT CSNAME.SHORT_TITLE,0 as temp,"
				+ " PARTIMAPPING.SEARCHED_PARTICIPANT_ID,PARTI.LAST_NAME,PARTI.FIRST_NAME,"
				+ " to_char(CREATION_DATE,'" + Constants.DATE_FORMAT + "') as CREATION_DATE,"
				+ " PARTIMAPPING.NO_OF_MATCHED_PARTICIPANTS,"
				+ " count(*) OVER (partition by PARTIMAPPING.SEARCHED_PARTICIPANT_ID) CNT1,"
				+ " ROW_NUMBER() OVER(partition by PARTIMAPPING.SEARCHED_PARTICIPANT_ID ORDER BY CSNAME.SHORT_TITLE) SEQ1"
				+ " FROM  MATCHED_PARTICIPANT_MAPPING PARTIMAPPING JOIN CATISSUE_PARTICIPANT PARTI "
				+ " ON PARTI.IDENTIFIER=PARTIMAPPING.SEARCHED_PARTICIPANT_ID  JOIN EMPI_PARTICIPANT_USER_MAPPING "
				+ " ON PARTIMAPPING.SEARCHED_PARTICIPANT_ID=EMPI_PARTICIPANT_USER_MAPPING.PARTICIPANT_ID ,"
				+ " ( SELECT SHORT_TITLE,PARTICIPANT_ID FROM CATISSUE_CLINICAL_STUDY_REG CSR JOIN CATISSUE_SPECIMEN_PROTOCOL CSP "
				+ " ON CSR.CLINICAL_STUDY_ID=CSP.IDENTIFIER) CSNAME "
				+ " WHERE EMPI_PARTICIPANT_USER_MAPPING.USER_ID="
				+ userId
				+ "  AND PARTIMAPPING.NO_OF_MATCHED_PARTICIPANTS!= -1"
				+ "  and CSNAME.PARTICIPANT_ID=PARTIMAPPING.SEARCHED_PARTICIPANT_ID ) where SEQ1=CNT1 start with SEQ1=1 connect by prior SEQ1+1=SEQ1 " +
						" and prior SEARCHED_PARTICIPANT_ID=SEARCHED_PARTICIPANT_ID "
				+ " ORDER BY CREATION_DATE ";

		return query;

	}

	public int getTotalCount(Long userId) throws DAOException
	{
		int count = 0;
		JDBCDAO dao = null;
		try
		{
			dao = ParticipantManagerUtility.getJDBCDAO();
			String query = "SELECT count(*) FROM MATCHED_PARTICIPANT_MAPPING PARTIMAPPING "
					+ " JOIN EMPI_PARTICIPANT_USER_MAPPING ON "
					+ " PARTIMAPPING.SEARCHED_PARTICIPANT_ID=EMPI_PARTICIPANT_USER_MAPPING.PARTICIPANT_ID "
					+ " WHERE EMPI_PARTICIPANT_USER_MAPPING.USER_ID=? AND PARTIMAPPING.NO_OF_MATCHED_PARTICIPANTS!=?";

			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
			columnValueBeanList.add(new ColumnValueBean("USER_ID", userId, DBTypes.LONG));
			columnValueBeanList.add(new ColumnValueBean("NO_OF_MATCHED_PARTICIPANTS", "-1",
					DBTypes.INTEGER));
			List list = dao.executeQuery(query, null, columnValueBeanList);
			count = Integer.parseInt(((java.util.ArrayList) list.get(0)).get(0).toString());

		}
		finally
		{
			dao.closeSession();
		}
		return count;
	}

	/*protected String getCountQuery(String originalQuery)
	{
		return (new StringBuilder()).append("Select count(*) from (").append(originalQuery).append(") alias")
				.toString();
	}*/

	protected String putPageNumInSQL(String sql, int startIndex, int noOfRecords)
	{
		StringBuffer newSql = new StringBuffer(80);
		newSql.append("SELECT").append(" * ").append("FROM").append(" (").append("SELECT").append(" qry.*, ROWNUM rn ")
				.append("FROM").append(" (").append(sql).append(") qry WHERE ROWNUM <= ").append(
						startIndex + noOfRecords).append(") WHERE rn > ").append(startIndex);
		return newSql.toString();
	}

}
