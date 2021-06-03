/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.reader.appliance;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Preference Page for the Archiver Appliance Reader
 *
 * @author Rebecca Williams
 */
public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /**
     * Initialize
     */
    public PreferencePage() {
        super(GRID);
        setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, Activator.PLUGIN_ID));
    }

    /** {@inheritDoc} */
    @Override
    public void init(IWorkbench workbench) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    protected void createFieldEditors() {
        setMessage(Messages.PreferenceTitle);
        final Composite parent = getFieldEditorParent();
        addField(new BooleanFieldEditor(Preferences.SHOW_DISCONNECT, Messages.ShowDisconnect, parent));
    }
}
