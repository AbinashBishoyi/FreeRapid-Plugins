package cz.vity.freerapid.plugins.services.extabit_premium;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ExtabitPremiumServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "extabit.com_premium";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ExtabitPremiumFileRunner();
    }


    private static final String PLUGIN_CONFIG_FILE = "plugin_ExtabitPremium.xml";
    private volatile PremiumAccount config;

    @Override
    public void showOptions() {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    public PremiumAccount showConfigDialog() {
        return showAccountDialog(getConfig(), "Extabit Premium", PLUGIN_CONFIG_FILE);
    }

    public PremiumAccount getConfig() {
        synchronized (ExtabitPremiumServiceImpl.class) {
            if (config == null) {
                config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
            }
        }
        return config;
    }

    public void setConfig(PremiumAccount config) {
        this.config = config;
    }
}