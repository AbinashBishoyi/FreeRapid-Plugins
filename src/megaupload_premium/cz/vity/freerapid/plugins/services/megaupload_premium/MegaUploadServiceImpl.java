package cz.vity.freerapid.plugins.services.megaupload_premium;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class MegaUploadServiceImpl extends AbstractFileShareService {
    private static final String PLUGIN_CONFIG_FILE = "plugin_MegaUploadPremium.xml";
    private volatile PremiumAccount config;

    public String getName() {
        return "megaupload.com_premium";
    }

    public int getMaxDownloadsFromOneIP() {
        return 100;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MegaUploadFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    public PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "MegaUpload", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        if (config == null) {
            synchronized (MegaUploadServiceImpl.class) {
                config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
            }
        }

        return config;
    }

}