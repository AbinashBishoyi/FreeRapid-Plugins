package cz.vity.freerapid.plugins.services.miroriii;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author ntoskrnl
 */
public class MiroriiiServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "Miroriii_Settings.xml";
    private volatile MiroriiiSettingsConfig config;

    @Override
    public String getName() {
        return "miroriii.com";
    }

    @Override
    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MiroriiiFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        getPluginContext().getDialogSupport().showOKDialog(new MiroriiiSettingsPanel(this), "Miroriii settings");
        getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
    }

    public MiroriiiSettingsConfig getConfig() throws Exception {
        final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();

        if (config == null) {
            if (!storage.configFileExists(CONFIG_FILE)) {
                config = new MiroriiiSettingsConfig();
                config.setDefault();
            } else {
                config = storage.loadConfigFromFile(CONFIG_FILE, MiroriiiSettingsConfig.class);
            }
        }

        return config;
    }
}