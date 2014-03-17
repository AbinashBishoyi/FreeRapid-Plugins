/*
 * $Id: RapidShareSupport.java 981 2008-12-07 12:00:52Z Vity $
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

package cz.vity.freerapid.plugins.services.rapidshare_premium;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Support class for RS.
 *
 * @author Tomáš Procházka &lt;<a href="mailto:tomas.prochazka@atomsoft.cz">tomas.prochazka@atomsoft.cz</a>&gt;
 * @version $Revision: 981 $ ($Date: 2008-12-07 17:30:52 +0530 (Sun, 07 Dec 2008) $)
 */
class RapidShareSupport {


    /**
     * Do not instantiate RapidShareSupport.
     */
    private RapidShareSupport() {
    }

    public static String buildCookie(String login, String password) {
        if (login == null || password == null) return null;

        try {
            return login + "-" + URLEncoder.encode(password, "iso-8859-1");
        } catch (UnsupportedEncodingException ex) {
            try {
                return login + "-" + URLEncoder.encode(password, "UTF-8");
            } catch (UnsupportedEncodingException ex1) {
                Logger.getLogger(RapidShareSupport.class.getName()).log(Level.SEVERE, "Password encoding failed.");
            }
        }
        return "";
    }

}



