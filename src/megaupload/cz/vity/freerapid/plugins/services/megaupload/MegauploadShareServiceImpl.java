package cz.vity.freerapid.plugins.services.megaupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

/**
 * @author Ladislav Vitasek
 */
public class MegauploadShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "megaupload.com";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }


    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new MegauploadRunner().run(downloader);
    }

}