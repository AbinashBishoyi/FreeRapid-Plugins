package cz.vity.freerapid.plugins.services.hellshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
public class HellshareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "hellshare.com";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new HellshareRunner().run(downloader);
    }

}