package cz.vity.freerapid.plugins.services.repofile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class RepoFileServiceImpl extends XFileSharingServiceImpl {
	
	@Override
    public String getServiceTitle() {
        return "RepoFile";
    }
	
    @Override
    public String getName() {
        return "repofile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RepoFileFileRunner();
    }
}