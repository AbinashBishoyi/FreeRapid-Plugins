package cz.vity.freerapid.plugins.services.tunescoop;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class TuneScoopServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "TuneScoopSettings.xml";
    private volatile TuneScoopSettingsConfig config;

    public String getName() {
        return "tunescoop.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TuneScoopFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();

        if (getPluginContext().getDialogSupport().showOKCancelDialog(new TuneScoopSettingsPanel(this), "TuneScoop settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    public TuneScoopSettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();

        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new TuneScoopSettingsConfig();
            } else {
                config = storage.loadConfigFromFile(CONFIG_FILE, TuneScoopSettingsConfig.class);
            }
        }

        return config;
    }

}