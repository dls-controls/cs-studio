/*******************************************************************************
 * Copyright (c) 2014 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.pvtable.persistence;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.csstudio.apputil.xml.DOMHelper;
import org.csstudio.apputil.xml.XMLWriter;
import org.csstudio.display.pvtable.Preferences;
import org.csstudio.display.pvtable.model.PVTableItem;
import org.csstudio.display.pvtable.model.PVTableModel;
import org.csstudio.display.pvtable.model.VTypeHelper;
import org.epics.vtype.VType;
import org.epics.vtype.ValueFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Persist PVTableModel as EPICS Autosave file
 *  <p>
 *  This file format is used by the EPICS synApps 'autosave' module,
 *  written by APS/AOD/BCDA Tim Mooney,
 *  http://www.aps.anl.gov/bcda/synApps/autosave/autosave.html
 *  
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVTableAutosavePersistence extends PVTablePersistence
{
    final private static String END_MARKER = "<END>";
    
    /** @param stream XML stream
     *  @return PV table model
     *  @throws Exception on error
     */
    public PVTableModel read(final InputStream stream) throws Exception
    {
        final BufferedReader input = new BufferedReader(new InputStreamReader(stream));
        
        final PVTableModel model = new PVTableModel();
        int line_no = 0;
        for (String line = input.readLine();  line != null;   line = input.readLine())
        {
            ++line_no;
            line = line.trim();
            // Skip comments, empty lines
            if (line.startsWith("#")  ||  line.isEmpty())
                continue;
            // Ignore the end marker (and stop reading)
            if (line.startsWith(END_MARKER))
                break;
            
            // Parse "PV Value"
            final int sep = line.indexOf(' ');
            if (sep < 0)
            {
                Logger.getLogger(getClass().getName()).log(Level.WARNING, "Missing space after PV name in line {0}", line_no);
                continue;
            }
            final String pv_name = line.substring(0, sep);
            final String value = line.substring(sep + 1);
            model.addItem(pv_name, Preferences.getTolerance(), createValue(value));
        }
        
        return model;
    }
}
