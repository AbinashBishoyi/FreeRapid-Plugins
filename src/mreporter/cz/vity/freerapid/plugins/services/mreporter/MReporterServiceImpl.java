package cz.vity.freerapid.plugins.services.mreporter;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class MReporterServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "plugin_MReporter.xml";
    private volatile SettingsConfig config;

    @Override
    public String getName() {
        return "mreporter.ru";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MReporterFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new SettingsPanel(this), "MReporter settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    SettingsConfig getConfig() throws Exception {
        synchronized (MReporterServiceImpl.class) {
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
    }

    void setConfig(final SettingsConfig config) {
        synchronized (MReporterServiceImpl.class) {
            this.config = config;
        }
    }

}