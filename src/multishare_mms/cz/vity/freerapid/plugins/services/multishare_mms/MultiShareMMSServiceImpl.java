package cz.vity.freerapid.plugins.services.multishare_mms;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author JPEXS
 */
public class MultiShareMMSServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "MultiShare.cz (MMS)";
    private static final String PLUGIN_CONFIG_FILE = "plugin_MultiSharePremium.xml";
    private volatile PremiumAccount config;

    public String getName() {
        return "multisharemms.cz";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MultiShareMMSFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    public PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "MultiShare", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        if (config == null) {
            synchronized (MultiShareMMSServiceImpl.class) {
                config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
            }
        }

        return config;
    }

    public int getMaxDownloadsFromOneIP() {
        return 9;
    }

}