package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Kajda
 */
public class YouTubeServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "youtube.com";
    private static final String CONFIG_FILE = "YouTubeSettings.xml";
    private volatile YouTubeSettingsConfig config;

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
        return new YouTubeFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();

        if (getPluginContext().getDialogSupport().showOKCancelDialog(new YouTubeSettingsPanel(this), "YouTube settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    public YouTubeSettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();

        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new YouTubeSettingsConfig();
                config.setQualitySetting(1);
                config.setReversePlaylistOrder(false);
            } else {
                config = storage.loadConfigFromFile(CONFIG_FILE, YouTubeSettingsConfig.class);
            }
        }

        return config;
    }
}