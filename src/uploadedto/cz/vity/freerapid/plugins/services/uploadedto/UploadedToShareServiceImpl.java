package cz.vity.freerapid.plugins.services.uploadedto;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

/**
 * @author Ladislav Vitasek
 */
public class UploadedToShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "uploaded.to";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new UploadedToRunner().run(downloader);
    }

}
