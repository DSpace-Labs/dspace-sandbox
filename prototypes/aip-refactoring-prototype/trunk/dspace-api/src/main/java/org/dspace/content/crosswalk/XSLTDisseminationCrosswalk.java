/*
 * XSLTDisseminationCrosswalk.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.content.crosswalk;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Verifier;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.XSLTransformException;
import org.jdom.transform.XSLTransformer;

/**
 * Configurable XSLT-driven dissemination Crosswalk
 * <p>
 * See the XSLTCrosswalk superclass for details on configuration.
 * <p>
 * <h3>Additional Configuration of Dissemination crosswalk:</h3>
 * The disseminator also needs to be configured with an XML Namespace
 * (including prefix and URI) and an XML Schema for output format.  This
 * is configured on additional properties in the DSpace Configuration, i.e.:
 * <pre>
 *   crosswalk.dissemination.<i>PluginName</i>.namespace.<i>Prefix</i> = <i>namespace-URI</i>
 *   crosswalk.dissemination.<i>PluginName</i>.schemaLocation = <i>schemaLocation value</i>
 *   crosswalk.dissemination.<i>PluginName</i>.preferList = <i>boolean</i> (default is false)
 * </pre>
 * For example:
 * <pre>
 *   crosswalk.dissemination.qdc.namespace.dc = http://purl.org/dc/elements/1.1/
 *   crosswalk.dissemination.qdc.namespace.dcterms = http://purl.org/dc/terms/
 *   crosswalk.dissemination.qdc.schemaLocation = \
 *      http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2003/04/02/qualifieddc.xsd
 *   crosswalk.dissemination.qdc.preferList = true
 * </pre>
 *
 * @author Larry Stone
 * @version $Revision$
 * @see XSLTCrosswalk
 */
public class XSLTDisseminationCrosswalk
    extends XSLTCrosswalk
    implements DisseminationCrosswalk
{
    /** log4j category */
    private static Logger log = Logger.getLogger(XSLTDisseminationCrosswalk.class);

    private final static String DIRECTION = "dissemination";

    private static String aliases[] = makeAliases(DIRECTION);

    public static String[] getPluginNames()
    {
        return aliases;
    }

    // namespace and schema; don't worry about initializing these
    // until there's an instance, so do it in constructor.
    private String schemaLocation = null;

    private Namespace namespaces[] = null;

    private boolean preferList = false;

    // load the namespace and schema from config
    private void init()
        throws CrosswalkInternalException
    {
        if (namespaces != null || schemaLocation != null)
            return;
        String myAlias = getPluginInstanceName();
        if (myAlias == null)
        {
            log.error("Must use PluginManager to instantiate XSLTDisseminationCrosswalk so the class knows its name.");
            throw new CrosswalkInternalException("Must use PluginManager to instantiate XSLTDisseminationCrosswalk so the class knows its name.");
        }

        // all configs for this plugin instance start with this:
        String prefix = CONFIG_PREFIX+DIRECTION+"."+myAlias+".";

        // get the schema location string, should already be in the
        // right format for value of "schemaLocation" attribute.
        schemaLocation = ConfigurationManager.getProperty(prefix+"schemaLocation");
        if (schemaLocation == null)
            log.warn("No schemaLocation for crosswalk="+myAlias+", key="+prefix+"schemaLocation");

        // sanity check: schemaLocation should have space.
        else if (schemaLocation.length() > 0 && schemaLocation.indexOf(" ") < 0)
            log.warn("Possible INVALID schemaLocation (no space found) for crosswalk="+
                      myAlias+", key="+prefix+"schemaLocation"+
                      "\n\tCorrect format is \"{namespace} {schema-URL}\"");

        // grovel for namespaces of the form:
        //  crosswalk.diss.{PLUGIN_NAME}.namespace.{PREFIX} = {URI}
        String nsPrefix = prefix + "namespace.";
        Enumeration pe = ConfigurationManager.propertyNames();
        List nsList = new ArrayList();
        while (pe.hasMoreElements())
        {
            String key = (String)pe.nextElement();
            if (key.startsWith(nsPrefix))
                nsList.add(Namespace.getNamespace(key.substring(nsPrefix.length()),
                             ConfigurationManager.getProperty(key)));
        }
        namespaces = (Namespace[])nsList.toArray(new Namespace[nsList.size()]);

        preferList = ConfigurationManager.getBooleanProperty(prefix+"preferList", false);
    }

    /**
     * Return the namespace used by this crosswalk.
     *
     * @see DisseminationCrosswalk
     */
    public Namespace[] getNamespaces()
    {
        try
        {
            init();
        }
        catch (CrosswalkInternalException e)
        {
            log.error(e.toString());
        }
        return namespaces;
    }

    /**
     * Return the schema location used by this crosswalk.
     *
     * @see DisseminationCrosswalk
     */
    public String getSchemaLocation()
    {
        try
        {
            init();
        }
        catch (CrosswalkInternalException e)
        {
            log.error(e.toString());
        }
        return schemaLocation;
    }

    /**
     * Dessiminate the DSpace item, collection, or community.
     *
     * @see DisseminationCrosswalk
     */
    public Element disseminateElement(DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        int type = dso.getType();
        if (!(type == Constants.ITEM ||
              type == Constants.COLLECTION ||
              type == Constants.COMMUNITY))
            throw new CrosswalkObjectNotSupported("XSLTDisseminationCrosswalk can only crosswalk items, collections, and communities.");
        
        init();

        XSLTransformer xform = getTransformer(DIRECTION);
        if (xform == null)
            throw new CrosswalkInternalException("Failed to initialize transformer, probably error loading stylesheet.");

        try
        {
            Document ddim = new Document(createDIM(dso));
            Document result = xform.transform(ddim);
            Element root = result.getRootElement();
            root.detach();
            return root;
        }
        catch (XSLTransformException e)
        {
            log.error("Got error: "+e.toString());
            throw new CrosswalkInternalException("XSL translation failed: "+e.toString());
        }
    }

    /**
     * Disseminate the DSpace item, collection, or community.
     *
     * @see DisseminationCrosswalk
     */
    public List disseminateList(DSpaceObject dso)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        int type = dso.getType();
        if (!(type == Constants.ITEM ||
              type == Constants.COLLECTION ||
              type == Constants.COMMUNITY))
            throw new CrosswalkObjectNotSupported("XSLTDisseminationCrosswalk can only crosswalk a items, collections, and communities.");
        
        init();

        XSLTransformer xform = getTransformer(DIRECTION);
        if (xform == null)
            throw new CrosswalkInternalException("Failed to initialize transformer, probably error loading stylesheet.");

        try
        {
            return xform.transform(createDIM(dso).getChildren());
        }
        catch (XSLTransformException e)
        {
            log.error("Got error: "+e.toString());
            throw new CrosswalkInternalException("XSL translation failed: "+e.toString());
        }
    }

    /**
     * Determine is this crosswalk can dessiminate the given object.
     *
     * @see disseminationcrosswalk
     */
    public boolean canDisseminate(DSpaceObject dso)
    {
        return dso.getType() == Constants.ITEM;
    }

    /**
     * return true if this crosswalk prefers the list form over an singe
     * element, otherwise false.
     *
     * @see disseminationcrosswalk
     */
    public boolean preferList()
    {
        try
        {
            init();
        }
        catch (CrosswalkInternalException e)
        {
            log.error(e.toString());
        }
        return preferList;
    }
    
    /**
     * Generate an intermediate representation of a DSpace object.
     *
     * @param dso The dspace object to build a representation of.
     */
    public static Element createDIM(DSpaceObject dso, DCValue[] dcvs)
    {
        Element dim = new Element("dim", DIM_NS);
        String type = Constants.typeText[dso.getType()];
        dim.setAttribute("dspaceType",type);

        for (int i = 0; i < dcvs.length; i++)
        {
            DCValue dcv = dcvs[i];
            Element field =
            createField(dcv.schema, dcv.element, dcv.qualifier,
                        dcv.language, dcv.value);
            dim.addContent(field);
        }
        return dim;
    }

    /**
     * Generate an intermediate representation of a DSpace object.
     *
     * @param dso The dspace object to build a representation of.
     */
    public static Element createDIM(DSpaceObject dso)
    {
        if (dso.getType() == Constants.ITEM)
        {
            Item item = (Item) dso;
            return createDIM(dso, item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY));
        }
        else
        {
            Element dim = new Element("dim", DIM_NS);
            String type = Constants.typeText[dso.getType()];
            dim.setAttribute("dspaceType",type);
             
            if (dso.getType() == Constants.COLLECTION)
            {
                Collection collection = (Collection) dso;
                
                String description = collection.getMetadata("introductory_text");
                String description_abstract = collection.getMetadata("short_description");
                String description_table = collection.getMetadata("side_bar_text");
                String identifier_uri = "hdl:" + collection.getHandle();
                String provenance = collection.getMetadata("provenance_description");
                String rights = collection.getMetadata("copyright_text");
                String rights_license = collection.getMetadata("license");
                String title = collection.getMetadata("name");
                
                dim.addContent(createField("dc","description",null,null,description));
                dim.addContent(createField("dc","description","abstract",null,description_abstract));
                dim.addContent(createField("dc","description","tableofcontents",null,description_table));
                dim.addContent(createField("dc","identifier","uri",null,identifier_uri));
                dim.addContent(createField("dc","provenance",null,null,provenance));
                dim.addContent(createField("dc","rights",null,null,rights));
                dim.addContent(createField("dc","rights","license",null,rights_license));
                dim.addContent(createField("dc","title",null,null,title));
            }
            else if (dso.getType() == Constants.COMMUNITY)
            {
                Community community = (Community) dso;
                
                String description = community.getMetadata("introductory_text");
                String description_abstract = community.getMetadata("short_description");
                String description_table = community.getMetadata("side_bar_text");
                String identifier_uri = "hdl:" + community.getHandle();
                String rights = community.getMetadata("copyright_text");
                String title = community.getMetadata("name");
                
                dim.addContent(createField("dc","description",null,null,description));
                dim.addContent(createField("dc","description","abstract",null,description_abstract));
                dim.addContent(createField("dc","description","tableofcontents",null,description_table));
                dim.addContent(createField("dc","identifier","uri",null,identifier_uri));
                dim.addContent(createField("dc","rights",null,null,rights));
                dim.addContent(createField("dc","title",null,null,title));
            }
            // XXX FIXME: Nothing to crosswalk for bitstream?
            return dim;
        }
    }

    /**
     * Create a new DIM field element with the given attributes.
     *
     * @param schema The schema the DIM field belongs too.
     * @param element The element the DIM field belongs too.
     * @param qualifier The qualifier the DIM field belongs too.
     * @param language The language the DIM field belongs too.
     * @param value The value of the DIM field.
     * @return A new DIM field element
     */
    private static Element createField(String schema, String element, String qualifier, String language, String value)
    {
        Element field = new Element("field",DIM_NS);
        field.setAttribute("mdschema",schema);
        field.setAttribute("element",element);
        if (qualifier != null)
            field.setAttribute("qualifier",qualifier);
        if (language != null)
            field.setAttribute("lang",language);
        
        field.setText(checkedString(value));
        
        return field;
    }

    // Return string with non-XML characters (i.e. low control chars) excised.
    private static String checkedString(String value)
    {
        if (value == null)
            return null;
        String reason = Verifier.checkCharacterData(value);
        if (reason == null)
            return value;
        else
        {
            if (log.isDebugEnabled())
                log.debug("Filtering out non-XML characters in string, reason="+reason);
            StringBuffer result = new StringBuffer(value.length());
            for (int i = 0; i < value.length(); ++i)
            {
                char c = value.charAt(i);
                if (Verifier.isXMLCharacter((int)c))
                    result.append(c);
            }
            return result.toString();
        }
    }
}
