/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.converter.writer;

import java.util.logging.Logger;

import org.csstudio.opibuilder.converter.model.Edm_TwoDProfileMonitorClass;

/**
 * XML conversion class for Edm_TwoDProfileMonitorClass.
 *
 * @author Xihui Chen
 */
public class Opi_TwoDProfileMonitorClass extends OpiWidget {

    private static Logger log = Logger
            .getLogger("org.csstudio.opibuilder.converter.writer.Edm_TwoDProfileMonitorClass");
    private static final String typeId = "dawn.intensitygraph";
    private static final String name = "EDM Edm_TwoDProfileMonitorClass";
    private static final String version = "1.1";

    /**
     * Converts the Edm_TwoDProfileMonitorClass to OPI Dawn IntensityGraph XML.
     */
    public Opi_TwoDProfileMonitorClass(Context con, Edm_TwoDProfileMonitorClass r) {
        super(con, r);
        setTypeId(typeId);
        setName(name);
        setVersion(version);

        new OpiInt(widgetContext, "maximum", 255);
        new OpiInt(widgetContext, "minimum", 0);
        new OpiBoolean(widgetContext, "rgb_mode", false);

        // No legend or axes on the TwoDProfileMonitor
        new OpiBoolean(widgetContext, "show_ramp", false);
        new OpiBoolean(widgetContext, "x_axis_visible", false);
        new OpiBoolean(widgetContext, "y_axis_visible", false);


        if(r.getDataPvStr()!=null)
            new OpiString(widgetContext, "pv_name", convertPVName(r.getDataPvStr()));

        if (r.getUseFalseColourPvStr() != null) {
            String useFalseColourPvStr = r.getUseFalseColourPvStr();
            int color = 0;// Greyscale
            if (useFalseColourPvStr.equals("1")) { // JET
                color = 1; // JET
            }
            new OpiInt(widgetContext, "color_map", color);
        }

        if(r.isPvBasedDataSize() && r.getWidthPvStr() != null && r.getHeightPvStr() != null){
            createPVOutputRule(r, convertPVName(r.getWidthPvStr()), "data_width", "pv0", "DataWidthRule");
            createPVOutputRule(r, convertPVName(r.getHeightPvStr()), "data_height", "pv0", "DataHeightRule");
        }else{
            new OpiInt(widgetContext, "data_width", r.getDataWidth());
            new OpiInt(widgetContext, "data_height", Integer.parseInt(r.getHeightPvStr()));
        }

        log.config("Edm_twoDProfileMonitorClass written.");

    }

}
