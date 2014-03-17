package cz.vity.freerapid.plugins.services.swatupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class SwatUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "SwatUpload";
    }

    @Override
    public String getName() {
        return "swatupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SwatUploadFileRunner();
    }

}