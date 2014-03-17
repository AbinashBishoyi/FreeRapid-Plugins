package cz.vity.freerapid.plugins.services.vk;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class VkServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "plugin_Vk.xml";
    private volatile VkSettingsConfig config;

    @Override
    public String getName() {
        return "vk.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VkFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new VkSettingsPanel(this), "Vk settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    VkSettingsConfig getConfig() throws Exception {
        synchronized (VkServiceImpl.class) {
            final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
            if (config == null) {
                if (!storage.configFileExists(CONFIG_FILE)) {
                    config = new VkSettingsConfig();
                    config.setVideoQuality(VideoQuality._480);
                } else {
                    config = storage.loadConfigFromFile(CONFIG_FILE, VkSettingsConfig.class);
                }
            }
            return config;
        }
    }

    void setConfig(final VkSettingsConfig config) {
        synchronized (VkServiceImpl.class) {
            this.config = config;
        }
    }

}