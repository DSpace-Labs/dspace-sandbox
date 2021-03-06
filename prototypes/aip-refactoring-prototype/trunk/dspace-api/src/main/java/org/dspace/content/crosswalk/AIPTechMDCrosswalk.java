/*
 * AIPTechMDCrosswalk.java
 *
 * Version: $Revision: 1.2 $
 *
 * Date: $Date: 2006/03/27 02:57:09 $
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

import java.io.OutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import java.sql.SQLException;

import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.IngestionWrapper;
import org.dspace.content.Site;
import org.dspace.content.packager.PackageUtils;
import org.dspace.eperson.EPerson;
import org.dspace.authorize.AuthorizeException;
import org.dspace.handle.HandleManager;

import org.apache.log4j.Logger;

import org.jdom.Element;
import org.jdom.Namespace;

/**
 * Crosswalk of technical metadata for DSpace AIP.  This is
 * only intended for use by the METS AIP packager.   It borrows the
 * DIM XML format and DC field names, although it abuses the meaning
 * of Dublin Core terms and qualifiers because this format is
 * ONLY FOR DSPACE INTERNAL USE AND INGESTION.  It is needed to record
 * a complete and accurate image of all of the attributes an object
 * has in the RDBMS.
 *
 * It encodes the following common properties of all archival objects:
 *
 *   identifier.uri -- persistent identifier of object in URI form (e.g. Handle URN)
 *   relation.isPartOf -- persistent identifier of object's parent in URI form (e.g. Handle URN)
 *   relation.isReferencedBy -- if relevant, persistent identifier of other objects that map this one as a child.  May repeat.
 *
 * There may also be other fields, depending on the type of object,
 * which encode attributes that are not part of the descriptive metadata and
 * are not adequately covered by other technical MD formats (i.e. PREMIS).
 *
 *  Configuration entries:
 *    aip.ingest.createEperson -- boolean, create EPerson for Submitter
 *              automatically, on ingest, if it doesn't exist.
 *
 * @author Larry Stone
 * @version $Revision: 1.2 $
 */
public class AIPTechMDCrosswalk
    implements DisseminationCrosswalk, IngestionCrosswalk
{
    /** log4j category */
    private static Logger log = Logger.getLogger(AIPTechMDCrosswalk.class);

    /**
     * Get XML namespaces of the elements this crosswalk may return.
     * Returns the XML namespaces (as JDOM objects) of the root element.
     *
     * @return array of namespaces, which may be empty.
     */
    public Namespace[] getNamespaces()
    {
        Namespace result[] = new Namespace[1];
        result[0] = XSLTCrosswalk.DIM_NS;
        return result;
    }

    /**
     * Get the XML Schema location(s) of the target metadata format.
     * Returns the string value of the <code>xsi:schemaLocation</code>
     * attribute that should be applied to the generated XML.
     *  <p>
     * It may return the empty string if no schema is known, but crosswalk
     * authors are strongly encouraged to implement this call so their output
     * XML can be validated correctly.
     * @return SchemaLocation string, including URI namespace, followed by
     *  whitespace and URI of XML schema document, or empty string if unknown.
     */
    public String getSchemaLocation()
    {
        return "";
    }

    /**
     * Predicate: Can this disseminator crosswalk the given object.
     * Needed by OAI-PMH server implementation.
     *
     * @param dso  dspace object, e.g. an <code>Item</code>.
     * @return true when disseminator is capable of producing metadata.
     */
    public boolean canDisseminate(DSpaceObject dso)
    {
        return true;
    }

    /**
     * Predicate: Does this disseminator prefer to return a list of Elements,
     * rather than a single root Element?
     * <p>
     * Some metadata formats have an XML schema without a root element,
     * for example, the Dublin Core and Qualified Dublin Core formats.
     * This would be <code>true</code> for a crosswalk into QDC, since
     * it would "prefer" to return a list, since any root element it has
     * to produce would have to be part of a nonstandard schema.  In
     * most cases your implementation will want to return
     * <code>false</code>
     *
     * @return true when disseminator prefers you call disseminateList().
     */
    public boolean preferList()
    {
        return false;
    }

    /**
     * Execute crosswalk, returning List of XML elements.
     * Returns a <code>List</code> of JDOM <code>Element</code> objects representing
     * the XML produced by the crosswalk.  This is typically called when
     * a list of fields is desired, e.g. for embedding in a METS document
     * <code>xmlData</code> field.
     * <p>
     * When there are no results, an
     * empty list is returned, but never <code>null</code>.
     *
     * @param dso the  DSpace Object whose metadata to export.
     * @return results of crosswalk as list of XML elements.
     *
     * @throws CrosswalkInternalException (<code>CrosswalkException</code>) failure of the crosswalk itself.
     * @throws CrosswalkObjectNotSupported (<code>CrosswalkException</code>) Cannot crosswalk this kind of DSpace object.
     * @throws IOException  I/O failure in services this calls
     * @throws SQLException  Database failure in services this calls
     * @throws AuthorizeException current user not authorized for this operation.
     */
    public List disseminateList(DSpaceObject dso)
        throws CrosswalkException, IOException, SQLException,
               AuthorizeException
    {
        Element dim = disseminateElement(dso);
        return dim.getChildren();
    }

    /**
     * Execute crosswalk, returning one XML root element as
     * a JDOM <code>Element</code> object.
     * This is typically the root element of a document.
     * <p>
     *
     * @param dso the  DSpace Object whose metadata to export.
     * @return root Element of the target metadata, never <code>null</code>
     *
     * @throws CrosswalkInternalException (<code>CrosswalkException</code>) failure of the crosswalk itself.
     * @throws CrosswalkObjectNotSupported (<code>CrosswalkException</code>) Cannot crosswalk this kind of DSpace object.
     * @throws IOException  I/O failure in services this calls
     * @throws SQLException  Database failure in services this calls
     * @throws AuthorizeException current user not authorized for this operation.
     */
    public Element disseminateElement(DSpaceObject dso)
        throws CrosswalkException, IOException, SQLException,
               AuthorizeException
    {
        List dc = new ArrayList();
        if (dso.getType() == Constants.ITEM)
        {
            Item item = (Item)dso;
            EPerson is = item.getSubmitter();
            if (is != null)
                dc.add(makeDC("contributor", null, is.getEmail()));
            dc.add(makeDC("identifier", "uri", "hdl:" + item.getHandle()));
            Collection owningColl = item.getOwningCollection();
            String owner = owningColl.getHandle();
            if (owner != null)
                dc.add(makeDC("relation", "isPartOf", "hdl:"+owner));
            Collection inColl[] = item.getCollections();
            for (int i = 0; i < inColl.length; ++i)
            {
                if (inColl[i].getID() != owningColl.getID())
                {
                    String h = inColl[i].getHandle();
                    if (h != null)
                        dc.add(makeDC("relation", "isReferencedBy", "hdl:"+h));
                }
            }
            if (item.isWithdrawn())
                dc.add(makeDC("rights", "accessRights", "WITHDRAWN"));
        }
        else if (dso.getType() == Constants.BITSTREAM)
        {
            Bitstream bitstream = (Bitstream)dso;
            String bsName = bitstream.getName();
            if (bsName != null)
                dc.add(makeDC("title", null, bsName));
            String bsSource = bitstream.getSource();
            if (bsSource != null)
                dc.add(makeDC("title", "alternative", bsSource));
            String bsDesc = bitstream.getDescription();
            if (bsDesc != null)
                dc.add(makeDC("description", null, bsDesc));
            String bsUfmt = bitstream.getUserFormatDescription();
            if (bsUfmt != null)
                dc.add(makeDC("format", null, bsUfmt));
            BitstreamFormat bsf = bitstream.getFormat();
            dc.add(makeDC("format", "medium", bsf.getShortDescription()));
            dc.add(makeDC("format", "mimetype", bsf.getMIMEType()));
            dc.add(makeDC("format", "supportlevel", BitstreamFormat.supportLevelText[bsf.getSupportLevel()]));
            dc.add(makeDC("format", "internal", Boolean.toString(bsf.isInternal())));
        }
        else if (dso.getType() == Constants.COLLECTION)
        {
            Collection collection = (Collection)dso;
            dc.add(makeDC("identifier", "uri", "hdl:" + dso.getHandle()));
            Community owners[] = collection.getCommunities();
            String ownerHdl = owners[0].getHandle();
            if (ownerHdl != null)
                dc.add(makeDC("relation", "isPartOf", "hdl:" + ownerHdl));
            for (int i = 1; i < owners.length; ++i)
            {
                String h = owners[i].getHandle();
                if (h != null)
                    dc.add(makeDC("relation", "isReferencedBy", "hdl:" + h));
            }
        }
        else if (dso.getType() == Constants.COMMUNITY)
        {
            Community  community = (Community)dso;
            dc.add(makeDC("identifier", "uri", "hdl:" + dso.getHandle()));
            Community owner = community.getParentCommunity();
            String ownerHdl = null;
            if (owner == null)
                ownerHdl = Site.getSiteHandle();
            else
                ownerHdl = owner.getHandle();
            if (ownerHdl != null)
                dc.add(makeDC("relation", "isPartOf", "hdl:" + ownerHdl));
        }

        DCValue result[] = (DCValue[])dc.toArray(new DCValue[dc.size()]);
        return XSLTDisseminationCrosswalk.createDIM(dso, result);
    }

    private static DCValue makeDC(String element, String qualifier, String value)
    {
        DCValue dcv = new DCValue();
        dcv.schema = "dc";
        dcv.language = null;
        dcv.element = element;
        dcv.qualifier = qualifier;
        dcv.value = value;
        return dcv;
    }

    /**
     * Ingest a whole document.  Build Document object around root element,
     * and feed that to the transformation, since it may get handled
     * differently than a List of metadata elements.
     */
    public void ingest(Context context, DSpaceObject dso, Element root)
        throws CrosswalkException, IOException, SQLException, AuthorizeException
    {
        ingest(context, dso, root.getChildren());
    }

    /**
     * Translate metadata with XSL stylesheet and ingest it.
     * Translation produces a list of DIM "field" elements;
     * these correspond directly to Item.addMetadata() calls so
     * they are simply executed.
     */
    public void ingest(Context context, DSpaceObject dso, List dimList)
        throws CrosswalkException,
               IOException, SQLException, AuthorizeException
    {
        int type = dso.getType();

        // accumulate values for bitstream format in case we have to make one
        String bsfShortName = null;
        String bsfMIMEType = null;
        int bsfSupport = BitstreamFormat.KNOWN;
        boolean bsfInternal = false;

        Iterator di = dimList.iterator();
        while (di.hasNext())
        {
            Element field = (Element)di.next();

            // if we get <dim> in a list, recurse.
            if (field.getName().equals("dim") && field.getNamespace().equals(XSLTCrosswalk.DIM_NS))
                ingest(context, dso, field.getChildren());
            else if (field.getName().equals("field") && field.getNamespace().equals(XSLTCrosswalk.DIM_NS))
            {
                String schema = field.getAttributeValue("mdschema");
                if (schema.equals("dc"))
                {
                    String dcField = field.getAttributeValue("element");
                    String qualifier = field.getAttributeValue("qualifier");
                    if (qualifier != null)
                        dcField += "."+qualifier;
                    String value = field.getText();

                    if (type == Constants.BITSTREAM)
                    {
                        Bitstream bitstream = (Bitstream)dso;
                        if (dcField.equals("title"))
                        {
                            bitstream.setName(value);
                        }
                        else if (dcField.equals("title.alternative"))
                        {
                            bitstream.setSource(value);
                        }
                        else if (dcField.equals("description"))
                        {
                            bitstream.setDescription(value);
                        }
                        else if (dcField.equals("format"))
                        {
                            bitstream.setUserFormatDescription(value);
                        }
                        else if (dcField.equals("format.medium"))
                        {
                            bsfShortName = value;
                        }
                        else if (dcField.equals("format.mimetype"))
                        {
                            bsfMIMEType = value;
                        }
                        else if (dcField.equals("format.supportlevel"))
                        {
                            int sl = BitstreamFormat.getSupportLevelID(value);
                            if (sl < 0)
                                throw new MetadataValidationException("Got unrecognized value for bitstream support level: "+value);
                            else
                                bsfSupport = sl;
                        }
                        else if (dcField.equals("format.internal"))
                        {
                            bsfInternal = (Boolean.valueOf(value)).booleanValue();
                        }
                        else
                            log.warn("Got unrecognized DC field for Bitstream: "+dcField);
                    }
                    else if (type == Constants.INGESTION_ITEM ||
                             type == Constants.INGESTION_COMMUNITY ||
                             type == Constants.INGESTION_COLLECTION)
                    {
                        IngestionWrapper iw = (IngestionWrapper)dso;
                     
                        // submitter
                        if (dcField.equals("contributor"))
                        {
                            EPerson sub = EPerson.findByEmail(context, value);
                     
                            // if eperson doesn't exist yet, optionally create it:
                            if (sub == null)
                            {
                                if (ConfigurationManager.getBooleanProperty("aip.ingest.createEperson"))
                                {
                                    sub = EPerson.create(context);
                                    sub.setEmail(value);
                                    sub.setCanLogIn(false);
                                    sub.update();
                                }
                                else
                                    log.warn("Ignoring unknown Submitter="+value+" in AIP Tech MD, no matching EPerson and aip.ingest.createEperson is false.");
                            }
                            if (sub != null)
                                iw.setSubmitter(sub);
                        }
                        else if (dcField.equals("rights.accessRights"))
                        {
                            if (value.equalsIgnoreCase("WITHDRAWN"))
                                iw.setWithdrawn(true);
                        }
                        else if (dcField.equals("relation.isPartOf"))
                        {
                            String ph = decodeHandleURI(value);
                            if (ph != null)
                            {
                                DSpaceObject parent = HandleManager.resolveToObject(context, ph);
                                if (parent == null)
                                    log.error("Cannot set IngestionWrapper parent, handle lookup failed for: "+value);
                                else
                                    iw.setParent(parent);
                            }
                        }
                        else if (dcField.equals("identifier.uri"))
                        {
                            String hdl = decodeHandleURI(value);
                            if (hdl != null)
                                iw.setHandle(hdl);
                        }

                        // Ignore relation.isReferencedBy since it only
                        // lists _extra_ mapped parents, not the primary one.
                        // These get connected when collections are re-mapped.
                        else if (dcField.equals("relation.isReferencedBy"))
                            log.debug("Ignoring relation.isReferencedBy");

                        else
                            log.warn("Got unrecognized DC field for IngestionWrapper: "+dcField);
                    }
                    else if (type == Constants.COLLECTION)
                        log.warn("Got unrecognized DC field for Collection: "+dcField);
                    else if (dso.getType() == Constants.COMMUNITY)
                        log.warn("Got unrecognized DC field for Community: "+dcField);
                    else if (type == Constants.ITEM)
                        log.warn("Got unrecognized DC field for Item: "+dcField);
                     
                     
                }
                else
                    log.warn("Skipping DIM field with mdschema=\""+schema+"\".");

            }
            else
            {
                log.error("Got unexpected element in DIM list: "+field.toString());
                throw new MetadataValidationException("Got unexpected element in DIM list: "+field.toString());
            }
        }

        // final step: find or create bitstream format since it
        // takes the accumulation of a few values:
        if (type == Constants.BITSTREAM && bsfShortName != null)
        {
            BitstreamFormat bsf = BitstreamFormat.findByShortDescription(context, bsfShortName);
            if (bsf == null && bsfMIMEType != null)
                bsf = PackageUtils.findOrCreateBitstreamFormat(context,
                                                               bsfShortName,
                                                               bsfMIMEType,
                                                               bsfShortName,
                                                               bsfSupport,
                                                               bsfInternal);
            if (bsf != null)
                ((Bitstream)dso).setFormat(bsf);
            else
                log.warn("Failed to find or create bitstream format named \""+bsfShortName+"\"");
        }
    }

    // parse the hdl: URI/URN format into a raw Handle.
    private String decodeHandleURI(String value)
    {
        if (value.startsWith("hdl:"))
            return value.substring(4);
        else
            return null;
    }
}
