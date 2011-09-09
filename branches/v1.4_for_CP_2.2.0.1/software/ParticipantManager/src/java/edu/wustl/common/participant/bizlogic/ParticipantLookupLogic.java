
package edu.wustl.common.participant.bizlogic;

import edu.wustl.common.participant.client.IParticipantManagerLookupLogic;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerException;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.lookup.DefaultLookupParameters;
import edu.wustl.common.lookup.DefaultLookupResult;
import edu.wustl.common.lookup.LookupLogic;
import edu.wustl.common.lookup.LookupParameters;
import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.patientLookUp.domain.PatientInformation;
import edu.wustl.patientLookUp.lookUpServiceBizLogic.PatientInfoLookUpService;
import edu.wustl.patientLookUp.queryExecutor.SQLQueryExecutorImpl;
import edu.wustl.patientLookUp.util.PatientLookupException;

/**
 * The Class ParticipantLookupLogic.
 *
 * @author geeta_jaggal.
 *
 * The Class ParticipantLookupLogic.
 *
 * This class is for finding out the matching participant with respect
 * to given participant. It implements the lookUp method of LookupLogic
 * interface which returns the list of all matching participants to the
 * given participant.
 */
public class ParticipantLookupLogic implements IParticipantManagerLookupLogic
{

	/** The CUTOFFPOINTSFROMPROPERTIES. */
	protected static transient int CUTOFFPOINTSFROMPROPERTIES;

	/** The TOTALPOINTSFROMPROPERTIES. */
	protected static transient int TOTALPOINTSFROMPROPERTIES;

	/** The is ssn or pmi. */
	protected static transient boolean isSSNOrPMI;

	/** The exact match. */
	protected static transient boolean exactMatch;

	/** The cutoff points. */
	protected static transient int cutoffPoints;

	/** The total points. */
	protected static transient int totalPoints;

	/** The max no of participants to return. */
	protected static transient int maxNoOfParticipantsToReturn;

	/** The Constant logger. */

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
	public List<DefaultLookupResult> lookup(LookupParameters params) throws PatientLookupException
	{

		if (params == null)
		{
			throw new PatientLookupException("Params can not be null", null);
		}
		else
		{
			final List<DefaultLookupResult> matchingPartisList = searchMatchingParticipant(params,
					null);
			return matchingPartisList;
		}

	}

	/* (non-Javadoc)
	 * @see edu.wustl.common.lookup.LookupLogic#lookup(edu.wustl.common.lookup.LookupParameters)
	 */
	public List<DefaultLookupResult> lookup(LookupParameters params, Set<Long> csSet)
			throws PatientLookupException
	{
		if (params == null)
		{
			throw new PatientLookupException("Params can not be null", null);
		}
		else
		{
			final List<DefaultLookupResult> matchingPartisList = searchMatchingParticipant(params,
					csSet);
			return matchingPartisList;
		}

	}

	private PatientInformation populatePatientObject(LookupParameters params, Set<Long> protocolIdSet)
	{
		final DefaultLookupParameters participantParams = (DefaultLookupParameters) params;
		final IParticipant participant = (IParticipant) participantParams.getObject();
		final PatientInformation patientInfo = ParticipantManagerUtility
				.populatePatientObject(participant,protocolIdSet);
		cutoffPoints = Integer.parseInt(XMLPropertyHandler.getValue(Constants.EMPITHRESHOLD));
		maxNoOfParticipantsToReturn = Integer.parseInt(XMLPropertyHandler
				.getValue(Constants.EMPIMAXNOOFPATIENS));
		return patientInfo;
	}

	/**
	 * Search matching participant.
	 *
	 * @param patientInformation the patient information
	 *
	 * @return the list
	 *
	 * @throws PatientLookupException the patient lookup exception
	 * @throws ParticipantManagerException
	 * @throws ApplicationException the application exception
	 */
	protected List<DefaultLookupResult> searchMatchingParticipant(LookupParameters params,
			Set<Long> csSet) throws PatientLookupException
	{

		final PatientInformation patientInfoInput = populatePatientObject(params, csSet);

		final List<DefaultLookupResult> matchingPartisList = new ArrayList<DefaultLookupResult>();
		final PatientInfoLookUpService patientLookupObj = new PatientInfoLookUpService();
		PatientInformation patientInfo = null;
		JDBCDAO jdbcDAO = null;
		try
		{
			jdbcDAO = ParticipantManagerUtility.getJDBCDAO();
			final edu.wustl.patientLookUp.queryExecutor.IQueryExecutor queryExecutor = new SQLQueryExecutorImpl(
					jdbcDAO);
			final List patientInfoList = patientLookupObj.patientLookupService(patientInfoInput,
					queryExecutor, cutoffPoints, maxNoOfParticipantsToReturn);
			if (patientInfoList != null && !patientInfoList.isEmpty())
			{
				for (int i = 0; i < patientInfoList.size(); i++)
				{
					patientInfo = (PatientInformation) patientInfoList.get(i);
					final DefaultLookupResult result = new DefaultLookupResult();
					final IParticipant partcipantNew = (IParticipant) ParticipantManagerUtility
							.getParticipantInstance();
					if(null!=patientInfo.getId()){
					partcipantNew.setId(patientInfo.getId());
					}
					partcipantNew.setLastName(patientInfo.getLastName());
					partcipantNew.setFirstName(patientInfo.getFirstName());
					partcipantNew.setMiddleName(patientInfo.getMiddleName());
					partcipantNew.setBirthDate(patientInfo.getDob());
					partcipantNew.setDeathDate(patientInfo.getDeathDate());
					partcipantNew.setVitalStatus(patientInfo.getVitalStatus());
					partcipantNew.setGender(patientInfo.getGender());
					if(patientInfo.getUpi()!=null){
						partcipantNew.setEmpiId(patientInfo.getUpi());
					}
					partcipantNew.setActivityStatus(patientInfo.getActivityStatus());
					if (patientInfo.getSsn() != null && !"".equals(patientInfo.getSsn()))
					{
						final String ssn = ParticipantManagerUtility.getSSN(patientInfo.getSsn());
						partcipantNew.setSocialSecurityNumber(ssn);
					}
					final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> participantInfoMedicalIdentifierCollection = patientInfo
					.getPmiCollection();
					final Collection<IParticipantMedicalIdentifier<IParticipant, ISite>> participantMedicalIdentifierCollectionNew = new LinkedHashSet<IParticipantMedicalIdentifier<IParticipant, ISite>>();
					if (participantInfoMedicalIdentifierCollection != null
							&& participantInfoMedicalIdentifierCollection.size() > 0)
					{
						IParticipantMedicalIdentifier<IParticipant, ISite> participantMedicalIdentifier;
						for (Iterator<IParticipantMedicalIdentifier<IParticipant, ISite> > iterator = participantInfoMedicalIdentifierCollection
								.iterator(); iterator.hasNext(); participantMedicalIdentifierCollectionNew
								.add(participantMedicalIdentifier))
						{
							IParticipantMedicalIdentifier<IParticipant, ISite>  pmi =  iterator.next();
							participantMedicalIdentifier= pmi;
						}

					}
					partcipantNew
							.setParticipantMedicalIdentifierCollection(participantMedicalIdentifierCollectionNew);
					result.setObject(partcipantNew);
					result.setWeight(new Double(patientInfo.getMatchingScore()));
					matchingPartisList.add(result);
				}
			}

		}
		catch (BizLogicException e)
		{
			throw new PatientLookupException(e.getMessage(), e);
		}
		catch (DAOException daoExp)
		{
			throw new PatientLookupException(daoExp.getMsgValues(), daoExp);
		}
		catch (ParticipantManagerException e)
		{
			// TODO Auto-generated catch block
			throw new PatientLookupException(e.getMessage(), e);
		}
		finally
		{
			try
			{
				jdbcDAO.closeSession();
			}
			catch (DAOException daoExp)
			{
				// TODO Auto-generated catch block
				throw new PatientLookupException(daoExp.getMsgValues(), daoExp);
			}
		}
		return matchingPartisList;
	}
}