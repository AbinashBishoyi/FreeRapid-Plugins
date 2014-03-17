package cz.vity.freerapid.plugins.services.cosmobox;

import cz.vity.freerapid.plugins.services.xfilesharingcommon.XFileSharingCommonServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class CosmoBoxServiceImpl extends XFileSharingCommonServiceImpl {
    private final static String PLUGIN_CONFIG_FILE = "plugin_CosmoBox.xml";
    private final static String SERVICE_TITLE = "CosmoBox";

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
        return CosmoBoxServiceImpl.class;
    }
}