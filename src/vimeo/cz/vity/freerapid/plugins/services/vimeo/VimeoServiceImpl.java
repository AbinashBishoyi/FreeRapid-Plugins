package cz.vity.freerapid.plugins.services.vimeo;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class VimeoServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "plugin_Vimeo.xml";
    private volatile VimeoSettingsConfig config;

    @Override
    public String getName() {
        return "vimeo.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VimeoFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new VimeoSettingsPanel(this), "Vimeo settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    VimeoSettingsConfig getConfig() throws Exception {
        synchronized (VimeoServiceImpl.class) {
            final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
            if (config == null) {
                if (!storage.configFileExists(CONFIG_FILE)) {
                    config = new VimeoSettingsConfig();
                    config.setVideoQuality(VideoQuality.HD);
                } else {
                    config = storage.loadConfigFromFile(CONFIG_FILE, VimeoSettingsConfig.class);
                }
            }
            return config;
        }
    }

    void setConfig(final VimeoSettingsConfig config) {
        synchronized (VimeoServiceImpl.class) {
            this.config = config;
        }
    }

}