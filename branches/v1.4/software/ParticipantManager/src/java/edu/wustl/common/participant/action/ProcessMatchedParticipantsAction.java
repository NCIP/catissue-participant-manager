
package edu.wustl.common.participant.action;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
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
import edu.wustl.dao.exception.DAOException;

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
	private static final Logger LOGGER = Logger.getCommonLogger(ProcessMatchedParticipantsAction.class);

	/* (non-Javadoc)
	 * @see org.apache.struts.action.Action#execute(org.apache.struts.action.ActionMapping, org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public ActionForward executeSecureAction(final ActionMapping mapping, final ActionForm form,
			final HttpServletRequest request, final HttpServletResponse response) throws ApplicationException
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

			if (session.getAttribute(Constants.NEXT_PART_ID_TO_PROCESS) != null)
			{
				session.removeAttribute(Constants.NEXT_PART_ID_TO_PROCESS);
			}
			if (isDelete != null && !"".equals(isDelete) && Constants.YES.equals(isDelete) && particicipantId != null
					&& !"".equals(particicipantId))
			{
				ParticipantManagerUtility.deleteProcessedParticipant(Long.valueOf(particicipantId));
				setStatusMessage(request, "participant.processed.delete.success");
			}

			final ParticipantMatchingBizLogic bizLogic = new ParticipantMatchingBizLogic();

			final List list = bizLogic.getProcessedMatchedParticipants(userId, recordsPerPage);

			storeList(request, session, columnNames, list, recordsPerPage);

			target = edu.wustl.common.util.global.Constants.SUCCESS;

			return mapping.findForward(target);
		}
		catch (DAOException daoExp)
		{
			setErrorMessage(request);
			LOGGER.info(daoExp.getMessage());
			throw new ApplicationException(null, daoExp, daoExp.getMessage());
		}
		catch (Exception e)
		{
			LOGGER.info(e.getMessage());
			throw new ApplicationException(null, e, e.getMessage());
		}
	}

	/**
	 * Gets the column list.
	 *
	 * @return the column list
	 */
	private List<String> getColumnList()
	{
		final List<String> columnNames = new ArrayList<String>();
		columnNames.add("ID");
		columnNames.add("Last Name");
		columnNames.add("First Name");
		columnNames.add("Creation Date");
		columnNames.add("Matched Participants Count");
		columnNames.add("Clinical Study Name");
		return columnNames;
	}

	/**
	 * Gets the records per page.
	 *
	 * @param session the session
	 *
	 * @return the records per page
	 */
	private int getRecordsPerPage(final HttpSession session)
	{
		int recordsPerPage = 0;
		final String recordsPerPageSessionValue = (String) session
				.getAttribute(edu.wustl.common.util.global.Constants.RESULTS_PER_PAGE);
		if (recordsPerPageSessionValue == null)
		{
			recordsPerPage = Integer.parseInt(XMLPropertyHandler.getValue(Constants.NO_OF_RECORDS_PER_PAGE));
			session.setAttribute(edu.wustl.common.util.global.Constants.RESULTS_PER_PAGE, String
					.valueOf(recordsPerPage));
		}
		else
		{
			recordsPerPage = Integer.parseInt(recordsPerPageSessionValue);
		}
		return recordsPerPage;
	}

	/**
	 * Gets the user id.
	 *
	 * @param request the request
	 *
	 * @return the user id
	 */
	private Long getUserId(final HttpServletRequest request)
	{
		final SessionDataBean sessionDataBean = (SessionDataBean) request.getSession().getAttribute(
				edu.wustl.common.util.global.Constants.SESSION_DATA);
		final Long userId = sessionDataBean.getUserId();
		return userId;
	}

	/**
	 * Store list.
	 *
	 * @param request the request
	 * @param session the session
	 * @param columnNames the column names
	 * @param list the list
	 * @param recordsPerPage the records per page
	 * @throws DAOException
	 */
	private void storeList(final HttpServletRequest request, final HttpSession session, final List<String> columnNames,
			final List list, final int recordsPerPage) throws DAOException
	{
		final Long userId = getUserId(request);
		final ParticipantMatchingBizLogic bizLogic = new ParticipantMatchingBizLogic();
		final QuerySessionData querySessionData = new QuerySessionData();

		String sql = bizLogic.getQuery(userId);

		querySessionData.setRecordsPerPage(recordsPerPage);
		querySessionData.setTotalNumberOfRecords(bizLogic.getTotalCount(userId));
		querySessionData.setQueryResultObjectDataMap(null);
		querySessionData.setSql(sql);
		querySessionData.setHasConditionOnIdentifiedField(false);
		querySessionData.setSecureExecute(false);

		session.setAttribute(edu.wustl.common.util.global.Constants.QUERY_SESSION_DATA, querySessionData);
		session.setAttribute(Constants.IS_SIMPLE_SEARCH, Boolean.TRUE.toString());

		request.setAttribute(edu.wustl.common.util.global.Constants.PAGEOF, Constants.PAGE_OF_MATCHED_PARTIICPANTS);
		request.setAttribute(Constants.SPREADSHEET_DATA_LIST, list);
		request.setAttribute(edu.wustl.common.util.global.Constants.SPREADSHEET_COLUMN_LIST, columnNames);
		setStatusMessage(request, "process.participant.message");
	}

	/**
	 * Sets the status message.
	 *
	 * @param request the new status message
	 */
	private void setStatusMessage(final HttpServletRequest request, final String key)
	{
		ActionMessages messages = (ActionMessages) request.getAttribute(Globals.MESSAGE_KEY);
		if (messages == null)
		{
			messages = new ActionMessages();
		}
		messages.add("org.apache.struts.action.GLOBAL_MESSAGE", new ActionMessage(key));
		saveMessages(request, messages);
	}

	/**
	 * Sets the status message.
	 *
	 * @param request the request
	 * @param delStatus the del status
	 */
	private void setErrorMessage(final HttpServletRequest request)
	{
		ActionErrors actionErrors = (ActionErrors) request.getAttribute(Globals.ERROR_KEY);
		if (actionErrors == null)
		{
			actionErrors = new ActionErrors();
		}
		final ActionError actionError = new ActionError("participant.processed.delete.failure");
		actionErrors.add(ActionErrors.GLOBAL_ERROR, actionError);
		saveErrors(request, actionErrors);
	}
}
