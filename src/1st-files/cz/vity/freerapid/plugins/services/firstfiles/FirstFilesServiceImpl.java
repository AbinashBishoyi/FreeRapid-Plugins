package cz.vity.freerapid.plugins.services.firstfiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FirstFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "1st-Files";
    }

    @Override
    public String getName() {
        return "1st-files.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FirstFilesFileRunner();
    }

}