package edu.wustl.common.participant.dao;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.participant.client.IParticipantManager;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.utility.ParticipantManagerException;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.newdao.GenericHibernateDAO;
import edu.wustl.dao.query.generator.ColumnValueBean;
import edu.wustl.dao.query.generator.DBTypes;
import edu.wustl.patientLookUp.domain.PatientInformation;


public class CommonParticipantDAO extends GenericHibernateDAO<IParticipant, Long>
{
	private static final Logger LOGGER = Logger.getCommonLogger(CommonParticipantDAO.class);
	private static final String ID = "id";
	private static final String CP_ID_LIST = "cpIdList";
	private static final String PARTICIPANT_ID = "participantId";
	private static final String USER_ID = "userId";
	private static final String NO_OF_MATCHED_PARTICIPANT = "noOfMatchedParticipant";
	
	private static final String IS_PART_MATCH_WITHIN_CSCP_ENABLED ="isParticipantMatchWithinCSCPEnable";
	private static final String GET_PART_ID_LIST ="getPartcipantIdsList";
	private static final String GET_COLUMN_LIST ="getColumnList";
	private static final String GET_PART_EMPI_STATUS ="getPartiEMPIStatus";
	private static final String IS_PART_PROCESSING ="isParticipantIsProcessing";
	private static final String GET_PROCESSED_MATCHED_PART_ID ="getProcessedMatchedParticipantIds";
	private static final String IS_OLD_MATCHES_PRESENT ="isOldMatchesPresent";
	private static final String GET_MATCHED_PART_LIST ="retrieveMatchedParticipantList";
	private static final String GET_SEARCH_PART_IDS ="fetchSearchParticipantIds";
	private static final String GET_TOTAL_COUNT ="getTotalCount";
	
	private static final String SET_EMPI_SQL = "UPDATE CATISSUE_PARTICIPANT SET EMPI_ID_STATUS=? WHERE IDENTIFIER=?";
	private static final String UPDATE_OLD_EMPI_SQL = "INSERT INTO PARTICIPANT_EMPI_ID_MAPPING VALUES(?,?,?,?)";
	
	private static final String ADD_PART_TO_PROCESS_MSG_QUEUE = "INSERT INTO MATCHED_PARTICIPANT_MAPPING(NO_OF_MATCHED_PARTICIPANTS," +
			"CREATION_DATE,SEARCHED_PARTICIPANT_ID) VALUES(?,?,?)";
	
	private static final String UPDATE_PART_USER_MAP_SQL = "INSERT INTO EMPI_PARTICIPANT_USER_MAPPING VALUES(?,?)";
	private static final String DELETE_PROCESS_PART_SQL = "DELETE FROM MATCHED_PARTICIPANT_MAPPING WHERE SEARCHED_PARTICIPANT_ID=?";
	
	private static final String UPDATE_PROCESS_PART_SQL = "UPDATE MATCHED_PARTICIPANT_MAPPING SET NO_OF_MATCHED_PARTICIPANTS=0 "
			+ "WHERE SEARCHED_PARTICIPANT_ID=?";
	
	private static final String STORE_MATCH_PART_SQL = "INSERT INTO CATISSUE_MATCHED_PARTICIPANT(PARTICIPANT_ID,EMPI_ID,LAST_NAME,FIRST_"
		+ "NAME,MIDDLE_NAME,GENDER,SOCIAL_SECURITY_NUMBER,ACTIVITY_STATUS,VITAL_STATUS,PART"
		+ "ICIPANT_MRN,PARTICIPANT_RACE,SEARCHED_PARTICIPANT_ID,ORDER_NO,BIRTH_DATE,DEA"
		+ "TH_DATE) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	
	private static final String UPDATE_MATCH_PART_MAPPING = "UPDATE MATCHED_PARTICIPANT_MAPPING SET NO_OF_MATCHED_PARTICIPANTS = ? WHERE SEARCHED_PARTICIPANT_ID=?";

	
	
	public CommonParticipantDAO(String applicationName,SessionDataBean sessionDataBean) 
	{
		super(applicationName, sessionDataBean);
	}

	/**
	 * PUT in PMhql
	 * @param id
	 * @return
	 * @throws DAOException
	 */
	public List isParticipantMatchWithinCSCPEnable(Long id) throws DAOException
	{
		return executeNamedQuery(IS_PART_MATCH_WITHIN_CSCP_ENABLED,null,null,new ColumnValueBean(ID, id));
	}
	
	/**
	 * PUT in Clinportalhql
	 * @param cpIdList
	 * @return
	 * @throws DAOException
	 */
	public List getPartcipantIdsList(Set cpIdList) throws DAOException
	{
		List idListArray = null;
		if (cpIdList != null && !cpIdList.isEmpty())
		{
			idListArray = executeNamedQuery(GET_PART_ID_LIST,null,null,new ColumnValueBean(CP_ID_LIST, cpIdList));
		}
		return idListArray;
	}
	
	/**
	 * PUT in Clinportalhql
	 * @return
	 * @throws DAOException
	 */
	public List<Object[]> getColumnList() throws DAOException
	{
		return executeNamedQuery(GET_COLUMN_LIST,null,null,new ColumnValueBean("participant","Participant"));
	}
	
	
	/**
	 * @param participantI
	 * @return
	 * @throws DAOException
	 */
	public String getPartiEMPIStatus(long participantId) throws DAOException
	{

		String eMPIStatus = "";
		List list = executeNamedQuery(GET_PART_EMPI_STATUS,null,null, new ColumnValueBean(ID, participantId));
		if (!list.isEmpty())
		{
			eMPIStatus = (String) list.get(0);
			//Bug 23731
			if (eMPIStatus == null)
			{
				eMPIStatus = "";
			}
			// -- end --
		}
		return eMPIStatus;
	}
	
	/**
	 * PUT in PMhql
	 * @param id
	 * @return
	 * @throws DAOException
	 */
	public boolean isParticipantIsProcessing(Long id) throws DAOException
	{

		boolean status = false;
		List list = executeNamedQuery(IS_PART_PROCESSING,null,null, new ColumnValueBean(PARTICIPANT_ID, id));
		if (!list.isEmpty() && !"".equals(list.get(0)))
		{
			status = true;
		}
		return status;
	}
	
	/**
	 * PUT in PMhql
	 * @param userId
	 * @return
	 * @throws DAOException
	 */
	public List<Long> getProcessedMatchedParticipantIds(Long userId) throws DAOException
	{
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean(USER_ID, userId));
		columnValueBeanList.add(new ColumnValueBean(NO_OF_MATCHED_PARTICIPANT, Integer.valueOf("-1")));

		return executeNamedQuery(GET_PROCESSED_MATCHED_PART_ID,null,null, columnValueBeanList);

//		for (Long object : resultSet)
//		{
//			if(object!=null)
//			{
//				particpantIdColl.add(Long.valueOf(object.toString()));
//			}
//			ArrayList particpantIdList = (ArrayList) object;
//			if (particpantIdList != null && !particpantIdList.isEmpty())
//			{
//				particpantIdColl.add(Long.valueOf(particpantIdList.get(0).toString()));
//			}
//		}
//		return particpantIdColl;
	}
	
	
	
	public void setEMPIIdStatus(Long participantId, String status) throws DAOException
	{
		LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean(status));
		columnValueBeanList.add(new ColumnValueBean(participantId));
		columnValueBeans.add(columnValueBeanList);
		executeSQLUpdate(SET_EMPI_SQL, columnValueBeans);
	}
	
	public void updateOldEMPIDetails(Long participantId, String empiId) throws DAOException
	{
		final LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
		final LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		
		String temporaryParticipantId = participantId + "T";
		columnValueBeanList.add(new ColumnValueBean("PERMANENT_PARTICIPANT_ID", participantId,
				DBTypes.VARCHAR));
		columnValueBeanList.add(new ColumnValueBean("TEMPARARY_PARTICIPANT_ID",
				temporaryParticipantId, DBTypes.VARCHAR));
		columnValueBeanList.add(new ColumnValueBean("OLD_EMPI_ID", empiId, DBTypes.VARCHAR));
		columnValueBeanList.add(new ColumnValueBean("TEMPMRNDATE", new Timestamp(System
				.currentTimeMillis()), DBTypes.TIMESTAMP));
		
		columnValueBeans.add(columnValueBeanList);
		executeSQLUpdate(UPDATE_OLD_EMPI_SQL, columnValueBeans);
	}
	
	public void addParticipantToProcessMessageQueue(LinkedHashSet<Long> userIdSet,
			Long participantId) throws DAOException
	{
		// delete old data from DB to start up clean with matching process
		// Bug fix 18823
		deleteProcessedParticipant(participantId);

		Calendar cal = Calendar.getInstance();
		Date date = cal.getTime();
		LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();

		
		columnValueBeanList.add(new ColumnValueBean("NO_OF_MATCHED_PARTICIPANTS", Integer
				.valueOf(-1), DBTypes.LONG));
		columnValueBeanList.add(new ColumnValueBean("CREATION_DATE", date, DBTypes.DATE));
		columnValueBeanList.add(new ColumnValueBean("SEARCHED_PARTICIPANT_ID", participantId,
				DBTypes.LONG));
		columnValueBeans.add(columnValueBeanList);
		executeSQLUpdate(ADD_PART_TO_PROCESS_MSG_QUEUE, columnValueBeans);

		updateParticipantUserMapping(userIdSet, participantId);
	}
	
	/**
	 * Update participant user mapping.
	 *
	 * @param jdbcdao the jdbcdao
	 * @param userIdSet the user id set
	 * @param participantId the participant id
	 *
	 * @throws DAOException the DAO exception
	 */
	private void updateParticipantUserMapping(LinkedHashSet<Long> userIdSet, Long participantId) throws DAOException
	{
		Iterator iterator = userIdSet.iterator();
		while (iterator.hasNext())
		{
			Long userId = (Long) iterator.next();
			LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();

			columnValueBeanList.add(new ColumnValueBean("PARTICIPANT_ID", participantId,
					DBTypes.LONG));
			columnValueBeanList.add(new ColumnValueBean("USER_ID", userId, DBTypes.LONG));
			columnValueBeans.add(columnValueBeanList);
			executeSQLUpdate(UPDATE_PART_USER_MAP_SQL, columnValueBeans);
		}
	}
	
	public void deleteProcessedParticipant(Long id) throws DAOException
	{
		LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean(id));
		columnValueBeans.add(columnValueBeanList);
		executeSQLUpdate(DELETE_PROCESS_PART_SQL, columnValueBeans);
	}

	public void updateProcessedParticipant(Long id) throws DAOException
	{
		LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean(id));
		columnValueBeans.add(columnValueBeanList);
		executeSQLUpdate(UPDATE_PROCESS_PART_SQL, columnValueBeans);
	}
	
	/**
	 * PUT in PMhql
	 * @param participantId
	 * @return
	 * @throws DAOException
	 */
	public boolean isOldMatchesPresent(Long participantId) throws DAOException
	{
		List<Object> partIdList = executeNamedQuery(IS_OLD_MATCHES_PRESENT,null,null, new ColumnValueBean(PARTICIPANT_ID, participantId));
		boolean isOldMatchesPresent = false;
		if (!partIdList.isEmpty() && partIdList.get(0) != null)
		{
			isOldMatchesPresent = true;
		}
		return isOldMatchesPresent;
	}
	
	/**
	 * PUT in PMhql
	 * @param participantId
	 * @return
	 * @throws DAOException
	 * @throws BizLogicException
	 * @throws ParseException
	 * @throws ParticipantManagerException
	 */
	public List retrieveMatchedParticipantList(final long participantId)
	throws DAOException, BizLogicException, ParseException, ParticipantManagerException
	{
		return executeNamedQuery(GET_MATCHED_PART_LIST,null,null, new ColumnValueBean(PARTICIPANT_ID, participantId));
	}
	
	/**
	 * PUT in PMhql
	 * @return
	 * @throws DAOException
	 */
	public List<Long> fetchSearchParticipantIds() throws DAOException
	{
		return executeNamedQuery(GET_SEARCH_PART_IDS,null,null,new ColumnValueBean(NO_OF_MATCHED_PARTICIPANT, Integer.valueOf("-1")));
	}
	
	// CIDER PARTICIPANT MATCH
	public void storeMatchedParticipant(IParticipant participant,PatientInformation patientInformation,String raceValues,String mrnValue,int orderNo) throws DAOException
	{

		LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();;
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();;
		columnValueBeanList.add(new ColumnValueBean("PARTICIPANT_ID", patientInformation.getId(),
				DBTypes.LONG));
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
		columnValueBeanList.add(new ColumnValueBean("ORDER_NO", orderNo,
				DBTypes.LONG));
		columnValueBeanList
		.add(new ColumnValueBean("BIRTH_DATE", patientInformation.getDob(), DBTypes.DATE));
		columnValueBeanList.add(new ColumnValueBean("DEATH_DATE", patientInformation.getDeathDate(),
		DBTypes.DATE));
		
		columnValueBeans.add(columnValueBeanList);
		executeSQLUpdate(STORE_MATCH_PART_SQL, columnValueBeans);
	}
	
	public void updateMatchedPartiMapping(long searchPartiId, int noOfMathcedPaticipants)
	throws DAOException
	{
		Calendar.getInstance();
		LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean(noOfMathcedPaticipants));
		columnValueBeanList.add(new ColumnValueBean(searchPartiId));
		columnValueBeans.add(columnValueBeanList);
		executeSQLUpdate(UPDATE_MATCH_PART_MAPPING, columnValueBeans);
	}
	
	/**
	 * PUT in Clinportalhql
	 * @param userId
	 * @param recordsPerPage
	 * @return
	 * @throws DAOException
	 * @throws ParticipantManagerException
	 */
	public List getProcessedMatchedParticipants(Long userId, int recordsPerPage) throws DAOException, ParticipantManagerException
	{
		final IParticipantManager participantMgrImplObj = ParticipantManagerUtility.getParticipantMgrImplObj();
		String query = participantMgrImplObj.getProcessedMatchedParticipantQuery(userId);
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean(USER_ID, userId));
		columnValueBeanList.add(new ColumnValueBean(NO_OF_MATCHED_PARTICIPANT, Integer.valueOf("-1")));
		return executeSQLQuery(query, 0,recordsPerPage, columnValueBeanList);
	}


	/**
	 * PUT in PMhql
	 * @param userId
	 * @return
	 * @throws DAOException
	 */

	public int getTotalCount(Long userId) throws DAOException
	{
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean(USER_ID, userId));
		columnValueBeanList.add(new ColumnValueBean(NO_OF_MATCHED_PARTICIPANT,Integer.valueOf("-1")));
		List list = executeNamedQuery(GET_TOTAL_COUNT, null,null, columnValueBeanList);
		return Integer.parseInt(list.get(0).toString());
	}
}
