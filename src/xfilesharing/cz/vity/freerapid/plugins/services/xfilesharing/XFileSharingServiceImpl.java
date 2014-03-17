package cz.vity.freerapid.plugins.services.xfilesharing;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class XFileSharingServiceImpl extends AbstractFileShareService {
    public XFileSharingServiceImpl() {
        super();
    }

    @Override
    public String getName() {
        return "xfilesharing.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    public PluginRunner getPluginRunnerInstance() {
        return new XFileSharingRunner();
    }

}