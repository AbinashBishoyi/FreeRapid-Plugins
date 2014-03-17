package cz.vity.freerapid.plugins.services.ddlanime;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class DdlAnimeServiceImpl extends AbstractFileShareService {
    private static final String PLUGIN_CONFIG_FILE = "plugin_DdlAnime.xml";
    private volatile PremiumAccount config;

    @Override
    public String getName() {
        return "ddlani.me";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DdlAnimeFileRunner();
    }

    @Override
    public void showOptions() {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    public PremiumAccount showConfigDialog() {
        return showAccountDialog(getConfig(), "DDLAnime", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() {
        if (config == null) {
            synchronized (DdlAnimeServiceImpl.class) {
                config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
            }
        }
        return config;
    }

}