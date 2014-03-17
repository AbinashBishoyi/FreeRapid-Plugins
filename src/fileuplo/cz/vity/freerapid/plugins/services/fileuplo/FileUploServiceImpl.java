package cz.vity.freerapid.plugins.services.fileuplo;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FileUploServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FileUplo";
    }

    @Override
    public String getName() {
        return "fileuplo.de";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileUploFileRunner();
    }

}