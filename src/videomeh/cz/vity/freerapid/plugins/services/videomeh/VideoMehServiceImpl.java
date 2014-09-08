package cz.vity.freerapid.plugins.services.videomeh;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class VideoMehServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "plugin_VideoMeh.xml";
    private volatile SettingsConfig config;

    @Override
    public String getName() {
        return "videomeh.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VideoMehFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new SettingsPanel(this), "VideoMeh settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    SettingsConfig getConfig() throws Exception {
        synchronized (VideoMehServiceImpl.class) {
            final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
            if (config == null) {
                if (!storage.configFileExists(CONFIG_FILE)) {
                    config = new SettingsConfig();
                    config.setVideoQuality(VideoQuality._480);
                } else {
                    config = storage.loadConfigFromFile(CONFIG_FILE, SettingsConfig.class);
                }
            }
            return config;
        }
    }

    void setConfig(final SettingsConfig config) {
        synchronized (VideoMehServiceImpl.class) {
            this.config = config;
        }
    }

}