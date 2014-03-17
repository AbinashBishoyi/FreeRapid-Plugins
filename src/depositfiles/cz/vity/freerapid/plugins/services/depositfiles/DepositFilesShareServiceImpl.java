package cz.vity.freerapid.plugins.services.depositfiles;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

/**
 * @author Ladislav Vitasek
 */
public class DepositFilesShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "depositfiles.com";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }


    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new cz.vity.freerapid.plugins.services.depositfiles.DepositFilesRunner().run(downloader);
    }

}