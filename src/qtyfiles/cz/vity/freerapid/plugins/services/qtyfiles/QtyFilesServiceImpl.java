package cz.vity.freerapid.plugins.services.qtyfiles;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class QtyFilesServiceImpl extends AbstractFileShareService {
	private final static String PLUGIN_CONFIG_FILE = "plugin_QtyFiles.xml";
    private final static String SERVICE_TITLE = "QtyFiles";
	private volatile PremiumAccount config;

    @Override
    public String getName() {
        return "qtyfiles.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new QtyFilesFileRunner();
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
            synchronized (QtyFilesServiceImpl.class) {
                config = getAccountConfigFromFile(PLUGIN_CONFIG_FILE);
            }
        }
        return config;
    }

}