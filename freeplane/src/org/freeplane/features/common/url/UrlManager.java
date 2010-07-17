/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is created by Dimitry Polivaev in 2008.
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
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.common.url;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.freeplane.core.controller.Controller;
import org.freeplane.core.extension.IExtension;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.FileUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.common.map.MapController;
import org.freeplane.features.common.map.MapModel;
import org.freeplane.features.common.map.ModeController;
import org.freeplane.features.common.map.NodeModel;
import org.freeplane.features.common.map.MapWriter.Mode;
import org.freeplane.n3.nanoxml.XMLParseException;

/**
 * @author Dimitry Polivaev
 */
public class UrlManager implements IExtension {
	public static final String FREEPLANE_FILE_EXTENSION_WITHOUT_DOT = "mm";
	public static final String FREEPLANE_FILE_EXTENSION = "." + FREEPLANE_FILE_EXTENSION_WITHOUT_DOT;
	private static File lastCurrentDir = null;
	public static final String MAP_URL = "map_url";

	/**
	 * Creates a default reader that just reads the given file.
	 *
	 * @throws FileNotFoundException
	 */
	protected static Reader getActualReader(final InputStream file) throws FileNotFoundException {
		return new InputStreamReader(file, FileUtils.defaultCharset());
	}

	public static UrlManager getController(final ModeController modeController) {
		return (UrlManager) modeController.getExtension(UrlManager.class);
	}

	/**
	 * Creates a reader that pipes the input file through a XSLT-Script that
	 * updates the version to the current.
	 *
	 * @throws IOException
	 */
	public static Reader getUpdateReader(final File file, final String xsltScript) throws FileNotFoundException,
	        IOException {
		try {
			final URL updaterUrl = ResourceController.getResourceController().getResource(xsltScript);
			if (updaterUrl == null) {
				throw new IllegalArgumentException(xsltScript + " not found.");
			}
			final StringWriter writer = new StringWriter();
			final Result result = new StreamResult(writer);
			class TransformerRunnable implements Runnable {
				private Throwable thrownException = null;

				public void run() {
					final TransformerFactory transFact = TransformerFactory.newInstance();
					InputStream xsltInputStream = null;
					InputStream input = null;
					try {
						xsltInputStream = new BufferedInputStream(updaterUrl.openStream());
						final Source xsltSource = new StreamSource(xsltInputStream);
						input = new BufferedInputStream(new FileInputStream(file));
						final CleaningInputStream cleanedInput = new CleaningInputStream(input);
						final Reader reader = new InputStreamReader(cleanedInput, cleanedInput.isUtf8() ? Charset.forName("UTF-8") : FileUtils.defaultCharset());
						final Transformer trans = transFact.newTransformer(xsltSource);
						trans.transform(new StreamSource(reader), result);
					}
					catch (final Exception ex) {
						LogUtils.warn(ex);
						thrownException = ex;
					}
					finally {
						try {
							if (input != null) {
								input.close();
							}
							if (xsltInputStream != null) {
								xsltInputStream.close();
							}
						}
						catch (final IOException e) {
							e.printStackTrace();
						}
					}
				}

				public Throwable thrownException() {
					return thrownException;
				}
			}
			final TransformerRunnable transformer = new TransformerRunnable();
			final Thread transformerThread = new Thread(transformer, "XSLT");
			transformerThread.start();
			transformerThread.join();
			final Throwable thrownException = transformer.thrownException();
			if (thrownException != null) {
				throw new TransformerException(thrownException);
			}
			return new StringReader(writer.getBuffer().toString());
		}
		catch (final Exception ex) {
			final String message = ex.getMessage();
			UITools.errorMessage(TextUtils.formatText("update_failed", String.valueOf(message)));
			LogUtils.warn(ex);
			final InputStream input = new BufferedInputStream(new FileInputStream(file));
			return UrlManager.getActualReader(input);
		}
	}

	public static void install(final ModeController modeController, final UrlManager urlManager) {
		modeController.addExtension(UrlManager.class, urlManager);
	}

// 	final private Controller controller;
// 	final private ModeController modeController;

	public UrlManager(final ModeController modeController) {
		super();
//		this.modeController = modeController;
//		controller = modeController.getController();
		createActions();
	}

	/**
	 *
	 */
	private void createActions() {
	}

	public Controller getController() {
		return controller;
	}

	/**
	 * Creates a file chooser with the last selected directory as default.
	 */
	public JFileChooser getFileChooser(final FileFilter filter) {
		final JFileChooser chooser = new JFileChooser();
		final File parentFile = getMapsParentFile();
		if (parentFile != null && getLastCurrentDir() == null) {
			setLastCurrentDir(parentFile);
		}
		if (getLastCurrentDir() != null) {
			chooser.setCurrentDirectory(getLastCurrentDir());
		}
		if (filter != null) {
			chooser.addChoosableFileFilter(filter);
			chooser.setFileFilter(filter);
		}
		return chooser;
	}

	public File getLastCurrentDir() {
		return lastCurrentDir;
	}

	protected File getMapsParentFile() {
		final MapModel map = getController().getMap();
		if ((map != null) && (map.getFile() != null) && (map.getFile().getParentFile() != null)) {
			return map.getFile().getParentFile();
		}
		return null;
	}

	public ModeController getModeController() {
		return modeController;
	}

	public void handleLoadingException(final Exception ex) {
		final String exceptionType = ex.getClass().getName();
		if (exceptionType.equals("freeplane.main.XMLParseException")) {
			final int showDetail = JOptionPane.showConfirmDialog(getController().getViewController().getMapView(),
			    TextUtils.getText("map_corrupted"), "Freeplane", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
			if (showDetail == JOptionPane.YES_OPTION) {
				UITools.errorMessage(ex);
			}
		}
		else if (exceptionType.equals("java.io.FileNotFoundException")) {
			UITools.errorMessage(ex.getMessage());
		}
		else {
			LogUtils.severe(ex);
			UITools.errorMessage(ex);
		}
	}

	public void load(final URL url, final MapModel map) throws FileNotFoundException, IOException, XMLParseException,
	        URISyntaxException {
		setURL(map, url);
		InputStreamReader urlStreamReader = null;
		try {
			urlStreamReader = new InputStreamReader(url.openStream());
		}
		catch (final Exception ex) {
			UITools.errorMessage(TextUtils.format("url_open_error", url.toString()));
			LogUtils.warn(ex.getMessage());
			return;
		}
		try {
			final NodeModel root = modeController.getMapController().getMapReader().createNodeTreeFromXml(map,
			    urlStreamReader, Mode.FILE);
			urlStreamReader.close();
			if (root != null) {
				map.setRoot(root);
			}
			else {
				throw new IOException();
			}
		}
		catch (final Exception ex) {
			LogUtils.severe(ex);
			return;
		}
	}

	public void loadURL(URI uri) {
		final String uriString = uri.toString();
		if (uriString.startsWith("#")) {
			final String target = uri.getFragment();
			try {
				final MapController mapController = modeController.getMapController();
				final NodeModel node = mapController.getNodeFromID(target);
				if (node != null) {
					mapController.select(node);
				}
			}
			catch (final Exception e) {
				LogUtils.warn("link " + target + " not found", e);
				UITools.errorMessage(TextUtils.formatText("link_not_found", target));
			}
			return;
		}
		try {
			final String extension = FileUtils.getExtension(uri.getRawPath());
			uri = getAbsoluteUri(uri);
			try {
				if ((extension != null)
				        && extension.equals(UrlManager.FREEPLANE_FILE_EXTENSION_WITHOUT_DOT)) {
					final URL url = new URL(uri.getScheme(), uri.getHost(), uri.getPath());
					modeController.getMapController().newMap(url);
					final String ref = uri.getFragment();
					if (ref != null) {
						final ModeController newModeController = getController().getModeController();
						final MapController newMapController = newModeController.getMapController();
						newMapController.select(newMapController.getNodeFromID(ref));
					}
					return;
				}
				getController().getViewController().openDocument(uri);
			}
			catch (final Exception e) {
				LogUtils.warn("link " + uri + " not found", e);
				UITools.errorMessage(TextUtils.formatText("link_not_found", uri.toString()));
			}
			return;
		}
		catch (final MalformedURLException ex) {
			LogUtils.warn("URL " + uriString + " not found", ex);
			UITools.errorMessage(TextUtils.formatText("link_not_found", uriString));
		}
	}

	private URI getAbsoluteUri(final URI uri) throws MalformedURLException {
		if (uri.isAbsolute()) {
			return uri;
		}
		final MapModel map = getController().getMap();
		return getAbsoluteUri(map, uri);
	}

	public URI getAbsoluteUri(final MapModel map, final URI uri) throws MalformedURLException {
		if (uri.isAbsolute()) {
			return uri;
		}
		final String path = uri.getPath();
		try {
			final URL url = new URL(map.getURL(), path);
			return new URI(url.getProtocol(), url.getHost(), url.getPath(), uri.getQuery(), uri.getFragment());
		}
		catch (final URISyntaxException e) {
			LogUtils.warn(e);
			return null;
		}
	}

	public URL getAbsoluteUrl(final MapModel map, final URI uri) throws MalformedURLException {
		final String path = uri.isOpaque() ? uri.getSchemeSpecificPart() : uri.getPath();
		final StringBuilder sb = new StringBuilder(path);
		final String query = uri.getQuery();
		if (query != null) {
			sb.append('?');
			sb.append(query);
		}
		final String fragment = uri.getFragment();
		if (fragment != null) {
			sb.append('#');
			sb.append(fragment);
		}
		if (!uri.isAbsolute() || uri.isOpaque()) {
			final URL mapUrl = map.getURL();
			final String scheme = uri.getScheme();
			if (scheme == null || mapUrl.getProtocol().equals(scheme)) {
				final URL url = new URL(mapUrl, sb.toString());
				return url;
			}
		}
		final URL url = new URL(uri.getScheme(), uri.getHost(), uri.getPort(), sb.toString());
		return url;
	}

	public void setLastCurrentDir(final File lastCurrentDir) {
		UrlManager.lastCurrentDir = lastCurrentDir;
	}

	protected void setURL(final MapModel map, final URL url) {
		map.setURL(url);
	}
}
