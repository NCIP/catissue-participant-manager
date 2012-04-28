package edu.wustl.common.participant.dao;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
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
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.utility.Constants;
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
	
	public CommonParticipantDAO(String applicationName,SessionDataBean sessionDataBean) 
	{
		super(applicationName, sessionDataBean);
	}
	
	public Long getSiteIdByName(String siteName) throws DAOException
	{
		Long siteId = null;
		String hql = "Select id from " + ISite.class.getName() + " as site where site.name=:siteName";
		List<ColumnValueBean> valueList = new ArrayList<ColumnValueBean>();		
		valueList.add(new ColumnValueBean(siteName,siteName));
		List<Long> result = executeQuery(hql, null, null, valueList);
		if(result!=null&&!result.isEmpty())
		{
			siteId = result.get(0);
		}
		return siteId;
	}
	
	public IParticipant getParticipantById(final Long identifier) throws DAOException
	{
		String sourceObjectName = IParticipant.class.getName();
		String hql = "from "+sourceObjectName+" where id=:id";
		List<ColumnValueBean> valueList = new ArrayList<ColumnValueBean>();
		valueList.add(new ColumnValueBean("id",identifier));
		List<IParticipant> list = executeQuery(hql, null, null, valueList);
		IParticipant participant = null;
		if(!list.isEmpty())
		{
			participant = list.get(0);
		}
		return participant;

	}
	
	public List executeParticipantCodeQry(Set<Long> protocolIdList,String participantCode) throws DAOException
	{
		List patientInfoList = null;
		try
		{
			String hql = ParticipantManagerUtility.getParticipantCodeQry(protocolIdList);
			List<ColumnValueBean> valueList = new ArrayList<ColumnValueBean>();
			ColumnValueBean participantCodeValue = new ColumnValueBean("participantCode",participantCode);
			ColumnValueBean activityStatusValue = new ColumnValueBean("activityStatus","Disabled");
			valueList.add(participantCodeValue);
			valueList.add(activityStatusValue);
			patientInfoList =  executeQuery(hql, null,null,valueList);
		}
		catch(ParticipantManagerException exp)
		{
			LOGGER.error("ERROR WHILE executeParticipantCodeQry",exp);
		}
		return patientInfoList;
	}
	
	public List isParticipantMatchWithinCSCPEnable(Long id) throws DAOException
	{
		String query = "SELECT SP.PARTCIPNT_MATCH_WITHIN_CSCP FROM  CATISSUE_SPECIMEN_PROTOCOL SP WHERE SP.IDENTIFIER=:id";;
		List<ColumnValueBean> columnValueBeanList = new ArrayList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean("id", id));
		return executeSQLQuery(query,null,null,columnValueBeanList);
	}
	
	public List getPartcipantIdsList(Set cpIdList) throws DAOException
	{
		List idListArray = null;
		String query = "SELECT PARTICIPANT_ID FROM CATISSUE_CLINICAL_STUDY_REG WHERE CLINICAL_STUDY_ID in (:cpIdList)";

		if (cpIdList != null && !cpIdList.isEmpty())
		{
			List<ColumnValueBean> columnValueBeanList = new ArrayList<ColumnValueBean>();
			columnValueBeanList.add(new ColumnValueBean("cpIdList", cpIdList));
			idListArray = executeSQLQuery(query,null,null, columnValueBeanList);
		}
		return idListArray;
	}

	public List<Object[]> getColumnList() throws DAOException
	{

		String sql = "SELECT  columnData.COLUMN_NAME,displayData.DISPLAY_NAME FROM CATISSUE_INTERFACE_"
				+ "COLUMN_DATA columnData,CATISSUE_TABLE_RELATION relationData,CATISSUE_QUERY_TABLE"
				+ "_DATA tableData,CATISSUE_SEARCH_DISPLAY_DATA displayData where relationData.CHIL"
				+ "D_TABLE_ID = columnData.TABLE_ID and relationData.PARENT_TABLE_ID = tableData.TA"
				+ "BLE_ID and relationData.RELATIONSHIP_ID = displayData.RELATIONSHIP_ID and column"
				+ "Data.IDENTIFIER = displayData.COL_ID and tableData.ALIAS_NAME = 'Participant'";
		LOGGER.debug("DATA ELEMENT SQL : " + sql);
		return executeSQLQuery(sql,null,null, null);
	}
	
	public List isEMPIEnable(String query,Long participantId) throws DAOException
	{
		List statusList = null;
		List<ColumnValueBean> columnValueBeanList = new ArrayList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean("participantId", participantId));
		return executeSQLQuery(query,null,null, columnValueBeanList);
	}
	
	
	public String getPartiEMPIStatus(long participantId) throws DAOException
	{

		String eMPIStatus = "";
		String query = "SELECT EMPI_ID_STATUS FROM CATISSUE_PARTICIPANT  WHERE IDENTIFIER=:id";
		List<ColumnValueBean> columnValueBeanList = new ArrayList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean("id", participantId));
		List list = executeSQLQuery(query,null,null, columnValueBeanList);
		if (!list.isEmpty())
		{
			eMPIStatus = (String) list.get(0);
		}
		return eMPIStatus;
	}
	
	public boolean isParticipantIsProcessing(Long id) throws DAOException
	{

		boolean status = false;
		String query =  "SELECT * FROM MATCHED_PARTICIPANT_MAPPING WHERE SEARCHED_PARTICIPANT_ID = :participantId "
			+ "AND NO_OF_MATCHED_PARTICIPANTS != 0";
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean("participantId", id));
		List list = executeSQLQuery(query,null,null, columnValueBeanList);
		if (!list.isEmpty() && !"".equals(list.get(0)))
		{
			status = true;
		}
		return status;
	}
	
	public List<Long> getProcessedMatchedParticipantIds(Long userId) throws DAOException
	{
		List<Long> particpantIdColl = new ArrayList<Long>();
		String query = "SELECT SEARCHED_PARTICIPANT_ID FROM  MATCHED_PARTICIPANT_MAPPING PARTIMAPPING  "
				+ " JOIN EMPI_PARTICIPANT_USER_MAPPING ON PARTIMAPPING.SEARCHED_PARTICIPANT_ID=EMPI_PARTICIPANT_USER_MAPPING.PARTICIPANT_ID"
				+ " WHERE EMPI_PARTICIPANT_USER_MAPPING.USER_ID=:userId AND PARTIMAPPING.NO_OF_MATCHED_PARTICIPANTS!=:noOfMatchedParticipant";

		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean("userId", userId));
		columnValueBeanList.add(new ColumnValueBean("noOfMatchedParticipant", Integer.valueOf("-1")));

		return executeSQLQuery(query,null,null, columnValueBeanList);

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
		String sql = "UPDATE CATISSUE_PARTICIPANT SET EMPI_ID_STATUS=? WHERE IDENTIFIER=?";
		executeSQLUpdate(sql, columnValueBeans);
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
		
		final String sql = "INSERT INTO PARTICIPANT_EMPI_ID_MAPPING VALUES(?,?,?,?)";
		columnValueBeans.add(columnValueBeanList);
		executeSQLUpdate(sql, columnValueBeans);
	}
	
	public void addParticipantToProcessMessageQueue(LinkedHashSet<Long> userIdSet,
			Long participantId) throws DAOException
	{
		String query = "INSERT INTO MATCHED_PARTICIPANT_MAPPING(NO_OF_MATCHED_PARTICIPANTS,"
			+ "CREATION_DATE,SEARCHED_PARTICIPANT_ID) VALUES(?,?,?)";

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
		executeSQLUpdate(query, columnValueBeans);

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
			String query = "INSERT INTO EMPI_PARTICIPANT_USER_MAPPING VALUES(?,?)";
			Long userId = (Long) iterator.next();
			LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();

			columnValueBeanList.add(new ColumnValueBean("PARTICIPANT_ID", participantId,
					DBTypes.LONG));
			columnValueBeanList.add(new ColumnValueBean("USER_ID", userId, DBTypes.LONG));
			columnValueBeans.add(columnValueBeanList);
			executeSQLUpdate(query, columnValueBeans);
		}
	}
	
	public void deleteProcessedParticipant(Long id) throws DAOException
	{
		LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean(id));
		columnValueBeans.add(columnValueBeanList);
		String query = "DELETE FROM MATCHED_PARTICIPANT_MAPPING WHERE SEARCHED_PARTICIPANT_ID=?";
		executeSQLUpdate(query, columnValueBeans);
	}

	public void updateProcessedParticipant(Long id) throws DAOException
	{
		LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean(id));
		columnValueBeans.add(columnValueBeanList);
		String query = "UPDATE MATCHED_PARTICIPANT_MAPPING SET NO_OF_MATCHED_PARTICIPANTS=0 "
				+ "WHERE SEARCHED_PARTICIPANT_ID=?";
		executeSQLUpdate(query, columnValueBeans);
	}
	
	public List getSiteObject(final String facilityId) throws DAOException
	{
		String hql = "select id,name from "+ISite.class.getName()+ "where facilityId=:facilityId"; 
		List<ColumnValueBean> columnValueBeans = new ArrayList<ColumnValueBean>();
		columnValueBeans.add(new ColumnValueBean("facilityId",facilityId));
		return executeQuery(hql, null, null, columnValueBeans);
	}
	
	public String getEMPIIDStatus(Long participantId) throws DAOException
	{
		String query = "SELECT EMPI_ID_STATUS FROM CATISSUE_PARTICIPANT WHERE IDENTIFIER=:id";
		List<ColumnValueBean> colValueBeanList = new ArrayList<ColumnValueBean>();
		colValueBeanList.add(new ColumnValueBean("id", participantId));
		List<String> idList = executeSQLQuery(query,null,null, colValueBeanList);
		String empIdStatus = null;
		if (null != idList && !idList.isEmpty())
		{
			empIdStatus = idList.get(0);
		}
		return empIdStatus;
	}
	
	public boolean isOldMatchesPresent(Long participantId) throws DAOException
	{
		String query = "SELECT SEARCHED_PARTICIPANT_ID FROM MATCHED_PARTICIPANT_MAPPING "
			+ "WHERE SEARCHED_PARTICIPANT_ID=:participantId";
		List<ColumnValueBean> colValueBeanList = new ArrayList<ColumnValueBean>();
		colValueBeanList.add(new ColumnValueBean("participantId", participantId));
		List<Object> partIdList = executeSQLQuery(query,null,null, colValueBeanList);
		boolean isOldMatchesPresent = false;
		if (!partIdList.isEmpty() && partIdList.get(0) != null)
		{
			isOldMatchesPresent = true;
		}
		return isOldMatchesPresent;
	}
	
	public IParticipant getParticpantByEmpiId(String empiId) throws DAOException
	{
		IParticipant part = null;
		final String sourceObjectName = IParticipant.class.getName();
		String hql = "from " + sourceObjectName + " where empiId:empiId";
		List<ColumnValueBean> columnValueBeans = new ArrayList<ColumnValueBean>();
		columnValueBeans.add(new ColumnValueBean(Constants.PARTICIPANT_EMPIID,empiId));
		final List list = executeQuery(hql, null, null, columnValueBeans);
		if (!list.isEmpty())
		{
			part = (IParticipant) list.get(0);
		}
		return part;
	}
	
	public List retrieveMatchedParticipantList(final long participantId)
	throws DAOException, BizLogicException, ParseException, ParticipantManagerException
	{
		final String query = "SELECT * FROM CATISSUE_MATCHED_PARTICIPANT WHERE SEARCHED_PARTICIPANT_ID=:participantId order by ORDER_NO";
		final List<ColumnValueBean> columnValueBeanList = new ArrayList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean("participantId", participantId));
		return executeSQLQuery(query,null,null, columnValueBeanList);
	}
	
	public List<Long> fetchSearchParticipantIds() throws DAOException
	{
		final String query = "SELECT SEARCHED_PARTICIPANT_ID FROM MATCHED_PARTICIPANT_MAPPING WHERE NO_OF_MATCHED_PARTICIPANTS=:noOfMatchedParticpants";
		final LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean("noOfMatchedParticpants", "-1"));
		return executeSQLQuery(query,null,null, columnValueBeanList);
	}
	
	// CIDER PARTICIPANT MATCH
	public void storeMatchedParticipant(IParticipant participant,PatientInformation patientInformation,String raceValues,String mrnValue,int orderNo) throws DAOException
	{

		LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();;
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();;
		String query = "INSERT INTO CATISSUE_MATCHED_PARTICIPANT(PARTICIPANT_ID,EMPI_ID,LAST_NAME,FIRST_"
			+ "NAME,MIDDLE_NAME,GENDER,SOCIAL_SECURITY_NUMBER,ACTIVITY_STATUS,VITAL_STATUS,PART"
			+ "ICIPANT_MRN,PARTICIPANT_RACE,SEARCHED_PARTICIPANT_ID,ORDER_NO,BIRTH_DATE,DEA"
			+ "TH_DATE) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
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
		executeSQLUpdate(query, columnValueBeans);
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
		String query = (new StringBuilder()).append(
				"UPDATE MATCHED_PARTICIPANT_MAPPING SET NO_OF_MATCHED_PARTICIPANTS = ?").append(
				" WHERE SEARCHED_PARTICIPANT_ID=?").toString();
		executeSQLUpdate(query, columnValueBeans);
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
	 * @throws ParticipantManagerException
	 * @throws BizLogicException
	 */
	public List<String> getClinicalStudyNames(Long participantId) throws DAOException, ParticipantManagerException
	{
		IParticipantManager participantManagerImplObj = ParticipantManagerUtility.getParticipantMgrImplObj();
		String query= participantManagerImplObj.getClinicalStudyNamesQuery();


		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean("participantId", participantId));
		return executeSQLQuery(query,null,null,columnValueBeanList);
	}
	
	public List getProcessedMatchedParticipants(Long userId, int recordsPerPage) throws DAOException, ParticipantManagerException
	{
		final IParticipantManager participantMgrImplObj = ParticipantManagerUtility.getParticipantMgrImplObj();
		String query = participantMgrImplObj.getProcessedMatchedParticipantQuery(userId);
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean("userId", userId));
		columnValueBeanList.add(new ColumnValueBean("noOfMacthedParticipant", "-1"));
		return executeSQLQuery(query, 0,recordsPerPage, columnValueBeanList);
	}



	public int getTotalCount(Long userId) throws DAOException
	{
		String query = "SELECT count(*) FROM MATCHED_PARTICIPANT_MAPPING PARTIMAPPING "
			+ " JOIN EMPI_PARTICIPANT_USER_MAPPING ON "
			+ " PARTIMAPPING.SEARCHED_PARTICIPANT_ID=EMPI_PARTICIPANT_USER_MAPPING.PARTICIPANT_ID "
			+ " WHERE EMPI_PARTICIPANT_USER_MAPPING.USER_ID=:userId AND PARTIMAPPING.NO_OF_MATCHED_PARTICIPANTS!=:noOfMacthedParticipant";

		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean("userId", userId));
		columnValueBeanList.add(new ColumnValueBean("noOfMacthedParticipant", "-1"));
		List list = executeSQLQuery(query, null,null, columnValueBeanList);
		return Integer.parseInt(list.get(0).toString());
	}
}
