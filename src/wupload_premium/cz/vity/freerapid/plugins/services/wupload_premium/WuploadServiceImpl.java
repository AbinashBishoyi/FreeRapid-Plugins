package cz.vity.freerapid.plugins.services.wupload_premium;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class WuploadServiceImpl extends AbstractFileShareService {
    private static final String PLUGIN_CONFIG_FILE = "plugin_WuploadPremium.xml";
    private volatile PremiumAccount config;

    @Override
    public String getName() {
        return "wupload.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new WuploadFileRunner();
    }

    @Override
    public void showOptions() {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    PremiumAccount showConfigDialog() {
        return showAccountDialog(getConfig(), "Wupload", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() {
        synchronized (WuploadServiceImpl.class) {
            if (config == null) {
                config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
            }
        }
        return config;
    }

    void setConfig(PremiumAccount config) {
        this.config = config;
    }

}