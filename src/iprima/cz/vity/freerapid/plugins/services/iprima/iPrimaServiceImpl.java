package cz.vity.freerapid.plugins.services.iprima;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author JPEXS
 */
public class iPrimaServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "iPrimaSettings.xml";
    private volatile iPrimaSettingsConfig config;
    @Override
    public String getName() {
        return "iprima.cz";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new iPrimaFileRunner();
    }
    
    @Override
    public void showOptions() throws Exception {
        super.showOptions();

        if (getPluginContext().getDialogSupport().showOKCancelDialog(new iPrimaSettingsPanel(this), "iPrima.cz settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }
    
    public iPrimaSettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();

        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new iPrimaSettingsConfig();
                config.setQualitySetting(1);
            } else {
                config = storage.loadConfigFromFile(CONFIG_FILE, iPrimaSettingsConfig.class);
            }
        }

        return config;
    }

}