/*
 * Filename.......: RapidShareConfigProvider.java
 * Project........: cz.vity.freerapid.plugins.services.rapidshare_premium
 * Last modified..: $Date: 2008-09-17 19:24:33 +0530 (Wed, 17 Sep 2008) $
 * Revision.......: $Revision: 571 $
 * Author.........: Tomáš Procházka <tomas.prochazka@atomsoft.cz>
 * Created date...: 15.9.2008 8:05:39 GMT +2
 */

package cz.vity.freerapid.plugins.services.rapidshare_premium;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.LocalStorage;

/**
 * Provide configuration for RS premium plugin.
 *
 * @author Tomáš Procházka &lt;<a href="mailto:tomas.prochazka@atomsoft.cz">tomas.prochazka@atomsoft.cz</a>&gt;
 * @version $Revision: 571 $ ($Date: 2008-09-17 19:24:33 +0530 (Wed, 17 Sep 2008) $)
 */
class RapidShareConfigProvider {

	private final static Logger logger = Logger.getLogger(RapidShareConfigProvider.class.getName());
	private final String CONFIG_FILE_NAME = "RapidSharePremium.xml";
	private final LocalStorage storage;
	private RapidShareConfig config = null;

	public static RapidShareConfigProvider getInstance() {
		if (INSTANCE == null) {
			synchronized (RapidShareConfigProvider.class) {
				if (INSTANCE == null) {
					INSTANCE = new RapidShareConfigProvider();
				}
			}
		}
		return INSTANCE;
	}
	private static volatile RapidShareConfigProvider INSTANCE = null;

	/** Constructor */
	private RapidShareConfigProvider() {
		ApplicationContext appContext = Application.getInstance().getContext();
		storage = appContext.getLocalStorage();
	}

	public synchronized RapidShareConfig getConfig() {
		if (this.config == null) {
			config = loadConfigFromFile(CONFIG_FILE_NAME);
		}
		if (this.config == null) {
			config = new RapidShareConfig();
		}
		return config;
	}

	public synchronized void save(RapidShareConfig config) {
		this.config = config;
		saveConfigToFile(config);
	}

	public void clear() {
		this.save(new RapidShareConfig());
	}

	private RapidShareConfig loadConfigFromFile(String fileName) {
		XMLDecoder d = null;
		try {
			d = new XMLDecoder(storage.openInputFile(fileName), null, null, RapidShareConfig.class.getClassLoader());
			Object bean = d.readObject();
			return (RapidShareConfig) bean;
		} catch (IOException ex) {
			return null;
		} finally {
			if (d != null) {
				d.close();
			}
		}
	}

	private void saveConfigToFile(RapidShareConfig config) {
		ClassLoader threadCL = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(RapidShareConfig.class.getClassLoader());
		try {
			XMLEncoder e = null;
			try {
				e = new XMLEncoder(storage.openOutputFile(CONFIG_FILE_NAME));
				e.writeObject(config);
			} finally {
				if (e != null) {
					e.close();
				}
			}
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Can't store settings", ex);
		} finally {
			Thread.currentThread().setContextClassLoader(threadCL);
		}
	}
}

