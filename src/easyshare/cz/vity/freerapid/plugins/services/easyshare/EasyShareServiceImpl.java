package cz.vity.freerapid.plugins.services.easyshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

/**
 * @author Ladislav Vitasek
 */
public class EasyShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "easy-share.com";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new EasyShareRunner().run(downloader);
    }

}