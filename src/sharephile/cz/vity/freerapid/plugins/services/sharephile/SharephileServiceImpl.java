package cz.vity.freerapid.plugins.services.sharephile;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author JPEXS
 */
public class SharephileServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "sharephile.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SharephileFileRunner();
    }
    
    public int getMaxDownloadsFromOneIP(){
       return 1;
    }

}