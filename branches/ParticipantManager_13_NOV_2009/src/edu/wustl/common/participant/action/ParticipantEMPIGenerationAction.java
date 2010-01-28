/*
 *  Class For generating EMPI for the participant
 */

package edu.wustl.common.participant.action;

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
import edu.wustl.common.domain.AbstractDomainObject;
import edu.wustl.common.exception.AssignDataException;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.participant.actionForm.IParticipantForm;
import edu.wustl.common.participant.bizlogic.EMPIParticipantRegistrationBizLogic;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.Utility;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.exception.DAOException;


/**
 * The Class ParticipantEMPIGenerationAction.
 *
 * @author geeta_jaggal
 * The Class ParticipantEMPIGenerationAction : used for generating eMPI for participant.
 */
public class ParticipantEMPIGenerationAction extends CommonAddEditAction
{

	/** The logger. */
	private static final  Logger LOGGER = Logger.getCommonLogger(ParticipantEMPIGenerationAction.class);

	/**
	 *  Method for generating eMPI id for the participant.
	 */
	public ActionForward executeXSS(final ActionMapping mapping, final ActionForm form,
			final HttpServletRequest request, final HttpServletResponse response)
	{
		ActionForward forward = null;
		final IParticipantForm participantForm = (IParticipantForm) form;
		final String isGenerateHL7 = (String) request.getParameter("isGenerateHL7");
		final String isGenerateEMPID = request.getParameter("isGenerateEMPIID");
		try
		{
			// If user selects ignore and generate the eMPI
			if (isGenerateHL7.equalsIgnoreCase(Constants.YES))
			{
				if (ParticipantManagerUtility.isParticipantValidForEMPI(participantForm
						.getLastName(), participantForm.getFirstName(), Utility
						.parseDate(participantForm.getBirthDate())))
				{
					participantForm.setOperation(edu.wustl.common.util.global.Constants.EDIT);
					participantForm.setEmpiIdStatus(Constants.EMPI_ID_PENDING);
					forward = super.executeXSS(mapping, (AbstractActionForm) participantForm, request,
							response);
					if (!forward.getName().equals(edu.wustl.common.util.global.Constants.FAILURE))
					{
						// Send the registration message to CDR.
						registerPatientToEMPI(request, participantForm);
						// Delete that participant from the processed message
						// queue.
						ParticipantManagerUtility.deleteProcessedParticipant(participantForm
								.getId());
					}
				}
			}
			else if (isGenerateEMPID.equals(Constants.YES))
			{
				// chech the status first if its waiting for generating eMPI thn
				// can't edit it util it get eMPi id.
				final String eMPIStatus = ParticipantManagerUtility.getPartiEMPIStatus(participantForm
						.getId());
				if (eMPIStatus.equals(Constants.EMPI_ID_PENDING))
				{
					forward= mapping.findForward(edu.wustl.common.util.global.Constants.SUCCESS);
				}else{
					generateEMPI(request, participantForm);
					forward = mapping.findForward(edu.wustl.common.util.global.Constants.SUCCESS);
				}
			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			LOGGER.info("Error while generating EMPI for the participant \n");
			LOGGER.info(e.getMessage());
		}
		return forward;

	}

	/**
	 * Register patient to empi.
	 *
	 * @param request the request
	 * @param participantForm the participant form
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws AssignDataException the assign data exception
	 */
	private void registerPatientToEMPI(final HttpServletRequest request, final IParticipantForm participantForm)
			throws BizLogicException, AssignDataException
	{
		final IParticipant participant = (IParticipant) ParticipantManagerUtility
				.getParticipantInstance();
		((AbstractDomainObject) participant).setAllValues((AbstractActionForm) participantForm);
		participant.setId(participantForm.getId());
		final EMPIParticipantRegistrationBizLogic bizLogic = new EMPIParticipantRegistrationBizLogic();
		String regStatusMessage = "";
		try
		{
			bizLogic.registerPatientToeMPI(participant);
			regStatusMessage = edu.wustl.common.util.global.Constants.SUCCESS;
		}
		catch (Exception e)
		{
			regStatusMessage = edu.wustl.common.util.global.Constants.FAILURE;
			regStatusMessage = e.getMessage();
		}
		setStatusMessage(request, regStatusMessage);
	}

	/**
	 * Sets the status message.
	 *
	 * @param request the request
	 * @param regStatus the reg status
	 */
	private void setStatusMessage(final HttpServletRequest request, final String regStatus)
	{
		final ActionMessages actionMsgs = new ActionMessages();
		if (regStatus.equals(edu.wustl.common.util.global.Constants.SUCCESS))
		{
			actionMsgs.add(ActionErrors.GLOBAL_MESSAGE, new ActionMessage(
					"participant.empi.registration.success.message"));
		}
		else if (regStatus.equals(edu.wustl.common.util.global.Constants.FAILURE))
		{

			ActionErrors actionErrors = (ActionErrors) request.getAttribute(Globals.ERROR_KEY);
			if (actionErrors == null)
			{
				actionErrors = new ActionErrors();
			}
			final ActionError actionError = new ActionError(
					"participant.empi.registration.failure.message", regStatus);
			actionErrors.add(ActionErrors.GLOBAL_ERROR, actionError);
			saveErrors(request, actionErrors);
		}
		else
		{
			actionMsgs.add(ActionErrors.GLOBAL_MESSAGE, new ActionMessage(
					"participant.empi.registration.notvalid.message"));
		}
		saveMessages(request, actionMsgs);
	}

	/**
	 * Generate empi.
	 *
	 * @param request the request
	 * @param participantForm the participant form
	 *
	 * @throws DAOException the DAO exception
	 */
	private void generateEMPI(final HttpServletRequest request, final IParticipantForm participantForm)
			throws DAOException
	{
		final SessionDataBean sessionDataBean = (SessionDataBean) request.getSession().getAttribute(
				Constants.SESSION_DATA);
		try
		{
			// Process participant for CIDER participant matching.
			ParticipantManagerUtility.addParticipantToProcessMessageQueue(sessionDataBean
					.getUserId(), participantForm.getId());
			setStatusMessage(request);
		}
		catch (DAOException e)
		{
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}
	}

	/**
	 * Sets the message.
	 *
	 * @param request the new message
	 */
	private void setStatusMessage(final HttpServletRequest request)
	{
		ActionMessages actionMsgs = (ActionMessages) request.getAttribute(Globals.MESSAGE_KEY);
		if (actionMsgs == null)
		{
			actionMsgs = new ActionMessages();
		}
		actionMsgs.add(ActionErrors.GLOBAL_MESSAGE, new ActionMessage(
				"participant.empiid.generation.message"));
		saveMessages(request, actionMsgs);
	}

}
