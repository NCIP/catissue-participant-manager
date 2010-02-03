
package edu.wustl.common.participant.action;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
import edu.wustl.common.util.logger.Logger;

import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.exception.DAOException;

/**
 * @author geeta_jaggal
 *
 * The Class MatchedParticipantsSearchAction.
 * This class is used for fetching stored matched
 * participant from DB for a particiapnt in the  message board.
 */
public class MatchedParticipantsSearchAction extends CommonSearchAction
{

	/** The Constant logger. */
	private static final  Logger LOGGER = Logger.getCommonLogger(MatchedParticipantsSearchAction.class);
	/**
	 * For displaying the matched participants from eMPI
	 */
	public ActionForward executeXSS(final ActionMapping mapping,final ActionForm form,
			final HttpServletRequest request, final HttpServletResponse response) throws ApplicationException
	{
		ActionForward forward = null;
		final AbstractActionForm participantForm = (AbstractActionForm) form;
		try
		{
			forward = super.executeXSS(mapping, participantForm, request, response);
			if (!forward.getName().equals(edu.wustl.common.util.global.Constants.FAILURE))
			{
				final String obj = request
						.getParameter(edu.wustl.common.util.global.Constants.SYSTEM_IDENTIFIER);
				Long identifier = Long.valueOf(Utility.toLong(obj));
				if (identifier.longValue() == 0L)
				{
					identifier = Long
							.valueOf(edu.wustl.common.util.global.Constants.SYSTEM_IDENTIFIER);
				}
				// fetch the stored matched participant for the participant in the message board
				fetchMatchedParticipantsFromDB(identifier.longValue(), request);
			}
		}
		catch (Exception e)
		{
			LOGGER.info(e.getMessage());
			throw new ApplicationException(null, e, e.getMessage());
		}
		return forward;
	}

	/**
	 * Gets the session data.
	 *
	 * @param request the request
	 *
	 * @return the session data
	 */
	protected SessionDataBean getSessionData(final HttpServletRequest request)
	{
		return (SessionDataBean) request.getSession().getAttribute(
				edu.wustl.common.util.global.Constants.SESSION_DATA);
	}

	/**
	 * Fetch matched participants from db.
	 *
	 * @param participantId the participant id
	 * @param request the request
	 *
	 * @throws ParseException the parse exception
	 * @throws BizLogicException the biz logic exception
	 * @throws Exception the exception
	 * @throws DAOException the DAO exception
	 */
	private void fetchMatchedParticipantsFromDB(final long participantId, final HttpServletRequest request)
			throws DAOException, BizLogicException, ParseException
	{
		JDBCDAO dao = null;
		List<DefaultLookupResult> matchPartpantLst = null;
		try
		{
			dao = ParticipantManagerUtility.getJDBCDAO();

			final String query = getSelectQuery(participantId);
			final List matchPartpantLstTemp = dao.executeQuery(query);
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
			LOGGER.info(e.getMessage());
			throw new DAOException(e.getErrorKey(), e, e.getMsgValues());
		}
		finally
		{
			dao.closeSession();
		}
	}

	/**
	 * Sets the status message.
	 *
	 * @param request the new status message
	 *
	 * @throws DAOException the DAO exception
	 */
	private void setStatusMessage(final HttpServletRequest request) throws DAOException
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
	 * @param matchPartpantLstTmp the match partpant lst tmp
	 *
	 * @return the list
	 *
	 * @throws ParseException the parse exception
	 * @throws BizLogicException the biz logic exception
	 * @throws Exception the exception
	 */
	private List<DefaultLookupResult> populateParticipantList(final List matchPartpantLstTmp)
			throws BizLogicException, ParseException

	{
		final List<DefaultLookupResult> matchPartpantLst = new ArrayList<DefaultLookupResult>();
		for (int i = 0; i < matchPartpantLstTmp.size(); i++)
		{
			final List participantValueList = (List) matchPartpantLstTmp.get(i);
			if (!participantValueList.isEmpty() && !participantValueList.get(0).equals(null)
					&& !participantValueList.get(0).equals(""))
			{
				final IParticipant participant = getParticipantObj(participantValueList);
				final DefaultLookupResult result = new DefaultLookupResult();
				result.setObject(participant);
				matchPartpantLst.add(result);
			}
		}
		return matchPartpantLst;
	}

	/**
	 * Gets the participant obj.
	 *
	 * @param participantValueList the participant value list
	 *
	 * @return the participant obj
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws ParseException the parse exception
	 * @throws Exception the exception
	 */
	private IParticipant getParticipantObj(final List participantValueList) throws BizLogicException,
			ParseException
	{
		final IParticipant participant = (IParticipant) ParticipantManagerUtility
				.getParticipantInstance();

		participant.setId(Long.valueOf((String) participantValueList.get(0)));
		participant.setEmpiId((String) participantValueList.get(1));
		participant.setLastName((String) participantValueList.get(2));
		participant.setFirstName((String) participantValueList.get(3));
		participant.setMiddleName((String) participantValueList.get(4));
		if (participantValueList.get(5) != null && participantValueList.get(5) != "")
		{
			final String dateStr = (String) participantValueList.get(5);
			final Date date = Utility.parseDate(dateStr, Constants.DATE_FORMAT);
			participant.setBirthDate(date);
		}
		participant.setGender((String) participantValueList.get(6));
		participant.setSocialSecurityNumber((String) participantValueList.get(7));
		participant.setActivityStatus((String) participantValueList.get(8));
		if (participantValueList.get(9) != null && participantValueList.get(9) != "")
		{
			final String dateStr = (String) participantValueList.get(9);
			final Date date = Utility.parseDate(dateStr, Constants.DATE_FORMAT);
			participant.setDeathDate(date);
		}
		participant.setVitalStatus((String) participantValueList.get(10));
		final String mrnString = (String) participantValueList.get(11);
		if (mrnString != null && mrnString != "")
		{
			final Collection partiMediIdColn = getPartiMediIdColnCollection(mrnString);
			participant.setParticipantMedicalIdentifierCollection(partiMediIdColn);
		}
		final String raceString = (String) participantValueList.get(12);
		if (raceString != null && raceString != "")
		{
			final Collection raceCollection = getRaceCollection(raceString);
			participant.setRaceCollection(raceCollection);
		}
		return participant;
	}

	/**
	 * Gets the race collection.
	 *
	 * @param raceString the race string
	 *
	 * @return the race collection
	 *
	 * @throws BizLogicException the biz logic exception
	 */
	private Collection getRaceCollection(final String raceString) throws BizLogicException
	{
		final Collection<IRace> raceCollection = new LinkedHashSet<IRace>();
		final String racevalues[] = raceString.split(",");
		for (int i = 0; i < racevalues.length; i++)
		{
			final IRace race = (IRace) ParticipantManagerUtility.getRaceInstance();
			final String raceName = racevalues[i];
			race.setRaceName(raceName);
			raceCollection.add(race);
		}
		return raceCollection;
	}

	/**
	 * Gets the parti medi id coln collection.
	 *
	 * @param mrnString the mrn string
	 *
	 * @return the parti medi id coln collection
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws Exception the exception
	 */
	private Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> getPartiMediIdColnCollection(
			final String mrnString) throws BizLogicException
	{
		final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> partiMediIdColn = new LinkedHashSet<IParticipantMedicalIdentifier<IParticipant, ISite>>();
		final String values[] = mrnString.split(",");
		for (int i = 0; i < values.length; i++)
		{
			final String value = values[i];
			final IParticipantMedicalIdentifier<IParticipant, ISite> participantMedicalIdentifier = getParticipantMedicalIdentifierObj(value);
			partiMediIdColn.add(participantMedicalIdentifier);
		}

		return partiMediIdColn;
	}

	/**
	 * Gets the participant medical identifier obj.
	 *
	 * @param value the value
	 *
	 * @return the participant medical identifier obj
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws Exception the exception
	 */
	private IParticipantMedicalIdentifier<IParticipant, ISite> getParticipantMedicalIdentifierObj(
			final String value) throws BizLogicException

	{
		final IParticipantMedicalIdentifier<IParticipant, ISite> participantMedicalIdentifier = (IParticipantMedicalIdentifier<IParticipant, ISite>) ParticipantManagerUtility
				.getPMIInstance();
		final String values[] = value.split(":");
		final String mrn = values[0];
		final String facilityId = values[1];
		final ISite siteObj = ParticipantManagerUtility.getSiteObject(facilityId);
		participantMedicalIdentifier.setMedicalRecordNumber(mrn);
		participantMedicalIdentifier.setSite(siteObj);
		return participantMedicalIdentifier;
	}

	/**
	 * Gets the select query.
	 *
	 * @param identifier the identifier
	 *
	 * @return the select query
	 */
	private String getSelectQuery(final long identifier)
	{
		return "SELECT * FROM CATISSUE_MATCHED_PARTICIPANT WHERE SEARCHED_PARTICIPANT_ID='"
				+ identifier + "'";

	}

	/**
	 * Store lists.
	 *
	 * @param request the request
	 * @param matchPartpantLst the match partpant lst
	 *
	 * @throws DAOException the DAO exception
	 */
	private void storeLists(final HttpServletRequest request, final List<DefaultLookupResult> matchPartpantLst)
			throws DAOException
	{
		final ActionMessages messages = new ActionMessages();
		final List<String> columnList = ParticipantManagerUtility.getColumnHeadingList();
		request.setAttribute(edu.wustl.common.util.global.Constants.SPREADSHEET_COLUMN_LIST,
				columnList);
		final List<List<String>> pcpantDisplayLst = ParticipantManagerUtility
				.getParticipantDisplayList(matchPartpantLst);
		request.setAttribute(Constants.SPREADSHEET_DATA_LIST, pcpantDisplayLst);
		final HttpSession session = request.getSession();
		session.setAttribute("MatchedParticpant", matchPartpantLst);
		if (request.getAttribute("continueLookup") == null)
		{
			saveMessages(request, messages);
		}
	}
}
