package cz.vity.freerapid.plugins.services.depositfiles;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;
import cz.vity.freerapid.plugins.services.depositfiles.DepositFilesRunner;

import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek
 */
public class DepositFilesShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "depositfiles.com";
    private final static Pattern pattern = Pattern.compile("http://(www\\.)?depositfiles\\.com/.*", Pattern.CASE_INSENSITIVE);

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    public boolean supportsURL(String url) {
        return pattern.matcher(url).matches();
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new cz.vity.freerapid.plugins.services.depositfiles.DepositFilesRunner().run(downloader);
    }

}