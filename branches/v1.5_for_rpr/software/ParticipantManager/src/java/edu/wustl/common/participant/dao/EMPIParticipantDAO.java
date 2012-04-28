package edu.wustl.common.participant.dao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.participant.client.IParticipantManager;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.utility.ParticipantManagerException;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.newdao.GenericHibernateDAO;
import edu.wustl.dao.query.generator.ColumnValueBean;
import edu.wustl.dao.query.generator.DBTypes;


public class EMPIParticipantDAO extends GenericHibernateDAO<IParticipant, Long>
{

	private static final Logger LOGGER = Logger.getCommonLogger(EMPIParticipantDAO.class);
	
	public EMPIParticipantDAO(String applicationName, SessionDataBean sessionDataBean)
	{
		super(applicationName, sessionDataBean);
	}
	
	public List<Object[]> getTempParticipantIdFromParticipantEMPIMapping(Long participantId) throws DAOException
	{
		String query = "SELECT * FROM PARTICIPANT_EMPI_ID_MAPPING WHERE PERMANENT_PARTICIPANT_ID=:participantId";
		LinkedList<ColumnValueBean> colValueBeanList = new LinkedList<ColumnValueBean>();
		colValueBeanList.add(new ColumnValueBean("participantId", participantId));
		return executeSQLQuery(query, null,null,colValueBeanList);
	}
	
	public List<Object[]> executeQueryForPICordinators(Long participantId) throws DAOException,ParticipantManagerException
	{
		final String hql = getQueryForPICordinators();
		List<ColumnValueBean> columnValueBeans = new ArrayList<ColumnValueBean>();
		columnValueBeans.add(new ColumnValueBean("participantId",participantId));
		return executeQuery(hql, null,null,columnValueBeans);
	}	
	
	private String getQueryForPICordinators() throws ParticipantManagerException
	{
		IParticipantManager participantManagerImplObj = ParticipantManagerUtility.getParticipantMgrImplObj();
		return participantManagerImplObj.getPICordinatorsofProtocol();

	}
	
	public void executeQueryForStatusUpdate(Long participantId) throws DAOException
	{
		String queryForStatusUpdate = "UPDATE CATISSUE_PARTICIPANT SET EMPI_ID = '', "
			+ "EMPI_ID_STATUS='PENDING' WHERE IDENTIFIER = ?";
		final LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
		final LinkedList<ColumnValueBean> colValBeanList = new LinkedList<ColumnValueBean>();
		colValBeanList.add(new ColumnValueBean("IDENTIFIER",participantId));
		columnValueBeans.add(colValBeanList);
		executeSQLUpdate(queryForStatusUpdate,columnValueBeans);
	}
	
	public String getPermanentId(final String clinPortalId) throws DAOException
	{
		String permanentId = null;
		final String query = "SELECT PERMANENT_PARTICIPANT_ID FROM PARTICIPANT_EMPI_ID_MAPPING WHERE TEMPARARY_PARTICIPANT_ID=:clinPortalId";
		final LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean(clinPortalId));
		List result = executeSQLQuery(query, null, null, columnValueBeanList);
		if(!result.isEmpty())
		{
			permanentId = result.get(0).toString();
		}
		return permanentId;
	}
	
	public String getOldEmpiId(final String clinPortalId) throws DAOException
	{
		String oldEmpiID = null;
		final LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean("clinPortalId",clinPortalId));
		final String query = "SELECT * FROM PARTICIPANT_EMPI_ID_MAPPING WHERE PERMANENT_PARTICIPANT_ID=:clinPortalId ORDER BY TEMPMRNDATE DESC";

		List<Object[]> idList = executeSQLQuery(query, null,null,columnValueBeanList);
		if (!idList.isEmpty()) 
		{
			Object[] obj = (Object[])idList.get(0);
			oldEmpiID = obj[2].toString();
		}
		return oldEmpiID;
	}
	
	public void storeMergeMessage( final String hl7Message,final String messageType ,final String status) throws DAOException
	{
		long idenifier = 0L;
		String insQuery = null;
		Long identifier = 0L;
		long maxId = 0;
		String query ="SELECT MAX(IDENTIFIER) from PARTICIPANT_MERGE_MESSAGES";
		try
		{
			final List maxIdList = executeSQLQuery(query,null,null,null);
			if (!maxIdList.isEmpty())
			{
				maxId = ((Long) maxIdList.get(0)).longValue();
			}
			final Calendar cal = Calendar.getInstance();
			final java.util.Date date = cal.getTime();
			idenifier = Long.valueOf(maxId+1);
			insQuery = "INSERT INTO PARTICIPANT_MERGE_MESSAGES VALUES(?,?,?,?,?)";
			final LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
			final LinkedList<ColumnValueBean> colValBeanList = new LinkedList<ColumnValueBean>();
			colValBeanList.add(new ColumnValueBean("IDENTIFIER", Long.valueOf(idenifier),DBTypes.INTEGER));
			colValBeanList.add(new ColumnValueBean("MESSAGE_TYPE", messageType, DBTypes.VARCHAR));
			colValBeanList.add(new ColumnValueBean("MESSAGE_DATE", date, DBTypes.DATE));
			colValBeanList.add(new ColumnValueBean("HL7_MESSAGE", hl7Message,DBTypes.VARCHAR));
			colValBeanList.add(new ColumnValueBean("MESSAGE_STATUS", status, DBTypes.VARCHAR));
			columnValueBeans.add(colValBeanList);
			executeSQLUpdate(insQuery, columnValueBeans);
			LOGGER.info("\n \n  ----------- STORED MERGE MESSAGE ----------  \n\n");
			LOGGER.info(hl7Message);
		}
		catch (DAOException e)
		{
			LOGGER
					.info("\n \n --------  ERROR WHILE STORING THE FOLLOWING MERGE MESSAGE ----------\n\n\n");
			LOGGER.info(hl7Message);
			LOGGER.info(e.getMessage());
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}
	}
}
	
