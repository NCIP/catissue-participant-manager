
package java.edu.wustl.common.participant.action;

import java.edu.wustl.common.participant.actionForm.IParticipantForm;
import java.edu.wustl.common.participant.domain.IParticipant;
import java.edu.wustl.common.participant.utility.Constants;
import java.edu.wustl.common.participant.utility.ParticipantManagerUtility;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import edu.wustl.common.action.SecureAction;
import edu.wustl.common.actionForm.AbstractActionForm;
import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.factory.AbstractFactoryConfig;
import edu.wustl.common.factory.IDomainObjectFactory;
import edu.wustl.common.lookup.DefaultLookupResult;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.exception.DAOException;

/**
 * @author geeta_jaggal
 *
 * The Class ParticipantLookupAction. :
 * Used for finding the matched participants from local db.
 */
public class ParticipantLookupAction extends SecureAction
{

	/**
	 * Method for performing participant look up.
	 */
	public ActionForward executeSecureAction(final ActionMapping mapping, final ActionForm form,
			final HttpServletRequest request, final HttpServletResponse response) throws ApplicationException
	{
		final AbstractActionForm abstractForm = (AbstractActionForm) form;

		final IParticipantForm participantForm = (IParticipantForm) form;
		String target = null;
		try
		{
			final boolean isForward = checkForwardToParticipantSelectAction(request, abstractForm
					.isAddOperation());
			if (isForward)
			{
				target = "participantSelect";
			}
			else
			{
				IDomainObjectFactory domainObjectFactory;

				domainObjectFactory = AbstractFactoryConfig.getInstance().getDomainObjectFactory();

				final edu.wustl.common.domain.AbstractDomainObject abstractDomain = domainObjectFactory
						.getDomainObject(abstractForm.getFormId(), abstractForm);
				final IParticipant participant = (IParticipant) abstractDomain;
				final boolean isCallToLkupLgic = ParticipantManagerUtility
						.isCallToLookupLogicNeeded(participant);
				if (isCallToLkupLgic)
				{
					final List<DefaultLookupResult> matchPartpantLst = getListOfMatchingParticipants(
							participant, request, participantForm.getCpId());

					if (!matchPartpantLst.isEmpty())
					{
						target = edu.wustl.common.util.global.Constants.SUCCESS;
						storeLists(request, matchPartpantLst);
					}
					else
					{
						target = Constants.PARTICIPANT_ADD_FORWARD;
					}

					/*
					if (matchPartpantLst == null || matchPartpantLst.isEmpty())
					{
						target = Constants.PARTICIPANT_ADD_FORWARD;
					}
					else
					{
						target = processListForMatchWithinCS(request, matchPartpantLst,
								participantForm.getCpId());
					}
					*/
				}
				else
				{
					target = Constants.PARTICIPANT_ADD_FORWARD;
				}
				setRequestAttributes(request);

			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			throw new ApplicationException(null, e, e.getMessage());
		}
		return mapping.findForward(target);
	}

	/**
	 * Gets the list of matching participants.
	 *
	 * @param participant the participant
	 * @param request the request
	 *
	 * @return the list of matching participants
	 * @throws Exception
	 *
	 * @throws Exception the exception
	 */
	private List<DefaultLookupResult> getListOfMatchingParticipants(final IParticipant participant,
			final HttpServletRequest request, final Long csId) throws Exception

	{
		final edu.wustl.common.beans.SessionDataBean sessionDataBean = getSessionData(request);
		final List<DefaultLookupResult> matchPartpantLst = ParticipantManagerUtility
				.getListOfMatchingParticipants(participant, sessionDataBean, null, csId);
		return matchPartpantLst;
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
		messages.add("org.apache.struts.action.GLOBAL_MESSAGE", new ActionMessage(
				"participant.lookup.success",
				"Submit was not successful because some matching participants found."));
		final List columnList = ParticipantManagerUtility.getColumnHeadingList();
		request.setAttribute(edu.wustl.common.util.global.Constants.SPREADSHEET_COLUMN_LIST,
				columnList);
		final List pcpantDisplayLst = ParticipantManagerUtility
				.getParticipantDisplayList(matchPartpantLst);
		request.setAttribute(Constants.SPREADSHEET_DATA_LIST, pcpantDisplayLst);
		final HttpSession session = request.getSession();
		session.setAttribute("MatchedParticpant", matchPartpantLst);
		if (request.getAttribute("continueLookup") == null)
		{
			saveMessages(request, messages);
		}
	}

	/**
	 * Check forward to participant select action.
	 *
	 * @param request the request
	 * @param isAddOperation the is add operation
	 *
	 * @return true, if successful
	 */
	private boolean checkForwardToParticipantSelectAction(final HttpServletRequest request,
			final boolean isAddOperation)
	{
		boolean isForward = false;
		final String participantId = "participantId";
		if (request.getParameter("continueLookup") == null
				&& request.getAttribute("continueLookup") == null)
		{
			if (isAddOperation)
			{
				if (request.getParameter(participantId) != null
						&& !request.getParameter(participantId).equals("null")
						&& !request.getParameter(participantId).equals("")
						&& !request.getParameter(participantId).equals("0"))
				{
					Logger.out.info("inside the participant mapping");
					isForward = true;
				}
			}
			else if (request.getParameter("generateeMPIIdforPartiId") == null
					|| "".equals(request.getParameter("generateeMPIIdforPartiId")))
			{
				isForward = false;
			}
			else
			{
				isForward = true;
			}
		}
		return isForward;
	}

	/**
	 * Sets the request attributes.
	 *
	 * @param request the new request attributes
	 */
	private void setRequestAttributes(final HttpServletRequest request)
	{
		if (request.getParameter(edu.wustl.common.util.global.Constants.SUBMITTED_FOR) != null
				&& !request.getParameter(edu.wustl.common.util.global.Constants.SUBMITTED_FOR)
						.equals(""))
		{
			request.setAttribute(edu.wustl.common.util.global.Constants.SUBMITTED_FOR, request
					.getParameter(edu.wustl.common.util.global.Constants.SUBMITTED_FOR));
		}
		if (request.getParameter(edu.wustl.common.util.global.Constants.FORWARD_TO) != null
				&& !request.getParameter(edu.wustl.common.util.global.Constants.FORWARD_TO).equals(
						""))
		{
			request.setAttribute(edu.wustl.common.util.global.Constants.FORWARD_TO, request
					.getParameter(edu.wustl.common.util.global.Constants.FORWARD_TO));
		}
		request.setAttribute("participantId", "");
	}
}
