package edu.wustl.common.participant.dao;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.utility.ParticipantManagerException;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.newdao.GenericHibernateDAO;
import edu.wustl.dao.query.generator.ColumnValueBean;
import edu.wustl.dao.query.generator.DBTypes;


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
	
//	public List executeParticipantCodeQry(Set<Long> protocolIdList,String participantCode) throws DAOException
//	{
//		List patientInfoList = null;
//		try
//		{
//			String hql = ParticipantManagerUtility.getParticipantCodeQry(protocolIdList);
//			List<ColumnValueBean> valueList = new ArrayList<ColumnValueBean>();
//			ColumnValueBean participantCodeValue = new ColumnValueBean("participantCode",participantCode);
//			ColumnValueBean activityStatusValue = new ColumnValueBean("activityStatus","Disabled");
//			valueList.add(participantCodeValue);
//			valueList.add(activityStatusValue);
//			patientInfoList =  executeQuery(hql, null,null,valueList);
//		}
//		catch(ParticipantManagerException exp)
//		{
//			LOGGER.error("ERROR WHILE executeParticipantCodeQry",exp);
//		}
//		return patientInfoList;
//	}
//	
//	public List isParticipantMatchWithinCSCPEnable(Long id) throws DAOException
//	{
//		String query = "SELECT SP.PARTCIPNT_MATCH_WITHIN_CSCP FROM  CATISSUE_SPECIMEN_PROTOCOL SP WHERE SP.IDENTIFIER=:id";;
//		List<ColumnValueBean> columnValueBeanList = new ArrayList<ColumnValueBean>();
//		columnValueBeanList.add(new ColumnValueBean("id", id));
//		return executeSQLQuery(query,columnValueBeanList);
//	}
//	
//	public List getPartcipantIdsList(Set cpIdList) throws DAOException
//	{
//		List idListArray = null;
//		String query = "SELECT PARTICIPANT_ID FROM CATISSUE_CLINICAL_STUDY_REG WHERE CLINICAL_STUDY_ID in (%s)";
//
//		if (cpIdList != null && !cpIdList.isEmpty())
//		{
//			StringBuffer cpIdStringBuffer = new StringBuffer(cpIdList.toString());
//			cpIdStringBuffer.deleteCharAt(0);
//			cpIdStringBuffer.deleteCharAt(cpIdStringBuffer.length()-1);
//			idListArray = executeSQLQuery(query, null);
//		}
//		return idListArray;
//	}
//
//	public List getColumnList() throws DAOException
//	{
//		List<String> displayList = new ArrayList<String>();
//
//		String sql = "SELECT  columnData.COLUMN_NAME,displayData.DISPLAY_NAME FROM CATISSUE_INTERFACE_"
//				+ "COLUMN_DATA columnData,CATISSUE_TABLE_RELATION relationData,CATISSUE_QUERY_TABLE"
//				+ "_DATA tableData,CATISSUE_SEARCH_DISPLAY_DATA displayData where relationData.CHIL"
//				+ "D_TABLE_ID = columnData.TABLE_ID and relationData.PARENT_TABLE_ID = tableData.TA"
//				+ "BLE_ID and relationData.RELATIONSHIP_ID = displayData.RELATIONSHIP_ID and column"
//				+ "Data.IDENTIFIER = displayData.COL_ID and tableData.ALIAS_NAME = 'Participant'";
//		LOGGER.debug("DATA ELEMENT SQL : " + sql);
//		return executeSQLQuery(sql, null);
//	}
//	
//	public List isEMPIEnable(String query,Long participantId) throws DAOException
//	{
//		List statusList = null;
//		List<ColumnValueBean> columnValueBeanList = new ArrayList<ColumnValueBean>();
//		columnValueBeanList.add(new ColumnValueBean("participantId", participantId));
//		return executeSQLQuery(query, columnValueBeanList);
//	}
//	
//	public String getPartiEMPIStatus(long participantId) throws DAOException
//	{
//
//		String eMPIStatus = "";
//		String query = "SELECT EMPI_ID_STATUS FROM CATISSUE_PARTICIPANT  WHERE IDENTIFIER=:id";
//		List<ColumnValueBean> columnValueBeanList = new ArrayList<ColumnValueBean>();
//		columnValueBeanList.add(new ColumnValueBean("id", participantId));
//		List list = executeSQLQuery(query, columnValueBeanList);
//		if (!list.isEmpty() && !"".equals(list.get(0)))
//		{
//			List statusList = (List) list.get(0);
//			if (!statusList.isEmpty())
//			{
//				eMPIStatus = (String) statusList.get(0);
//			}
//		}
//		return eMPIStatus;
//	}
//	
//	public boolean isParticipantIsProcessing(Long id) throws DAOException
//	{
//
//		boolean status = false;
//		String query =  "SELECT * FROM MATCHED_PARTICIPANT_MAPPING WHERE SEARCHED_PARTICIPANT_ID = :participantId "
//			+ "AND NO_OF_MATCHED_PARTICIPANTS != 0";
//		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//		columnValueBeanList.add(new ColumnValueBean("participantId", id));
//		List list = executeSQLQuery(query, columnValueBeanList);
//		if (!list.isEmpty() && !"".equals(list.get(0)))
//		{
//			status = true;
//		}
//		return status;
//	}
//	
//	public List<Long> getProcessedMatchedParticipantIds(Long userId) throws DAOException
//	{
//		List<Long> particpantIdColl = new ArrayList<Long>();
//		String query = "SELECT SEARCHED_PARTICIPANT_ID FROM  MATCHED_PARTICIPANT_MAPPING PARTIMAPPING  "
//				+ " JOIN EMPI_PARTICIPANT_USER_MAPPING ON PARTIMAPPING.SEARCHED_PARTICIPANT_ID=EMPI_PARTICIPANT_USER_MAPPING.PARTICIPANT_ID"
//				+ " WHERE EMPI_PARTICIPANT_USER_MAPPING.USER_ID=:userId AND PARTIMAPPING.NO_OF_MATCHED_PARTICIPANTS!=:noOfMatchedParticipant";
//
//		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//		columnValueBeanList.add(new ColumnValueBean("userId", userId));
//		columnValueBeanList.add(new ColumnValueBean("noOfMatchedParticipant", Integer.valueOf("-1")));
//
//		List resultSet = executeSQLQuery(query, columnValueBeanList);
//
//		for (Object object : resultSet)
//		{
//			ArrayList particpantIdList = (ArrayList) object;
//			if (particpantIdList != null && !particpantIdList.isEmpty())
//			{
//				particpantIdColl.add(Long.valueOf(particpantIdList.get(0).toString()));
//			}
//		}
//		return particpantIdColl;
//	}
//	
//	public void setEMPIIdStatus(Long participantId, String status) throws DAOException
//	{
//		LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
//		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//		columnValueBeanList.add(new ColumnValueBean(status));
//		columnValueBeanList.add(new ColumnValueBean(participantId));
//		columnValueBeans.add(columnValueBeanList);
//		String sql = "UPDATE CATISSUE_PARTICIPANT SET EMPI_ID_STATUS=? WHERE IDENTIFIER=?";
//		executeSQLUpdate(sql, columnValueBeans);
//	}
//	
//	public void updateOldEMPIDetails(Long participantId, String empiId) throws DAOException
//	{
//		final LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
//		final LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//		
//		String temporaryParticipantId = participantId + "T";
//		columnValueBeanList.add(new ColumnValueBean("PERMANENT_PARTICIPANT_ID", participantId,
//				DBTypes.VARCHAR));
//		columnValueBeanList.add(new ColumnValueBean("TEMPARARY_PARTICIPANT_ID",
//				temporaryParticipantId, DBTypes.VARCHAR));
//		columnValueBeanList.add(new ColumnValueBean("OLD_EMPI_ID", empiId, DBTypes.VARCHAR));
//		columnValueBeanList.add(new ColumnValueBean("TEMPMRNDATE", new Timestamp(System
//				.currentTimeMillis()), DBTypes.TIMESTAMP));
//		
//		final String sql = "INSERT INTO PARTICIPANT_EMPI_ID_MAPPING VALUES(?,?,?,?)";
//		columnValueBeans.add(columnValueBeanList);
//		executeSQLUpdate(sql, columnValueBeans);
//	}
//	
//	public void addParticipantToProcessMessageQueue(LinkedHashSet<Long> userIdSet,
//			Long participantId) throws DAOException
//	{
//		String query = "INSERT INTO MATCHED_PARTICIPANT_MAPPING(NO_OF_MATCHED_PARTICIPANTS,"
//			+ "CREATION_DATE,SEARCHED_PARTICIPANT_ID) VALUES(?,?,?)";
//
//		// delete old data from DB to start up clean with matching process
//		// Bug fix 18823
//		deleteProcessedParticipant(participantId);
//
//		Calendar cal = Calendar.getInstance();
//		Date date = cal.getTime();
//		LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
//		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//
//		
//		columnValueBeanList.add(new ColumnValueBean("NO_OF_MATCHED_PARTICIPANTS", Integer
//				.valueOf(-1), DBTypes.LONG));
//		columnValueBeanList.add(new ColumnValueBean("CREATION_DATE", date, DBTypes.DATE));
//		columnValueBeanList.add(new ColumnValueBean("SEARCHED_PARTICIPANT_ID", participantId,
//				DBTypes.LONG));
//		columnValueBeans.add(columnValueBeanList);
//		executeSQLUpdate(query, columnValueBeans);
//
//		updateParticipantUserMapping(userIdSet, participantId);
//	}
//	
//	/**
//	 * Update participant user mapping.
//	 *
//	 * @param jdbcdao the jdbcdao
//	 * @param userIdSet the user id set
//	 * @param participantId the participant id
//	 *
//	 * @throws DAOException the DAO exception
//	 */
//	private void updateParticipantUserMapping(LinkedHashSet<Long> userIdSet, Long participantId) throws DAOException
//	{
//		Iterator iterator = userIdSet.iterator();
//		while (iterator.hasNext())
//		{
//			String query = "INSERT INTO EMPI_PARTICIPANT_USER_MAPPING VALUES(?,?)";
//			Long userId = (Long) iterator.next();
//			LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
//			LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//
//			columnValueBeanList.add(new ColumnValueBean("PARTICIPANT_ID", participantId,
//					DBTypes.LONG));
//			columnValueBeanList.add(new ColumnValueBean("USER_ID", userId, DBTypes.LONG));
//			columnValueBeans.add(columnValueBeanList);
//			executeSQLUpdate(query, columnValueBeans);
//		}
//	}
//	
//	public void deleteProcessedParticipant(Long id) throws DAOException
//	{
//		LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
//		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//		columnValueBeanList.add(new ColumnValueBean(id));
//		columnValueBeans.add(columnValueBeanList);
//		String query = "DELETE FROM MATCHED_PARTICIPANT_MAPPING WHERE SEARCHED_PARTICIPANT_ID=?";
//		executeSQLUpdate(query, columnValueBeans);
//	}
//
//	public void updateProcessedParticipant(Long id) throws DAOException
//	{
//		LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
//		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//		columnValueBeanList.add(new ColumnValueBean(id));
//		columnValueBeans.add(columnValueBeanList);
//		String query = "UPDATE MATCHED_PARTICIPANT_MAPPING SET NO_OF_MATCHED_PARTICIPANTS=0 "
//				+ "WHERE SEARCHED_PARTICIPANT_ID=?";
//		executeSQLUpdate(query, columnValueBeans);
//	}
//	
//	public List getSiteObject(final String facilityId) throws DAOException
//	{
//		String hql = "select id,name from "+ISite.class.getName()+ "where facilityId=:facilityId"; 
//		List<ColumnValueBean> columnValueBeans = new ArrayList<ColumnValueBean>();
//		columnValueBeans.add(new ColumnValueBean("facilityId",facilityId));
//		return executeQuery(hql, null, null, columnValueBeans);
//	}
}
