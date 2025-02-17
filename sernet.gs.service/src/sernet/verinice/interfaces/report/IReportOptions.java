/*******************************************************************************
 * Copyright (c) 2010 Robert Schuster <r.schuster@tarent.de>.
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
 *     Robert Schuster <r.schuster@tarent.de> - initial API and implementation
 ******************************************************************************/
package sernet.verinice.interfaces.report;

import java.io.File;

/**
 * Collection of options that is used to set up a report generation process.
 * 
 * @author Robert Schuster <r.schuster@tarent.de>
 *
 */
public interface IReportOptions {
	public static final String CLASSIFICATION_HINT = "classification_hint";
	String getClassificationHint();
	IOutputFormat getOutputFormat();

	File getOutputFile();
	
	boolean isToBeCompressed();
	
	boolean isToBeEncrypted();
	
	/**
	 * The optional root element for the report.
	 */
	void setRootElement(Integer rootElement);
	
	/**
	 * The optional root elements for the report.
	 */
	void setRootElements(Integer[] rootElements);

	/**
	 * Gets the root element for the report.
	 * @return
	 */
    Integer getRootElement();
    
    /**
     * Get multiple root elements for the report.
     * @return
     */
    Integer[] getRootElements();
    
    /**
     * returns url of verinice server to communicate with
     * @return
     */
    String getServerURL();
    
}
