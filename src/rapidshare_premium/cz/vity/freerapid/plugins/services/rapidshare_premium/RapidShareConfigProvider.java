/*
 * Filename.......: RapidShareConfigProvider.java
 * Project........: cz.vity.freerapid.plugins.services.rapidshare_premium
 * Last modified..: $Date: 2008-09-16 19:30:16 +0530 (Tue, 16 Sep 2008) $
 * Revision.......: $Revision: 556 $
 * Author.........: Tomáš Procházka <tomas.prochazka@atomsoft.cz>
 * Created date...: 15.9.2008 8:05:39 GMT +2
 */

package cz.vity.freerapid.plugins.services.rapidshare_premium;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.LocalStorage;

/**
 * 
 *
 * @author Tomáš Procházka &lt;<a href="mailto:tomas.prochazka@atomsoft.cz">tomas.prochazka@atomsoft.cz</a>&gt;
 * @version $Revision: 556 $ ($Date: 2008-09-16 19:30:16 +0530 (Tue, 16 Sep 2008) $)
 */
public class RapidShareConfigProvider {

	private final static Logger logger = Logger.getLogger(RapidShareConfigProvider.class.getName());
	private final String CONFIG = "RapidSharePremium.xml";
	private LocalStorage storage;

	/** Constructor */
	public RapidShareConfigProvider() {
		ApplicationContext appContext = Application.getInstance().getContext();
		storage = appContext.getLocalStorage();

	}

	public RapidShareConfig getConfig() {
		RapidShareConfig rv = null;
		try {
			rv = loadConfig(CONFIG);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Can't load settings", ex);
		}
		if (rv == null) {
			rv = new RapidShareConfig();
		}
		return rv;
	}

	public void save(RapidShareConfig config) {
		ClassLoader threadCL = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(RapidShareConfig.class.getClassLoader());

		try {
			XMLEncoder e = null;
			try {
				e = new XMLEncoder(storage.openOutputFile(CONFIG));
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

	public void clear() {
		this.save(new RapidShareConfig());
	}

	private RapidShareConfig loadConfig(String fileName) throws IOException {
		InputStream ist = null;
		try {
			ist = storage.openInputFile(fileName);
		} catch (IOException e) {
			return null;
		}

		XMLDecoder d = null;
		try {
			d = new XMLDecoder(ist,null,null,RapidShareConfig.class.getClassLoader());
			Object bean = d.readObject();
			return (RapidShareConfig) bean;
		} finally {
			if (d != null) {
				d.close();
			}
		}
	}
}

