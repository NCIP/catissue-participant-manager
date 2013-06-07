/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue-participant-manager/LICENSE.txt for details.
 */

package edu.wustl.common.participant.client;

import java.util.List;
import java.util.Set;

import edu.wustl.common.lookup.LookupLogic;
import edu.wustl.common.lookup.LookupParameters;


public interface IParticipantManagerLookupLogic extends LookupLogic
{
	/**
	 * @param params Lookup Parameters.
	 * @return List
	 * @throws Exception Exception.
	 */
	List lookup(LookupParameters params,Set<Long> csList) throws Exception;
}
