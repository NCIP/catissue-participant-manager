/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue-participant-manager/LICENSE.txt for details.
 */

package edu.wustl.common.participant.action;

import java.text.ParseException;
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
import edu.wustl.common.participant.actionForm.IParticipantForm;
import edu.wustl.common.participant.bizlogic.CommonParticipantBizlogic;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerException;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.Utility;
import edu.wustl.common.util.logger.Logger;
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
	private static final Logger LOGGER = Logger
			.getCommonLogger(MatchedParticipantsSearchAction.class);

	/**
	 * For displaying the matched participants from eMPI
	 */
	public ActionForward executeXSS(final ActionMapping mapping, final ActionForm form,
			final HttpServletRequest request, final HttpServletResponse response)
			throws ApplicationException
	{
		ActionForward forward = null;
		final AbstractActionForm participantForm = (AbstractActionForm) form;
		final SessionDataBean sessionDataBean = (SessionDataBean) request.getSession()
				.getAttribute(edu.wustl.common.util.global.Constants.SESSION_DATA);
		final Long userId = sessionDataBean.getUserId();
		final List<Long> participantIds = ParticipantManagerUtility
				.getProcessedMatchedParticipantIds(userId);
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
				int indexOfCurrentPart = 0;
				int nextMatchedParticpantIndex = 0;
				if (participantIds.contains(identifier))
				{
					indexOfCurrentPart = participantIds.indexOf(identifier);
				}

				if (indexOfCurrentPart >= 0 && ((participantIds.size() - 1) > indexOfCurrentPart))
				{
					nextMatchedParticpantIndex = indexOfCurrentPart + 1;

				}
				request.getSession().setAttribute(Constants.NEXT_PART_ID_TO_PROCESS,
						participantIds.get(nextMatchedParticpantIndex));
				// fetch the stored matched participant for the participant in the message board
				final IParticipantForm partiForm = (IParticipantForm) form;
				fetchMatchedParticipantsFromDB(identifier.longValue(), request, partiForm);

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
	 * @throws ParticipantManagerException
	 */
	private void fetchMatchedParticipantsFromDB(final long participantId,
			final HttpServletRequest request, final IParticipantForm partiForm)
			throws DAOException, BizLogicException, ParseException, ParticipantManagerException
	{
		List<DefaultLookupResult> matchPartpantLst = null;

			final String ssn = partiForm.getSocialSecurityNumberPartA().concat(
					partiForm.getSocialSecurityNumberPartB()).concat(
					partiForm.getSocialSecurityNumberPartC());
			CommonParticipantBizlogic bizLogic = new CommonParticipantBizlogic();
			matchPartpantLst = bizLogic.retrieveMatchedParticipantList(participantId);
			if (matchPartpantLst != null && !matchPartpantLst.isEmpty())
			{

				storeLists(request, matchPartpantLst);

				if ((partiForm.getBirthDate() == null || "".equals(partiForm.getBirthDate()))
						&& (ssn == null || "".equals(ssn)))
				{
					request.setAttribute(Constants.EMPI_GENERATION_FIELDS_INSUFFICIENT,
							Constants.TRUE);
				}
			}
			else
			{
				setStatusMessage(request, "participant.empiid.zero.match.message");
				ParticipantManagerUtility.deleteProcessedParticipant(Long
						.valueOf(participantId),getSessionData(request));
				request.setAttribute(Constants.ZERO_MATCHES, Constants.TRUE);
				if ((partiForm.getBirthDate() == null || "".equals(partiForm.getBirthDate()))
						&& (ssn == null || "".equals(ssn)))
				{
					setStatusMessage(request, "participant.empiid.generation.incomplete.detail");
				}
			}
			request.setAttribute(Constants.IS_GENERATE_EMPI_PAGE, Constants.TRUE);



	}



	/**
	 * Sets the status message.
	 *
	 * @param request the new status message
	 *
	 * @throws DAOException the DAO exception
	 */
	private void setStatusMessage(final HttpServletRequest request, String key) throws DAOException
	{
		ActionMessages actionMsgs = (ActionMessages) request
				.getAttribute("org.apache.struts.action.ACTION_MESSAGE");
		if (actionMsgs == null)
		{
			actionMsgs = new ActionMessages();
		}
		actionMsgs.add("org.apache.struts.action.GLOBAL_MESSAGE", new ActionMessage(key));
		saveMessages(request, actionMsgs);
	}






	/**
	 * Store lists.
	 *
	 * @param request the request
	 * @param matchPartpantLst the match partpant lst
	 *
	 * @throws DAOException the DAO exception
	 */
	private void storeLists(final HttpServletRequest request,
			final List<DefaultLookupResult> matchPartpantLst) throws DAOException
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
		request.setAttribute(Constants.MATCHED_PARTICIPANTS_FOUND_FROM_EMPI, Constants.TRUE);
		if (request.getAttribute("continueLookup") == null)
		{
			saveMessages(request, messages);
		}
	}
}
