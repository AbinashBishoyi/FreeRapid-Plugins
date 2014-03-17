package cz.vity.freerapid.plugins.services.movzap;

import cz.vity.freerapid.plugins.services.xfilesharingcommon.XFileSharingCommonServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class MovZapServiceImpl extends XFileSharingCommonServiceImpl {
    private final static String PLUGIN_CONFIG_FILE = "plugin_MovZap.xml";
    private final static String SERVICE_TITLE = "MovZap";

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

    @Override
    protected String getPluginConfigFile() {
        return PLUGIN_CONFIG_FILE;
    }

    @Override
    protected String getPluginServiceTitle() {
        return SERVICE_TITLE;
    }

    @Override
    protected Class getImplClass() {
        return MovZapServiceImpl.class;
    }
}