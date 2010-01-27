package edu.wustl.common.participant.action;

import edu.wustl.common.action.BaseAction;
import edu.wustl.common.actionForm.AbstractActionForm;
import edu.wustl.common.factory.AbstractFactoryConfig;
import edu.wustl.common.factory.IDomainObjectFactory;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.exception.DAOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

public class ParticipantLookupAction extends BaseAction
{

    public ParticipantLookupAction()
    {
    }

    public ActionForward executeAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
        throws Exception
    {
        AbstractActionForm abstractForm = (AbstractActionForm)form;
        List matchPartpantLst = null;
        String target = null;
        boolean isForward = checkForwardToParticipantSelectAction(request, abstractForm.isAddOperation());
        if(isForward)
        {
            target = "participantSelect";
        } else
        {
            IDomainObjectFactory domainObjectFactory = AbstractFactoryConfig.getInstance().getDomainObjectFactory();
            edu.wustl.common.domain.AbstractDomainObject abstractDomain = domainObjectFactory.getDomainObject(abstractForm.getFormId(), abstractForm);
            IParticipant participant = (IParticipant)abstractDomain;
            boolean isCallToLkupLgic = ParticipantManagerUtility.isCallToLookupLogicNeeded(participant);
            if(isCallToLkupLgic)
            {
                matchPartpantLst = getListOfMatchingParticipants(participant, request);
                if(matchPartpantLst == null || matchPartpantLst.isEmpty())
                {
                    target = "participantAdd";
                } else
                {
                    storeLists(request, matchPartpantLst);
                    target = "success";
                }
            } else
            {
                target = "participantAdd";
            }
            setRequestAttributes(request);
        }
        return mapping.findForward(target);
    }

    private List getListOfMatchingParticipants(IParticipant participant, HttpServletRequest request)
        throws Exception
    {
        edu.wustl.common.factory.IFactory factory = AbstractFactoryConfig.getInstance().getBizLogicFactory();
        edu.wustl.common.beans.SessionDataBean sessionDataBean = getSessionData(request);
        List matchPartpantLst = ParticipantManagerUtility.getListOfMatchingParticipants(participant, sessionDataBean, null);
        return matchPartpantLst;
    }

    private void storeLists(HttpServletRequest request, List matchPartpantLst)
        throws DAOException
    {
        ActionMessages messages = new ActionMessages();
        messages.add("org.apache.struts.action.GLOBAL_MESSAGE", new ActionMessage("participant.lookup.success", "Submit was not successful because some matching participants found."));
        List columnList = ParticipantManagerUtility.getColumnHeadingList();
        request.setAttribute("spreadsheetColumnList", columnList);
        List pcpantDisplayLst = ParticipantManagerUtility.getParticipantDisplayList(matchPartpantLst);
        request.setAttribute("spreadsheetDataList", pcpantDisplayLst);
        HttpSession session = request.getSession();
        session.setAttribute("MatchedParticpant", matchPartpantLst);
        if(request.getAttribute("continueLookup") == null)
        {
            saveMessages(request, messages);
        }
    }

    private boolean checkForwardToParticipantSelectAction(HttpServletRequest request, boolean isAddOperation)
    {
        boolean isForward = false;
        String participantId = "participantId";
        if(request.getParameter("continueLookup") == null && request.getAttribute("continueLookup") == null)
        {
            if(isAddOperation)
            {
                if(request.getParameter(participantId) != null && !request.getParameter(participantId).equals("null") && !request.getParameter(participantId).equals("") && !request.getParameter(participantId).equals("0"))
                {
                    Logger.out.info("inside the participant mapping");
                    isForward = true;
                }
            } else
            if(request.getParameter("generateeMPIIdforPartiId") == null || "".equals(request.getParameter("generateeMPIIdforPartiId")))
            {
                isForward = false;
            } else
            {
                isForward = true;
            }
        }
        return isForward;
    }

    private void setRequestAttributes(HttpServletRequest request)
    {
        if(request.getParameter("submittedFor") != null && !request.getParameter("submittedFor").equals(""))
        {
            request.setAttribute("submittedFor", request.getParameter("submittedFor"));
        }
        if(request.getParameter("forwardTo") != null && !request.getParameter("forwardTo").equals(""))
        {
            request.setAttribute("forwardTo", request.getParameter("forwardTo"));
        }
        request.setAttribute("participantId", "");
    }
}
