package cz.vity.freerapid.plugins.services.photobucket;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class PhotoBucketServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "photobucket.com";
    }

    @Override
    public int getMaxDownloadsFromOneIP() {
        return 10;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PhotoBucketFileRunner();
    }

}