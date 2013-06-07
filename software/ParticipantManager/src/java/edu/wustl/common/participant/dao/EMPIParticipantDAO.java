/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue-participant-manager/LICENSE.txt for details.
 */

package edu.wustl.common.participant.dao;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.newdao.GenericHibernateDAO;
import edu.wustl.dao.query.generator.ColumnValueBean;
import edu.wustl.dao.query.generator.DBTypes;


public class EMPIParticipantDAO extends GenericHibernateDAO<IParticipant, Long>
{

	private static final Logger LOGGER = Logger.getCommonLogger(EMPIParticipantDAO.class);
	private static final String GET_PERMANANTID = "getPermanentId";
	private static final String GET_OLD_EMPIID = "getOldEmpiId";

	private static final String QUERY_FOR_STATUS_UPDATE = "UPDATE CATISSUE_PARTICIPANT SET EMPI_ID = '', "
		+ "EMPI_ID_STATUS='PENDING' WHERE IDENTIFIER = ?";

	private static final String MAX_ID_SQL ="SELECT MAX(IDENTIFIER) from PARTICIPANT_MERGE_MESSAGES";
	private static final String INS_QUERY = "INSERT INTO PARTICIPANT_MERGE_MESSAGES VALUES(?,?,?,?,?)";

	public EMPIParticipantDAO(String applicationName, SessionDataBean sessionDataBean)
	{
		super(applicationName, sessionDataBean);
	}
	/**
	 * PUT in PMhql
	 * @param participantId
	 * @return
	 * @throws DAOException
	 */
	public List<Object[]> getTempParticipantIdFromParticipantEMPIMapping(Long participantId) throws DAOException
	{
		return executeNamedQuery("getTempParticipantIdFromParticipantEMPIMapping", null,null,new ColumnValueBean("participantId", participantId));
	}

	public void executeQueryForStatusUpdate(Long participantId) throws DAOException
	{

		final LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
		final LinkedList<ColumnValueBean> colValBeanList = new LinkedList<ColumnValueBean>();
		colValBeanList.add(new ColumnValueBean("IDENTIFIER",participantId));
		columnValueBeans.add(colValBeanList);
		executeSQLUpdate(QUERY_FOR_STATUS_UPDATE,columnValueBeans);
	}

	/**
	 * PUT in PMhql
	 * @param clinPortalId
	 * @return
	 * @throws DAOException
	 */
	public String getPermanentId(final String clinPortalId) throws DAOException
	{
		String permanentId = null;


			List result = executeNamedQuery(GET_PERMANANTID, null, null, new ColumnValueBean(
					"clinPortalId", clinPortalId));
			if (!result.isEmpty())
			{
				permanentId = result.get(0).toString();
			}

		return permanentId;
	}

	/**
	 * PUT in PMhql
	 * @param clinPortalId
	 * @return
	 * @throws DAOException
	 */
	public String getOldEmpiId(final String clinPortalId) throws DAOException
	{
		String oldEmpiID = null;
		List<Object[]> idList = executeNamedQuery(GET_OLD_EMPIID, null,null,new ColumnValueBean("clinPortalId",clinPortalId));
		if (!idList.isEmpty())
		{
			Object[] obj = idList.get(0);
			oldEmpiID = obj[2].toString();
		}
		return oldEmpiID;
	}

	public void storeMergeMessage( final String hl7Message,final String messageType ,final String status) throws DAOException
	{
		long idenifier = 0L;
		Long identifier = 0L;
		long maxId = 0;

		try
		{
			final List maxIdList = executeSQLQuery(MAX_ID_SQL,null,null,null);
			if (!maxIdList.isEmpty())
			{
				maxId = ((Long) maxIdList.get(0)).longValue();
			}
			final Calendar cal = Calendar.getInstance();
			final java.util.Date date = cal.getTime();
			idenifier = Long.valueOf(maxId+1);

			final LinkedList<LinkedList<ColumnValueBean>> columnValueBeans = new LinkedList<LinkedList<ColumnValueBean>>();
			final LinkedList<ColumnValueBean> colValBeanList = new LinkedList<ColumnValueBean>();
			colValBeanList.add(new ColumnValueBean("IDENTIFIER", Long.valueOf(idenifier),DBTypes.INTEGER));
			colValBeanList.add(new ColumnValueBean("MESSAGE_TYPE", messageType, DBTypes.VARCHAR));
			colValBeanList.add(new ColumnValueBean("MESSAGE_DATE", date, DBTypes.DATE));
			colValBeanList.add(new ColumnValueBean("HL7_MESSAGE", hl7Message,DBTypes.VARCHAR));
			colValBeanList.add(new ColumnValueBean("MESSAGE_STATUS", status, DBTypes.VARCHAR));
			columnValueBeans.add(colValBeanList);
			executeSQLUpdate(INS_QUERY, columnValueBeans);
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

