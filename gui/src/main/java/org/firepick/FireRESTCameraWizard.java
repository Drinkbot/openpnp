/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
*/

package org.firepick;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.support.JBindings.WrappedBinding;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.reference.camera.TableScannerCamera;
import org.openpnp.machine.reference.wizards.ReferenceCameraConfigurationWizard;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

@SuppressWarnings("serial")
public class FireRESTCameraWizard extends ReferenceCameraConfigurationWizard {
	private final FireRESTCamera camera;

	private JPanel panelGeneral;

	private WrappedBinding<FireRESTCamera, String, JTextField, String> sourceUriBinding;

	public FireRESTCameraWizard(
            FireRESTCamera camera) {
	    super(camera);
	    
		this.camera = camera;
		
		panelGeneral = new JPanel();
		contentPanel.add(panelGeneral);
		panelGeneral.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "General", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));
		
		lblUrl = new JLabel("URL");
		panelGeneral.add(lblUrl, "2, 2, right, default");
		
		textFieldUrl = new JTextField();
		panelGeneral.add(textFieldUrl, "4, 2, 3, 1, fill, default");
		textFieldUrl.setColumns(10);
		
		lblCache = new JLabel("Cache");
		panelGeneral.add(lblCache, "2, 4");
		
		lblCacheInfo = new JLabel("");
		panelGeneral.add(lblCacheInfo, "4, 4");
		
		btnDelete = new JButton("Delete Cache");
		btnDelete.setAction(deleteCacheAction);
		panelGeneral.add(btnDelete, "6, 4");
	}

	@Override
	public void createBindings() {
	    super.createBindings();
	    
		sourceUriBinding = addWrappedBinding(camera, "sourceUri", textFieldUrl, "text");
		bind(UpdateStrategy.READ, camera, "cacheSizeDescription", lblCacheInfo, "text");
	}
	
	@Override
	public void validateInput() throws Exception {
	    super.validateInput();
	    
		if (sourceUriBinding.getWrapper().getValue() != null) {
			String sourceUri = sourceUriBinding.getWrapper().getValue().trim();
			if (!sourceUri.endsWith("/")) {
				sourceUri += "/";
			}
			sourceUriBinding.getWrapper().setValue(sourceUri);
		}
	}

	private JLabel lblUrl;
	private JTextField textFieldUrl;
	private JLabel lblCache;
	private JLabel lblCacheInfo;
	private JButton btnDelete;
	private final Action deleteCacheAction = new SwingAction();
	private class SwingAction extends AbstractAction {
		public SwingAction() {
			putValue(NAME, "Delete Cache");
			putValue(SHORT_DESCRIPTION, "Delete the on disk cache.");
		}
		public void actionPerformed(ActionEvent e) {
			try {
				camera.clearCache();
			}
			catch (Exception ex) {
				MessageBoxes.errorBox(getTopLevelAncestor(), "Error", ex);
			}
		}
	}
}