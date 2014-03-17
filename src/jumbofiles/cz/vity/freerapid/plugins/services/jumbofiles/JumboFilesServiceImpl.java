package cz.vity.freerapid.plugins.services.jumbofiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class JumboFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "JumboFiles";
    }

    @Override
    public String getName() {
        return "jumbofiles.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new JumboFilesFileRunner();
    }
}