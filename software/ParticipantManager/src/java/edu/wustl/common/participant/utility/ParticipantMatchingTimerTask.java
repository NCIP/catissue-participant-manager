
package edu.wustl.common.participant.utility;

import java.util.TimerTask;

import javax.naming.InitialContext;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import edu.wustl.common.participant.bizlogic.ParticipantMatchingBizLogic;
import edu.wustl.common.util.logger.Logger;

/**
 * @author geeta_jaggal.
 * The Class ParticipantMatchingTimerTask :Used to perform the CIDER participants match
 * asynchronously.
 */
public class ParticipantMatchingTimerTask extends TimerTask
{

	private static final Logger logger = Logger.getCommonLogger(ParticipantMatchingTimerTask.class);

	/* (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	public void run()
	{
		UserTransaction transaction = null;
		try
		{
			transaction = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
			if (transaction.getStatus() == Status.STATUS_NO_TRANSACTION)
			{
				logger.info("=========== Starting a new Transaction ================");
				transaction.begin();
			}
			new ParticipantMatchingBizLogic().perFormParticipantMatch();
		}
		catch (Exception e)
		{
			logger.error("Error during participant timer task", e);
			System.out.println("Error during participant timer task");
			try
			{
				if(transaction!=null)
					transaction.rollback();
			}
			catch (final Exception rollbackFailed)
			{
				logger.error("Transaction failed !", rollbackFailed);
			}
		}
	}

	

//	/**
//	 * Fetch search participant ids.
//	 *
//	 * @return the list
//	 *
//	 * @throws DAOException the DAO exception
//	 */
//	private List fetchSearchParticipantIds() throws DAOException
//	{
//
//		JDBCDAO dao = null;
//		List idList = null;
//		try
//		{
//			dao = ParticipantManagerUtility.getJDBCDAO();
//			final String query = "SELECT SEARCHED_PARTICIPANT_ID FROM MATCHED_PARTICIPANT_MAPPING WHERE NO_OF_MATCHED_PARTICIPANTS=?";
//			final LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
//			columnValueBeanList.add(new ColumnValueBean("NO_OF_MATCHED_PARTICIPANTS", "-1",
//					DBTypes.INTEGER));
//			idList = dao.executeQuery(query, null, columnValueBeanList);
//		}
//		finally
//		{
//			dao.closeSession();
//		}
//		return idList;
//	}
}
