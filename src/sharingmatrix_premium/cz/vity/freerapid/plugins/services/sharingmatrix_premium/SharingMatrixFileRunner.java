package cz.vity.freerapid.plugins.services.sharingmatrix_premium;

import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class SharingMatrixFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SharingMatrixFileRunner.class.getName());
    private boolean badConfig = false;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        Matcher m= Pattern.compile("http://(www\\d?\\.)?sharingmatrix\\.com/(file/.+)").matcher(fileURL);
        if(m.matches()){
            final String fileSonicUrl="http://www.filesonic.com/en/"+m.group(2);
            httpFile.setNewURL(new URL(fileSonicUrl));
            httpFile.setState(DownloadState.QUEUED);
            httpFile.setPluginID("");
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        runCheck();
    }

}