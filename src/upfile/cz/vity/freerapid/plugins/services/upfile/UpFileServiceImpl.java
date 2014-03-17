package cz.vity.freerapid.plugins.services.upfile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UpFileServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UpFile";
    }

    @Override
    public String getName() {
        return "upfile.biz";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UpFileFileRunner();
    }

}