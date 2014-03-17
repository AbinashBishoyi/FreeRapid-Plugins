package cz.vity.freerapid.plugins.services.speedyshare_premium;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl, premium by birchie
 */
public class SpeedyShareServiceImpl extends AbstractFileShareService {
    private static final String PLUGIN_CONFIG_FILE = "plugin_SpeedySharePremium.xml";
    private volatile PremiumAccount config;

    public String getName() {
        return "speedyshare.com_premium";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SpeedyShareFileRunner();
    }


    @Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "SpeedyShare", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        synchronized (SpeedyShareServiceImpl.class) {
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