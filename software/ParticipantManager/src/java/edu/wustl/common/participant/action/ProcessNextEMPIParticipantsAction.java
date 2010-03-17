/**
 *
 */

package edu.wustl.common.participant.action;

import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import edu.wustl.common.action.SecureAction;
import edu.wustl.common.beans.SessionDataBean;

/**
 * @author suhas_khot
 *
 */
public class ProcessNextEMPIParticipantsAction extends SecureAction
{

	/* (non-Javadoc)
	 * @see org.apache.struts.action.Action#execute(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public ActionForward executeSecureAction(final ActionMapping mapping, final ActionForm form,
			final HttpServletRequest request, final HttpServletResponse response) throws Exception
	{
		Long identifier = null;
		String pageOf = null;
		ActionForward actionFwd = null;
		final SessionDataBean sessionDataBean = (SessionDataBean) request.getSession()
				.getAttribute(edu.wustl.common.util.global.Constants.SESSION_DATA);
		final Long userId = sessionDataBean.getUserId();
		final List<Long> participantIds = ParticipantManagerUtility
				.getProcessedMatchedParticipantIds(userId);
		final ActionForward actionforward = new ActionForward();
		if (participantIds != null && !participantIds.isEmpty())
		{
			Long participantId = null;
			final Object nextMatchPartId = request.getSession().getAttribute(
					Constants.NEXT_PART_ID_TO_PROCESS);
			if (nextMatchPartId != null && participantIds.contains(nextMatchPartId))
			{
				participantId = (Long) nextMatchPartId;
			}
			else
			{
				participantId = (Long) participantIds.get(0);
			}
			identifier = Long.valueOf(participantId.toString());
			pageOf = "pageOfMatchedParticipant";
			request.setAttribute(edu.wustl.common.util.global.Constants.SYSTEM_IDENTIFIER,
					identifier);
			request.setAttribute(Constants.PAGE_OF, pageOf);

			actionFwd = mapping.findForward(Constants.SUCCESS);
			actionforward.setContextRelative(false);
			actionforward.setName(actionFwd.getName());
			final String path = actionFwd.getPath() + "?id=" + identifier + "&pageOf=" + pageOf;
			actionforward.setPath(path);
			actionforward.setRedirect(true);
		}
		else
		{
			pageOf="pageOfMatchedParticipant";
			actionFwd = mapping.findForward("ProcessMatchedParticipants");
			actionforward.setContextRelative(false);
			actionforward.setName(actionFwd.getName());
			final String path = actionFwd.getPath()+ "?pageOf=" + pageOf + "&identifierFieldIndex="+0;
			actionforward.setPath(path);
			actionforward.setRedirect(true);
		}
		return actionforward;
	}
}
