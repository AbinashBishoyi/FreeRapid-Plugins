package cz.vity.freerapid.plugins.services.multiupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author ntoskrnl
 */
public class MultiUploadServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "MultiUploadSettings.xml";
    private volatile MultiUploadSettingsConfig config;

    public String getName() {
        return "multiupload.com";
    }

    public int getMaxDownloadsFromOneIP() {
        return 9;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MultiUploadFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        getPluginContext().getDialogSupport().showOKDialog(new MultiUploadSettingsPanel(this), "MultiUpload settings");
        getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
    }

    public MultiUploadSettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();

        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new MultiUploadSettingsConfig();
                config.setDefault();
            } else {
                config = storage.loadConfigFromFile(CONFIG_FILE, MultiUploadSettingsConfig.class);
            }
        }

        return config;
    }
}