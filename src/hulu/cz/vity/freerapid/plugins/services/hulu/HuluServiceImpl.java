package cz.vity.freerapid.plugins.services.hulu;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class HuluServiceImpl extends AbstractFileShareService {
    private static final String PLUGIN_CONFIG_FILE = "plugin_Hulu.xml";
    private volatile HuluSettingsConfig config;

    @Override
    public String getName() {
        return "hulu.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HuluFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new HuluSettingsPanel(this), "Hulu settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, PLUGIN_CONFIG_FILE);
        }
    }

    public HuluSettingsConfig getConfig() throws Exception {
        synchronized (HuluServiceImpl.class) {
            final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
            if (config == null) {
                if (!storage.configFileExists(PLUGIN_CONFIG_FILE)) {
                    config = new HuluSettingsConfig();
                } else {
                    config = storage.loadConfigFromFile(PLUGIN_CONFIG_FILE, HuluSettingsConfig.class);
                }
            }
            return config;
        }
    }

    public void setConfig(HuluSettingsConfig config) {
        this.config = config;
    }

}