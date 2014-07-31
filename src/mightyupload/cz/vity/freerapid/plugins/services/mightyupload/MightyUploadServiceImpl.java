package cz.vity.freerapid.plugins.services.mightyupload;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MightyUploadServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MightyUpload";
    }

    @Override
    public String getName() {
        return "mightyupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MightyUploadFileRunner();
    }

}