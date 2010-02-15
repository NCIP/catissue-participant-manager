
package edu.wustl.common.participant.action;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import edu.wustl.common.action.SecureAction;
import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.participant.bizlogic.ParticipantMatchingBizLogic;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.common.util.global.QuerySessionData;
import edu.wustl.common.util.logger.Logger;

// TODO: Auto-generated Javadoc
/**
 * The Class ProcessMatchedParticipantsAction.
 *
 * @author geeta_jaggal
 * @created-on Nov 16, 2009
 * The Class ProcessMatchedParticipantsAction :
 * Used for displaying processed matched participants.
 */
public class ProcessMatchedParticipantsAction extends SecureAction
{

	/** The Constant logger. */
	private static final Logger LOGGER = Logger
			.getCommonLogger(ProcessMatchedParticipantsAction.class);

	/* (non-Javadoc)
	 * @see org.apache.struts.action.Action#execute(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public ActionForward executeSecureAction(final ActionMapping mapping, final ActionForm form,
			final HttpServletRequest request, final HttpServletResponse response)
			throws ApplicationException
	{
		final HttpSession session = request.getSession();
		String target = null;
		try
		{
			final int recordsPerPage = getRecordsPerPage(session);
			final String isDelete = request.getParameter(Constants.IS_DELETE_PARTICIPANT);
			final String particicipantId = request.getParameter("participantId");
			final List<String> columnNames = getColumnList();
			final Long userId = getUserId(request);

			if(session.getAttribute(Constants.NEXT_PART_ID_TO_PROCESS)!=null)
			{
				session.removeAttribute(Constants.NEXT_PART_ID_TO_PROCESS);
			}
			if (isDelete != null && isDelete != "" && isDelete.equalsIgnoreCase(Constants.YES)
					&& particicipantId != null && particicipantId != "")
			{
				final boolean delStatus = ParticipantManagerUtility.deleteProcessedParticipant(Long
						.valueOf(particicipantId));
				setDelStatusMessage(request, delStatus);
			}

			final ParticipantMatchingBizLogic bizLogic = new ParticipantMatchingBizLogic();

			final List list = bizLogic.getProcessedMatchedParticipants(userId);

			storeList(request, session, columnNames, list, recordsPerPage);

			target = edu.wustl.common.util.global.Constants.SUCCESS;

			return mapping.findForward(target);
		}
		catch (Exception e)
		{
			LOGGER.info(e.getMessage());
			throw new ApplicationException(null, e, e.getMessage());
		}
	}

	private List<String> getColumnList()
	{
		final List<String> columnNames = new ArrayList<String>();
		columnNames.add("ID");
		columnNames.add("Last Name");
		columnNames.add("First Name");
		columnNames.add("Creation Date");
		columnNames.add("Matched Participants Count");
		columnNames.add("Clinical Stydy Name");
		return columnNames;
	}

	private int getRecordsPerPage(final HttpSession session)
	{
		int recordsPerPage = 0;
		final String recordsPerPageSessionValue = (String) session
				.getAttribute(edu.wustl.common.util.global.Constants.RESULTS_PER_PAGE);
		if (recordsPerPageSessionValue == null)
		{
			recordsPerPage = Integer.parseInt(XMLPropertyHandler
					.getValue(Constants.NO_OF_RECORDS_PER_PAGE));
			session.setAttribute(edu.wustl.common.util.global.Constants.RESULTS_PER_PAGE,
					(new StringBuilder()).append(recordsPerPage).append("").toString());
		}
		else
		{
			recordsPerPage = Integer.parseInt(recordsPerPageSessionValue);
		}
		return recordsPerPage;
	}

	private Long getUserId(final HttpServletRequest request)
	{
		final SessionDataBean sessionDataBean = (SessionDataBean) request.getSession()
				.getAttribute(edu.wustl.common.util.global.Constants.SESSION_DATA);
		final Long userId = sessionDataBean.getUserId();
		return userId;
	}

	private void storeList(final HttpServletRequest request, final HttpSession session,
			final List<String> columnNames, final List list, final int recordsPerPage)
	{
		final QuerySessionData querySessionData = new QuerySessionData();
		querySessionData.setRecordsPerPage(recordsPerPage);
		querySessionData.setTotalNumberOfRecords(list.size());
		session.setAttribute(edu.wustl.common.util.global.Constants.QUERY_SESSION_DATA,
				querySessionData);
		session.setAttribute(Constants.IS_SIMPLE_SEARCH, Boolean.TRUE.toString());

		request.setAttribute(edu.wustl.common.util.global.Constants.PAGEOF,
				Constants.PAGE_OF_MATCHED_PARTIICPANTS);
		request.setAttribute(Constants.SPREADSHEET_DATA_LIST, list);
		request.setAttribute(edu.wustl.common.util.global.Constants.SPREADSHEET_COLUMN_LIST,
				columnNames);
		setStatusMessage(request);
	}

	private void setStatusMessage(HttpServletRequest request){
		ActionMessages messages = (ActionMessages) request.getAttribute(Globals.MESSAGE_KEY);
		if (messages == null)
		{
			messages = new ActionMessages();
		}
		messages.add("org.apache.struts.action.GLOBAL_MESSAGE", new ActionMessage("process.participant.message"));
		saveMessages(request, messages);
	}

	/**
	 * Sets the status message.
	 *
	 * @param request the request
	 * @param delStatus the del status
	 */
	private void setDelStatusMessage(final HttpServletRequest request, final boolean delStatus)
	{
		final ActionMessages actionMsgs = new ActionMessages();
		if (delStatus)
		{
			actionMsgs.add("GLOBAL_MESSAGE", new ActionMessage(
					"participant.processed.delete.success"));
		}
		else
		{
			actionMsgs.add("GLOBAL_MESSAGE", new ActionMessage(
					"participant.processed.delete.failure"));
		}
		saveMessages(request, actionMsgs);
	}
}
