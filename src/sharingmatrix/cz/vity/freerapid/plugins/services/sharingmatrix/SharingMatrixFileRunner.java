package cz.vity.freerapid.plugins.services.sharingmatrix;

import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kajda, JPEXS, RickCL, codeboy2k
 * @since 0.82
 */
class SharingMatrixFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SharingMatrixFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        Matcher m=Pattern.compile("http://(www\\d?\\.)?sharingmatrix\\.com/(file/.+)").matcher(fileURL);
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
