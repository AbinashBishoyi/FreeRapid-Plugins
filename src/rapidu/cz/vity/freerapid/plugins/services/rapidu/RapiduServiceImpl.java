package cz.vity.freerapid.plugins.services.rapidu;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class RapiduServiceImpl extends AbstractFileShareService {
    private static final String PLUGIN_CONFIG_FILE = "plugin_RapiduAccount.xml";
    private volatile PremiumAccount config;

    @Override
    public String getName() {
        return "rapidu.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RapiduFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "Rapidu.net", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        synchronized (RapiduServiceImpl.class) {
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