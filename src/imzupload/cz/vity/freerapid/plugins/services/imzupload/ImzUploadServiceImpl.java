package cz.vity.freerapid.plugins.services.imzupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class ImzUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ImzUpload";
    }

    @Override
    public String getName() {
        return "imzupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ImzUploadFileRunner();
    }

}