
package edu.wustl.common.participant.utility;

import java.util.List;
import java.util.TimerTask;

import edu.wustl.common.participant.bizlogic.ParticipantMatchingBizLogic;
import edu.wustl.common.util.global.CommonServiceLocator;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.daofactory.DAOConfigFactory;
import edu.wustl.dao.daofactory.IDAOFactory;
import edu.wustl.dao.exception.DAOException;

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Per form participant match.
	 *
	 * @throws Exception the exception
	 */
	private void perFormParticipantMatch() throws Exception
	{
		List participantIdList = fetchSearchParticipantIds();
		if (!participantIdList.isEmpty() && participantIdList.get(0) != "")
		{
			ParticipantMatchingBizLogic bizLogic = new ParticipantMatchingBizLogic();
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
			dao.openSession(null);
			String query = "SELECT SEARCHED_PARTICIPANT_ID FROM MATCHED_PARTICIPANT_MAPPING WHERE NO_OF_MATCHED_PARTICIPANTS='-1'";
			idList = dao.executeQuery(query);
		}
		finally
		{
			dao.closeSession();
		}
		return idList;
	}
}
