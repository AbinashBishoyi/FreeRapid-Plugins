package cz.vity.freerapid.plugins.services.rd_fs;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Rd_FsServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Rd-Fs";
    }

    @Override
    public String getName() {
        return "rd-fs.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Rd_FsFileRunner();
    }

}