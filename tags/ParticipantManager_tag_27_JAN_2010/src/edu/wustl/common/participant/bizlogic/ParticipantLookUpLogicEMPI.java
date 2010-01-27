package edu.wustl.common.participant.bizlogic;

import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.lookup.*;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.patientLookUp.domain.PatientInformation;
import edu.wustl.patientLookUp.lookUpServiceBizLogic.PatientInfoLookUpService;
import edu.wustl.patientLookUp.util.PatientLookUpFactory;
import edu.wustl.patientLookUp.util.PatientLookupException;
import java.util.ArrayList;
import java.util.List;

public class ParticipantLookUpLogicEMPI implements LookupLogic {

	protected int cutoffPoints;
	protected int totalPoints;
	protected int maxNoOfParticipantsToReturn;

	public ParticipantLookUpLogicEMPI() {
	}

	public List lookup(LookupParameters params) throws Exception {
		if (params == null) {
			throw new Exception("Params can not be null");
		} else {
			DefaultLookupParameters participantParams = (DefaultLookupParameters) params;
			IParticipant participant = (IParticipant) participantParams
					.getObject();
			PatientInformation patientInformation = ParticipantManagerUtility
					.populatePatientObject(participant);
			cutoffPoints = Integer.valueOf(
					XMLPropertyHandler.getValue("empi.threshold")).intValue();
			maxNoOfParticipantsToReturn = Integer.valueOf(
					XMLPropertyHandler.getValue("empi.MaxNoOfPatients"))
					.intValue();
			List empiParticipantsList = searchMatchingParticipantFromEMPI(
					participant, patientInformation);
			return empiParticipantsList;
		}
	}

	protected List searchMatchingParticipantFromEMPI(IParticipant participant,
			PatientInformation patientInformation) throws BizLogicException,
			PatientLookupException {
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
		cutoffPoints = Integer.valueOf(
				XMLPropertyHandler.getValue("empi.threshold")).intValue();
		maxNoOfPatients = Integer.valueOf(
				XMLPropertyHandler.getValue("empi.MaxNoOfPatients")).intValue();
		List participantsEMPI;
		try {
			PatientInfoLookUpService lookUpEMPI = new PatientInfoLookUpService();
			edu.wustl.patientLookUp.queryExecutor.IQueryExecutor xQueyExecutor = PatientLookUpFactory
					.getQueryExecutorImpl(dbURL, dbUser, dbPassword, dbDriver,
							dbSchema);
			participantsEMPI = lookUpEMPI.patientLookupService(
					patientInformation, xQueyExecutor, cutoffPoints,
					maxNoOfPatients);
			if (participantsEMPI != null && participantsEMPI.size() > 0) {
				for (int i = 0; i < participantsEMPI.size(); i++) {
					PatientInformation empiPatientInformation = (PatientInformation) participantsEMPI
							.get(i);
					empiPatientInformation.setActivityStatus("Active");
					empiPatientInformation.setVitalStatus("");
					empiPatientInformation.setIsFromEMPI("YES");
					if (empiPatientInformation.getSsn() != null
							&& empiPatientInformation.getSsn() != "") {
						String ssn = ParticipantManagerUtility
								.getSSN(empiPatientInformation.getSsn());
						empiPatientInformation.setSsn(ssn);
					} else {
						empiPatientInformation.setSsn("");
					}
					empiPatientInformation.setId(new Long(0 - i));
					matchingParticipantsList.add(empiPatientInformation);
				}

			}
			return participantsEMPI;
		} catch (Exception e) {
			e.printStackTrace();
			throw new PatientLookupException(e.getMessage(), e);
		}

	}
}
