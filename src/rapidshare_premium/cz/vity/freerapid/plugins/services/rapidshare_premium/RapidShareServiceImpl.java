/*
 * $Id: RapidShareServiceImpl.java 979 2008-12-07 10:53:46Z ATom $
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
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;
import cz.vity.freerapid.utilities.LogUtils;

import java.util.logging.Logger;

/**
 * @author Ladislav Vitásek &amp; Tomáš Procházka &lt;<a href="mailto:tomas.prochazka@atomsoft.cz">tomas.prochazka@atomsoft.cz</a>&gt;
 */
public class RapidShareServiceImpl extends AbstractFileShareService {

    private final static Logger logger = Logger.getLogger(RapidShareServiceImpl.class.getName());
    private static final String SERVICE_NAME = "RapidShare.com (premium)";
    private static final String PLUGIN_CONFIG_FILE = "plugin_RapidSharePremium.xml";

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public int getMaxDownloadsFromOneIP() {
        return Integer.MAX_VALUE;
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
        PremiumAccount pa = getConfig();
        pa = getPluginContext().getDialogSupport().showAccountDialog(pa, "RapidShare");//vysledek bude Premium ucet - Rapidshare
        if (pa != null) {
            try {
                getPluginContext().getConfigurationStorageSupport().storeConfigToFile(pa, PLUGIN_CONFIG_FILE);
            } catch (Exception e) {
                LogUtils.processException(logger, e);
            }
        }
    }

    public PremiumAccount getConfig() throws Exception {
        if (config == null) {
            synchronized (RapidShareServiceImpl.class) {
                if (config == null) {
                    if (getPluginContext().getConfigurationStorageSupport().configFileExists(PLUGIN_CONFIG_FILE)) {
                        try {
                            config = getPluginContext().getConfigurationStorageSupport().loadConfigFromFile(PLUGIN_CONFIG_FILE, PremiumAccount.class);
                        } catch (Exception e) {
                            LogUtils.processException(logger, e);
                            config = new PremiumAccount();
                        }
                    } else {
                        config = new PremiumAccount();
                    }
                }
            }
        }

        return config;
    }
    private volatile PremiumAccount config;
}
