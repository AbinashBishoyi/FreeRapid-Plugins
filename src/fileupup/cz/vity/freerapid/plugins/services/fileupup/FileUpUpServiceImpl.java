package cz.vity.freerapid.plugins.services.fileupup;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileUpUpServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileUpUp";
    }

    @Override
    public String getName() {
        return "fileupup.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileUpUpFileRunner();
    }

}