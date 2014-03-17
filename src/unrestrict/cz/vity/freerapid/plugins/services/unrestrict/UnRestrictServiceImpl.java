package cz.vity.freerapid.plugins.services.unrestrict;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UnRestrictServiceImpl extends AbstractFileShareService {
    private static final String PLUGIN_CONFIG_FILE = "plugin_UnRestrict_Premium.xml";
    private volatile PremiumAccount config;

    @Override
    public String getName() {
        return "unrestrict.li";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new cz.vity.freerapid.plugins.services.unrestrict.UnRestrictFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "UnRestrict", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        synchronized (cz.vity.freerapid.plugins.services.unrestrict.UnRestrictServiceImpl.class) {
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