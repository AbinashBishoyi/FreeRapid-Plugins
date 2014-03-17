package cz.vity.freerapid.plugins.services.directmirror;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class DirectMirrorServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "plugin_DirectMirrorSettings.xml";
    private volatile DirectMirrorSettingsConfig config;

    @Override
    public String getName() {
        return "directmirror.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DirectMirrorFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new DirectMirrorSettingsPanel(this), "DirectMirror settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    public DirectMirrorSettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new DirectMirrorSettingsConfig();
                config.setQueueAllLinks(true);
            } else {
                config = storage.loadConfigFromFile(CONFIG_FILE, DirectMirrorSettingsConfig.class);
            }
        }
        return config;
    }

    public void setConfig(final DirectMirrorSettingsConfig config) {
        synchronized (DirectMirrorServiceImpl.class) {
            this.config = config;
        }
    }

}