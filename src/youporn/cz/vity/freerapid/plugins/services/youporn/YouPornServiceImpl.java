package cz.vity.freerapid.plugins.services.youporn;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class YouPornServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "plugin_YouPorn.xml";
    private volatile YouPornSettingsConfig config;

    @Override
    public String getName() {
        return "youporn.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new YouPornFileRunner();
    }


    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new YouPornSettingsPanel(this), "YouPorn settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    YouPornSettingsConfig getConfig() throws Exception {
        synchronized (YouPornServiceImpl.class) {
            final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
            if (config == null) {
                if (!storage.configFileExists(CONFIG_FILE)) {
                    config = new YouPornSettingsConfig();
                    config.setVideoQuality(1);  // Medium
                } else {
                    config = storage.loadConfigFromFile(CONFIG_FILE, YouPornSettingsConfig.class);
                }
            }
            return config;
        }
    }

    public String getVideoDescription() throws Exception {
        return (new YouPornSettingsPanel(this).getQualityDescription(config.getVideoQuality()));
    }

    public String getVideoFormat() throws Exception {
        return (new YouPornSettingsPanel(this).getQualityType(config.getVideoQuality()));
    }
}