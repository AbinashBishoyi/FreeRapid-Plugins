package cz.vity.freerapid.plugins.services.raagfm;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class RaagFmServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "RaagFmSettings.xml";
    private volatile RaagFmSettingsConfig config;

    @Override
    public String getName() {
        return "raag.fm";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RaagFmFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();

        if (getPluginContext().getDialogSupport().showOKCancelDialog(new RaagFmSettingsPanel(this), "Raag.fm settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    public RaagFmSettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new RaagFmSettingsConfig();
                config.setQualitySetting(1); //320 kpbs
            } else {
                config = storage.loadConfigFromFile(CONFIG_FILE, RaagFmSettingsConfig.class);
            }
        }
        return config;
    }

}