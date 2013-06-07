/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue-participant-manager/LICENSE.txt for details.
 */

package edu.wustl.common.participant.test.domain;

import edu.wustl.common.participant.domain.IUser;


public class UserTestObject implements IUser
{

	private Boolean adminuser;
	private Long csmUserId;
	private String firstName;
	private Long id;
	private String lastName;
	private String loginName;
	private String roleId;
	
	public Boolean getAdminuser()
	{
		return adminuser;
	}
	
	public void setAdminuser(Boolean adminuser)
	{
		this.adminuser = adminuser;
	}
	
	public Long getCsmUserId()
	{
		return csmUserId;
	}
	
	public void setCsmUserId(Long csmUserId)
	{
		this.csmUserId = csmUserId;
	}
	
	public String getFirstName()
	{
		return firstName;
	}
	
	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
	}
	
	public Long getId()
	{
		return id;
	}
	
	public void setId(Long id)
	{
		this.id = id;
	}
	
	public String getLastName()
	{
		return lastName;
	}
	
	public void setLastName(String lastName)
	{
		this.lastName = lastName;
	}
	
	public String getLoginName()
	{
		return loginName;
	}
	
	public void setLoginName(String loginName)
	{
		this.loginName = loginName;
	}
	
	public String getRoleId()
	{
		return roleId;
	}
	
	public void setRoleId(String roleId)
	{
		this.roleId = roleId;
	}
	
}
