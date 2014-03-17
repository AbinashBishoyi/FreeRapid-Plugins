package cz.vity.freerapid.plugins.services.putlocker;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class PutLockerServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "plugin_PutLocker.xml";
    private volatile PutLockerSettingsConfig config;

    @Override
    public String getName() {
        return "putlocker.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PutLockerFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new PutLockerSettingsPanel(this), "PutLocker settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    PutLockerSettingsConfig getConfig() throws Exception {
        synchronized (PutLockerServiceImpl.class) {
            final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
            if (config == null) {
                if (!storage.configFileExists(CONFIG_FILE)) {
                    config = new PutLockerSettingsConfig();
                } else {
                    config = storage.loadConfigFromFile(CONFIG_FILE, PutLockerSettingsConfig.class);
                }
            }
            return config;
        }
    }

    void setConfig(final PutLockerSettingsConfig config) {
        synchronized (PutLockerServiceImpl.class) {
            this.config = config;
        }
    }

}