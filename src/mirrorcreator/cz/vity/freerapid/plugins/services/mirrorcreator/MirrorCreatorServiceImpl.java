package cz.vity.freerapid.plugins.services.mirrorcreator;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class MirrorCreatorServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "plugin_MirrorCreatorSettings.xml";
    private volatile MirrorCreatorSettingsConfig config;

    @Override
    public String getName() {
        return "mirrorcreator.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MirrorCreatorFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new MirrorCreatorSettingsPanel(this), "MirrorCreator settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    public MirrorCreatorSettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new MirrorCreatorSettingsConfig();
                config.setQueueAllLinks(true);
            } else {
                config = storage.loadConfigFromFile(CONFIG_FILE, MirrorCreatorSettingsConfig.class);
            }
        }
        return config;
    }

    public void setConfig(final MirrorCreatorSettingsConfig config) {
        synchronized (MirrorCreatorServiceImpl.class) {
            this.config = config;
        }
    }

}