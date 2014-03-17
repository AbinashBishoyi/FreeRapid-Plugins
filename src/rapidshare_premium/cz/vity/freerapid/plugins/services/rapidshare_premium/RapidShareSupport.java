/*
 * $Id: RapidShareSupport.java 1789 2009-06-19 09:25:50Z Atom $
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

/**
 * Support class for RS.
 *
 * @author Tomáš Procházka &lt;<a href="mailto:tomas.prochazka@atomsoft.cz">tomas.prochazka@atomsoft.cz</a>&gt;
 * @version $Revision: 1789 $ ($Date: 2009-06-19 14:55:50 +0530 (Fri, 19 Jun 2009) $)
 */
class RapidShareSupport {

    /**
     * Do not instantiate RapidShareSupport.
     */
    private RapidShareSupport() {
    }

    public static int getSecondToMidnight() {
		Calendar now = Calendar.getInstance();
		Calendar midnight =	Calendar.getInstance();
		midnight.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)+1, 0, 0, 1);
		return (int) ((midnight.getTimeInMillis() - now.getTimeInMillis()) / 1000f);
    }

}



