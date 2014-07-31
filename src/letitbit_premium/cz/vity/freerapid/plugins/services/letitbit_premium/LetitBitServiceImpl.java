package cz.vity.freerapid.plugins.services.letitbit_premium;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class LetitBitServiceImpl extends AbstractFileShareService {

    private static final String PLUGIN_CONFIG_FILE = "plugin_LetitBitPremium.xml";
    private volatile PremiumAccount config;

    @Override
    public String getName() {
        return "letitbit.net_premium";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LetitBitFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    public PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "LetitBit", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        if (config == null) {
            synchronized (LetitBitServiceImpl.class) {
                if (config == null) {
                    config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
                }
            }
        }
        return config;
    }

    void setConfig(PremiumAccount config) {
        this.config = config;
    }

}