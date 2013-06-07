/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue-participant-manager/LICENSE.txt for details.
 */

package gov.nih.nci.system.query.hql;

import java.util.List;

import gov.nih.nci.system.query.SDKQuery;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

public class InsertHQLQuery extends HQLCriteria implements HQLManipulationQuery,SDKQuery
{
	private static final long serialVersionUID = 1L;

	public InsertHQLQuery(String hql, List<Object> parameters) {
		super(hql, parameters);
	}
	public InsertHQLQuery(String hql) {
		super(hql);
	}
}