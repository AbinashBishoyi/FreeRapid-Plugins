package cz.vity.freerapid.plugins.services.mixturecloud;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class MixtureCloudServiceImpl extends AbstractFileShareService {
    private final String configFile = "plugin_MixtureCloud.xml";
    private volatile PremiumAccount config;

    @Override
    public String getName() {
        return "mixturecloud.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MixtureCloudFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        final PremiumAccount pa = showConfigDialog();
        if (pa != null) {
            setConfig(pa);
        }
    }

    public PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "MixtureCloud", configFile);
    }

    public PremiumAccount getConfig() throws Exception {
        if (config == null) {
            synchronized (MixtureCloudServiceImpl.class) {
                config = getAccountConfigFromFile(configFile);
            }
        }
        return config;
    }

    public void setConfig(final PremiumAccount config) {
        synchronized (MixtureCloudServiceImpl.class) {
            this.config = config;
        }
    }

}