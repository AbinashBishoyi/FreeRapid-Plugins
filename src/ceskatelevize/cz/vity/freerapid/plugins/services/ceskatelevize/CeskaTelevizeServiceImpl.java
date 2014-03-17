package cz.vity.freerapid.plugins.services.ceskatelevize;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author JPEXS
 */
public class CeskaTelevizeServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "CeskaTelevizeSettings.xml";
    private volatile CeskaTelevizeSettingsConfig config;

    @Override
    public String getName() {
        return "ceskatelevize.cz";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CeskaTelevizeFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new CeskaTelevizeSettingsPanel(this), "CeskaTelevize settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    public CeskaTelevizeSettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new CeskaTelevizeSettingsConfig();
            } else {
                config = storage.loadConfigFromFile(CONFIG_FILE, CeskaTelevizeSettingsConfig.class);
            }
        }
        return config;
    }

    public void setConfig(final CeskaTelevizeSettingsConfig config) {
        synchronized (CeskaTelevizeServiceImpl.class) {
            this.config = config;
        }
    }

}