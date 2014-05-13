package cz.vity.freerapid.plugins.services.liveleak;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class LiveLeakServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "liveleak.com";
    private static final String CONFIG_FILE = "plugin_LiveLeakSettings.xml";
    private volatile SettingsConfig config;

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
        return new LiveLeakFileRunner();
    }


    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new SettingsPanel(this), "Live Leak settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    public SettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new SettingsConfig();
            } else {
                config = storage.loadConfigFromFile(CONFIG_FILE, SettingsConfig.class);
            }
        }
        return config;
    }

    public void setConfig(final SettingsConfig config) {
        synchronized (LiveLeakServiceImpl.class) {
            this.config = config;
        }
    }

}