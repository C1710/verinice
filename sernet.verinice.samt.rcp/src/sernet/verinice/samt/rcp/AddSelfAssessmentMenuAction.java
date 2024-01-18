package sernet.verinice.samt.rcp;

import java.io.IOException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import sernet.gs.ui.rcp.main.Activator;
import sernet.gs.ui.rcp.main.ExceptionUtil;
import sernet.hui.common.VeriniceContext;
import sernet.springclient.RightsServiceClient;
import sernet.verinice.interfaces.ActionRightIDs;
import sernet.verinice.interfaces.CommandException;
import sernet.verinice.interfaces.IInternalServerStartListener;
import sernet.verinice.rcp.RightEnabledUserInteraction;

public class AddSelfAssessmentMenuAction
        implements IWorkbenchWindowActionDelegate, RightEnabledUserInteraction {

    public static final String ID = "sernet.verinice.samt.rcp.AddSelfAssessmentMenuAction"; //$NON-NLS-1$

    @Override
    public void dispose() {
        // do nothing
    }

    @Override
    public void init(IWorkbenchWindow window) {
        // do nothing
    }

    @Override
    public void run(IAction action) {
        if (checkRights()) {
            CreateNewSelfAssessmentService createSamtService = new CreateNewSelfAssessmentService();
            try {
                createSamtService.createSelfAssessment();
            } catch (CommandException e) {
                ExceptionUtil.log(e, Messages.AddSelfAssessmentMenuAction_1);
            } catch (IOException e) {
                ExceptionUtil.log(e, Messages.AddSelfAssessmentMenuAction_2);
            }
        }
    }

    @Override
    public void selectionChanged(final IAction action, ISelection selection) {
        if (Activator.getDefault().isStandalone()
                && !Activator.getDefault().getInternalServer().isRunning()) {
            IInternalServerStartListener listener = e -> {
                if (e.isStarted()) {
                    action.setEnabled(checkRights());
                }
            };
            Activator.getDefault().getInternalServer().addInternalServerStatusListener(listener);
        } else {
            action.setEnabled(checkRights());
        }
    }

    /*
     * @see sernet.verinice.interfaces.RightEnabledUserInteraction#getRightID()
     */
    @Override
    public String getRightID() {
        return ActionRightIDs.ADDSECURITYASSESSMENT;
    }

}