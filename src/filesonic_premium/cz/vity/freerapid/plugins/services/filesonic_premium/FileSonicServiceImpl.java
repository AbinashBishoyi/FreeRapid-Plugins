package cz.vity.freerapid.plugins.services.filesonic_premium;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author valankar
 */
public class FileSonicServiceImpl extends AbstractFileShareService {

    private static final String PLUGIN_CONFIG_FILE = "plugin_FileSonicPremium.xml";

    @Override
    public String getName() {
        return "filesonic.com_premium";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileSonicFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) {
            config = pa;
        }
    }

    public PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "FileSonic", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        if (config == null) {
            synchronized (FileSonicServiceImpl.class) {
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