
package edu.wustl.common.participant.utility;

import java.io.FileNotFoundException;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * @author geeta_jaggal.
 * The Class RaceGenderCodesProperyHandler.
 */
public class RaceGenderCodesProperyHandler
{

	/** The race gender codes prop. */
	private static Properties raceGenderCodesProp = null;

	/** The document. */
	private static Document document = null;

	/**
	 * Instantiates a new race gender codes propery handler.
	 */
	public RaceGenderCodesProperyHandler()
	{
	}

	/**
	 * Inits the.
	 *
	 * @param path the path
	 *
	 * @throws Exception the exception
	 */
	public static void init(String path) throws Exception
	{
		try
		{
			java.io.InputStream iStream = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(path);
			if (iStream != null)
			{
				raceGenderCodesProp = new Properties();
				DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dbuilder = dbfactory.newDocumentBuilder();
				document = dbuilder.parse(iStream);
				populateProperyFile(document);
			}
			else
			{
				throw new FileNotFoundException("patientLookUpService.properties file not Found");
			}
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			throw new FileNotFoundException();
		}
	}

	/**
	 * Populate propery file.
	 *
	 * @param document the document
	 */
	private static void populateProperyFile(Document document)
	{
		Element root = document.getDocumentElement();
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			Node child = children.item(i);
			if (!(child instanceof Element))
			{
				continue;
			}
			NodeList subChildNodes = child.getChildNodes();
			boolean isNameFound = false;
			String pName = null;
			for (int j = 0; j < subChildNodes.getLength(); j++)
			{
				Node subchildNode = subChildNodes.item(j);
				String subNodeName = subchildNode.getNodeName();
				if (subNodeName.equals("name"))
				{
					pName = subchildNode.getFirstChild().getNodeValue();
				}
				if (!subNodeName.equals("value"))
				{
					continue;
				}
				String pValue = "";
				if (subchildNode != null && subchildNode.getFirstChild() != null)
				{
					pValue = subchildNode.getFirstChild().getNodeValue();
				}
				raceGenderCodesProp.put(pName, pValue);
			}

		}

	}

	/**
	 * Gets the value.
	 *
	 * @param propertyName the property name
	 *
	 * @return the value
	 *
	 * @throws Exception the exception
	 */
	public static String getValue(String propertyName) throws Exception
	{
		String value = null;
		try
		{
			if (raceGenderCodesProp == null)
			{
				init("HL7MesRaceGenderCodes.xml");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Exception(e.getMessage(), e);
		}
		if (propertyName != null)
		{
			value = raceGenderCodesProp.getProperty(propertyName);
		}
		return value;
	}

}
