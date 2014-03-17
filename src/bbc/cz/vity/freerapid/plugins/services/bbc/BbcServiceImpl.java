package cz.vity.freerapid.plugins.services.bbc;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class BbcServiceImpl extends AbstractFileShareService {
    private static final String PLUGIN_CONFIG_FILE = "plugin_BBC.xml";
    private volatile SettingsConfig config;

    @Override
    public String getName() {
        return "bbc.co.uk";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BbcFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new SettingsPanel(this), "BBC settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, PLUGIN_CONFIG_FILE);
        }
    }

    public SettingsConfig getConfig() throws Exception {
        synchronized (BbcServiceImpl.class) {
            final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
            if (config == null) {
                if (!storage.configFileExists(PLUGIN_CONFIG_FILE)) {
                    config = new SettingsConfig();
                } else {
                    config = storage.loadConfigFromFile(PLUGIN_CONFIG_FILE, SettingsConfig.class);
                }
            }
            return config;
        }
    }

    public void setConfig(SettingsConfig config) {
        this.config = config;
    }

}