package cz.vity.freerapid.plugins.services.filecloudio;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class FileCloudIoServiceImpl extends AbstractFileShareService {
    private final String configFile = "plugin_FileCloud.xml";
    private volatile PremiumAccount config;

    @Override
    public String getName() {
        return "filecloud.io";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileCloudIoFileRunner();
    }

    @Override
    public void showOptions() throws Exception {
        final PremiumAccount pa = showConfigDialog();
        if (pa != null) {
            setConfig(pa);
        }
    }

    public PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), "FileCloud", configFile);
    }

    public PremiumAccount getConfig() throws Exception {
        if (config == null) {
            synchronized (FileCloudIoServiceImpl.class) {
                config = getAccountConfigFromFile(configFile);
            }
        }
        return config;
    }

    public void setConfig(final PremiumAccount config) {
        synchronized (FileCloudIoServiceImpl.class) {
            this.config = config;
        }
    }

}