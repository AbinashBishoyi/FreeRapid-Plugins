package cz.vity.freerapid.plugins.services.sharerapid;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Jan Smejkal (edit from CZshare profi to RapidShare)
 */
public class ShareRapidServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "share-rapid.com";
    private static final String PLUGIN_CONFIG_FILE = "plugin_ShareRapid.xml";
    private volatile PremiumAccount config;

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ShareRapidRunner();
    }

    @Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    public PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "CZshare", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        if (config == null) {
            synchronized (ShareRapidServiceImpl.class) {
                config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
            }
        }

        return config;
    }

}