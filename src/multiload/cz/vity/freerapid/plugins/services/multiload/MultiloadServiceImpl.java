package cz.vity.freerapid.plugins.services.multiload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity+JPEXS
 */
public class MultiloadServiceImpl extends AbstractFileShareService {
    private volatile MultiloadSettingsConfig config;
    private static final String CONFIG_FILE = "MultiloadSettings.xml";

    public String getName() {
        return "multiload.cz";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MultiloadFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();

        if (getPluginContext().getDialogSupport().showOKCancelDialog(new MultiloadSettingsPanel(this), "Multiload settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    public MultiloadSettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();

        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new MultiloadSettingsConfig();
                config.setServerSetting(6);
            } else {
                config = storage.loadConfigFromFile(CONFIG_FILE, MultiloadSettingsConfig.class);
            }
        }

        return config;
    }
}
