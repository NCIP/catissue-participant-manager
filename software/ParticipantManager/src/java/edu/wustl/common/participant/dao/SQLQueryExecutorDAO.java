package edu.wustl.common.participant.dao;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.utility.ParticipantManagerException;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.dao.exception.DAOException;
import edu.wustl.dao.newdao.GenericHibernateDAO;
import edu.wustl.dao.query.generator.ColumnValueBean;
import edu.wustl.patientLookUp.domain.PatientInformation;
import edu.wustl.patientLookUp.util.Constants;
import edu.wustl.patientLookUp.util.PatientLookupException;


public class SQLQueryExecutorDAO extends GenericHibernateDAO<IParticipant, Long>
{

	public SQLQueryExecutorDAO(String applicationName, SessionDataBean sessionDataBean)
	{
		super(applicationName, sessionDataBean);
	}

	public List executeQueryForName(String lastName, Set<Long> protocolIdSet,
			String participantObjName) throws DAOException, ParticipantManagerException
	{
		String fetchByLNameQry = ParticipantManagerUtility.getLastNameQry(protocolIdSet,
				participantObjName);
		List<ColumnValueBean> columnValueBeans = new ArrayList<ColumnValueBean>();
		columnValueBeans.add(new ColumnValueBean("lastName",lastName + "%"));
		columnValueBeans.add(new ColumnValueBean("activityStatus",Constants.ACTIVITY_STATUS_DISABLED));
		return executeQuery(fetchByLNameQry, null,null,columnValueBeans);
	}

	public List<PatientInformation> executetQueryForPhonetic(String metaPhone,
			Set<Long> protocolIdSet, String participantObjName) throws DAOException, ParticipantManagerException
	{
		String fetchByMetaPhoneQry = ParticipantManagerUtility.getMetaPhoneQry(protocolIdSet,
				participantObjName);
		List<ColumnValueBean> columnValueBeans = new ArrayList<ColumnValueBean>();
		columnValueBeans.add(new ColumnValueBean("metaPhoneCode",metaPhone));
		columnValueBeans.add(new ColumnValueBean("activityStatus",Constants.ACTIVITY_STATUS_DISABLED));
		return executeQuery(fetchByMetaPhoneQry, null,null,columnValueBeans);
	}

	/**
	 * This method will fetch the SSN matched patients from database.
	 * @param ssn - Social security number
	 * @return list of SSN matched patients
	 * @throws PatientLookupException : PatientLookupException
	 * @throws ParticipantManagerException 
	 */
	public List<PatientInformation> executetQueryForSSN(String ssn, Set<Long> protocolIdSet,
			String participantObjName) throws DAOException, ParticipantManagerException
	{
		String fetchBySSNQry = ParticipantManagerUtility.getSSNQuery(protocolIdSet,
				participantObjName);
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean("socialSecurityNumber",ssn));
		columnValueBeanList.add(new ColumnValueBean("activityStatus",Constants.ACTIVITY_STATUS_DISABLED));

		return executeQuery(fetchBySSNQry, null,null,columnValueBeanList);
	}

	/**
	 * This method will fetch the MRN matched patients from database.
	 * @param mrn : User entered MRN value
	 * @return list of mrn matched patients from db
	 * @throws ParticipantManagerException 
	 * @throws PatientLookupException : PatientLookupException
	 */
	public List<PatientInformation> executeQueryForMRN(String mrn, String siteId,
			Set<Long> protocolIdSet, String pmiObjName) throws DAOException, ParticipantManagerException
	{
		String fetchByMRNQry = ParticipantManagerUtility.getMRNQuery(protocolIdSet, pmiObjName);
		LinkedList<ColumnValueBean> columnValueBeanList = new LinkedList<ColumnValueBean>();
		columnValueBeanList.add(new ColumnValueBean("medicalRecordNumber",mrn));
		columnValueBeanList.add(new ColumnValueBean("siteId",Long.valueOf(siteId)));
		columnValueBeanList.add(new ColumnValueBean("activityStatus",Constants.ACTIVITY_STATUS_DISABLED));
		return executeQuery(fetchByMRNQry,null,null,columnValueBeanList);
	}	
}
