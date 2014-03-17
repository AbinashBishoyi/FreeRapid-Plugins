package cz.vity.freerapid.plugins.services.asixfiles;

import cz.vity.freerapid.plugins.services.xfilesharingcommon.XFileSharingCommonServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class AsixFilesServiceImpl extends XFileSharingCommonServiceImpl {
    private final static String PLUGIN_CONFIG_FILE = "plugin_AsixFiles.xml";
    private final static String SERVICE_TITLE = "AsixFiles";

    @Override
    public Class getImplClass() {
        return AsixFilesServiceImpl.class;
    }

    @Override
    public String getPluginConfigFile() {
        return PLUGIN_CONFIG_FILE;
    }


    @Override
    public String getPluginServiceTitle() {
        return SERVICE_TITLE;
    }
    

    @Override
    public String getName() {
        return "asixfiles.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    public PluginRunner getPluginRunnerInstance() {
        return new AsixFilesFileRunner();
    }

}