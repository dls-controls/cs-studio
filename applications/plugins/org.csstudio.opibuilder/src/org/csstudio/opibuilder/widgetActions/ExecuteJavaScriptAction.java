package org.csstudio.opibuilder.widgetActions;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

import org.csstudio.opibuilder.OPIBuilderPlugin;
import org.csstudio.opibuilder.properties.FilePathProperty;
import org.csstudio.opibuilder.properties.WidgetPropertyCategory;
import org.csstudio.opibuilder.script.ScriptService;
import org.csstudio.opibuilder.util.ConsoleService;
import org.csstudio.opibuilder.util.ResourceUtil;
import org.csstudio.opibuilder.widgetActions.WidgetActionFactory.ActionType;
import org.csstudio.platform.ui.util.UIBundlingThread;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Script;

/**The action executing plain javascript.
 * @author Xihui Chen
 *
 */
public class ExecuteJavaScriptAction extends AbstractWidgetAction {

	public static final String PROP_PATH = "path";//$NON-NLS-1$
	private Script script;
	private ImporterTopLevel scriptScope;
	private Context scriptContext;

	@Override
	protected void configureProperties() {
		addProperty(new FilePathProperty(
				PROP_PATH, "File Path", WidgetPropertyCategory.Basic, new Path(""),
				new String[]{"js"}));

	}

	@Override
	public ActionType getActionType() {
		return ActionType.EXECUTE_JAVASCRIPT;
	}

	@Override
	public void run() {
		try {
			if(script == null){
				scriptContext = ScriptService.getInstance().getScriptContext();
				scriptScope = new ImporterTopLevel(scriptContext);
				//read file
				IPath absolutePath = getPath();
				if(!getPath().isAbsolute()){
		    		absolutePath =
		    			ResourceUtil.buildAbsolutePath(getWidgetModel(), getPath());
		    	}
				InputStream inputStream = ResourceUtil.pathToInputStream(absolutePath);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(inputStream));

				//compile
				script = scriptContext.compileReader(reader, "script", 1, null); //$NON-NLS-1$
				inputStream.close();
				reader.close();
			}


			UIBundlingThread.getInstance().addRunnable(new Runnable() {

				public void run() {

						try {
							script.exec(scriptContext, scriptScope);
						} catch (Exception e) {
							final String message =  "Error exists in script " + getPath();
		                    OPIBuilderPlugin.getLogger().log(Level.WARNING, message, e);
							ConsoleService.getInstance().writeError(message + "\n" + e.getMessage()); //$NON-NLS-1$
						}
				}
			});
		} catch (Exception e) {
			final String message = "Failed to execute JavaScript: " + getPath();
            OPIBuilderPlugin.getLogger().log(Level.WARNING, message, e);
			ConsoleService.getInstance().writeError(message + "\n" + e.getMessage()); //$NON-NLS-1$
		}
	}

	private IPath getPath(){
		return (IPath)getPropertyValue(PROP_PATH);
	}



	@Override
	public String getDefaultDescription() {
		return super.getDefaultDescription() + " " + getPath(); //$NON-NLS-1$
	}

}
