package org.csstudio.opibuilder.editparts;

import java.util.logging.Level;

import org.csstudio.opibuilder.OPIBuilderPlugin;
import org.csstudio.opibuilder.model.AbstractWidgetModel;
import org.csstudio.opibuilder.model.DisplayModel;
import org.csstudio.opibuilder.util.WidgetsService;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

/**The central factory to create editpart for all widgets.
 * @author Sven Wende (class of same name in SDS)
 * @author Xihui Chen
 */
public class WidgetEditPartFactory implements EditPartFactory {

	private ExecutionMode executionMode;

	public WidgetEditPartFactory(ExecutionMode executionMode) {
		this.executionMode = executionMode;
	}

	public EditPart createEditPart(EditPart context, Object model) {
		EditPart part = getPartForModel(model);
		if(part != null){
			part.setModel(model);
			if(part instanceof AbstractBaseEditPart)
				((AbstractBaseEditPart)part).setExecutionMode(executionMode);
		}
		return part;
	}

	@SuppressWarnings("nls")
    private EditPart getPartForModel(Object model){
		if(model instanceof DisplayModel)
			return new DisplayEditpart();
		if(model instanceof AbstractWidgetModel){
			AbstractBaseEditPart editpart =
				WidgetsService.getInstance().getWidgetDescriptor(
					((AbstractWidgetModel)model).getTypeID()).getWidgetEditpart();
			return editpart;
		}
        OPIBuilderPlugin.getLogger().log(Level.WARNING,
                "Cannot create editpart for model object {0}",
                model == null ? "null" : model.getClass().getName());
		return null;
	}
}
