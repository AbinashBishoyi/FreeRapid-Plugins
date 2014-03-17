package cz.vity.freerapid.plugins.services.megaupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Ladislav Vitasek
 */
public class MegauploadShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "megaupload.com";
    private static final String PLUGIN_CONFIG_FILE = "plugin_MegaUpload.xml";
    private volatile PremiumAccount config;

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MegauploadRunner();
    }

    @Override
    public void showOptions() {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    public PremiumAccount showConfigDialog() {
        return showAccountDialog(getConfig(), "MegaUpload", PLUGIN_CONFIG_FILE);
    }

    public PremiumAccount getConfig() {
        synchronized (MegauploadShareServiceImpl.class) {
            if (config == null) {
                config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
            }
        }
        return config;
    }

    public void setConfig(PremiumAccount config) {
        this.config = config;
    }

}