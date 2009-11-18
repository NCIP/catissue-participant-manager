
package edu.wustl.common.participant.bizlogic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.lookup.DefaultLookupParameters;
import edu.wustl.common.lookup.DefaultLookupResult;
import edu.wustl.common.lookup.LookupLogic;
import edu.wustl.common.lookup.LookupParameters;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.common.util.global.CommonServiceLocator;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.daofactory.DAOConfigFactory;
import edu.wustl.dao.daofactory.IDAOFactory;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.patientLookUp.domain.PatientInformation;
import edu.wustl.patientLookUp.lookUpServiceBizLogic.PatientInfoLookUpService;
import edu.wustl.patientLookUp.queryExecutor.SQLQueryExecutorImpl;
import edu.wustl.patientLookUp.util.PatientLookupException;

// TODO: Auto-generated Javadoc
/**
 * @author geeta_jaggal.
 *
 * The Class ParticipantLookupLogic.
 *
 * 		This class is for finding out the matching participant with respect
 * 		to given participant. It implements the lookUp method of LookupLogic
 * 		interface which returns the list of all matching participants to the
 * 		given participant.
 */
public class ParticipantLookupLogic implements LookupLogic
{

	/** The CUTOFFPOINTSFROMPROPERTIES. */
	protected static int CUTOFFPOINTSFROMPROPERTIES;

	/** The TOTALPOINTSFROMPROPERTIES. */
	protected static int TOTALPOINTSFROMPROPERTIES;

	/** The is ssn or pmi. */
	protected boolean isSSNOrPMI;

	/** The exact match. */
	protected boolean exactMatch;

	/** The cutoff points. */
	protected int cutoffPoints;

	/** The total points. */
	protected int totalPoints;

	/** The max no of participants to return. */
	protected int maxNoOfParticipantsToReturn;

	/**
	 * Instantiates a new participant lookup logic.
	 */
	public ParticipantLookupLogic()
	{
		isSSNOrPMI = false;
		exactMatch = true;
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
			cutoffPoints = Integer.valueOf(XMLPropertyHandler.getValue(Constants.EMPITHRESHOLD))
					.intValue();
			maxNoOfParticipantsToReturn = Integer.valueOf(
					XMLPropertyHandler.getValue(Constants.EMPIMAXNOOFPATIENS)).intValue();
			List matchingParticipantsList = searchMatchingParticipant(patientInformation);
			return matchingParticipantsList;
		}
	}

	/**
	 * Search matching participant.
	 *
	 * @param patientInformation the patient information
	 *
	 * @return the list
	 *
	 * @throws PatientLookupException the patient lookup exception
	 * @throws ApplicationException the application exception
	 */
	protected List searchMatchingParticipant(PatientInformation patientInformation)
			throws PatientLookupException, ApplicationException
	{
		List matchingParticipantsList = new ArrayList();
		PatientInfoLookUpService patientLookupObj = new PatientInfoLookUpService();
		JDBCDAO jdbcDAO =  null;
		try
		{
			jdbcDAO = ParticipantManagerUtility.getJDBCDAO();
			edu.wustl.patientLookUp.queryExecutor.IQueryExecutor queryExecutor = new SQLQueryExecutorImpl(
					jdbcDAO);
			List patientInfoList = patientLookupObj.patientLookupService(patientInformation,
					queryExecutor, cutoffPoints, maxNoOfParticipantsToReturn);
			if (patientInfoList != null && !patientInfoList.isEmpty())
			{
				for (int i = 0; i < patientInfoList.size(); i++)
				{
					patientInformation = (PatientInformation) patientInfoList.get(i);
					DefaultLookupResult result = new DefaultLookupResult();
					IParticipant partcipantNew = (IParticipant) ParticipantManagerUtility
							.getParticipantInstance();
					partcipantNew.setId(patientInformation.getId());
					partcipantNew.setLastName(patientInformation.getLastName());
					partcipantNew.setFirstName(patientInformation.getFirstName());
					partcipantNew.setMiddleName(patientInformation.getMiddleName());
					partcipantNew.setBirthDate(patientInformation.getDob());
					partcipantNew.setDeathDate(patientInformation.getDeathDate());
					partcipantNew.setVitalStatus(patientInformation.getVitalStatus());
					partcipantNew.setGender(patientInformation.getGender());
					partcipantNew.setEmpiId(patientInformation.getUpi());
					partcipantNew.setActivityStatus(patientInformation.getActivityStatus());
					if (patientInformation.getSsn() != null && patientInformation.getSsn() != "")
					{
						String ssn = ParticipantManagerUtility.getSSN(patientInformation.getSsn());
						partcipantNew.setSocialSecurityNumber(ssn);
					}
					Collection participantInfoMedicalIdentifierCollection = patientInformation
							.getParticipantMedicalIdentifierCollection();
					Collection participantMedicalIdentifierCollectionNew = new LinkedHashSet();
					if (participantInfoMedicalIdentifierCollection != null
							&& participantInfoMedicalIdentifierCollection.size() > 0)
					{
						IParticipantMedicalIdentifier participantMedicalIdentifier;
						for (Iterator iterator = participantInfoMedicalIdentifierCollection
								.iterator(); iterator.hasNext(); participantMedicalIdentifierCollectionNew
								.add(participantMedicalIdentifier))
						{
							String mrn = (String) iterator.next();
							String siteId = (String) iterator.next();
							String siteName = (String) iterator.next();
							ISite site = (ISite) ParticipantManagerUtility.getSiteInstance();
							site.setId(Long.valueOf(siteId));
							site.setName(siteName);
							participantMedicalIdentifier = (IParticipantMedicalIdentifier) ParticipantManagerUtility
									.getPMIInstance();
							participantMedicalIdentifier.setMedicalRecordNumber(mrn);
							participantMedicalIdentifier.setSite(site);
						}

					}
					partcipantNew
							.setParticipantMedicalIdentifierCollection(participantMedicalIdentifierCollectionNew);
					result.setObject(partcipantNew);
					matchingParticipantsList.add(result);
				}
			}

		}
		catch (DAOException daoExp)
		{
			throw new ApplicationException(daoExp.getErrorKey(), daoExp, daoExp.getMsgValues());
		}finally {
			jdbcDAO.closeSession();
		}
		return matchingParticipantsList;
	}
}