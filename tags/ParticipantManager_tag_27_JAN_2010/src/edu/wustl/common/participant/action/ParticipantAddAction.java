

package edu.wustl.common.participant.action;

import java.text.ParseException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import edu.wustl.common.action.CommonAddEditAction;
import edu.wustl.common.actionForm.AbstractActionForm;
import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.bizlogic.DefaultBizLogic;
import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.participant.actionForm.IParticipantForm;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.Utility;
import edu.wustl.common.util.global.CommonServiceLocator;
import edu.wustl.common.util.global.Constants;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.daofactory.DAOConfigFactory;
import edu.wustl.dao.daofactory.IDAOFactory;
import edu.wustl.dao.exception.DAOException;

// TODO: Auto-generated Javadoc

/**
 * @author geeta_jaggal
 *  The Class ParticipantAddAction.
 */
public class ParticipantAddAction extends CommonAddEditAction
{

	/** The logger. */
	private static Logger logger = Logger.getCommonLogger(ParticipantAddAction.class);

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * edu.wustl.common.action.CommonAddEditAction#execute(org.apache.struts
	 * .action.ActionMapping, org.apache.struts.action.ActionForm,
	 * javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
	{
		ActionForward forward = null;
		IParticipantForm participantForm = (IParticipantForm) form;
		SessionDataBean sessionDataBean = (SessionDataBean) request.getSession().getAttribute(
				Constants.SESSION_DATA);
		try
		{
				// Add the new participant
				participantForm.setOperation(Constants.ADD);
				forward = super.execute(mapping, (AbstractActionForm)participantForm, request, response);

				if (!forward.getName().equals("failure"))
				{
					// if for CS eMPI is enable then set the eMPI status as pending if its eligible
					if (ParticipantManagerUtility.csEMPIStatus(participantForm.getId()))
					{
						if (ParticipantManagerUtility.isParticipantValidForEMPI(participantForm.getLastName(), participantForm
								.getFirstName(), Utility.parseDate(participantForm.getBirthDate())))
						{
							// Process participant for CIDER participant matching.
							ParticipantManagerUtility.addParticipantToProcessMessageQueue(sessionDataBean.getUserId(),participantForm.getId());
						}
					}
				}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			logger.info("Error while registering the participant \n");
			logger.info(e.getMessage());

		}
		return forward;
	}





}
