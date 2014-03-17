package cz.vity.freerapid.plugins.services.dailymotion;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 */
public class DailymotionServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "dailymotion.com";
    private static final String CONFIG_FILE = "plugin_DailyMotionSettings.xml";
    private volatile DailymotionSettingsConfig config;

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
        return new DailymotionRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new DailymotionSettingsPanel(this), "DailyMotion settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    public DailymotionSettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new DailymotionSettingsConfig();
            } else {
                config = storage.loadConfigFromFile(CONFIG_FILE, DailymotionSettingsConfig.class);
            }
        }
        return config;
    }

    public void setConfig(final DailymotionSettingsConfig config) {
        synchronized (DailymotionServiceImpl.class) {
            this.config = config;
        }
    }


}
