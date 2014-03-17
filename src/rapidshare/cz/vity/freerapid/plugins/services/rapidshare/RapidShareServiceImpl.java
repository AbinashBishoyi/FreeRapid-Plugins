package cz.vity.freerapid.plugins.services.rapidshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek
 */
public class RapidShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "RapidShare.com";
    private final static Pattern pattern = Pattern.compile("http://(www\\.)?rapidshare\\.com/files/[0-9]*/.*", Pattern.CASE_INSENSITIVE);

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
        new RapidShareRunner().run(downloader);
    }

}
