/**
 * CommonDefaultBizLogic.java
 * Purpose:
 */
package edu.wustl.common.participant.bizlogic;

import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.bizlogic.DefaultBizLogic;
import edu.wustl.common.exception.BizLogicException;
import edu.wustl.common.exception.ErrorKey;
import edu.wustl.dao.DAO;
import edu.wustl.security.exception.SMException;
import edu.wustl.security.global.Permissions;
import edu.wustl.security.privilege.PrivilegeCache;
import edu.wustl.security.privilege.PrivilegeManager;


/**
 * @author geeta_jaggal
 * @created-on Nov 20, 2009
 */
public class CommonDefaultBizLogic extends DefaultBizLogic
{


	/**
	 *
	 * This method return true if authorized user.
	 * @param dao DAO object.
	 * @param domainObject Domain object.
	 * @param sessionDataBean  SessionDataBean object.
	 * @throws BizLogicException generic BizLogic Exception
	 * @return true if authorized user.
	 *
	 */

	//@see edu.wustl.common.bizlogic.IBizLogic#
	// isAuthorized(edu.wustl.common.dao.AbstractDAO, java.lang.Object, edu.wustl.common.beans.SessionDataBean)
	public boolean isAuthorized(DAO dao, Object domainObject, SessionDataBean sessionDataBean)
			throws BizLogicException
	{
		boolean isAuthorized = false;
		try
		{
			PrivilegeManager privilegeManager = PrivilegeManager.getInstance();
			if (sessionDataBean == null)
			{
				isAuthorized = false;
			}
			else
			{
				if (domainObject == null)
				{
					isAuthorized = true;
				}
				else
				{
					PrivilegeCache privilegeCache = privilegeManager
							.getPrivilegeCache(sessionDataBean.getUserName());
					isAuthorized = privilegeCache.hasPrivilege(
							getObjectIdForSecureMethodAccess(domainObject), Permissions.EXECUTE);
				}
			}

			if (!isAuthorized)
			{
				throw new BizLogicException(ErrorKey.getErrorKey("access.execute.action.denied"),
						null, "");

			}
		}
		catch (SMException smException)
		{
			throw handleSMException(smException);
		}

		return isAuthorized;
	}

	/**
	 * Returns the object id of the protection element that represents
	 * the Action that is being requested for invocation.
	 * @param clazz
	 * @return
	 */
	protected String getObjectIdForSecureMethodAccess(Object domainObject)
	{
		return domainObject.getClass().getName();
	}

	/**
	 * @param e
	 * @return
	 */
	protected BizLogicException handleSMException(SMException e)
	{

		String message = "Security Exception: " + e.getMessage();
		if (e.getCause() != null)
			message = message + " : " + e.getCause().getMessage();
		return new BizLogicException(ErrorKey.getErrorKey("error.security"), e,
				"Security Exception");
	}
}
