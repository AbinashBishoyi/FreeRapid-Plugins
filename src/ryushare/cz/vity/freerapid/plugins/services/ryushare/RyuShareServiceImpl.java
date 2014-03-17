package cz.vity.freerapid.plugins.services.ryushare;

import cz.vity.freerapid.plugins.services.xfilesharingcommon.XFileSharingCommonServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class RyuShareServiceImpl extends XFileSharingCommonServiceImpl {
    private static final String PLUGIN_CONFIG_FILE = "plugin_RyuShare.xml";
    private static final String SERVICE_TITLE = "RyuShare";

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

    @Override
    protected Class getImplClass() {
        return RyuShareServiceImpl.class;
    }

    @Override
    protected String getPluginConfigFile() {
        return PLUGIN_CONFIG_FILE;
    }

    @Override
    protected String getPluginServiceTitle() {
        return SERVICE_TITLE;
    }
}