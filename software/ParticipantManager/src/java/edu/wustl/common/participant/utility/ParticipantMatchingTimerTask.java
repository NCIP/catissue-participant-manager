/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue-participant-manager/LICENSE.txt for details.
 */

package edu.wustl.common.participant.utility;

import edu.wustl.common.participant.bizlogic.ParticipantMatchingBizLogic;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.query.generator.ColumnValueBean;
import edu.wustl.dao.query.generator.DBTypes;

/**
 * @author geeta_jaggal.
 * The Class ParticipantMatchingTimerTask :Used to perform the CIDER participants match
 * asynchronously.
 */
public class ParticipantMatchingTimerTask extends TimerTask
{

	/* (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	public void run()
	{
		try
		{
			perFormParticipantMatch();
		}
		catch (Exception e)
		{

			//logger.info(e.getMessage());
			System.out.println("Error during participant timer task");
		}
	}

	/**
	 * Per form participant match.
	 *
	 * @throws Exception the exception
	 */
	private void perFormParticipantMatch() throws Exception
	{
		final List participantIdList = fetchSearchParticipantIds();
		if (!participantIdList.isEmpty() && !"".equals(participantIdList.get(0)))
		{
			final ParticipantMatchingBizLogic bizLogic = new ParticipantMatchingBizLogic();
			bizLogic.perFormParticipantMatch(participantIdList);
		}
	}

	/**
	 * Fetch search participant ids.
	 *
	 * @return the list
	 *
	 * @throws DAOException the DAO exception
	 */
	private List fetchSearchParticipantIds() throws DAOException
	{

		JDBCDAO dao = null;
		List idList = null;
		try
		{
			dao = ParticipantManagerUtility.getJDBCDAO();
			final String query = "SELECT SEARCHED_PARTICIPANT_ID FROM MATCHED_PARTICIPANT_MAPPING WHERE NO_OF_MATCHED_PARTICIPANTS=?";
			final LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
			columnValueBeanList.add(new ColumnValueBean("NO_OF_MATCHED_PARTICIPANTS", "-1",
					DBTypes.INTEGER));
			idList = dao.executeQuery(query, null, columnValueBeanList);
		}
		finally
		{
			dao.closeSession();
		}
		return idList;
	}
}
