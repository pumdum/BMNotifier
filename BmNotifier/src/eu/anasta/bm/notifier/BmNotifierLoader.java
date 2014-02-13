package eu.anasta.bm.notifier;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.jdt.internal.jarinjarloader.RsrcURLStreamHandlerFactory;

public class BmNotifierLoader {
	static final String REDIRECTED_CLASS_PATH_MANIFEST_NAME  = "Rsrc-Class-Path";  //$NON-NLS-1$
	static final String REDIRECTED_MAIN_CLASS_MANIFEST_NAME  = "Rsrc-Main-Class";  //$NON-NLS-1$
	static final String DEFAULT_REDIRECTED_CLASSPATH         = "";  //$NON-NLS-1$
	static final String MAIN_METHOD_NAME                     = "main";  //$NON-NLS-1$
	static final String JAR_INTERNAL_URL_PROTOCOL_WITH_COLON = "jar:rsrc:";  //$NON-NLS-1$
	static final String JAR_INTERNAL_SEPARATOR               = "!/";  //$NON-NLS-1$
	static final String INTERNAL_URL_PROTOCOL_WITH_COLON     = "rsrc:";  //$NON-NLS-1$
	static final String INTERNAL_URL_PROTOCOL                = "rsrc";  //$NON-NLS-1$
	static final String PATH_SEPARATOR                       = "/";  //$NON-NLS-1$
	static final String CURRENT_DIR                          = "./";  //$NON-NLS-1$
	static final String UTF8_ENCODING                        = "UTF-8";  //$NON-NLS-1$
	
	private static class ManifestInfo {
		String rsrcMainClass;
		String[] rsrcClassPath;
	}

	public static void main(String[] args) throws Throwable {
		ManifestInfo mi = getManifestInfo();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL.setURLStreamHandlerFactory(new RsrcURLStreamHandlerFactory(cl));
		URL[] rsrcUrls = new URL[mi.rsrcClassPath.length+1];
		for (int i = 0; i < mi.rsrcClassPath.length; i++) {
			String rsrcPath = mi.rsrcClassPath[i];
			if (rsrcPath.endsWith(PATH_SEPARATOR)) 
				rsrcUrls[i] = new URL(INTERNAL_URL_PROTOCOL_WITH_COLON + rsrcPath); 
			else
				rsrcUrls[i] = new URL(JAR_INTERNAL_URL_PROTOCOL_WITH_COLON + rsrcPath + JAR_INTERNAL_SEPARATOR);    
		}
		rsrcUrls[mi.rsrcClassPath.length] = getSWTClassloader();
		ClassLoader jceClassLoader = new URLClassLoader(rsrcUrls, null);
		Thread.currentThread().setContextClassLoader(jceClassLoader);
		Class<?> c = Class.forName(mi.rsrcMainClass, true, jceClassLoader);
		Method main = c.getMethod(MAIN_METHOD_NAME, new Class[]{args.getClass()}); 
		main.invoke((Object)null, new Object[]{args});
	}

	private static ManifestInfo getManifestInfo() throws IOException {
		Enumeration<URL> resEnum;
		resEnum = Thread.currentThread().getContextClassLoader()
				.getResources(JarFile.MANIFEST_NAME);
		while (resEnum.hasMoreElements()) {
			try {
				URL url = (URL) resEnum.nextElement();
				InputStream is = url.openStream();
				if (is != null) {
					ManifestInfo result = new ManifestInfo();
					Manifest manifest = new Manifest(is);
					Attributes mainAttribs = manifest.getMainAttributes();
					result.rsrcMainClass = mainAttribs
							.getValue("Rsrc-Main-Class");
					String rsrcCP = mainAttribs
							.getValue("Rsrc-Class-Path");
					result.rsrcClassPath = rsrcCP.split(" ");
					if ((result.rsrcMainClass != null)
							&& !result.rsrcMainClass.trim().equals("")) //$NON-NLS-1$
						return result;
				}
			} catch (Exception e) {
				// Silently ignore wrong manifests on classpath?
			}
		}
		System.err
				.println("Missing attributes for JarRsrcLoader in Manifest "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return null;
	}

	private static URL getSWTClassloader() {
		String swtFileName = getSwtJarName();
		try {
			URL swtFileUrl = new URL(JAR_INTERNAL_URL_PROTOCOL_WITH_COLON + swtFileName + JAR_INTERNAL_SEPARATOR);
			return swtFileUrl;
		} catch (MalformedURLException exx) {
			throw new RuntimeException(exx);
		}
	}

	private static String getSwtJarName() {
		// Detect OS
		String osName = System.getProperty("os.name").toLowerCase();
		String swtFileNameOsPart = osName.contains("win") ? "win" : osName
				.contains("mac") ? "osx" : osName.contains("linux")
				|| osName.contains("nix") ? "linux" : "";
		if ("".equals(swtFileNameOsPart)) {
			throw new RuntimeException("Launch failed: Unknown OS name: "
					+ osName);
		}

		// Detect 32bit vs 64 bit
		String swtFileNameArchPart = getArch();

		String swtFileName = "swt-" + swtFileNameOsPart + swtFileNameArchPart
				+ ".jar";
		return swtFileName;
	}

	private static String getArch() {
		// Detect 32bit vs 64 bit
		String jvmArch = System.getProperty("os.arch").toLowerCase();
		String arch = (jvmArch.contains("64") ? "64" : "32");
		return arch;
	}

}
