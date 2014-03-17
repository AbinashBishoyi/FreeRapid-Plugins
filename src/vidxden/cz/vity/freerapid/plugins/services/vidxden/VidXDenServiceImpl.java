package cz.vity.freerapid.plugins.services.vidxden;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class VidXDenServiceImpl extends AbstractFileShareService {
	private final static String PLUGIN_CONFIG_FILE = "plugin_VidXDen.xml";
    private final static String SERVICE_TITLE = "VidXDen";
	private volatile PremiumAccount config;

    @Override
    public String getName() {
        return "vidxden.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VidXDenFileRunner();
    }
	
	@Override
    public void showOptions() throws Exception {
        PremiumAccount pa = showConfigDialog();
        if (pa != null) config = pa;
    }

    public PremiumAccount showConfigDialog() throws Exception {
        return showAccountDialog(getConfig(), SERVICE_TITLE, PLUGIN_CONFIG_FILE);
    }

    PremiumAccount getConfig() throws Exception {
        if (config == null) {
            synchronized (VidXDenServiceImpl.class) {
                config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
            }
        }
        return config;
    }

}