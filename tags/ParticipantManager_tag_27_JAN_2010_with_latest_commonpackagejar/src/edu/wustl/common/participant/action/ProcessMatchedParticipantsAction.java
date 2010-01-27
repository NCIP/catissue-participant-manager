
package edu.wustl.common.participant.action;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

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
public class ProcessMatchedParticipantsAction extends Action
{

	/** The Constant logger. */
	private static final Logger logger = Logger
			.getCommonLogger(ProcessMatchedParticipantsAction.class);

	/* (non-Javadoc)
	 * @see org.apache.struts.action.Action#execute(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws ApplicationException
	{
		HttpSession session = request.getSession();
		String target = null;
		try
		{
			int recordsPerPage = getRecordsPerPage(session);
			String isDelete = request.getParameter(Constants.IS_DELETE_PARTICIPANT);
			String particicipantId = request.getParameter("participantId");
			List<String> columnNames = getColumnList();
			Long userId = getUserId(request);

			if (isDelete != null && isDelete != "" && isDelete.equalsIgnoreCase(Constants.YES)
					&& particicipantId != null && particicipantId != "")
			{
				boolean delStatus = ParticipantManagerUtility.deleteProcessedParticipant(Long
						.valueOf(particicipantId));
				setStatusMessage(request, delStatus);
			}

			ParticipantMatchingBizLogic bizLogic = new ParticipantMatchingBizLogic();
			List list = bizLogic.getProcessedMatchedParticipants(userId);

			storeList(request, session, columnNames, list, recordsPerPage);

			target = edu.wustl.common.util.global.Constants.SUCCESS;

			return mapping.findForward(target);
		}
		catch (Exception e)
		{
			logger.info(e.getMessage());
			throw new ApplicationException(null, e, e.getMessage());
		}
	}

	private List<String> getColumnList()
	{
		List<String> columnNames = new ArrayList<String>();
		columnNames.add("ID");
		columnNames.add("Last Name");
		columnNames.add("First Name");
		columnNames.add("Creation Date");
		columnNames.add("Matched Participants Count");
		columnNames.add("Clinical Stydy Name");
		return columnNames;
	}

	private int getRecordsPerPage(HttpSession session)
	{
		int recordsPerPage = 0;
		String recordsPerPageSessionValue = (String) session
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
			recordsPerPage = (Integer.valueOf((recordsPerPageSessionValue))).intValue();
		}
		return recordsPerPage;
	}

	private Long getUserId(HttpServletRequest request)
	{
		SessionDataBean sessionDataBean = (SessionDataBean) request.getSession().getAttribute(
				edu.wustl.common.util.global.Constants.SESSION_DATA);
		Long userId = sessionDataBean.getUserId();
		return userId;
	}

	private void storeList(HttpServletRequest request, HttpSession session,
			List<String> columnNames, List list, int recordsPerPage)
	{
		QuerySessionData querySessionData = new QuerySessionData();
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
	}

	/**
	 * Sets the status message.
	 *
	 * @param request the request
	 * @param delStatus the del status
	 */
	private void setStatusMessage(HttpServletRequest request, boolean delStatus)
	{
		ActionMessages actionMsgs = new ActionMessages();
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
