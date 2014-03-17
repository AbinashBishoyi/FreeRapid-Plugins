package cz.vity.freerapid.plugins.services.alldebrid;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class AllDebridServiceImpl extends AbstractFileShareService {
    private static final String PLUGIN_CONFIG_FILE = "plugin_AllDebrid_Premium.xml";
    private volatile PremiumAccount config;

    @Override
    public String getName() {
        return "alldebrid.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new cz.vity.freerapid.plugins.services.alldebrid.AllDebridFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "AllDebrid", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        synchronized (cz.vity.freerapid.plugins.services.alldebrid.AllDebridServiceImpl.class) {
            if (config == null) {
                config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
            }
        }
        return config;
    }

    void setConfig(final PremiumAccount config) {
        this.config = config;
    }
}