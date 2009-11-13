
package edu.wustl.common.participant.utility;

import java.util.List;
import java.util.TimerTask;

import edu.wustl.common.participant.bizlogic.ParticipantMatchingBizLogic;
import edu.wustl.common.util.global.CommonServiceLocator;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.daofactory.DAOConfigFactory;
import edu.wustl.dao.daofactory.IDAOFactory;
import edu.wustl.dao.exception.DAOException;

public class ParticipantMatchingTimerTask extends TimerTask
{

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

	private void perFormParticipantMatch() throws Exception
	{
		List participantIdList = fetchSearchParticipantIds();
		if (!participantIdList.isEmpty() && participantIdList.get(0) != "")
		{
			ParticipantMatchingBizLogic bizLogic = new ParticipantMatchingBizLogic();
			bizLogic.perFormParticipantMatch(participantIdList);
		}
	}

	private List fetchSearchParticipantIds() throws DAOException
	{
		String appName = CommonServiceLocator.getInstance().getAppName();
		IDAOFactory daoFactory = DAOConfigFactory.getInstance().getDAOFactory(appName);
		JDBCDAO dao = daoFactory.getJDBCDAO();
		dao.openSession(null);
		String query = "SELECT SEARCHED_PARTICIPANT_ID FROM MATCHED_PARTICIPANT_MAPPING WHERE NO_OF_MATCHED_PARTICIPANTS='-1'";
		List idList = dao.executeQuery(query);
		dao.closeSession();
		return idList;
	}
}
