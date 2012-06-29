package edu.wustl.common.participant.bizlogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.wustl.common.lookup.DefaultLookupParameters;
import edu.wustl.common.lookup.DefaultLookupResult;
import edu.wustl.common.lookup.LookupParameters;
import edu.wustl.common.participant.client.IParticipantManagerLookupLogic;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.dao.DAO;
import edu.wustl.dao.query.generator.ColumnValueBean;


public class ParticipantHashcodeLookupLogic implements IParticipantManagerLookupLogic
{

	public List lookup(LookupParameters params, Set<Long> protocolIdList) throws Exception
	{
		List<DefaultLookupResult> matchParticipantList = null;
		DefaultLookupParameters defLookupParam = (DefaultLookupParameters)params;
		IParticipant participant = (IParticipant) defLookupParam.getObject();
		DAO dao = ParticipantManagerUtility.getDAO();
		String fetchByNameQry = ParticipantManagerUtility.getParticipantCodeQry(protocolIdList);
		List<ColumnValueBean> valueList = new ArrayList<ColumnValueBean>();
		ColumnValueBean participantCodeValue = new ColumnValueBean(participant.getParticipantCode());
		ColumnValueBean activityStatusValue = new ColumnValueBean("Disabled");
		valueList.add(participantCodeValue);
		valueList.add(activityStatusValue);
		List patientInfoList =  dao.executeQuery(fetchByNameQry, valueList);
		if(!patientInfoList.isEmpty())
		{
			IParticipant patientInfo = (IParticipant) patientInfoList.get(0);
			matchParticipantList = new ArrayList<DefaultLookupResult>();

			final DefaultLookupResult result = new DefaultLookupResult();
			final IParticipant partcipantNew =  patientInfo.clone();

			result.setObject(partcipantNew);
			result.setWeight(new Double(100));
			matchParticipantList.add(result);
		}
		dao.closeSession();
		return matchParticipantList;
	}

	public List lookup(LookupParameters params) throws Exception
	{
		return lookup(params, null);
	}

	public boolean isCallToLookupLogicNeeded(IParticipant participant)
	{
		return true;
	}
}
