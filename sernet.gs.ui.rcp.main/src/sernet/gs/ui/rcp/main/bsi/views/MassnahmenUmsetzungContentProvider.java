/*******************************************************************************
 * Copyright (c) 2009 Alexander Koderman <ak[at]sernet[dot]de>.
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either version 3 
 * of the License, or (at your option) any later version.
 *     This program is distributed in the hope that it will be useful,    
 * but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 *     You should have received a copy of the GNU Lesser General Public 
 * License along with this program. 
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Alexander Koderman <ak[at]sernet[dot]de> - initial API and implementation
 *     Robert Schuster <r.schuster@tarent.de> - added compound specific support
 ******************************************************************************/
package sernet.gs.ui.rcp.main.bsi.views;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

import sernet.gs.service.Retriever;
import sernet.gs.ui.rcp.main.Activator;
import sernet.gs.ui.rcp.main.ExceptionUtil;
import sernet.gs.ui.rcp.main.bsi.filter.MassnahmenUmsetzungFilter;
import sernet.gs.ui.rcp.main.common.model.CnAElementFactory;
import sernet.gs.ui.rcp.main.common.model.PlaceHolder;
import sernet.gs.ui.rcp.main.service.ServiceFactory;
import sernet.verinice.interfaces.CommandException;
import sernet.verinice.model.bpm.TodoViewItem;
import sernet.verinice.model.bsi.Anwendung;
import sernet.verinice.model.bsi.BSIModel;
import sernet.verinice.model.bsi.BausteinUmsetzung;
import sernet.verinice.model.bsi.Client;
import sernet.verinice.model.bsi.Gebaeude;
import sernet.verinice.model.bsi.IBSIModelListener;
import sernet.verinice.model.bsi.ITVerbund;
import sernet.verinice.model.bsi.MassnahmenUmsetzung;
import sernet.verinice.model.bsi.NetzKomponente;
import sernet.verinice.model.bsi.Person;
import sernet.verinice.model.bsi.Raum;
import sernet.verinice.model.bsi.Server;
import sernet.verinice.model.bsi.SonstIT;
import sernet.verinice.model.common.ChangeLogEntry;
import sernet.verinice.model.common.CnALink;
import sernet.verinice.model.common.CnATreeElement;
import sernet.verinice.model.common.NullListener;
import sernet.verinice.service.commands.task.FindMassnahmeById;

/**
 * Gets Massnahmen from current BSIModel and reacts to model changes.
 * 
 * Update performed in synchronized thread, but only if necessary, to optimize
 * performance.
 * 
 * @author koderman[at]sernet[dot]de
 *
 */
class MassnahmenUmsetzungContentProvider implements IStructuredContentProvider {

    private static final Logger LOG = Logger.getLogger(MassnahmenUmsetzungContentProvider.class);

    private static final int ADD = 0;
    private static final int UPDATE = 1;
    private static final int REMOVE = 2;
    private static final int REFRESH = 3;

    private TableViewer viewer;
    private GenericMassnahmenView todoView;

    private IBSIModelListener modelListener = new NullListener() {

        @Override
        public void childAdded(CnATreeElement category, CnATreeElement child) {
            if (child instanceof BausteinUmsetzung && isOfInterest(child)) {
                reloadMeasures();
            } else if (child instanceof ITVerbund) {
                todoView.compoundAdded((ITVerbund) child);
            }
        }

        @Override
        public void linkChanged(CnALink old, CnALink link, Object source) {
            if (link.getDependency() instanceof Person) {
                updateViewer(REFRESH, null);
            }
        }

        @Override
        public void linksAdded(Collection<CnALink> links) {
            if (links.stream().anyMatch(link -> link.getDependency() instanceof Person)) {
                reloadMeasures();
            }
        }

        @Override
        public void linkRemoved(CnALink link) {
            if (link.getDependency() instanceof Person) {
                reloadMeasures();
            }
        }

        @Override
        public void childChanged(CnATreeElement child) {
            if (child instanceof MassnahmenUmsetzung) {
                try {
                    if (!isOfInterest(child)) {
                        LOG.debug("MassnahmenUmsetzung is not of interest for view: " + child);
                        return;
                    }
                    Activator.inheritVeriniceContextState();
                    FindMassnahmeById command = new FindMassnahmeById(child.getDbId());
                    command = ServiceFactory.lookupCommandService().executeCommand(command);
                    List<TodoViewItem> items = command.getAll();
                    if (!items.isEmpty()) {
                        TodoViewItem item = items.get(0);
                        updateViewer(UPDATE, item);
                    }
                } catch (CommandException e) {
                    Logger.getLogger(this.getClass())
                            .debug("Fehler beim Aktualisieren von TodoView", e);
                }
            } else if (child instanceof ITVerbund) {
                todoView.compoundChanged((ITVerbund) child);
            }
        }

        @Override
        public void childRemoved(CnATreeElement category, CnATreeElement child) {
            if (child instanceof ITVerbund) {
                todoView.compoundRemoved((ITVerbund) child);
            } else if (canContainMeasures(child) && isOfInterest(child)) {
                // When an element has been deleted it could have contained
                // BausteinUmsetzung
                // and MassnahmenUmsetzung instances. If this happens in an
                // ITVerbund we are
                // watching, reload the measures.
                reloadMeasures();
            }
        }

        /**
         * @deprecated Es soll stattdessen {@link #modelRefresh(Object)}
         *             verwendet werden
         */
        @Override
        public void modelRefresh() {

            modelRefresh(null);
        }

        @Override
        public void modelRefresh(Object source) {
            if (source != null) {
                todoView.setLoadBlockNumber(0);
                todoView.getLoadMoreAction().setEnabled(true);
                reloadMeasures();
            }
        }

        @Override
        public void databaseChildAdded(CnATreeElement child) {
            if (child instanceof BausteinUmsetzung && isOfInterest(child)) {
                reloadMeasures();
            }
        }

        @Override
        public void databaseChildChanged(CnATreeElement child) {
            childChanged(child);
        }

        @Override
        public void databaseChildRemoved(CnATreeElement child) {
            childRemoved(child.getParent(), child);
        }

        /*
         * @see sernet.gs.ui.rcp.main.bsi.model.IBSIModelListener#
         * databaseChildRemoved(java.lang.Integer)
         */
        public void databaseChildRemoved(ChangeLogEntry entry) {
            // TODO server: remove element
            // TODO akoderman really? this seems to be working fine.
        }

    };

    public MassnahmenUmsetzungContentProvider(GenericMassnahmenView todoView) {
        this.todoView = todoView;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        this.viewer = (TableViewer) viewer;
        BSIModel model = CnAElementFactory.getLoadedModel();
        // When the DB is closed the clearing of the massnahmen table causes
        // this method to be called.
        // However at that time no model exists anymore. So we can safely skip
        // this part.
        if (model != null) {
            model.removeBSIModelListener(modelListener);
            model.addBSIModelListener(modelListener);
        }
    }

    @SuppressWarnings("unchecked")
    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof PlaceHolder) {
            return new Object[] { inputElement };
        }
        List<TodoViewItem> mns = (List<TodoViewItem>) inputElement;
        return mns.toArray(new Object[mns.size()]);

    }

    @Override
    public void dispose() {
        BSIModel model = CnAElementFactory.getLoadedModel();
        model.removeBSIModelListener(modelListener);
    }

    void reloadMeasures() {
        Display.getDefault().asyncExec(() -> {
            try {
                todoView.reloadMeasures();
            } catch (RuntimeException e) {
                ExceptionUtil.log(e, "Konnte Realisierungsplan nicht neu laden.");
            }
        });
    }

    void updateViewer(final int type, final Object child) {
        if (Display.getCurrent() != null) {
            switch (type) {
            case ADD:
                viewer.add(child);
                return;
            case UPDATE:
                viewer.update(child, new String[] { MassnahmenUmsetzungFilter.UMSETZUNG_PROPERTY });
                return;
            case REMOVE:
                viewer.remove(child);
                return;
            case REFRESH:
                viewer.refresh();
                return;
            }
            return;
        }
        Display.getDefault().asyncExec(() -> {
            switch (type) {
            case ADD:
                viewer.add(child);
                return;
            case UPDATE:
                viewer.update(child, new String[] { MassnahmenUmsetzungFilter.UMSETZUNG_PROPERTY });
                return;
            case REMOVE:
                viewer.remove(child);
                return;
            case REFRESH:
                viewer.refresh();
                return;
            }
        });
    }

    /**
     * Returns whether the given {@link CnATreeElement} instance is of interest
     * for the view.
     * 
     * <p>
     * Such an instance is of interest when it belongs to the currently selected
     * IT-Verbund of the view.
     * </p>
     * 
     * <p>
     * This method is needed to decide whether the view's model will be updated
     * when the given {@link MassnahmenUmsetzung} instance changed or a
     * {@link BausteinUmsetzung} instance got removed etc.
     * </p>
     */
    private boolean isOfInterest(CnATreeElement child) {
        ITVerbund expectedCompound = todoView.getCurrentCompound();

        // No compound selected -> nothing is of interest.
        if (expectedCompound == null) {
            return false;
        }
        // Otherwise climb the tree.
        CnATreeElement parent = child.getParent();
        while (!(parent instanceof ITVerbund)) {
            if (parent == null) {
                LOG.warn("Element with no IT-Verbund ancestor. Skipping ...");
                return false;
            }
            parent = Retriever.checkRetrieveParent(parent);
            parent = parent.getParent();
        }

        return parent.equals(expectedCompound);
    }

    private boolean canContainMeasures(CnATreeElement child) {
        // TODO rschus: Could be more elegantly solved by adding a method
        // 'canHaveMeasures'
        // to CnATreeElement, implement it to return false by default and
        // override it in the
        // classes below to return true.
        Class<?>[] classes = { BausteinUmsetzung.class, Anwendung.class, Server.class, Client.class,
                SonstIT.class, Gebaeude.class, NetzKomponente.class, Raum.class };

        for (Class<?> c : classes) {
            if (c.isAssignableFrom(child.getClass())) {
                return true;
            }
        }

        return false;
    }
}
