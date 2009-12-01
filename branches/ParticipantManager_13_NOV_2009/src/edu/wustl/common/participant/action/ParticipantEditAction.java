
package edu.wustl.common.participant.action;

import java.text.ParseException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import edu.wustl.common.action.CommonAddEditAction;
import edu.wustl.common.actionForm.AbstractActionForm;
import edu.wustl.common.domain.AbstractDomainObject;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.participant.actionForm.IParticipantForm;
import edu.wustl.common.participant.bizlogic.EMPIParticipantRegistrationBizLogic;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.Utility;
import edu.wustl.common.util.global.CommonServiceLocator;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.daofactory.DAOConfigFactory;
import edu.wustl.dao.daofactory.IDAOFactory;
import edu.wustl.dao.exception.DAOException;

/**
 * @author geeta_jaggal.
 * The Class ParticipantEditAction : User for updating the particiapnt.
 * If the EMPI is off for CS simply edit the participant else if its not in pending state
 *
 */
public class ParticipantEditAction extends CommonAddEditAction
{

	private static final Logger logger = Logger.getCommonLogger(ParticipantEditAction.class);

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
		try
		{

			// if the eMPI is on for Cs .. generate new eMPI id for the new
			// values.
			if (ParticipantManagerUtility.isEMPIEnable(participantForm.getId()))
			{
				// if the
				if (Constants.EMPI_ID_CREATED.equals(participantForm.getEmpiIdStatus()))
				{
					if (ParticipantManagerUtility.isParticipantValidForEMPI(participantForm
							.getLastName(), participantForm.getFirstName(), Utility
							.parseDate(participantForm.getBirthDate())))
					{
						participantForm.setEmpiIdStatus(Constants.EMPI_ID_PENDING);
					}
					forward = super.execute(mapping, (AbstractActionForm) participantForm, request,
							response);
					if (!forward.getName().equals(edu.wustl.common.util.global.Constants.FAILURE))
					{
						// register new patient in EMPI to generate new eMPI id for the updated participant
						regNewPatientToEMPI(participantForm);
					}
				}
				else if (Constants.EMPI_ID_PENDING.equals(participantForm.getEmpiIdStatus()))
				{
					forward = mapping.findForward(edu.wustl.common.util.global.Constants.SUCCESS);
				}
				else
				{
					forward = super.execute(mapping, (AbstractActionForm) participantForm, request,
							response);
				}
			}
			else
			{
				// if teh eMPI is OFF for Cs then simply update the participant.
				forward = super.execute(mapping, (AbstractActionForm) participantForm, request,
						response);
			}

		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			logger.info(e.getMessage());
		}
		return forward;
	}

	/**
	 * Reg new patient to empi.
	 *
	 * @param participantForm
	 *            the participant form
	 *
	 * @throws BizLogicException
	 *             the biz logic exception
	 * @throws ParseException
	 *             the parse exception
	 * @throws Exception
	 *             the exception
	 */
	private void regNewPatientToEMPI(IParticipantForm participantForm) throws BizLogicException,
			ParseException, Exception
	{
		String permanentPartiId = null;
		String tempararyPartiId = null;
		String oldeMPIId = null;
		oldeMPIId = participantForm.getEmpiId();
		tempararyPartiId = participantForm.getId() + "T";
		if (ParticipantManagerUtility.isParticipantValidForEMPI(participantForm.getLastName(),
				participantForm.getFirstName(), Utility.parseDate(participantForm.getBirthDate())))
		{
			if (oldeMPIId != null && oldeMPIId != "")
			{
				permanentPartiId = String.valueOf(participantForm.getId());
				mapParticipantId(oldeMPIId, permanentPartiId, tempararyPartiId);
			}
			if (!participantForm.getEmpiIdStatus().equals(Constants.EMPI_ID_CREATED))
			{
				sendHL7RegMes(participantForm, tempararyPartiId);
			}
		}
	}

	/**
	 * Map participant id.
	 *
	 * @param oldeMPIId
	 *            the olde mpi id
	 * @param permanentPartiId
	 *            the permanent parti id
	 * @param tempararyPartiId
	 *            the temparary parti id
	 *
	 * @throws DAOException
	 *             the DAO exception
	 */
	private void mapParticipantId(String oldeMPIId, String permanentPartiId, String tempararyPartiId)
			throws DAOException
	{
		JDBCDAO jdbcDao = null;
		try
		{
			jdbcDao = ParticipantManagerUtility.getJDBCDAO();
			String sql = "INSERT INTO PARTICIPANT_EMPI_ID_MAPPING VALUES('" + permanentPartiId
					+ "','" + tempararyPartiId + "','" + oldeMPIId + "')";
			jdbcDao.executeUpdate(sql);
			jdbcDao.commit();
		}
		catch (DAOException e)
		{
			logger.info("ERROE WHILE UPDATING THE PARTICIPANT EMPI STATUS");
			throw new DAOException(e.getErrorKey(), e, e.getMsgValues());
		}
		finally
		{
			jdbcDao.closeSession();
		}
	}

	/**
	 * Send h l7 reg mes.
	 *
	 * @param participantForm
	 *            the participant form
	 * @param tempararyPartiId
	 *            the temparary parti id
	 *
	 * @throws BizLogicException
	 *             the biz logic exception
	 * @throws Exception
	 *             the exception
	 */
	private void sendHL7RegMes(IParticipantForm participantForm, String tempararyPartiId)
			throws BizLogicException, Exception
	{

		IParticipant participant = (IParticipant) ParticipantManagerUtility
				.getParticipantInstance();
		((AbstractDomainObject) participant).setAllValues((AbstractActionForm) participantForm);
		participant.setId(participantForm.getId());

		EMPIParticipantRegistrationBizLogic eMPIPartiReg = new EMPIParticipantRegistrationBizLogic();
		eMPIPartiReg.setTempMrnId(tempararyPartiId);
		eMPIPartiReg.registerPatientToeMPI(participant);
	}
}
