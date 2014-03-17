package cz.vity.freerapid.plugins.services.filefactory_premium;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author ntoskrnl
 */
public class FileFactoryServiceImpl extends AbstractFileShareService {
    private static final String PLUGIN_CONFIG_FILE = "plugin_FileFactoryPremium.xml";
    private volatile PremiumAccount config;

    @Override
    public String getName() {
        return "filefactory.com_premium";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileFactoryFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    public PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "FileFactory", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        if (config == null) {
            synchronized (FileFactoryServiceImpl.class) {
                config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
            }
        }

        return config;
    }

    public void setConfig(PremiumAccount config) {
        this.config = config;
    }
}