/*******************************************************************************
 * Copyright (c) 2009 Robert Schuster.
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
 *     Robert Schuster - initial API and implementation
 ******************************************************************************/
package sernet.verinice.service.commands;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;

import sernet.verinice.interfaces.GenericCommand;
import sernet.verinice.interfaces.IAuthAwareCommand;
import sernet.verinice.interfaces.IAuthService;
import sernet.verinice.interfaces.IBaseDao;
import sernet.verinice.interfaces.INoAccessControl;
import sernet.verinice.model.common.configuration.Configuration;

/**
 * Loads the configuration item of the currently logged in user.
 */
@SuppressWarnings("serial")
public class LoadCurrentUserConfiguration extends GenericCommand
        implements IAuthAwareCommand, INoAccessControl {

    private static final Logger log = Logger.getLogger(LoadCurrentUserConfiguration.class);

    private Configuration configuration = null;

    private transient IAuthService authService;

    public void execute() {
        String user = authService.getUsername();

        IBaseDao<Configuration, Serializable> dao = getDaoFactory().getDAO(Configuration.class);
        List<Configuration> confs = dao.findAll();

        for (Configuration c : confs) {
            if (user.equals(c.getUser())) {
                c.getRoles();
                configuration = c;
                if (log.isDebugEnabled()) {
                    log.debug("Account found: " + configuration);

                }
                return;
            }
        }
        if (log.isInfoEnabled()) {
            log.info("No account with user name: " + user + " found in list with " + confs.size()
                    + " entries.");
        }
    }

    /**
     * @return The {@link Configuration} instance of the currently logged in
     *         user or <code>null</code> if there is none.
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    public IAuthService getAuthService() {
        return authService;
    }

    public void setAuthService(IAuthService service) {
        authService = service;
    }
}
