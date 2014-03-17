package cz.vity.freerapid.plugins.services.ryushare;

import cz.vity.freerapid.plugins.services.xfilesharingcommon.AccountServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class RyuShareServiceImpl extends AccountServiceImpl {
    private static final String PLUGIN_CONFIG_FILE = "plugin_RyuShare.xml";
    private static final String SERVICE_TITLE = "RyuShare";

    public RyuShareServiceImpl() {
        super(PLUGIN_CONFIG_FILE, SERVICE_TITLE, RyuShareServiceImpl.class);
    }

    @Override
    public String getName() {
        return "ryushare.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    public PluginRunner getPluginRunnerInstance() {
        return new RyuShareFileRunner();
    }
}