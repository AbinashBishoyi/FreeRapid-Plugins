package cz.vity.freerapid.plugins.services.movzap;

import cz.vity.freerapid.plugins.services.xfilesharing.AccountServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class MovZapServiceImpl extends AccountServiceImpl {
    private final static String PLUGIN_CONFIG_FILE = "plugin_MovZap.xml";
    private final static String SERVICE_TITLE = "MovZap";

    public MovZapServiceImpl() {
        super(PLUGIN_CONFIG_FILE, SERVICE_TITLE, MovZapServiceImpl.class);
    }

    @Override
    public String getName() {
        return "movzap.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    public PluginRunner getPluginRunnerInstance() {
        return new MovZapFileRunner();
    }

}