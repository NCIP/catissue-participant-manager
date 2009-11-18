
package edu.wustl.common.participant.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import edu.wustl.common.action.CommonSearchAction;
import edu.wustl.common.actionForm.AbstractActionForm;
import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.bizlogic.DefaultBizLogic;
import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.lookup.DefaultLookupResult;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.IRace;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.Utility;
import edu.wustl.common.util.global.CommonServiceLocator;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.daofactory.DAOConfigFactory;
import edu.wustl.dao.daofactory.IDAOFactory;
import edu.wustl.dao.exception.DAOException;

/**
 *
 * @author geeta_jaggal
 * The Class MatchedParticipantsSearchAction.
 * This class is used for fetching stored matched
 * participant from DB for a particiapnt in the  message board.
 */
public class MatchedParticipantsSearchAction extends CommonSearchAction
{

	/**
	 * Instantiates a new matched participants search action.
	 */
	public MatchedParticipantsSearchAction()
	{
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * edu.wustl.common.action.CommonSearchAction#execute(org.apache.struts.
	 * action.ActionMapping, org.apache.struts.action.ActionForm,
	 * javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws ApplicationException
	{
		ActionForward forward = null;
		AbstractActionForm participantForm = (AbstractActionForm) form;
		try
		{
			forward = super.execute(mapping, participantForm, request, response);
			if (!forward.getName().equals(edu.wustl.common.util.global.Constants.FAILURE))
			{
				String obj = request.getParameter("id");
				Long identifier = Long.valueOf(Utility.toLong(obj));
				if (identifier.longValue() == 0L)
				{
					identifier = (Long) request.getAttribute("id");
				}
				// fetch the stored matched participant for the participant in the message board
				fetchMatchedParticipantsFromDB(identifier.longValue(), request);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new ApplicationException(null, e, e.getMessage());
		}
		return forward;
	}

	/**
	 * Gets the session data.
	 *
	 * @param request
	 *            the request
	 *
	 * @return the session data
	 */
	protected SessionDataBean getSessionData(HttpServletRequest request)
	{
		return (SessionDataBean) request.getSession().getAttribute(edu.wustl.common.util.global.Constants.SESSION_DATA);
	}

	/**
	 * Fetch matched participants from db.
	 *
	 * @param participantId
	 *            the participant id
	 * @param request
	 *            the request
	 *
	 * @throws Exception
	 *             the exception
	 */
	private void fetchMatchedParticipantsFromDB(long participantId, HttpServletRequest request)
			throws Exception
	{
		JDBCDAO dao = null;
		List<DefaultLookupResult> matchPartpantLst = null;
		try
		{
			dao = ParticipantManagerUtility.getJDBCDAO();

			String query = getSelectQuery(participantId);
			List matchPartpantLstTemp = dao.executeQuery(query);
			if (matchPartpantLstTemp != null && !matchPartpantLstTemp.isEmpty())
			{
				matchPartpantLst = populateParticipantList(matchPartpantLstTemp);
				storeLists(request, matchPartpantLst);
			}
			else
			{
				setStatusMessage(request);
				ParticipantManagerUtility.deleteProcessedParticipant(Long.valueOf(participantId));
			}

		}
		catch (DAOException e)
		{
			e.printStackTrace();
			throw new DAOException(e.getErrorKey(), e, e.getMsgValues());
		}finally{
			dao.closeSession();
		}
	}

	/**
	 * Sets the status message.
	 *
	 * @param request
	 *            the new status message
	 *
	 * @throws DAOException
	 *             the DAO exception
	 */
	private void setStatusMessage(HttpServletRequest request) throws DAOException
	{
		ActionMessages actionMsgs = (ActionMessages) request
				.getAttribute("org.apache.struts.action.ACTION_MESSAGE");
		if (actionMsgs == null)
		{
			actionMsgs = new ActionMessages();
		}
		actionMsgs.add("org.apache.struts.action.GLOBAL_MESSAGE", new ActionMessage(
				"participant.empiid.match.message"));
		saveMessages(request, actionMsgs);
	}

	/**
	 * Populate participant list.
	 *
	 * @param matchPartpantLstTmp
	 *            the match partpant lst tmp
	 *
	 * @return the list
	 *
	 * @throws Exception
	 *             the exception
	 */
	private List<DefaultLookupResult> populateParticipantList(List matchPartpantLstTmp)
			throws Exception
	{
		List<DefaultLookupResult> matchPartpantLst = new ArrayList<DefaultLookupResult>();
		for (int i = 0; i < matchPartpantLstTmp.size(); i++)
		{
			List participantValueList = (List) matchPartpantLstTmp.get(i);
			if (!participantValueList.isEmpty() && participantValueList.get(0) != null
					&& participantValueList.get(0) != "")
			{
				IParticipant participant = getParticipantObj(participantValueList);
				DefaultLookupResult result = new DefaultLookupResult();
				result.setObject(participant);
				matchPartpantLst.add(result);
			}
		}
		return matchPartpantLst;
	}

	/**
	 * Gets the participant obj.
	 *
	 * @param participantValueList
	 *            the participant value list
	 *
	 * @return the participant obj
	 *
	 * @throws Exception
	 *             the exception
	 */
	private IParticipant getParticipantObj(List participantValueList) throws Exception
	{
		IParticipant participant = (IParticipant) ParticipantManagerUtility
				.getParticipantInstance();
		String dateStr = null;
		java.util.Date date = null;
		participant.setId(Long.valueOf((String) participantValueList.get(0)));
		participant.setEmpiId((String) participantValueList.get(1));
		participant.setLastName((String) participantValueList.get(2));
		participant.setFirstName((String) participantValueList.get(3));
		participant.setMiddleName((String) participantValueList.get(4));
		if (participantValueList.get(5) != null && participantValueList.get(5) != "")
		{
			dateStr = (String) participantValueList.get(5);
			date = Utility.parseDate(dateStr, Constants.DATE_FORMAT);
			participant.setBirthDate(date);
		}
		participant.setGender((String) participantValueList.get(6));
		participant.setSocialSecurityNumber((String) participantValueList.get(7));
		participant.setActivityStatus((String) participantValueList.get(8));
		if (participantValueList.get(9) != null && participantValueList.get(9) != "")
		{
			dateStr = (String) participantValueList.get(9);
			date = Utility.parseDate(dateStr,Constants.DATE_FORMAT);
			participant.setDeathDate(date);
		}
		participant.setVitalStatus((String) participantValueList.get(10));
		String mrnString = (String) participantValueList.get(11);
		if (mrnString != null && mrnString != "")
		{
			Collection partiMediIdColn = getPartiMediIdColnCollection(mrnString);
			participant.setParticipantMedicalIdentifierCollection(partiMediIdColn);
		}
		String raceString = (String) participantValueList.get(12);
		if (raceString != null && raceString != "")
		{
			Collection raceCollection = getRaceCollection(raceString);
			participant.setRaceCollection(raceCollection);
		}
		participant.setIsFromEMPI((String) participantValueList.get(13));
		return participant;
	}

	/**
	 * Gets the race collection.
	 *
	 * @param raceString
	 *            the race string
	 *
	 * @return the race collection
	 *
	 * @throws BizLogicException
	 *             the biz logic exception
	 */
	private Collection getRaceCollection(String raceString) throws BizLogicException
	{
		Collection<IRace> raceCollection = new LinkedHashSet<IRace>();
		String racevalues[] = raceString.split(",");
		for (int i = 0; i < racevalues.length; i++)
		{
			IRace race = (IRace) ParticipantManagerUtility.getRaceInstance();
			String raceName = racevalues[i];
			race.setRaceName(raceName);
			raceCollection.add(race);
		}
		return raceCollection;
	}

	/**
	 * Gets the parti medi id coln collection.
	 *
	 * @param mrnString
	 *            the mrn string
	 *
	 * @return the parti medi id coln collection
	 *
	 * @throws Exception
	 *             the exception
	 */
	private Collection getPartiMediIdColnCollection(String mrnString) throws Exception
	{
		Collection partiMediIdColn = new LinkedHashSet();
		String values[] = mrnString.split(",");
		for (int i = 0; i < values.length; i++)
		{
			String value = values[i];
			IParticipantMedicalIdentifier participantMedicalIdentifier = getParticipantMedicalIdentifierObj(value);
			partiMediIdColn.add(participantMedicalIdentifier);
		}

		return partiMediIdColn;
	}

	/**
	 * Gets the participant medical identifier obj.
	 *
	 * @param value
	 *            the value
	 *
	 * @return the participant medical identifier obj
	 *
	 * @throws Exception
	 *             the exception
	 */
	private IParticipantMedicalIdentifier getParticipantMedicalIdentifierObj(String value)
			throws Exception
	{
		IParticipantMedicalIdentifier participantMedicalIdentifier = (IParticipantMedicalIdentifier) ParticipantManagerUtility
				.getPMIInstance();
		String values[] = value.split(":");
		String mrn = values[0];
		String facilityId = values[1];
		ISite siteObj = ParticipantManagerUtility.getSiteObject(facilityId);
		participantMedicalIdentifier.setMedicalRecordNumber(mrn);
		participantMedicalIdentifier.setSite(siteObj);
		return participantMedicalIdentifier;
	}



	/**
	 * Gets the select query.
	 *
	 * @param identifier
	 *            the identifier
	 *
	 * @return the select query
	 */
	private String getSelectQuery(long identifier)
	{
		String query = (new StringBuilder()).append(
				"SELECT * FROM CATISSUE_MATCHED_PARTICIPANT WHERE SEARCHED_PARTICIPANT_ID='")
				.append(identifier).append("'").toString();
		return query;
	}

	/**
	 * Store lists.
	 *
	 * @param request
	 *            the request
	 * @param matchPartpantLst
	 *            the match partpant lst
	 *
	 * @throws DAOException
	 *             the DAO exception
	 */
	private void storeLists(HttpServletRequest request, List<DefaultLookupResult> matchPartpantLst)
			throws DAOException
	{
		ActionMessages messages = new ActionMessages();
		List<String> columnList = ParticipantManagerUtility.getColumnHeadingList();
		request.setAttribute(edu.wustl.common.util.global.Constants.SPREADSHEET_COLUMN_LIST, columnList);
		List<List<String>> pcpantDisplayLst = ParticipantManagerUtility
				.getParticipantDisplayList(matchPartpantLst);
		request.setAttribute(Constants.SPREADSHEET_DATA_LIST, pcpantDisplayLst);
		HttpSession session = request.getSession();
		session.setAttribute("MatchedParticpant", matchPartpantLst);
		if (request.getAttribute("continueLookup") == null)
		{
			saveMessages(request, messages);
		}
	}
}
