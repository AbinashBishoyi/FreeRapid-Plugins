package cz.vity.freerapid.plugins.services.filebox;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Kajda
 */
public class FileboxServiceImpl extends XFileSharingServiceImpl {
    @Override
    public String getServiceTitle() {
        return "FileBox";
    }

    @Override
    public String getName() {
        return "filebox.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileboxFileRunner();
    }
}
