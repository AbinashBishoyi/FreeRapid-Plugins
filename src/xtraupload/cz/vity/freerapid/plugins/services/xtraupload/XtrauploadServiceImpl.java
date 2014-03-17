package cz.vity.freerapid.plugins.services.xtraupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

/**
 * @author Ladislav Vitasek
 */
public class XtrauploadServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "xtraupload.de";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 4;
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new XtrauploadRunner().run(downloader);
    }

}