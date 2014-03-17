/*
 * Filename.......: RapidShareConfig.java
 * Project........: cz.vity.freerapid.plugins.services.rapidshare_premium
 * Last modified..: $Date: 2008-09-16 19:30:16 +0530 (Tue, 16 Sep 2008) $
 * Revision.......: $Revision: 556 $
 * Author.........: Tomáš Procházka <tomas.prochazka@atomsoft.cz>
 * Created date...: 15.9.2008 7:12:51 GMT +2
 */

package cz.vity.freerapid.plugins.services.rapidshare_premium;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * POJO for RS account configuration.
 *
 * @author Tomáš Procházka &lt;<a href="mailto:tomas.prochazka@atomsoft.cz">tomas.prochazka@atomsoft.cz</a>&gt;
 * @version $Revision: 556 $ ($Date: 2008-09-16 19:30:16 +0530 (Tue, 16 Sep 2008) $)
 */
public class RapidShareConfig {

	private String login;
	private String password;

	public RapidShareConfig() {
	}

	public RapidShareConfig(String login, String password) {
		this.login = login;
		this.password = password;
	}

	public String getLogin() {
		return login;
	}

	public String getPassword() {
		return password;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getCookie() {
		if (login == null || password == null) return null;
		
		try {
			return login + "-" + URLEncoder.encode(password, "iso-8859-1");
		} catch (UnsupportedEncodingException ex) {
			try {
				return login + "-" + URLEncoder.encode(password, "UTF-8");
			} catch (UnsupportedEncodingException ex1) {
			}
		}
		return "";
	}

}



