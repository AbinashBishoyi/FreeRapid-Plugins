package cz.vity.freerapid.plugins.services.keep2share_premium;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Keep2Share_PremiumServiceImpl extends AbstractFileShareService {
    private static final String PLUGIN_CONFIG_FILE = "plugin_Keep2SharePremium.xml";
    private volatile PremiumAccount config;

    @Override
    public String getName() {
        return "keep2share.cc";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Keep2Share_PremiumFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    public PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "Keep2Share", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        if (config == null) {
            synchronized (Keep2Share_PremiumServiceImpl.class) {
                config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
            }
        }
        return config;
    }

    public void setConfig(final PremiumAccount config) {
        synchronized (Keep2Share_PremiumServiceImpl.class) {
            this.config = config;
        }
    }
}