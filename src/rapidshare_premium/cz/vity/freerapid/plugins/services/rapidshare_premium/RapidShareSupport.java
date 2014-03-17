/*
 * $Id: RapidShareSupport.java 2480 2010-06-30 05:43:36Z wordrider $
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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Support class for RS.
 *
 * @author Tomáš Procházka &lt;<a href="mailto:tomas.prochazka@atomsoft.cz">tomas.prochazka@atomsoft.cz</a>&gt;
 * @version $Revision: 2480 $ ($Date: 2010-06-30 11:13:36 +0530 (Wed, 30 Jun 2010) $)
 */
class RapidShareSupport {

    /**
     * Do not instantiate RapidShareSupport.
     */
    private RapidShareSupport() {
    }

    public static int getSecondToMidnight() {
        Calendar now = Calendar.getInstance();
        Calendar midnight = Calendar.getInstance();
        midnight.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH) + 1, 0, 0, 1);
        return (int) ((midnight.getTimeInMillis() - now.getTimeInMillis()) / 1000f);
    }

    public static Map<String,String> parseRapidShareResponse(String response) {
        Map<String,String> map = new HashMap<String, String>();
        String[] lines = response.split("\n");
        for (String line : lines) {
            String[] parts = line.split("=");
            if (parts.length > 1) {
                map.put(parts[0], parts[1]);
            } else {
                map.put(parts[0], "");
            }
        }
        return map;
    }
}
