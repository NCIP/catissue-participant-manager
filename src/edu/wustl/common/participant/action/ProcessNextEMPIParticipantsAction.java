/**
 *
 */

package edu.wustl.common.participant.action;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import edu.wustl.common.action.SecureAction;
import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;

/**
 * @author suhas_khot
 *
 */
public class ProcessNextEMPIParticipantsAction extends SecureAction
{


	/* (non-Javadoc)
	 * @see org.apache.struts.action.Action#execute(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public ActionForward executeSecureAction(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		Long identifier = null;
		String pageOf = null;
		ActionForward actionFwd = null;
		final SessionDataBean sessionDataBean = (SessionDataBean) request.getSession()
		.getAttribute(edu.wustl.common.util.global.Constants.SESSION_DATA);
		final Long userId = sessionDataBean.getUserId();
		List<Long> participantIds = ParticipantManagerUtility.getProcessedMatchedParticipantIds(userId);

		if (participantIds != null && !participantIds.isEmpty())
		{
			Long participantId = null;
			Object nextMatchPartId = request.getSession().getAttribute(Constants.NEXT_PART_ID_TO_PROCESS);
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
			request.setAttribute(edu.wustl.common.util.global.Constants.SYSTEM_IDENTIFIER, identifier);
			request.setAttribute(Constants.PAGE_OF, pageOf);
		}
		actionFwd = mapping.findForward(Constants.SUCCESS);
		ActionForward actionforward = new ActionForward();
		actionforward.setContextRelative(false);
		actionforward.setName(actionFwd.getName());
		String path = actionFwd.getPath() + "?id=" + identifier + "&pageOf=" + pageOf;
		actionforward.setPath(path);
		actionforward.setRedirect(true);
		return actionforward;
	}
}
