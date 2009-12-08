
package edu.wustl.common.participant.utility;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.codec.language.Metaphone;

import edu.wustl.common.exception.ApplicationException;
import edu.wustl.dao.JDBCDAO;
import edu.wustl.dao.exception.DAOException;

/**
 * @author geeta_jaggal.
 * This call is used for updating all the existing
 * paticipants with last name metaphone code.
 * The Class UpdateParticipantMetaPhoneInfo.
 */
public class UpdateParticipantMetaPhoneInfo
{

	private UpdateParticipantMetaPhoneInfo(){

	}

	/**
	 * The main method.
	 *
	 * @param args
	 *            : db related configuration values
	 *
	 * @throws SQLException
	 *             : SQLException
	 * @throws IOException
	 *             : IOException
	 * @throws ClassNotFoundException
	 *             : ClassNotFoundException
	 * @throws ApplicationException
	 *             : ApplicationException
	 */
	public static void main(final String[] args) throws SQLException, IOException,
			ClassNotFoundException, ApplicationException
	{
		insertMetaPhoneCodeForLastName();

	}

	/**
	 * This method will insert metaPhonic codes for last name in the participant
	 * table.
	 *
	 * @throws DAOException
	 *             the DAO exception
	 * @throws ApplicationException
	 *             : ApplicationException
	 * @throws SQLException
	 *             : SQLException
	 */
	public static void insertMetaPhoneCodeForLastName() throws DAOException
	{

		String lNameMetaPhone = null;
		String sql = "select identifier,last_name from catissue_participant";
		Metaphone metaPhoneObj = new Metaphone();

		JDBCDAO dao = null;
		try
		{
			dao = ParticipantManagerUtility.getJDBCDAO();

			List list = dao.executeQuery(sql);
			if (list != null && !list.isEmpty())
			{
				for (int i = 0; i < list.size(); i++)
				{
					List idNameList = (List) list.get(i);
					String identifier = (String) idNameList.get(0);
					String lastName = (String) idNameList.get(1);
					lNameMetaPhone = metaPhoneObj.metaphone(lastName);
					updateMetaPhone(dao, identifier, lNameMetaPhone);
				}
			}
			dao.commit();
		}
		catch (DAOException e)
		{
			dao.rollback();
			throw new DAOException(e.getErrorKey(), e, e.getMessage());
		}
		finally
		{
			dao.closeSession();
		}
	}

	/**
	 * Update meta phone.
	 *
	 * @param dao
	 *            the dao
	 * @param identifier
	 *            the identifier
	 * @param lNameMetaPhone
	 *            the l name meta phone
	 *
	 * @throws DAOException
	 *             the DAO exception
	 */
	private static void updateMetaPhone(JDBCDAO dao, String identifier, String lNameMetaPhone)
			throws DAOException
	{
		dao.executeUpdate("update catissue_participant set lName_metaPhone='" + lNameMetaPhone
				+ "'" + "  where identifier=" + identifier);
	}
}
