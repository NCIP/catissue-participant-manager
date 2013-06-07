/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue_participant_manager/LICENSE.txt for details.
 */

package edu.wustl.common.participant.utility;


public class ParticipantManagerException extends Exception
{
	/**serialVersionUID.*/
	private static final long serialVersionUID = 1L;

	/**
	 * @param message : Exception message
	 * @param cause : Cause for the exception
	 */
	public ParticipantManagerException(String message,Throwable cause)
	{
		super(message, cause);
	}
}
