package cz.vity.freerapid.plugins.services.hellshare_full;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
public class HellshareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "hellshare.com (full)";
    private static final String PLUGIN_CONFIG_FILE = "plugin_HellShareFull.xml";
    private volatile PremiumAccount config;

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HellshareRunner();
    }

    @Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    public PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "HellShare", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        if (config == null) {
            synchronized (HellshareServiceImpl.class) {
                config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
            }
        }

        return config;
    }

    public int getMaxDownloadsFromOneIP() {
        return 9;
    }

}