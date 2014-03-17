package cz.vity.freerapid.plugins.services.nova;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author JPEXS
 */
public class NovaServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "NovaSettings.xml";
    private volatile NovaSettingsConfig config;
   
    @Override
    public String getName() {
        return "nova.cz";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new NovaFileRunner();
    }
    
    @Override
    public void showOptions() throws Exception {
        super.showOptions();

        if (getPluginContext().getDialogSupport().showOKCancelDialog(new NovaSettingsPanel(this), "Nova.cz settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    public NovaSettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();

        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new NovaSettingsConfig();
                config.setQualitySetting(1);
            } else {
                config = storage.loadConfigFromFile(CONFIG_FILE, NovaSettingsConfig.class);
            }
        }

        return config;
    }

}