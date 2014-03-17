package cz.vity.freerapid.plugins.services.asixfiles;

import cz.vity.freerapid.plugins.services.xfilesharingcommon.AccountServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class AsixFilesServiceImpl extends AccountServiceImpl {
    private final static String PLUGIN_CONFIG_FILE = "plugin_AsixFiles.xml";
    private final static String SERVICE_TITLE = "AsixFiles";

    public AsixFilesServiceImpl() {
        super(PLUGIN_CONFIG_FILE, SERVICE_TITLE, AsixFilesServiceImpl.class);
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