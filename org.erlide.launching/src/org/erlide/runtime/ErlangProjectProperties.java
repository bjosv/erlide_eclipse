/*******************************************************************************
 * Copyright (c) 2004 Eric Merritt and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/

package org.erlide.runtime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.erlide.basiccore.ErlLogger;
import org.erlide.runtime.backend.BackendManager;
import org.erlide.runtime.backend.IBackend;

public class ErlangProjectProperties {

	private static final String PATH_SEP = ";";

	private IProject project;

	private boolean fOtpProjectStructure = IPrefConstants.DEFAULT_OTP_PROJECT_STRUCTURE;

	private String fSourceDirs = IPrefConstants.DEFAULT_SOURCE_DIRS;

	private String fIncludeDirs = IPrefConstants.DEFAULT_INCLUDE_DIRS;

	private String fOutputDir = IPrefConstants.DEFAULT_OUTPUT_DIR;

	private String fExternalIncludes = IPrefConstants.DEFAULT_EXTERNAL_INCLUDES;

	private String fBackendNodeName = IPrefConstants.DEFAULT_BACKEND_NODE_NAME;

	private List<IPropertyChangeListener> fListeners;

	public ErlangProjectProperties() {
	}

	public ErlangProjectProperties(IProject prj) {
		project = prj;
		fListeners = new ArrayList<IPropertyChangeListener>(5);
		load();
	}

	public void load() {
		if (project == null) {
			return;
		}

		final File codepath = project.getFile(".codepath").getRawLocation()
				.toFile();
		final Properties prefs = new Properties();
		try {
			prefs.load(new FileInputStream(codepath));
		} catch (final IOException e) {
		}

		fOtpProjectStructure = Boolean
				.getBoolean(prefs
						.getProperty(
								IPrefConstants.PROJECT_OTP_PROJECT_STRUCTURE,
								Boolean
										.toString(IPrefConstants.DEFAULT_OTP_PROJECT_STRUCTURE)));
		fSourceDirs = prefs.getProperty(IPrefConstants.PROJECT_SOURCE_DIRS,
				IPrefConstants.DEFAULT_SOURCE_DIRS);
		fIncludeDirs = prefs.getProperty(IPrefConstants.PROJECT_INCLUDE_DIRS,
				IPrefConstants.DEFAULT_INCLUDE_DIRS);
		fOutputDir = prefs.getProperty(IPrefConstants.PROJECT_OUTPUT_DIR,
				IPrefConstants.DEFAULT_OUTPUT_DIR);
		fExternalIncludes = prefs.getProperty(
				IPrefConstants.PROJECT_EXTERNAL_INCLUDES,
				IPrefConstants.DEFAULT_EXTERNAL_INCLUDES);
		fBackendNodeName = prefs.getProperty(
				IPrefConstants.PROJECT_BACKEND_NODE_NAME,
				IPrefConstants.DEFAULT_BACKEND_NODE_NAME);
	}

	public void store() {
		if (project == null) {
			return;
		}

		final File codepath = project.getFile(".codepath").getRawLocation()
				.toFile();
		final Properties prefs = new Properties();

		prefs.put(IPrefConstants.PROJECT_OTP_PROJECT_STRUCTURE, Boolean
				.toString(fOtpProjectStructure));
		prefs.put(IPrefConstants.PROJECT_SOURCE_DIRS, fSourceDirs);
		prefs.put(IPrefConstants.PROJECT_INCLUDE_DIRS, fIncludeDirs);
		prefs.put(IPrefConstants.PROJECT_OUTPUT_DIR, fOutputDir);
		prefs.put(IPrefConstants.PROJECT_EXTERNAL_INCLUDES, fExternalIncludes);
		prefs.put(IPrefConstants.PROJECT_BACKEND_NODE_NAME, fBackendNodeName);

		try {
			prefs.store(new FileOutputStream(codepath), null);
		} catch (final IOException e) {
		}
	}

	public String getIncludeDirsString() {
		return fIncludeDirs;
	}

	public void setIncludeDirsString(String includeDirs) {
		fIncludeDirs = includeDirs;
	}

	public String[] getIncludeDirs() {
		return unpack(fIncludeDirs);
	}

	public void setIncludeDirs(String[] includeDirs) {
		fIncludeDirs = pack(includeDirs);
	}

	public boolean hasOtpProjectStructure() {
		return fOtpProjectStructure;
	}

	public void setOtpProjectStructure(boolean otpProjectStructure) {
		fOtpProjectStructure = otpProjectStructure;
	}

	public String getOutputDir() {
		return fOutputDir;
	}

	public void setOutputDir(String outputDir) {
		if (!fOutputDir.equals(outputDir)) {
			IBackend b = BackendManager.getDefault().get(project);

			String p = project.getLocation().append(fOutputDir).toString();
			b.getCodeManager().removePathA(p);

			p = project.getLocation().append(outputDir).toString();
			b.getCodeManager().addPathA(p);
		}
		fOutputDir = outputDir;
	}

	public String getSourceDirsString() {
		return fSourceDirs;
	}

	public void setSourceDirsString(String sourceDirs) {
		fSourceDirs = sourceDirs;
	}

	public String[] getSourceDirs() {
		return unpack(fSourceDirs);
	}

	public void setSourceDirs(String[] sourceDirs) {
		fSourceDirs = pack(sourceDirs);
	}

	public String buildCommandLine() {
		if (project != null) {
			final String incs = buildIncludeDirs(getIncludeDirs());
			return " -pa " + project.getLocation().append(fOutputDir) + incs;
		} else {
			return "";
		}
	}

	public String buildIncludeDirs(String[] dirs) {
		final StringBuffer incs = new StringBuffer();
		for (String element : dirs) {
			final IPath loc = project.getLocation();
			IPath inc = new Path(element);
			ErlLogger.log("* " + inc);
			if (!inc.isAbsolute()) {
				ErlLogger.log("  not abs!");
				inc = loc.append(inc);
				ErlLogger.log("  " + inc);
			}
			incs.append(" -I").append(inc.toString());
		}
		return incs.toString();
	}

	public void copyFrom(ErlangProjectProperties bprefs) {
		fIncludeDirs = bprefs.fIncludeDirs;
		fSourceDirs = bprefs.fSourceDirs;
		fOutputDir = bprefs.fOutputDir;
		fOtpProjectStructure = bprefs.fOtpProjectStructure;
		fBackendNodeName = bprefs.fBackendNodeName;
	}

	public static String pack(String[] strs) {
		final StringBuffer b = new StringBuffer();
		for (int i = 0; i < strs.length; i++) {
			b.append(strs[i]);
			if (i < strs.length - 1) {
				b.append(PATH_SEP);
			}
		}
		return b.toString();
	}

	public String[] getExternalIncludes() {
		return unpack(fExternalIncludes);
	}

	private String[] unpack(String str) {
		final String[] res = str.split(PATH_SEP);
		for (int i = 0; i < res.length; i++) {
			res[i] = res[i].trim();
		}
		return res;
	}

	public String getExternalIncludesString() {
		return fExternalIncludes;
	}

	public void setExternalIncludes(String[] externalIncludes) {
		fExternalIncludes = pack(externalIncludes);
	}

	public IProject getProject() {
		return project;
	}

	void addPropertyChangeListener(IPropertyChangeListener listener) {
		if (!fListeners.contains(listener)) {
			fListeners.add(listener);
		}
	}

	void removePropertyChangeListener(IPropertyChangeListener listener) {
		fListeners.remove(listener);
	}

	void notifyListeners() {
		// for (IPropertyChangeListener listener : fListeners) {
		// listener.propertyChange(new PropertyChangeEvent(null, null, null,
		// null));
		// }
	}

	public String getBackendNodeName() {
		return fBackendNodeName;
	}

	public void setBackendNodeName(String backendNodeName) {
		fBackendNodeName = backendNodeName;
	}

}
