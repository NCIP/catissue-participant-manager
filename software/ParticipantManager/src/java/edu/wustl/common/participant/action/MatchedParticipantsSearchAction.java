
package edu.wustl.common.participant.action;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import edu.wustl.common.action.CommonSearchAction;
import edu.wustl.common.actionForm.AbstractActionForm;
import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.lookup.DefaultLookupResult;
import edu.wustl.common.participant.actionForm.IParticipantForm;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.IRace;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerException;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.Utility;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.query.generator.ColumnValueBean;
import edu.wustl.dao.query.generator.DBTypes;

/**
 * @author geeta_jaggal
 *
 * The Class MatchedParticipantsSearchAction.
 * This class is used for fetching stored matched
 * participant from DB for a particiapnt in the  message board.
 */
public class MatchedParticipantsSearchAction extends CommonSearchAction
{

	/** The Constant logger. */
	private static final Logger LOGGER = Logger
			.getCommonLogger(MatchedParticipantsSearchAction.class);

	/**
	 * For displaying the matched participants from eMPI
	 */
	public ActionForward executeXSS(final ActionMapping mapping, final ActionForm form,
			final HttpServletRequest request, final HttpServletResponse response)
			throws ApplicationException
	{
		ActionForward forward = null;
		final AbstractActionForm participantForm = (AbstractActionForm) form;
		final SessionDataBean sessionDataBean = (SessionDataBean) request.getSession()
				.getAttribute(edu.wustl.common.util.global.Constants.SESSION_DATA);
		final Long userId = sessionDataBean.getUserId();
		final List<Long> participantIds = ParticipantManagerUtility
				.getProcessedMatchedParticipantIds(userId);
		try
		{
			forward = super.executeXSS(mapping, participantForm, request, response);
			if (!forward.getName().equals(edu.wustl.common.util.global.Constants.FAILURE))
			{
				final String obj = request
						.getParameter(edu.wustl.common.util.global.Constants.SYSTEM_IDENTIFIER);
				Long identifier = Long.valueOf(Utility.toLong(obj));
				if (identifier.longValue() == 0L)
				{
					identifier = Long
							.valueOf(edu.wustl.common.util.global.Constants.SYSTEM_IDENTIFIER);
				}
				int indexOfCurrentPart = 0;
				int nextMatchedParticpantIndex = 0;
				if (participantIds.contains(identifier))
				{
					indexOfCurrentPart = participantIds.indexOf(identifier);
				}

				if (indexOfCurrentPart >= 0 && ((participantIds.size() - 1) > indexOfCurrentPart))
				{
					nextMatchedParticpantIndex = indexOfCurrentPart + 1;

				}
				request.getSession().setAttribute(Constants.NEXT_PART_ID_TO_PROCESS,
						participantIds.get(nextMatchedParticpantIndex));
				// fetch the stored matched participant for the participant in the message board
				final IParticipantForm partiForm = (IParticipantForm) form;
				fetchMatchedParticipantsFromDB(identifier.longValue(), request, partiForm);

			}
		}
		catch (Exception e)
		{
			LOGGER.info(e.getMessage());
			throw new ApplicationException(null, e, e.getMessage());
		}
		return forward;
	}

	/**
	 * Gets the session data.
	 *
	 * @param request the request
	 *
	 * @return the session data
	 */
	protected SessionDataBean getSessionData(final HttpServletRequest request)
	{
		return (SessionDataBean) request.getSession().getAttribute(
				edu.wustl.common.util.global.Constants.SESSION_DATA);
	}

	/**
	 * Fetch matched participants from db.
	 *
	 * @param participantId the participant id
	 * @param request the request
	 *
	 * @throws ParseException the parse exception
	 * @throws BizLogicException the biz logic exception
	 * @throws Exception the exception
	 * @throws DAOException the DAO exception
	 * @throws ParticipantManagerException
	 */
	private void fetchMatchedParticipantsFromDB(final long participantId,
			final HttpServletRequest request, final IParticipantForm partiForm)
			throws DAOException, BizLogicException, ParseException, ParticipantManagerException
	{
		JDBCDAO dao = null;
		List<DefaultLookupResult> matchPartpantLst = null;
		try
		{
			final String ssn = partiForm.getSocialSecurityNumberPartA().concat(
					partiForm.getSocialSecurityNumberPartB()).concat(
					partiForm.getSocialSecurityNumberPartC());

			dao = ParticipantManagerUtility.getJDBCDAO();

			final String query = "SELECT * FROM CATISSUE_MATCHED_PARTICIPANT WHERE SEARCHED_PARTICIPANT_ID=? order by ORDER_NO";

			final LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
			columnValueBeanList.add(new ColumnValueBean("SEARCHED_PARTICIPANT_ID", participantId,
					DBTypes.LONG));
			final List matchPartpantLstTemp = dao.executeQuery(query, null, columnValueBeanList);
			if (matchPartpantLstTemp != null && !matchPartpantLstTemp.isEmpty())
			{
				matchPartpantLst = populateParticipantList(matchPartpantLstTemp);
				storeLists(request, matchPartpantLst);

				if ((partiForm.getBirthDate() == null || "".equals(partiForm.getBirthDate()))
						&& (ssn == null || "".equals(ssn)))
				{
					request.setAttribute(Constants.EMPI_GENERATION_FIELDS_INSUFFICIENT,
							Constants.TRUE);
				}
			}
			else
			{
				setStatusMessage(request, "participant.empiid.zero.match.message");
				ParticipantManagerUtility.deleteProcessedParticipant(Long
						.valueOf(participantId));
				request.setAttribute(Constants.ZERO_MATCHES, Constants.TRUE);
				if ((partiForm.getBirthDate() == null || "".equals(partiForm.getBirthDate()))
						&& (ssn == null || "".equals(ssn)))
				{
					setStatusMessage(request, "participant.empiid.generation.incomplete.detail");
				}
			}
			request.setAttribute(Constants.IS_GENERATE_EMPI_PAGE, Constants.TRUE);

		}
		catch (DAOException e)
		{
			LOGGER.info(e.getMessage());
			throw new DAOException(e.getErrorKey(), e, e.getMsgValues());
		}
		finally
		{
			dao.closeSession();
		}
	}

	/**
	 * Sets the status message.
	 *
	 * @param request the new status message
	 *
	 * @throws DAOException the DAO exception
	 */
	private void setStatusMessage(final HttpServletRequest request, String key) throws DAOException
	{
		ActionMessages actionMsgs = (ActionMessages) request
				.getAttribute("org.apache.struts.action.ACTION_MESSAGE");
		if (actionMsgs == null)
		{
			actionMsgs = new ActionMessages();
		}
		actionMsgs.add("org.apache.struts.action.GLOBAL_MESSAGE", new ActionMessage(key));
		saveMessages(request, actionMsgs);
	}

	/**
	 * Populate participant list.
	 *
	 * @param matchPartpantLstTmp the match partpant lst tmp
	 *
	 * @return the list
	 *
	 * @throws ParseException the parse exception
	 * @throws BizLogicException the biz logic exception
	 * @throws ParticipantManagerException
	 * @throws Exception the exception
	 */
	private List<DefaultLookupResult> populateParticipantList(final List matchPartpantLstTmp)
			throws BizLogicException, ParseException, ParticipantManagerException

	{
		final List<DefaultLookupResult> matchPartpantLst = new ArrayList<DefaultLookupResult>();
		for (int i = 0; i < matchPartpantLstTmp.size(); i++)
		{
			final List participantValueList = (List) matchPartpantLstTmp.get(i);
			if (!participantValueList.isEmpty() && participantValueList.get(0) != null
					&& !"".equals(participantValueList.get(0)))
			{
				final IParticipant participant = getParticipantObj(participantValueList);
				final DefaultLookupResult result = new DefaultLookupResult();
				result.setObject(participant);
				matchPartpantLst.add(result);
			}
		}
		return matchPartpantLst;
	}

	/**
	 * Gets the participant obj.
	 *
	 * @param participantValueList the participant value list
	 *
	 * @return the participant obj
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws ParseException the parse exception
	 * @throws ParticipantManagerException
	 * @throws Exception the exception
	 */
	private IParticipant getParticipantObj(final List participantValueList)
			throws BizLogicException, ParseException, ParticipantManagerException
	{
		final IParticipant participant = (IParticipant) ParticipantManagerUtility
				.getParticipantInstance();

		participant.setId(Long.valueOf((String) participantValueList.get(0)));
		participant.setEmpiId((String) participantValueList.get(1));

		final String lastName = (String) participantValueList.get(2);
		final String firstName = (String) participantValueList.get(3);
		final String middleName = (String) participantValueList.get(4);

		participant.setLastName(ParticipantManagerUtility.modifyNameWithProperCase(lastName));
		participant.setFirstName(ParticipantManagerUtility.modifyNameWithProperCase(firstName));
		participant.setMiddleName(ParticipantManagerUtility.modifyNameWithProperCase(middleName));
		if (participantValueList.get(5) != null && !"".equals(participantValueList.get(5)))
		{
			final String dateStr = (String) participantValueList.get(5);
			final Date date = Utility.parseDate(dateStr, Constants.DATE_FORMAT);
			participant.setBirthDate(date);
		}
		participant.setGender((String) participantValueList.get(6));
		participant.setSocialSecurityNumber((String) participantValueList.get(7));
		participant.setActivityStatus((String) participantValueList.get(8));
		if (participantValueList.get(9) != null && !("".equals(participantValueList.get(9))))
		{
			final String dateStr = (String) participantValueList.get(9);
			final Date date = Utility.parseDate(dateStr, Constants.DATE_FORMAT);
			participant.setDeathDate(date);
		}
		participant.setVitalStatus((String) participantValueList.get(10));
		final String mrnString = (String) participantValueList.get(11);
		if (mrnString != null && !"".equals(mrnString))
		{
			final Collection partiMediIdColn = getPartiMediIdColnCollection(mrnString);
			participant.setParticipantMedicalIdentifierCollection(partiMediIdColn);
		}
		final String raceString = (String) participantValueList.get(12);
		if (raceString != null && !"".equals(raceString))
		{
			final Collection raceCollection = getRaceCollection(raceString);
			participant.setRaceCollection(raceCollection);
		}
		return participant;
	}

	/**
	 * Gets the race collection.
	 *
	 * @param raceString the race string
	 *
	 * @return the race collection
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws ParticipantManagerException
	 */
	private Collection getRaceCollection(final String raceString) throws BizLogicException,
			ParticipantManagerException
	{
		final Collection<IRace> raceCollection = new LinkedHashSet<IRace>();
		final String racevalues[] = raceString.split(",");
		for (int i = 0; i < racevalues.length; i++)
		{
			final IRace race = (IRace) ParticipantManagerUtility.getRaceInstance();
			final String raceName = racevalues[i];
			race.setRaceName(raceName);
			raceCollection.add(race);
		}
		return raceCollection;
	}

	/**
	 * Gets the parti medi id coln collection.
	 *
	 * @param mrnString the mrn string
	 *
	 * @return the parti medi id coln collection
	 * mrnId + ":" + facilityId + ":" + siteName;
	 * @throws BizLogicException the biz logic exception
	 * @throws ParticipantManagerException
	 * @throws Exception the exception
	 */
	private Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> getPartiMediIdColnCollection(
			final String mrnString) throws BizLogicException, ParticipantManagerException
	{
		final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> partiMediIdColn = new LinkedHashSet<IParticipantMedicalIdentifier<IParticipant, ISite>>();
		final String values[] = mrnString.split(",");
		for (int i = 0; i < values.length; i++)
		{
			final String value = values[i];
			final IParticipantMedicalIdentifier<IParticipant, ISite> participantMedicalIdentifier = getParticipantMedicalIdentifierObj(value);
			if (participantMedicalIdentifier != null)
			{
				partiMediIdColn.add(participantMedicalIdentifier);
			}
		}

		return partiMediIdColn;
	}

	/**
	 * Gets the participant medical identifier obj.
	 *
	 * @param value the value
	 *
	 * @return the participant medical identifier obj
	 * mrnId + ":" + facilityId + ":" + siteName;
	 * @throws BizLogicException the biz logic exception
	 * @throws ParticipantManagerException
	 * @throws Exception the exception
	 */
	private IParticipantMedicalIdentifier<IParticipant, ISite> getParticipantMedicalIdentifierObj(
			final String value) throws BizLogicException, ParticipantManagerException

	{
		IParticipantMedicalIdentifier<IParticipant, ISite> participantMedicalIdentifier = null;
		final String values[] = value.split(":");
		final String mrn = values[0];
		final String facilityId = values[1];
		final ISite siteObj = ParticipantManagerUtility.getSiteObject(facilityId);
		if (siteObj != null)
		{
			participantMedicalIdentifier = ParticipantManagerUtility.getPMIInstance();
			participantMedicalIdentifier.setMedicalRecordNumber(mrn);
			siteObj.setFacilityId(facilityId);
			participantMedicalIdentifier.setSite(siteObj);
		}
		return participantMedicalIdentifier;
	}

	/**
	 * Store lists.
	 *
	 * @param request the request
	 * @param matchPartpantLst the match partpant lst
	 *
	 * @throws DAOException the DAO exception
	 */
	private void storeLists(final HttpServletRequest request,
			final List<DefaultLookupResult> matchPartpantLst) throws DAOException
	{
		final ActionMessages messages = new ActionMessages();
		final List<String> columnList = ParticipantManagerUtility.getColumnHeadingList();
		request.setAttribute(edu.wustl.common.util.global.Constants.SPREADSHEET_COLUMN_LIST,
				columnList);
		final List<List<String>> pcpantDisplayLst = ParticipantManagerUtility
				.getParticipantDisplayList(matchPartpantLst);
		request.setAttribute(Constants.SPREADSHEET_DATA_LIST, pcpantDisplayLst);
		final HttpSession session = request.getSession();
		session.setAttribute("MatchedParticpant", matchPartpantLst);
		request.setAttribute(Constants.MATCHED_PARTICIPANTS_FOUND_FROM_EMPI, Constants.TRUE);
		if (request.getAttribute("continueLookup") == null)
		{
			saveMessages(request, messages);
		}
	}
}
