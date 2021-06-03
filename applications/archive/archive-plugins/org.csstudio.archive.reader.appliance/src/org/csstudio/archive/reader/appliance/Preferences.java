/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.reader.appliance;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;

/** Access to preferences for the Archiver Appliance reader.
 *
 *  <p>See preferences.ini for explanation of settings.
 *
 *  @author Rebecca Williams
 */
public class Preferences {
    final public static String SHOW_DISCONNECT = "show_disconnect";

    static public boolean showDisconnect() {
        final IPreferencesService prefs = Platform.getPreferencesService();
        if (prefs == null)
            return false;
        return prefs.getBoolean(Activator.PLUGIN_ID, SHOW_DISCONNECT, false, null);
    }
}
