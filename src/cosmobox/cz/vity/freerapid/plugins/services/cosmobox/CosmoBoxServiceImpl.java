package cz.vity.freerapid.plugins.services.cosmobox;

import cz.vity.freerapid.plugins.services.xfilesharing.AccountServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class CosmoBoxServiceImpl extends AccountServiceImpl {
    private final static String PLUGIN_CONFIG_FILE = "plugin_CosmoBox.xml";
    private final static String SERVICE_TITLE = "CosmoBox";

    public CosmoBoxServiceImpl() {
        super(PLUGIN_CONFIG_FILE, SERVICE_TITLE, CosmoBoxServiceImpl.class);
    }

    @Override
    public String getName() {
        return "cosmobox.org";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    public PluginRunner getPluginRunnerInstance() {
        return new CosmoBoxFileRunner();
    }

}