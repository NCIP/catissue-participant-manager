/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue-participant-manager/LICENSE.txt for details.
 */

package edu.wustl.common.participant.bizlogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.lookup.DefaultLookupParameters;
import edu.wustl.common.lookup.LookupParameters;
import edu.wustl.common.participant.client.IParticipantManagerLookupLogic;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.common.util.logger.Logger;
import edu.wustl.patientLookUp.domain.PatientInformation;
import edu.wustl.patientLookUp.lookUpServiceBizLogic.PatientInfoLookUpService;
import edu.wustl.patientLookUp.util.PatientLookUpFactory;
import edu.wustl.patientLookUp.util.PatientLookupException;

// TODO: Auto-generated Javadoc
/**
 * The Class ParticipantLookUpLogicEMPI.
 */
public class ParticipantLookUpLogicEMPI implements IParticipantManagerLookupLogic
{

	/** The cutoff points. */
	protected int cutoffPoints;

	/** The total points. */
	protected static transient int totalPoints;

	/** The max no of participants to return. */
	protected static transient int maxPartipntsToReturn;

	/** The Constant logger. */
	private static final Logger LOGGER = Logger.getCommonLogger(ParticipantLookUpLogicEMPI.class);

	/* (non-Javadoc)
	 * @see edu.wustl.common.lookup.LookupLogic#lookup(edu.wustl.common.lookup.LookupParameters)
	 */
	public List lookup(final LookupParameters params,Set<Long> csSet) throws PatientLookupException
	{
		cutoffPoints = getCutOffPoints(params);
		return  lookup(params);
	}

	/**
	 * Cut off point can be given explicitely or used default.
	 * If threshhold is given then it will be used or default value will be returned.
	 * @param threshHold threshhold passed by caller
	 * @return threshhold if valida value is present else will return default value.
	 */
	private int getCutOffPoints(LookupParameters params)
	{
		final DefaultLookupParameters participantParams = (DefaultLookupParameters) params;
		int cutoff;
		final Integer threshold = participantParams.getThreshold();
		if(threshold!=null && threshold>0)
		{
			cutoff=threshold;

		}
		else
		{
			cutoff = Integer
				.valueOf(XMLPropertyHandler.getValue(Constants.EMPITHRESHOLD));
		}
		return cutoff;
	}

	/**
	 * Search matching participant from empi.
	 *
	 * @param participant the participant
	 * @param patientInformation the patient information
	 *
	 * @return the list
	 *
	 * @throws BizLogicException the biz logic exception
	 * @throws PatientLookupException the patient lookup exception
	 */
	protected List<PatientInformation> searchMatchingParticipantFromEMPI(final IParticipant participant,
			final PatientInformation patientInfo) throws BizLogicException, PatientLookupException
	{

		final String dbURL = XMLPropertyHandler.getValue(Constants.EMPIDBURL);
		final String dbUser = XMLPropertyHandler.getValue(Constants.EMPIDBUSERNAME);
		final String dbPassword = XMLPropertyHandler.getValue(Constants.EMPIDBUSERPASSWORD);
		final String dbDriver = XMLPropertyHandler.getValue(Constants.EMPIDBDRIVERNAME);
		final String dbSchema = XMLPropertyHandler.getValue(Constants.EMPIDBSCHEMA);

		try
		{
			final PatientInfoLookUpService lookUpEMPI = new PatientInfoLookUpService();
			final edu.wustl.patientLookUp.queryExecutor.IQueryExecutor xQueyExecutor = PatientLookUpFactory
					.getQueryExecutorImpl(dbURL, dbUser, dbPassword, dbDriver, dbSchema);
			final List<PatientInformation> participantsEMPI = lookUpEMPI.patientLookupService(
					patientInfo, xQueyExecutor, cutoffPoints, maxPartipntsToReturn);

			final List<PatientInformation> matchingParticipantsList = processMatchedList(participantsEMPI);

			return matchingParticipantsList;
		}
		catch (Exception e)
		{
			LOGGER.info(e.getMessage());
			throw new PatientLookupException(e.getMessage(), e);
		}

	}

	private List<PatientInformation> processMatchedList(final List<PatientInformation> participantsEMPI)
	{
		final List<PatientInformation> matchedPartipntsList = new ArrayList<PatientInformation>();
		String lastName = null;
		String firstName = null;
		if (participantsEMPI != null && !participantsEMPI.isEmpty())
		{
			for (int i = 0; i < participantsEMPI.size(); i++)
			{
				final PatientInformation empiPatientInformation = participantsEMPI
						.get(i);
				lastName = empiPatientInformation.getLastName();
				firstName = empiPatientInformation.getFirstName();
				empiPatientInformation.setLastName(lastName.toLowerCase());
				empiPatientInformation.setFirstName(firstName.toLowerCase());
				empiPatientInformation.setActivityStatus("Active");
				empiPatientInformation.setVitalStatus("");
				if (empiPatientInformation.getSsn() != null
						&& !"".equals(empiPatientInformation.getSsn()))
				{
					final String ssn = ParticipantManagerUtility.getSSN(empiPatientInformation.getSsn());
					empiPatientInformation.setSsn(ssn);
				}
				else
				{
					empiPatientInformation.setSsn("");
				}

				empiPatientInformation.setId(Long.valueOf((0 - i)));
				matchedPartipntsList.add(empiPatientInformation);
			}

		}
		return matchedPartipntsList;
	}

	public List lookup(LookupParameters params) throws PatientLookupException
	{
		// TODO Auto-generated method stub
		List empiParticipantsList = null;
		try
		{
			if (params == null)
			{
				throw new PatientLookupException("Params can not be null", null);
			}
			else
			{
				final DefaultLookupParameters participantParams = (DefaultLookupParameters) params;
				final IParticipant participant = (IParticipant) participantParams.getObject();
				final PatientInformation patientInfo = ParticipantManagerUtility
						.populatePatientObject(participant,null);

				maxPartipntsToReturn = Integer.valueOf(XMLPropertyHandler
						.getValue(Constants.EMPIMAXNOOFPATIENS));
				empiParticipantsList = searchMatchingParticipantFromEMPI(participant,
						patientInfo);

			}
		}
		catch (Exception e)
		{
			LOGGER.info(e.getMessage());
			throw new PatientLookupException(e.getMessage(), e);
		}
		return empiParticipantsList;
	}

	public boolean isCallToLookupLogicNeeded(IParticipant participant)
	{
		boolean flag = true;
		if ((participant.getFirstName() == null || participant.getFirstName().length() == 0)
				&& (participant.getMiddleName() == null || participant.getMiddleName().length() == 0)
				&& (participant.getLastName() == null || participant.getLastName().length() == 0)
				&& (participant.getSocialSecurityNumber() == null || participant
						.getSocialSecurityNumber().length() == 0)
				&& participant.getBirthDate() == null
				&& (participant.getParticipantMedicalIdentifierCollection() == null || participant
						.getParticipantMedicalIdentifierCollection().size() == 0))
		{
			flag = false;
		}
		return flag;
	}
}
