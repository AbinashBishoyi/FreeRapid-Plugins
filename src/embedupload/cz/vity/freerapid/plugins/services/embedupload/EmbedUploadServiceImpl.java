package cz.vity.freerapid.plugins.services.embedupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class EmbedUploadServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "EmbedUploadSettings.xml";
    private volatile EmbedUploadSettingsConfig config;

    @Override
    public String getName() {
        return "embedupload.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new EmbedUploadFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new EmbedUploadSettingsPanel(this), "EmbedUpload settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    public EmbedUploadSettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new EmbedUploadSettingsConfig();
                config.setQueueAllLinks(false);
            } else {
                config = storage.loadConfigFromFile(CONFIG_FILE, EmbedUploadSettingsConfig.class);
            }
        }
        return config;
    }

    public void setConfig(final EmbedUploadSettingsConfig config) {
        synchronized (EmbedUploadServiceImpl.class) {
            this.config = config;
        }
    }

}