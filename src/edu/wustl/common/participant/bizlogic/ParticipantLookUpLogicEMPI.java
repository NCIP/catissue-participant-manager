
package edu.wustl.common.participant.bizlogic;

import java.util.ArrayList;
import java.util.List;

import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.lookup.DefaultLookupParameters;
import edu.wustl.common.lookup.LookupLogic;
import edu.wustl.common.lookup.LookupParameters;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.patientLookUp.domain.PatientInformation;
import edu.wustl.patientLookUp.lookUpServiceBizLogic.PatientInfoLookUpService;
import edu.wustl.patientLookUp.util.PatientLookUpFactory;
import edu.wustl.patientLookUp.util.PatientLookupException;

// TODO: Auto-generated Javadoc
/**
 * The Class ParticipantLookUpLogicEMPI.
 */
public class ParticipantLookUpLogicEMPI implements LookupLogic
{

	/** The cutoff points. */
	protected int cutoffPoints;

	/** The total points. */
	protected int totalPoints;

	/** The max no of participants to return. */
	protected int maxNoOfParticipantsToReturn;

	/**
	 * Instantiates a new participant look up logic empi.
	 */
	public ParticipantLookUpLogicEMPI()
	{
	}

	/* (non-Javadoc)
	 * @see edu.wustl.common.lookup.LookupLogic#lookup(edu.wustl.common.lookup.LookupParameters)
	 */
	public List lookup(LookupParameters params) throws Exception
	{
		if (params == null)
		{
			throw new Exception("Params can not be null");
		}
		else
		{
			DefaultLookupParameters participantParams = (DefaultLookupParameters) params;
			IParticipant participant = (IParticipant) participantParams.getObject();
			PatientInformation patientInformation = ParticipantManagerUtility
					.populatePatientObject(participant);
			cutoffPoints = Integer.valueOf(XMLPropertyHandler.getValue("empi.threshold"))
					.intValue();
			maxNoOfParticipantsToReturn = Integer.valueOf(
					XMLPropertyHandler.getValue("empi.MaxNoOfPatients")).intValue();
			List empiParticipantsList = searchMatchingParticipantFromEMPI(participant,
					patientInformation);
			return empiParticipantsList;
		}
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
	protected List searchMatchingParticipantFromEMPI(IParticipant participant,
			PatientInformation patientInformation) throws BizLogicException, PatientLookupException
	{
		List matchingParticipantsList;
		String dbURL;
		String dbUser;
		String dbPassword;
		String dbDriver;
		String dbSchema;
		int maxNoOfPatients;
		matchingParticipantsList = new ArrayList();
		dbURL = XMLPropertyHandler.getValue("empi.DBURL");
		dbUser = XMLPropertyHandler.getValue("empi.DBUserName");
		dbPassword = XMLPropertyHandler.getValue("empi.DBUserPassword");
		dbDriver = XMLPropertyHandler.getValue("empi.DBDriverName");
		dbSchema = XMLPropertyHandler.getValue("empi.DBSchema");
		cutoffPoints = Integer.valueOf(XMLPropertyHandler.getValue("empi.threshold")).intValue();
		maxNoOfPatients = Integer.valueOf(XMLPropertyHandler.getValue("empi.MaxNoOfPatients"))
				.intValue();
		List participantsEMPI;
		String lastName=null;
		String firstName=null;
		try
		{
			PatientInfoLookUpService lookUpEMPI = new PatientInfoLookUpService();
			edu.wustl.patientLookUp.queryExecutor.IQueryExecutor xQueyExecutor = PatientLookUpFactory
					.getQueryExecutorImpl(dbURL, dbUser, dbPassword, dbDriver, dbSchema);
			participantsEMPI = lookUpEMPI.patientLookupService(patientInformation, xQueyExecutor,
					cutoffPoints, maxNoOfPatients);
			if (participantsEMPI != null && participantsEMPI.size() > 0)
			{
				for (int i = 0; i < participantsEMPI.size(); i++)
				{
					PatientInformation empiPatientInformation = (PatientInformation) participantsEMPI
							.get(i);
					lastName =empiPatientInformation.getLastName();
					firstName=empiPatientInformation.getFirstName();
					empiPatientInformation.setLastName(lastName.toLowerCase());
					empiPatientInformation.setFirstName(firstName.toLowerCase());
					empiPatientInformation.setActivityStatus("Active");
					empiPatientInformation.setVitalStatus("");
					empiPatientInformation.setIsFromEMPI("YES");
					if (empiPatientInformation.getSsn() != null
							&& empiPatientInformation.getSsn() != "")
					{
						String ssn = ParticipantManagerUtility.getSSN(empiPatientInformation
								.getSsn());
						empiPatientInformation.setSsn(ssn);
					}
					else
					{
						empiPatientInformation.setSsn("");
					}

					empiPatientInformation.setId(new Long(0 - i));
					matchingParticipantsList.add(empiPatientInformation);
				}

			}
			return participantsEMPI;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new PatientLookupException(e.getMessage(), e);
		}

	}
}
