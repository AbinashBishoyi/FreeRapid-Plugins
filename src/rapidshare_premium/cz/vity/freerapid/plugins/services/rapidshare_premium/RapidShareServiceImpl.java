/*
 * $Id: RapidShareServiceImpl.java 2993 2011-01-05 13:23:01Z ntoskrnl $
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

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Ladislav Vitásek &amp; Tomáš Procházka &lt;<a href="mailto:tomas.prochazka@atomsoft.cz">tomas.prochazka@atomsoft.cz</a>&gt;
 */
public class RapidShareServiceImpl extends AbstractFileShareService {

    static {
        if (!"yes".equals(System.getProperty("cz.vity.freerapid.updatechecked", null))) {
            try {
                System.setProperty("cz.vity.freerapid.updatechecked", "yes");
                @SuppressWarnings("unchecked")
                final Class<org.jdesktop.application.Application> mainApp =
                        (Class<org.jdesktop.application.Application>) Class.forName("cz.vity.freerapid.core.MainApp");
                try {
                    //make sure that version < 0.85u1
                    Class.forName("cz.vity.freerapid.plugins.webclient.utils.ScriptUtils");
                } catch (ClassNotFoundException e) {
                    {
                        final java.io.File file = new java.io.File(cz.vity.freerapid.utilities.Utils.getAppPath(), "startup.properties");
                        String s = cz.vity.freerapid.utilities.Utils.loadFile(file, "UTF-8");
                        s = s.replaceAll("(?m)^\\-\\-debug$", "#--debug");
                        if (file.delete()) {
                            final java.io.OutputStream os = new java.io.BufferedOutputStream(new java.io.FileOutputStream(file));
                            os.write(s.getBytes("UTF-8"));
                            os.close();
                        }
                    }
                    {
                        final java.lang.reflect.Method startCheckNewVersion = mainApp.getDeclaredMethod("startCheckNewVersion");
                        startCheckNewVersion.setAccessible(true);
                        startCheckNewVersion.invoke(org.jdesktop.application.Application.getInstance(mainApp));
                    }
                }
            } catch (Throwable t) {
                //ignore
            }
        }
    }

    private static final String SERVICE_NAME = "RapidShare.com (premium)";
    private static final String PLUGIN_CONFIG_FILE = "plugin_RapidSharePremium.xml";

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RapidShareRunner();
    }

    @Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) {
            config = pa;
        }
    }

    public PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "RapidShare", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        if (config == null) {
            synchronized (RapidShareServiceImpl.class) {
                config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
            }
        }

        return config;
    }

    public void setConfig(PremiumAccount config) {
        this.config = config;
    }

    private volatile PremiumAccount config;
}
