package cz.vity.freerapid.plugins.services.channel9;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Abinash Bishoyi
 */
public class Channel9ServiceImpl extends AbstractFileShareService {

    private static final String CONFIG_FILE = "Channel9Settings.xml";
    private volatile Channel9SettingsConfig config;

    @Override
    public String getName() {
        return "channel9.msdn.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Channel9FileRunner();
    }

    public Channel9SettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();

        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new Channel9SettingsConfig();
                config.setQualitySetting(1);
            } else {
                config = storage.loadConfigFromFile(CONFIG_FILE, Channel9SettingsConfig.class);
            }
        }

        return config;
    }

    public void setConfig(final Channel9SettingsConfig config) {
        synchronized (Channel9ServiceImpl.class) {
            this.config = config;
        }
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();

        if (getPluginContext().getDialogSupport().showOKCancelDialog(new ChannelSettingsPanel(this), "Quality settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }


}