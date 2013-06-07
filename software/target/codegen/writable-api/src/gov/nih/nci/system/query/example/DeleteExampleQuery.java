/*L
 *  Copyright Washington University in St. Louis
 *  Copyright SemanticBits
 *  Copyright Persistent Systems
 *  Copyright Krishagni
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/catissue-participant-manager/LICENSE.txt for details.
 */

package gov.nih.nci.system.query.example;


public class DeleteExampleQuery extends ExampleQuery implements ExampleManipulationQuery
{
	private static final long serialVersionUID = 1L;

	public DeleteExampleQuery(Object example) {
		super(example);
	}
}