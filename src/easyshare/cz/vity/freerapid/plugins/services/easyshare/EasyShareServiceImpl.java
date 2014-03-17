package cz.vity.freerapid.plugins.services.easyshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek
 */
public class EasyShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "easy-share.com";
    private final static Pattern pattern = Pattern.compile("http://([a-zA-Z_0-9])*?\\.easy-share\\.com/.*", Pattern.CASE_INSENSITIVE);

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
        new EasyShareRunner().run(downloader);
    }

}