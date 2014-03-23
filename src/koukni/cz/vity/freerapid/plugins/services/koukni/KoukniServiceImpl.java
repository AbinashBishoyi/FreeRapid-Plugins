package cz.vity.freerapid.plugins.services.koukni;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.ConfigurationStorageSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class KoukniServiceImpl extends AbstractFileShareService {
    private static final String CONFIG_FILE = "plugin_Koukni.xml";
    private volatile KoukniSettingsConfig config;

    @Override
    public String getName() {
        return "koukni.cz";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new KoukniFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        super.showOptions();
        if (getPluginContext().getDialogSupport().showOKCancelDialog(new KoukniSettingsPanel(this), "Koukni.cz settings")) {
            getPluginContext().getConfigurationStorageSupport().storeConfigToFile(config, CONFIG_FILE);
        }
    }

    KoukniSettingsConfig getConfig() throws Exception {
        synchronized (KoukniServiceImpl.class) {
            final ConfigurationStorageSupport storage = getPluginContext().getConfigurationStorageSupport();
            if (config == null) {
                if (!storage.configFileExists(CONFIG_FILE)) {
                    config = new KoukniSettingsConfig();
                    setVideoFormat(1);  // 720p
                } else {
                    config = storage.loadConfigFromFile(CONFIG_FILE, KoukniSettingsConfig.class);
                }
            }
            return config;
        }
    }

    public void setVideoFormat(final int format) throws Exception {
        config.setVideoQuality(format);
    }

    public String getVideoFormat() throws Exception {
        return (new KoukniSettingsPanel(this).getQualityString(config.getVideoQuality()));
    }

}