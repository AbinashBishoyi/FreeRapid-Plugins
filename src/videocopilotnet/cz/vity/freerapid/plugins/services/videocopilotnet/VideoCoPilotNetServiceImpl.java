package cz.vity.freerapid.plugins.services.videocopilotnet;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class VideoCoPilotNetServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "plugin_VideoCoPilotNet.xml";
    private volatile VideoCoPilotNetSettingsConfig config;

    @Override
    public String getName() {
        return "videocopilot.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VideoCoPilotNetFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new VideoCoPilotNetSettingsPanel(this), "VideoCoPilot settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    VideoCoPilotNetSettingsConfig getConfig() throws Exception {
        synchronized (VideoCoPilotNetServiceImpl.class) {
            final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
            if (config == null) {
                if (!storage.configFileExists(CONFIG_FILE)) {
                    config = new VideoCoPilotNetSettingsConfig();
                    config.setVideoQuality(VideoQuality.HD);
                    config.setDownloadProject(true);
                } else {
                    config = storage.loadConfigFromFile(CONFIG_FILE, VideoCoPilotNetSettingsConfig.class);
                }
            }
            return config;
        }
    }

    void setConfig(final VideoCoPilotNetSettingsConfig config) {
        synchronized (VideoCoPilotNetServiceImpl.class) {
            this.config = config;
        }
    }


}