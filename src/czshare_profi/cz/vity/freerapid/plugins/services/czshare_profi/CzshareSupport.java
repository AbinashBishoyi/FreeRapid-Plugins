/*
 * $Id: RapidShareSupport.java 1022 2008-12-09 20:10:51Z ATom $
 *
 * Copyright (C) 2007  Tomáš Procházka & Ladislav Vitásek
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package cz.vity.freerapid.plugins.services.czshare_profi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Support class for RS.
 *
 * @author Tomáš Procházka &lt;<a href="mailto:tomas.prochazka@atomsoft.cz">tomas.prochazka@atomsoft.cz</a>&gt;
 * @version $Revision: 1022 $ ($Date: 2008-12-09 21:10:51 +0100 (út, 09 XII 2008) $)
 */
class CzshareSupport {


    /**
     * Do not instantiate CzshareSupport.
     */
    private CzshareSupport() {
    }

    public static String buildCookie(String login, String password) {
        if (login == null || password == null) return null;

        try {
            return login + "-" + URLEncoder.encode(password, "iso-8859-1");
        } catch (UnsupportedEncodingException ex) {
            try {
                return login + "-" + URLEncoder.encode(password, "UTF-8");
            } catch (UnsupportedEncodingException ex1) {
                Logger.getLogger(CzshareSupport.class.getName()).log(Level.SEVERE, "Password encoding failed.");
            }
        }
        return "";
    }

    public static int getSecondToMidnight() {
		Calendar now = Calendar.getInstance();
		Calendar midnight =	Calendar.getInstance();
		midnight.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)+1, 0, 0, 1);
		return (int) ((midnight.getTimeInMillis() - now.getTimeInMillis()) / 1000f);
    }

}



