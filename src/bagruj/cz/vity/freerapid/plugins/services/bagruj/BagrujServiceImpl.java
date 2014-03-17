package cz.vity.freerapid.plugins.services.bagruj;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
public class BagrujServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "bagruj.cz";
    private static final String PLUGIN_CONFIG_FILE = "plugin_Bagruj.xml";
    private volatile PremiumAccount config;
    private int maxDown = 1;

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return maxDown;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BagrujRunner();
    }

    @Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    public PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "Bagruj", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        if (config == null) {
            synchronized (BagrujServiceImpl.class) {
                config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
            }
        }

        return config;
    }

    void SetMaxDown(int m) {
        maxDown=m;
    }

}