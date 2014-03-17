package cz.vity.freerapid.plugins.services.myurl;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Arthur Gunawan
 */
public class MyurlServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "myurl.in";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MyurlFileRunner();
    }

}
