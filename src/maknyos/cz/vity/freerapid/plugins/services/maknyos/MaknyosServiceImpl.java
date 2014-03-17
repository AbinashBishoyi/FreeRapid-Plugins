package cz.vity.freerapid.plugins.services.maknyos;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Alex
 */
public class MaknyosServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "maknyos.com";
    private static final String PLUGIN_CONFIG_FILE = "plugin_Maknyos.xml";
    private volatile PremiumAccount config;

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MaknyosRunner();
    }

    @Override
    public void showOptions() {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    public PremiumAccount showConfigDialog() {
        return showAccountDialog(getConfig(), "Maknyos", PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() {
        if (config == null) {
            synchronized (MaknyosServiceImpl.class) {
                config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
            }
        }
        return config;
    }

}
