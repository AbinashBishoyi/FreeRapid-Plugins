package cz.vity.freerapid.plugins.services.multi_debrid;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Multi_DebridServiceImpl extends AbstractFileShareService {
    private static final String PLUGIN_CONFIG_FILE = "plugin_MultiDebrid_Premium.xml";
    private volatile PremiumAccount config;

    @Override
    public String getName() {
        return "multi-debrid.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Multi_DebridFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "Multi-Debrid (use API password)", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        synchronized (Multi_DebridServiceImpl.class) {
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