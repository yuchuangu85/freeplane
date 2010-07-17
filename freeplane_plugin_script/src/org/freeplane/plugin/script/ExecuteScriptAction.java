/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2009 Volker Boerchers
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
package org.freeplane.plugin.script;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.freeplane.core.controller.Controller;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.FileUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.common.map.NodeModel;
import org.freeplane.features.mindmapmode.MModeController;

/**
 * Action that executes a script defined by filename.
 * 
 * @author vboerchers
 */
//not needed: @ActionLocationDescriptor(locations = { "/menu_bar/extras/first/scripting" })
public class ExecuteScriptAction extends AFreeplaneAction {
	private static final long serialVersionUID = 1L;

	/** controls how often a script is executed in case of a multi selection. */
	public enum ExecutionMode {
		/** once with <code>node</code> set to one selected (random) node. */
		ON_SINGLE_NODE,
		/** n times for n selected nodes, once for each node. */
		ON_SELECTED_NODE,
		/** script on every selected node and recursively on all of its children. */
		ON_SELECTED_NODE_RECURSIVELY
	}

	private final String script;
	private final ExecutionMode mode;
	private final boolean cacheContent;
	private String content;

	public ExecuteScriptAction(final Controller controller, final String scriptName, final String menuItemName,
	                           final String script, final ExecutionMode mode, final boolean cacheContent) {
		super(ExecuteScriptAction.makeMenuItemKey(scriptName, mode), menuItemName, null);
		this.script = script;
		this.mode = mode;
		this.cacheContent = cacheContent;
	}

	private static String makeMenuItemKey(final String scriptName, final ExecutionMode mode) {
		return scriptName + "_" + mode.toString().toLowerCase();
	}

	public void actionPerformed(final ActionEvent e) {
		getController().getViewController().setWaitingCursor(true);
		try {
			String scriptContent = getContentIfCached();
			if (scriptContent == null) {
				scriptContent = FileUtils.slurpFile(script);
			}
			final List<NodeModel> nodes = new ArrayList<NodeModel>();
			if (mode == ExecutionMode.ON_SINGLE_NODE) {
				nodes.add(getController().getSelection().getSelected());
			}
			else {
				nodes.addAll(getController().getSelection().getSelection());
			}
			final MModeController modeController = (MModeController) getController().getModeController();
			modeController.startTransaction();
			for (final NodeModel node : nodes) {
				try {
					if (mode == ExecutionMode.ON_SELECTED_NODE_RECURSIVELY) {
						// TODO: ensure that a script is invoked only once on every node?
						// (might be a problem with recursive actions if parent and child
						// are selected.)
						ScriptingEngine.executeScriptRecursive(modeController, node, scriptContent);
					}
					else {
						ScriptingEngine.executeScript(modeController, node, scriptContent);
					}
                }
                catch (ExecuteScriptException ex) {
                	LogUtils.warn("error executing script " + script + " - giving up");
                	modeController.delayedRollback();
                	UITools.errorMessage(TextUtils.getText("ExecuteScriptError.text"));
                	return;
                }
			}
			modeController.delayedCommit();
		}
		catch (final IOException ex) {
			LogUtils.warn("error reading " + script, ex);
			UITools.errorMessage(TextUtils.getText("ReadScriptError.text"));
		}
		finally {
			getController().getViewController().setWaitingCursor(false);
		}
	}

	private String getContentIfCached() throws IOException {
		if (cacheContent && content == null) {
			content = FileUtils.slurpFile(script);
			// oops, logtool seems to be inoperable right now
			LogUtils.info("cached " + String.format("%.1f", content.length() / 1000.) + " KB for script " + script);
			System.out
			    .println("cached " + String.format("%.1f", content.length() / 1000.) + " KB for script " + script);
		}
		return content;
	}

	static String getExecutionModeKey(final ExecutionMode executionMode) {
		switch (executionMode) {
			case ON_SINGLE_NODE:
				return "ExecuteScriptOnSingleNode.text";
			case ON_SELECTED_NODE:
				return "ExecuteScriptOnSelectedNode.text";
			case ON_SELECTED_NODE_RECURSIVELY:
				return "ExecuteScriptOnSelectedNodeRecursively.text";
			default:
				throw new AssertionError("unknown ExecutionMode " + executionMode);
		}
	}
}
