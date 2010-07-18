/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2010 Volker Boerchers
 *
 *  This file author is Volker Boerchers
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 */
package org.freeplane.features.mindmapmode.link;

import java.awt.event.ActionEvent;

import org.freeplane.core.controller.Controller;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.features.common.link.LinkController;
import org.freeplane.features.common.map.NodeModel;

class AddMenuItemLinkAction extends AFreeplaneAction {
	private static final long serialVersionUID = 1L;

	public AddMenuItemLinkAction(final Controller controller) {
		super(AddMenuItemLinkAction.class.getSimpleName());
	}

	public void actionPerformed(final ActionEvent e) {
		final NodeModel selectedNode = getModeController().getMapController().getSelectedNode();
		final SelectMenuItemDialog dialog = new SelectMenuItemDialog(selectedNode);
		final String menuItemKey = dialog.getMenuItemKey();
		if (menuItemKey != null) {
			((MLinkController) LinkController.getController(getModeController())).setLink(selectedNode, LinkController
			    .createMenuItemLink(menuItemKey), false);
		}
	}
}
