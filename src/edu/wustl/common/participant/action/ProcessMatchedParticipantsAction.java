package edu.wustl.common.participant.action;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.participant.bizlogic.ParticipantMatchingBizLogic;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.common.util.global.Constants;
import edu.wustl.common.util.global.QuerySessionData;

/**
 * The Class ProcessMatchedParticipantsAction.
 */
public class ProcessMatchedParticipantsAction extends Action {

	public ProcessMatchedParticipantsAction() {
	}

	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws ApplicationException {
		HttpSession session = request.getSession();
		String target;
		String recordsPerPageSessionValue = (String) session
				.getAttribute("numResultsPerPage");
		int recordsPerPage;
		try {
			if (recordsPerPageSessionValue == null) {
				recordsPerPage = Integer.parseInt(XMLPropertyHandler
						.getValue("resultView.noOfRecordsPerPage"));
				session.setAttribute("numResultsPerPage", (new StringBuilder())
						.append(recordsPerPage).append("").toString());
			} else {
				recordsPerPage = (new Integer(recordsPerPageSessionValue))
						.intValue();
			}
			String isDelete = request.getParameter("isDelete");
			String particicipantId = request.getParameter("participantId");
			ParticipantMatchingBizLogic bizLogic = new ParticipantMatchingBizLogic();
			List columnNames = new ArrayList();
			columnNames.add("ID");
			columnNames.add("Last Name");
			columnNames.add("First Name");
			columnNames.add("Creation Date");
			columnNames.add("Matched Participants Count");
			columnNames.add("Clinical Stydy Name");
			SessionDataBean sessionDataBean = (SessionDataBean) request
					.getSession().getAttribute("sessionData");
			Long userId = sessionDataBean.getUserId();
			if (isDelete != null && isDelete != ""
					&& isDelete.equalsIgnoreCase("yes")
					&& particicipantId != null && particicipantId != "") {
				boolean delStatus = ParticipantManagerUtility
						.deleteProcessedParticipant(Long
								.valueOf(particicipantId));
				setStatusMessage(request, delStatus);
			}
			List list = bizLogic.getProcessedMatchedParticipants(userId);
			QuerySessionData querySessionData = new QuerySessionData();
			querySessionData.setRecordsPerPage(recordsPerPage);
			querySessionData.setTotalNumberOfRecords(list.size());
			session.setAttribute("querySessionData", querySessionData);
			session.setAttribute("isSimpleSearch", Boolean.TRUE.toString());
			target = "success";
			request.setAttribute("pageOf", "pageOfMatchedParticipant");
			request.setAttribute("spreadsheetDataList", list);
			request.setAttribute("spreadsheetColumnList", columnNames);
			return mapping.findForward(target);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationException(null, e, e.getMessage());
		}
	}

	private void setStatusMessage(HttpServletRequest request, boolean delStatus) {
		ActionMessages actionMsgs = new ActionMessages();
		if (delStatus) {
			actionMsgs.add("org.apache.struts.action.GLOBAL_MESSAGE",
					new ActionMessage("participant.processed.delete.success"));
		} else {
			actionMsgs.add("org.apache.struts.action.GLOBAL_MESSAGE",
					new ActionMessage("participant.processed.delete.failure"));
		}
		saveMessages(request, actionMsgs);
	}
}
