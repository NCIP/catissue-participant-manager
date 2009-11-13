package edu.wustl.common.participant.bizlogic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import edu.wustl.common.exception.ApplicationException;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.lookup.DefaultLookupParameters;
import edu.wustl.common.lookup.DefaultLookupResult;
import edu.wustl.common.lookup.LookupLogic;
import edu.wustl.common.lookup.LookupParameters;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.IRace;
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
import edu.wustl.patientLookUp.queryExecutor.IQueryExecutor;
import edu.wustl.patientLookUp.queryExecutor.SQLQueryExecutorImpl;
import edu.wustl.patientLookUp.util.PatientLookUpFactory;
import edu.wustl.patientLookUp.util.PatientLookupException;

// TODO: Auto-generated Javadoc
/**
 * The Class ParticipantLookupLogic.
 *
 * @author falguni_sachde
 *
 *         This class is for finding out the matching participant with respect
 *         to given participant. It implements the lookUp method of LookupLogic
 *         interface which returns the list of all matching participants to the
 *         given participant.
 */
public class ParticipantLookupLogic implements LookupLogic {
 protected static int POINTSFORSSNEXACT;
    protected static int POINTSFORSSNPARTIAL;
    protected static int POINTSFORPMIEXACT;
    protected static int POINTSFORPMIPARTIAL;
    protected static int POINTSFORDOBEXACT;
    protected static int POINTSFORDOBPARTIAL;
    protected static int POINTSFORLASTNAMEEXACT;
    protected static int POINTSFORLASTNAMEPARTIAL;
    protected static int POINTSFORFIRSTNAMEEXACT;
    protected static int POINTSFORFIRSTNAMEPARTIAL;
    protected static int POINTSFORMIDDLENAMEEXACT;
    protected static int POINTSFORMIDDLENAMEPARTIAL;
    protected static int POINTSFORRACEEXACT;
    protected static int POINTSFORGENDEREXACT;
    protected static int BONUSPOINTS;
    protected static int MATCHCHARACTERSFORLASTNAME;
    protected static int CUTOFFPOINTSFROMPROPERTIES;
    protected static int TOTALPOINTSFROMPROPERTIES;
    protected boolean isSSNOrPMI;
    protected boolean exactMatch;
    protected int cutoffPoints;
    protected int totalPoints;
    protected int maxNoOfParticipantsToReturn;

    public ParticipantLookupLogic()
    {
        isSSNOrPMI = false;
        exactMatch = true;
    }

    public List lookup(LookupParameters params)
        throws Exception
    {
        if(params == null)
        {
            throw new Exception("Params can not be null");
        } else
        {
            DefaultLookupParameters participantParams = (DefaultLookupParameters)params;
            IParticipant participant = (IParticipant)participantParams.getObject();
            PatientInformation patientInformation = ParticipantManagerUtility.populatePatientObject(participant);
            cutoffPoints = Integer.valueOf(XMLPropertyHandler.getValue("empi.threshold")).intValue();
            maxNoOfParticipantsToReturn = Integer.valueOf(XMLPropertyHandler.getValue("empi.MaxNoOfPatients")).intValue();
            List matchingParticipantsList = searchMatchingParticipant(patientInformation);
            return matchingParticipantsList;
        }
    }

    protected List searchMatchingParticipant(PatientInformation patientInformation)
        throws PatientLookupException, ApplicationException
    {
        List matchingParticipantsList = new ArrayList();
        PatientInfoLookUpService patientLookupObj = new PatientInfoLookUpService();
        try
        {
            String appName = CommonServiceLocator.getInstance().getAppName();
            IDAOFactory daoFactory = DAOConfigFactory.getInstance().getDAOFactory(appName);
            JDBCDAO jdbcDAO = daoFactory.getJDBCDAO();
            jdbcDAO.openSession(null);
            edu.wustl.patientLookUp.queryExecutor.IQueryExecutor queryExecutor = new SQLQueryExecutorImpl(jdbcDAO);
            List patientInfoList = patientLookupObj.patientLookupService(patientInformation, queryExecutor, cutoffPoints, maxNoOfParticipantsToReturn);
            if(patientInfoList != null && !patientInfoList.isEmpty())
            {
                for(int i = 0; i < patientInfoList.size(); i++)
                {
                    patientInformation = (PatientInformation)patientInfoList.get(i);
                    DefaultLookupResult result = new DefaultLookupResult();
                    IParticipant partcipantNew = (IParticipant)ParticipantManagerUtility.getParticipantInstance();
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
                    if(patientInformation.getSsn() != null && patientInformation.getSsn() != "")
                    {
                        String ssn = ParticipantManagerUtility.getSSN(patientInformation.getSsn());
                        partcipantNew.setSocialSecurityNumber(ssn);
                    }
                    Collection participantInfoMedicalIdentifierCollection = patientInformation.getParticipantMedicalIdentifierCollection();
                    Collection participantMedicalIdentifierCollectionNew = new LinkedHashSet();
                    if(participantInfoMedicalIdentifierCollection != null && participantInfoMedicalIdentifierCollection.size() > 0)
                    {
                        IParticipantMedicalIdentifier participantMedicalIdentifier;
                        for(Iterator iterator = participantInfoMedicalIdentifierCollection.iterator(); iterator.hasNext(); participantMedicalIdentifierCollectionNew.add(participantMedicalIdentifier))
                        {
                            String mrn = (String)iterator.next();
                            String siteId = (String)iterator.next();
                            String siteName = (String)iterator.next();
                            ISite site = (ISite)ParticipantManagerUtility.getSiteInstance();
                            site.setId(Long.valueOf(siteId));
                            site.setName(siteName);
                            participantMedicalIdentifier = (IParticipantMedicalIdentifier)ParticipantManagerUtility.getPMIInstance();
                            participantMedicalIdentifier.setMedicalRecordNumber(mrn);
                            participantMedicalIdentifier.setSite(site);
                        }

                    }
                    partcipantNew.setParticipantMedicalIdentifierCollection(participantMedicalIdentifierCollectionNew);
                    result.setObject(partcipantNew);
                    matchingParticipantsList.add(result);
                }

            }
            jdbcDAO.closeSession();
        }
        catch(DAOException daoExp)
        {
            throw new ApplicationException(daoExp.getErrorKey(), daoExp, daoExp.getMsgValues());
        }
        return matchingParticipantsList;
    }
}