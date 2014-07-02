package cz.vity.freerapid.plugins.services.canalplus;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class CanalPlusServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "canalplus.fr";
    private static final String CONFIG_FILE = "plugin_CanalPlusSettings.xml";
    private volatile SettingsConfig config;

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
        return new CanalPlusFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new SettingsPanel(this), "CanalPlus settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    public SettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new SettingsConfig();
            } else {
                config = storage.loadConfigFromFile(CONFIG_FILE, SettingsConfig.class);
            }
        }
        return config;
    }

    public void setConfig(final SettingsConfig config) {
        synchronized (CanalPlusServiceImpl.class) {
            this.config = config;
        }
    }
}