package cz.vity.freerapid.plugins.services.streamcz;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;


/**
 * @author Ladislav Vitasek, Ludek Zika
 */
public class StreamCzServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "stream.cz";
    private static final String CONFIG_FILE = "plugin_StreamCzSettings.xml";
    private volatile SettingsConfig config;

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new StreamCzRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new SettingsPanel(this), "Stream.cz settings")) {
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
        synchronized (StreamCzServiceImpl.class) {
            this.config = config;
        }
    }

}