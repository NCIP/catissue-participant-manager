package edu.wustl.common.participant.bizlogic.test;

import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.verify;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.bizlogic.NewDefaultBizLogic;
import edu.wustl.common.participant.bizlogic.CommonParticipantBizlogic;
import edu.wustl.common.participant.client.IParticipantManager;
import edu.wustl.common.participant.dao.CommonParticipantDAO;
import edu.wustl.common.participant.domain.IParticipant;
import edu.wustl.common.participant.domain.IParticipantMedicalIdentifier;
import edu.wustl.common.participant.domain.ISite;
import edu.wustl.common.participant.domain.IUser;
import edu.wustl.common.participant.test.domain.ParticipantManagerTest;
import edu.wustl.common.participant.test.domain.ParticipantMedicalIdentifierTestObject;
import edu.wustl.common.participant.test.domain.ParticipantTestObject;
import edu.wustl.common.participant.test.domain.SiteTestObject;
import edu.wustl.common.participant.test.domain.UserTestObject;
import edu.wustl.common.participant.utility.Constants;
import edu.wustl.common.participant.utility.ParticipantManagerUtility;
import edu.wustl.common.util.XMLPropertyHandler;
import edu.wustl.common.util.global.CommonServiceLocator;
@RunWith(PowerMockRunner.class)
public class CommonParticipantBizLogicTest
{

	@Test
	@PrepareForTest({CommonParticipantBizlogic.class,CommonParticipantDAO.class,ParticipantManagerUtility.class,IParticipantManager.class})
	public void testInsert() throws Exception
	{
		IParticipant participant = new ParticipantTestObject();
		participant.setFirstName("a");
		participant.setLastName("b");
		IParticipantMedicalIdentifier pmi = new ParticipantMedicalIdentifierTestObject();
		pmi.setParticipant(participant);
		ISite site = new SiteTestObject();
		site.setName("site");
		pmi.setSite(site);
		pmi.setMedicalRecordNumber("1233");
		participant.setParticipantMedicalIdentifierCollection(new LinkedHashSet<IParticipantMedicalIdentifier<IParticipant,ISite>>());
		participant.getParticipantMedicalIdentifierCollection().add(pmi);
		
		PowerMock.mockStatic(ParticipantManagerUtility.class);
		CommonParticipantDAO commonParticipantDAOMock = PowerMock.createMock(CommonParticipantDAO.class);
		IParticipantManager participantManagerMock = PowerMock.createMock(IParticipantManager.class);
		
		expectNew(CommonParticipantDAO.class, CommonServiceLocator.getInstance().getAppName(),null).andReturn(commonParticipantDAOMock);
		EasyMock.expect(ParticipantManagerUtility.getParticipantMgrImplObj()).andReturn(participantManagerMock);
		
		EasyMock.expect(participantManagerMock.getSiteIdByName("site")).andReturn(1L);
		
		commonParticipantDAOMock.insert(participant);
		expectLastCall();
		
		replay(ParticipantManagerUtility.class);
		replay(commonParticipantDAOMock);
		replay(CommonParticipantDAO.class);

		replay(participantManagerMock);
		replay(ParticipantManagerTest.class);
		new CommonParticipantBizlogic().insert(participant, pmi);
		
		verify(commonParticipantDAOMock);
	}
	
	@Test
	@PrepareForTest({CommonParticipantBizlogic.class,CommonParticipantDAO.class,ParticipantManagerUtility.class,IParticipantManager.class})
	public void testUpdate() throws Exception
	{
		IParticipant participant = new ParticipantTestObject();
		participant.setFirstName("a");
		participant.setLastName("b");
		IParticipantMedicalIdentifier pmi = new ParticipantMedicalIdentifierTestObject();
		pmi.setParticipant(participant);
		ISite site = new SiteTestObject();
		site.setName("site");
		pmi.setSite(site);
		pmi.setMedicalRecordNumber("1233");
		participant.setParticipantMedicalIdentifierCollection(new LinkedHashSet<IParticipantMedicalIdentifier<IParticipant,ISite>>());
		participant.getParticipantMedicalIdentifierCollection().add(pmi);
		
		CommonParticipantDAO commonParticipantDAOMock = PowerMock.createMock(CommonParticipantDAO.class);
		
		expectNew(CommonParticipantDAO.class, CommonServiceLocator.getInstance().getAppName(),null).andReturn(commonParticipantDAOMock);
		
		
		commonParticipantDAOMock.update(participant,participant);
		expectLastCall();
		
		replay(ParticipantManagerUtility.class);
		replay(commonParticipantDAOMock);
		replay(CommonParticipantDAO.class);

		new CommonParticipantBizlogic().update(participant,participant);
		
		verify(commonParticipantDAOMock);
	}
	
	@Test
	@PrepareForTest({CommonParticipantBizlogic.class,CommonParticipantDAO.class,ParticipantManagerUtility.class,IParticipantManager.class})
	public void testPostInsert() throws Exception
	{
		
		IParticipant participant = new ParticipantTestObject();
		participant.setId(1L);
		participant.setFirstName("a");
		participant.setLastName("b");
		IParticipantMedicalIdentifier pmi = new ParticipantMedicalIdentifierTestObject();
		pmi.setParticipant(participant);
		ISite site = new SiteTestObject();
		site.setName("site");
		pmi.setSite(site);
		pmi.setMedicalRecordNumber("1233");
		participant.setParticipantMedicalIdentifierCollection(new LinkedHashSet<IParticipantMedicalIdentifier<IParticipant,ISite>>());
		participant.getParticipantMedicalIdentifierCollection().add(pmi);
		
		LinkedHashSet<Long> userIdSet = new LinkedHashSet<Long>();
		userIdSet.add(1L);
		
		PowerMock.mockStaticPartial(ParticipantManagerUtility.class,"getParticipantMgrImplObj");
		CommonParticipantDAO commonParticipantDAOMock = PowerMock.createMock(CommonParticipantDAO.class);
		IParticipantManager participantManagerMock = PowerMock.createMock(IParticipantManager.class);
		
		expectNew(CommonParticipantDAO.class, CommonServiceLocator.getInstance().getAppName(),null).andReturn(commonParticipantDAOMock).times(2);
		EasyMock.expect(ParticipantManagerUtility.getParticipantMgrImplObj()).andReturn(participantManagerMock);
		
		List statusList = new ArrayList();
		statusList.add("1");
		EasyMock.expect(participantManagerMock.isEmpiEnabled(1L)).andReturn(statusList);
		
		commonParticipantDAOMock.addParticipantToProcessMessageQueue(userIdSet, 1L);
		
		replay(ParticipantManagerUtility.class);
		replay(commonParticipantDAOMock);
		replay(CommonParticipantDAO.class);

		replay(participantManagerMock);
		replay(ParticipantManagerTest.class);
		new CommonParticipantBizlogic().postInsert(participant, userIdSet);
		
		verify(commonParticipantDAOMock);
	}
	
	@Test
	@PrepareForTest({CommonParticipantBizlogic.class,CommonParticipantDAO.class,ParticipantManagerUtility.class,IParticipantManager.class})
	public void testPostUpdate() throws Exception
	{
		
		IParticipant participant = new ParticipantTestObject();
		participant.setId(1L);
		participant.setFirstName("a");
		participant.setLastName("b");
		participant.setGridValueSelected(Constants.YES);
		IParticipantMedicalIdentifier pmi = new ParticipantMedicalIdentifierTestObject();
		pmi.setParticipant(participant);
		ISite site = new SiteTestObject();
		site.setName("site");
		pmi.setSite(site);
		pmi.setMedicalRecordNumber("1233");
		participant.setParticipantMedicalIdentifierCollection(new LinkedHashSet<IParticipantMedicalIdentifier<IParticipant,ISite>>());
		participant.getParticipantMedicalIdentifierCollection().add(pmi);
		
		
		
		IParticipant oldParticipant = new ParticipantTestObject();
		oldParticipant.setId(1L);
		oldParticipant.setFirstName("a");
		oldParticipant.setLastName("bc");
		oldParticipant.setEmpiId("11");
		IParticipantMedicalIdentifier oldPmi = new ParticipantMedicalIdentifierTestObject();
		oldPmi.setParticipant(oldParticipant);
		ISite oldSite = new SiteTestObject();
		oldSite.setId(1L);
		oldSite.setName("site");
		oldPmi.setSite(oldSite);
		oldPmi.setMedicalRecordNumber("1233");
		oldParticipant.setParticipantMedicalIdentifierCollection(new LinkedHashSet<IParticipantMedicalIdentifier<IParticipant,ISite>>());
		oldParticipant.getParticipantMedicalIdentifierCollection().add(oldPmi);
		
		LinkedHashSet<Long> userIdSet = new LinkedHashSet<Long>();
		userIdSet.add(1L);
		
		PowerMock.mockStaticPartial(ParticipantManagerUtility.class,"getParticipantMgrImplObj");
		CommonParticipantDAO commonParticipantDAOMock = PowerMock.createMock(CommonParticipantDAO.class);
		IParticipantManager participantManagerMock = PowerMock.createMock(IParticipantManager.class);
		
		expectNew(CommonParticipantDAO.class, CommonServiceLocator.getInstance().getAppName(),null).andReturn(commonParticipantDAOMock).anyTimes();
		EasyMock.expect(ParticipantManagerUtility.getParticipantMgrImplObj()).andReturn(participantManagerMock);
		
		List statusList = new ArrayList();
		statusList.add("1");
		EasyMock.expect(participantManagerMock.isEmpiEnabled(1L)).andReturn(statusList);
		
		commonParticipantDAOMock.updateOldEMPIDetails(participant.getId(), oldParticipant.getEmpiId());
		expectLastCall();
		commonParticipantDAOMock.setEMPIIdStatus(participant.getId(), Constants.EMPI_ID_PENDING);
		replay(ParticipantManagerUtility.class);
		replay(commonParticipantDAOMock);
		replay(CommonParticipantDAO.class);

		replay(participantManagerMock);
		replay(ParticipantManagerTest.class);
		new CommonParticipantBizlogic().postUpdate(oldParticipant,participant, userIdSet);
		
		verify(commonParticipantDAOMock);
	}
	
	@Test
	@PrepareForTest({CommonParticipantBizlogic.class,CommonParticipantDAO.class,ParticipantManagerUtility.class,IParticipantManager.class})
	public void testPostUpdateRegPatientToEmpi() throws Exception
	{
		
		IParticipant participant = new ParticipantTestObject();
		participant.setId(1L);
		participant.setFirstName("a");
		participant.setLastName("b");
		IParticipantMedicalIdentifier pmi = new ParticipantMedicalIdentifierTestObject();
		pmi.setParticipant(participant);
		ISite site = new SiteTestObject();
		site.setName("site");
		pmi.setSite(site);
		pmi.setMedicalRecordNumber("1233");
		participant.setParticipantMedicalIdentifierCollection(new LinkedHashSet<IParticipantMedicalIdentifier<IParticipant,ISite>>());
		participant.getParticipantMedicalIdentifierCollection().add(pmi);
		
		
		
		IParticipant oldParticipant = new ParticipantTestObject();
		oldParticipant.setId(1L);
		oldParticipant.setFirstName("a");
		oldParticipant.setLastName("bc");
		oldParticipant.setEmpiId("11");
		IParticipantMedicalIdentifier oldPmi = new ParticipantMedicalIdentifierTestObject();
		oldPmi.setParticipant(oldParticipant);
		ISite oldSite = new SiteTestObject();
		oldSite.setId(1L);
		oldSite.setName("site");
		oldPmi.setSite(oldSite);
		oldPmi.setMedicalRecordNumber("1233");
		oldParticipant.setParticipantMedicalIdentifierCollection(new LinkedHashSet<IParticipantMedicalIdentifier<IParticipant,ISite>>());
		oldParticipant.getParticipantMedicalIdentifierCollection().add(oldPmi);
		
		LinkedHashSet<Long> userIdSet = new LinkedHashSet<Long>();
		userIdSet.add(1L);
		
		PowerMock.mockStaticPartial(ParticipantManagerUtility.class,"getParticipantMgrImplObj");
		CommonParticipantDAO commonParticipantDAOMock = PowerMock.createMock(CommonParticipantDAO.class);
		IParticipantManager participantManagerMock = PowerMock.createMock(IParticipantManager.class);
		
		expectNew(CommonParticipantDAO.class, CommonServiceLocator.getInstance().getAppName(),null).andReturn(commonParticipantDAOMock).anyTimes();
		EasyMock.expect(ParticipantManagerUtility.getParticipantMgrImplObj()).andReturn(participantManagerMock);
		
		List statusList = new ArrayList();
		statusList.add("1");
		EasyMock.expect(participantManagerMock.isEmpiEnabled(1L)).andReturn(statusList);
		
		EasyMock.expect(commonParticipantDAOMock.getPartiEMPIStatus(participant.getId())).andReturn(Constants.EMPI_ID_CREATED);
		EasyMock.expect(commonParticipantDAOMock.isOldMatchesPresent(participant.getId())).andReturn(true);
		commonParticipantDAOMock.addParticipantToProcessMessageQueue(userIdSet,participant.getId());
		expectLastCall();
		
		replay(ParticipantManagerUtility.class);
		replay(commonParticipantDAOMock);
		replay(CommonParticipantDAO.class);

		replay(participantManagerMock);
		replay(ParticipantManagerTest.class);
		new CommonParticipantBizlogic().postUpdate(oldParticipant,participant, userIdSet);
		
		verify(commonParticipantDAOMock);
	}
	
	@Test
	@PrepareForTest({CommonParticipantBizlogic.class,CommonParticipantDAO.class,ParticipantManagerUtility.class,IParticipantManager.class})
	public void testRetrieveMatchedParticipantList() throws Exception
	{
		
		IParticipant participant = new ParticipantTestObject();

		
		PowerMock.mockStaticPartial(ParticipantManagerUtility.class,"getParticipantInstance");
		CommonParticipantDAO commonParticipantDAOMock = PowerMock.createMock(CommonParticipantDAO.class);
		
		expectNew(CommonParticipantDAO.class, CommonServiceLocator.getInstance().getAppName(),null).andReturn(commonParticipantDAOMock).anyTimes();
		EasyMock.expect(ParticipantManagerUtility.getParticipantInstance()).andReturn(participant);
		
		List matchedParticipant = new ArrayList();
		Object[] participantArray = new Object[13];
		participantArray[0] = 1L;
		participantArray[1] = "11";
		participantArray[2] = "as";
		participantArray[3] = "df";
		participantArray[4] = "sd";
		participantArray[5] = null;
		participantArray[6] = "Male Gender";
		participantArray[7] = "1212";
		participantArray[8] = "Active";
		participantArray[9] = null;
		participantArray[10] = "Alive";
		participantArray[11] = null;
		participantArray[12] = null;
		matchedParticipant.add(participantArray);
		EasyMock.expect(commonParticipantDAOMock.retrieveMatchedParticipantList(1L)).andReturn(matchedParticipant);
		
		replay(ParticipantManagerUtility.class);
		replay(commonParticipantDAOMock);
		replay(CommonParticipantDAO.class);

		new CommonParticipantBizlogic().retrieveMatchedParticipantList(1L);
		
		verify(commonParticipantDAOMock);
	}
	

}

