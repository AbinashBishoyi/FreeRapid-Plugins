package cz.vity.freerapid.plugins.services.eyesfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author bircihe
 */
public class EyesFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "EyesFile";
    }

    @Override
    public String getName() {
        return "eyesfile.ca";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new EyesFileFileRunner();
    }

}