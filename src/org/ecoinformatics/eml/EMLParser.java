/**
 *  '$RCSfile: EMLParser.java,v $'
 *  Copyright: 2000 Regents of the University of California and the
 *             National Center for Ecological Analysis and Synthesis
 *    Authors: @authors@
 *    Release: @release@
 *
 *   '$Author: berkley $'
 *     '$Date: 2002-09-24 22:12:30 $'
 * '$Revision: 1.2 $'
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.ecoinformatics.eml;

import java.io.*;
import java.util.*;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.InputSource;

import org.apache.xpath.XPathAPI;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.DocumentType;
import org.apache.xerces.dom.DocumentTypeImpl;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;

import edu.ucsb.nceas.configxml.*;

/**
 * This is a SAX parser to validate EML packages.  This parser will validate
 * an EML package with references based on the following rules:
 * <ul>
 *  <li>
 *  If a user wants to reuse content to indicate the repetition of an object,
 *  a reference must be used. you cannot have two identical ids in a document.
 *  </li>
 *  <li>
 *  "Local scope" is defined as identifiers unique only to a single instance
 *  document (if a document does not have a system or if scope is set to 'local'
 *  then all ids are defined as distinct content).
 *  </li>
 *  <li>
 *  System scope is defined as identifiers unique to an entire data management
 *  system (if two documents share a system string, then any IDs in those two
 *  documents that are identical refer to the same object).
 *  </li>
 *  <li>
 *  If an element references another element, it must not have an ID.
 *  </li>
 *  <li>
 *  All EML packages must have the 'eml' module as the root.
 *  </li>
 *  <li>
 *  The system and scope attribute are always optional except for at the
 *  'eml' module where the scope attribute is fixed as 'system'. The scope
 *  attribute defaults to 'local' for all other modules.
 *  </li>
 * </ul>
 */
public class EMLParser
{
  private String parserName;
  private ConfigXML config;
  private Key[] keys;
  private Keyref[] keyrefs;
  private Hashtable idHash = new Hashtable();
  private Hashtable idrefHash = new Hashtable();
  private File xml;


  /**
   * parses an eml file
   * @param xml the eml file to parse
   */
  public EMLParser(File xml)
         throws EMLParserException
  {
    this.xml = xml;
    try
    {
      config = new ConfigXML("@config.file@");
    }
    catch(Exception e)
    {
      throw new EMLParserException("Config file not found: " + e.getMessage());
    }

    parseConfig();
    getAllKeys();
    getAllKeyrefs();
    resolveKeys();
  }

  /**
   * make sure all ids are unique and hash the keys
   */
  private void getAllKeys()
  {
    for(int i=0; i<keys.length; i++)
    {
      try
      {
        NodeList keyNL = getPathContent(new FileInputStream(xml),
                                        keys[i].selector);
        for(int j=0; j<keyNL.getLength(); j++)
        {
          Node n = keyNL.item(j);
          Node id = XPathAPI.selectSingleNode(n, keys[i].field);
          String idval;

          if(id == null)
          {
            continue;
          }

          if(keys[i].field.indexOf("@") != -1)
          { //the field is an attribute
            idval = id.getNodeValue();
          }
          else
          { //the field is an element
            idval = id.getFirstChild().getNodeValue();
          }

           //try to get the id.  if it is already in the hash, throw exception
          Object o = idHash.get(idval);
          if(o == null)
          {
            idHash.put(new String(idval), new Integer(j));
            continue;  //continue on in the loop.
          }
          else
          {
            throw new EMLParserException("Error in xml document.  This " +
            "EML document is not valid because the id " +
            idval + " occurs " +
            "more than once.  IDs must be unique.");
          }
        }
      }
      catch(Exception e)
      {
        throw new EMLParserException("Error running xpath expression: " +
                                     keys[i].selector + " : " + e.getMessage());
      }
    }
  }

  /**
   * get all the keyrefs and make sure they don't have an id
   */
  private void getAllKeyrefs()
  {
    for(int i=0; i<keyrefs.length; i++)
    {
      try
      {
        NodeList keyrefNL = getPathContent(new FileInputStream(xml),
                                           keyrefs[i].selector);
        for(int j=0; j<keyrefNL.getLength(); j++)
        {
          Node n = keyrefNL.item(j);
          System.out.println("got n");
          Node id;
          if(keyrefs[i].field.equals("."))
          {
            id = n;
          }
          else
          {
            id = XPathAPI.selectSingleNode(n, keyrefs[i].field);
          }
          System.out.println("got id: " + id.getNodeName());
          String idval;

          if(id == null)
          {
            continue;
          }

          if(keyrefs[i].field.indexOf("@") != -1)
          { //the field is an attribute
            idval = id.getNodeValue();
          }
          else
          { //the field is an element
            idval = id.getFirstChild().getNodeValue();
          }
          System.out.println("selector: " + keyrefs[i].selector);
          System.out.println("field: " + keyrefs[i].field);
          System.out.println("idval: " + idval);

          int keyindex;
          Object o = idHash.get(idval);

          if(o == null)
          { //check to make sure the referenced key exists
            throw new EMLParserException("Error in xml document. This EML " +
              "instance is invalid because referenced id " + idval +
              " does not exist in the given keys.");
          }
          else
          {
            keyindex = ((Integer)o).intValue();
          }
          System.out.println("keyindex: " + keyindex);

/*
          //now make sure that what it is referring to is the right key
          Key referencedKey = keys[keyindex.intValue()];
          if(!referencedKey.name.equals(keyrefs[i].refer))
          {
            throw new EMLParserException("Error in xml document. This EML " +
              "instance is invalid because the keyref " + keyrefs[i].name +
              " must refer to a key by the name of " + keyrefs[i].refer +
              ". Instead it points at " + referencedKey.name);
          }

          //now make sure that the references parent meets the criteria
          //for the keys' xpath expression and that it does not have
          //an id itself

          Node parent = id.getParentNode();
          Node parentxpath = XPathAPI.selectSingleNode(parent,
                                                       referencedKey.selector);
          if(parentxpath == null)
          {
            throw new EMLParserException("Error in xml document. This EML " +
              "instance is invalid because this reference is nested in an " +
              "invalid tag.  It should be nested in " + referencedKey.name);
          }

          Node parentid = XPathAPI.selectSingleNode(parent,
                                                    referencedKey.field);
          if(parentid != null)
          {
            throw new EMLParserException("Error in xml document. This EML " +
              "instance is invalid because this element may not have an id " +
              "because it is being used in a keyref expression");
          }*/
        }
      }
      catch(Exception e)
      {
        throw new EMLParserException("Error processing keyrefs: " +
                                     keyrefs[i].selector + " : " + e.getMessage());
      }
    }
  }

  private void resolveKeys()
  {

  }

  /**
   * Gets the content of a path in an xml file
   */
  public static NodeList getPathContent(InputStream is, String xpath)
         throws Exception
  {
    InputSource in = new InputSource(is);
    DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
    dfactory.setNamespaceAware(true);
    Document doc = dfactory.newDocumentBuilder().parse(in);

    // Set up an identity transformer to use as serializer.
    Transformer serializer = TransformerFactory.newInstance().newTransformer();
    serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

    // Use the simple XPath API to select a nodeIterator.
    NodeList nl = XPathAPI.selectNodeList(doc, xpath);
    return nl;
  }

  private void parseConfig()
  {
    try
    { //parse the keys and keyrefs out of the config file
      NodeList keyNL = config.getPathContent("//key");
      keys = new Key[keyNL.getLength()];
      NodeList keyrefNL = config.getPathContent("//keyref");
      keyrefs = new Keyref[keyrefNL.getLength()];

      //get the keys
      for(int i=0; i<keyNL.getLength(); i++)
      {
        String name = "";
        String selector = "";
        String field = "";

        Node n = keyNL.item(i);

        //get the name
        NamedNodeMap atts = n.getAttributes();
        Node nameatt = atts.getNamedItem("name");
        name = nameatt.getNodeValue();

        //get the selector and field
        NodeList keychildren = n.getChildNodes();
        for(int j=0; j<keychildren.getLength(); j++)
        {
          Node kc = keychildren.item(j);
          if(kc.getNodeName().equals("selector"))
          {
            NamedNodeMap selectoratts = kc.getAttributes();
            Node selectoratt = selectoratts.getNamedItem("xpath");
            selector = selectoratt.getNodeValue();
          }
          else if(kc.getNodeName().equals("field"))
          {
            NamedNodeMap fieldatts = kc.getAttributes();
            Node fieldatt = fieldatts.getNamedItem("xpath");
            field = fieldatt.getNodeValue();
          }
        }

        if(name.equals("") | selector.equals("") | field.equals(""))
        {
          throw new EMLParserException("Error in config file.  All keys " +
                                       "must have a name, selector and field.");
        }

        keys[i] = new Key(name, selector, field);
      }

      //get the keyrefs
      for(int i=0; i<keyrefNL.getLength(); i++)
      {
        String name = "";
        String refer = "";
        String selector = "";
        String field = "";

        Node n = keyrefNL.item(i);

        //get the name and refer
        NamedNodeMap atts = n.getAttributes();
        Node nameatt = atts.getNamedItem("name");
        name = nameatt.getNodeValue();
        Node referatt = atts.getNamedItem("refer");
        refer = referatt.getNodeValue();

        //get the selector and field
        NodeList keychildren = n.getChildNodes();
        for(int j=0; j<keychildren.getLength(); j++)
        {
          Node kc = keychildren.item(j);
          if(kc.getNodeName().equals("selector"))
          {
            NamedNodeMap selectoratts = kc.getAttributes();
            Node selectoratt = selectoratts.getNamedItem("xpath");
            selector = selectoratt.getNodeValue();
          }
          else if(kc.getNodeName().equals("field"))
          {
            NamedNodeMap fieldatts = kc.getAttributes();
            Node fieldatt = fieldatts.getNamedItem("xpath");
            field = fieldatt.getNodeValue();
          }
        }

        if(name.equals("") | selector.equals("") |
           field.equals("") | refer.equals(""))
        {
          throw new EMLParserException("Error in config file.  All keys " +
                                       "must have a name, selector and field.");
        }

        keyrefs[i] = new Keyref(name, refer, selector, field);
      }
    }
    catch(Exception e)
    {
      throw new EMLParserException("Error parsing keys and keyrefs in config " +
                                   "file: " + e.getMessage());
    }
  }

  /**
   * class to represent a key
   */
  private class Key
  {
    protected String selector; //xpath expression for the selector
    protected String field;    //xpath expression for the field in the selector
    protected String name;     //name of the key

    Key(String name, String selector, String field)
    {
      this.name = name;
      this.selector = selector;
      this.field = field;
    }

    public String toString()
    {
      String s = "name: " + name + " selector: " + selector + " field: " + field;
      return s;
    }
  }

  /**
   * class to represent a keyref
   */
  private class Keyref
  {
    protected String name;     //name of the keyref
    protected String refer;    //the key that we are refering to
    protected String selector; //the selector for the keyref
    protected String field;    //the field in the selector

    Keyref(String name, String refer, String selector, String field)
    {
      this.name = name;
      this.refer = refer;
      this.selector = selector;
      this.field = field;
    }

    public String toString()
    {
      String s = "name: " + name + " refer: " + refer + " selector: " +
                  selector + " field: " + field;
      return s;
    }
  }
}
