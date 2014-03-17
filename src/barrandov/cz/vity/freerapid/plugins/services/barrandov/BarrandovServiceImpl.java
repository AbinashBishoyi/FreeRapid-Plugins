package cz.vity.freerapid.plugins.services.barrandov;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author JPEXS
 */
public class BarrandovServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "BarrandovSettings.xml";
    private volatile BarrandovSettingsConfig config;

    public String getName() {
        return "barrandov.tv";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BarrandovFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();

        if (getPluginContext().getDialogSupport().showOKCancelDialog(new BarrandovSettingsPanel(this), "Barrandov.tv settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    public BarrandovSettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();

        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new BarrandovSettingsConfig();
                config.setQualitySetting(2);
            } else {
                config = storage.loadConfigFromFile(CONFIG_FILE, BarrandovSettingsConfig.class);
            }
        }

        return config;
    }

}