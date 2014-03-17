package cz.vity.freerapid.plugins.services.uploadjockey;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import javax.swing.JOptionPane;

/**
 * @author Alex
 */
class UploadJockeyRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadJockeyRunner.class.getName());

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting run task " + fileURL);
        //<div class="uplink uplink_finished">
		//<a href="http://www.uploadjockey.com/redirect.php?url=aHR0cDovL3d3dy5maWxlZmFjdG9yeS5jb20vZmlsZS8=&key=297425" target="_blank" >FileFactory.com</a> </div>
        //<a href="http://www.uploadjockey.com/redirect.php?url=aHR0cDovL3cxMy5lYXN5LXNoYXJlLmNvbS8xNzAyNTY3NTYzLmh0bWw=&key=297425" target="_blank" >Easy-Share.com</a> </div>
        //<a href="http://www.uploadjockey.com/redirect.php?url=aHR0cDovL3JhcGlkc2hhcmUuY29tL2ZpbGVzLzE2NzU1NTM0OC9TaG9ydEtleXMudjIuMy4yLjEuSW5jbC5LZXltYWtlci1DT1JFLnJhcg==&key=297425" target="_blank" >Rapidshare.com</a> </div>
        //<a href="http://www.uploadjockey.com/redirect.php?url=aHR0cDovL3VwbG9hZGVkLnRv&key=297425" target="_blank" >Uploaded.to</a> </div>
        //<a href="http://www.uploadjockey.com/redirect.php?url=aHR0cDovL3d3dy5tZWdhdXBsb2FkLmNvbS8/ZD1QVUsxWVE4Vw==&key=297425" target="_blank" >Megaupload.com</a> </div><div class="uplink uplink_finished">
        //<a href="http://www.uploadjockey.com/redirect.php?url=aHR0cDovL2RlcG9zaXRmaWxlcy5jb20vZmlsZXMvZTd2ZnZnbjl1&key=297425" target="_blank" >DepositFiles.com</a> </div>

        //String x = JOptionPane.showInputDialog("Select server:");

        final GetMethod mJockey = getGetMethod(fileURL);
        if (makeRedirectedRequest(mJockey)) { //Load Jockey Page
            //Find File Factory // First for testing file removed error later make this last because there is >> Captcha
                    String ffOK = "Invalid";
                    String dpOK = "Invalid";

                   final Matcher ffMatch = getMatcherAgainstContent("<a href=\"([^\"]+)\" target=\"_blank\" >FileFactory");
                    if (ffMatch.find()) {
                        final GetMethod gFF = getGetMethod(ffMatch.group(1));
                            if (makeRedirectedRequest(gFF)) {
                                String ffContent = getContentAsString();
                                ffOK = checkFileFactory(ffContent);
                            }
                    }

                    final Matcher dpMatch = getMatcherAgainstContent("<a href=\"([^\"]+)\" target=\"_blank\" >DepositFiles");
                    if (dpMatch.find()) {
                        final GetMethod gDP = getGetMethod(dpMatch.group(1));
                            if (makeRedirectedRequest(gDP)) {
                                String dpContent = getContentAsString();
                                dpOK = checkDepositFiles(dpContent);
                            }
                    }

                    if (ffOK.equals("ok")) {
                            this.httpFile.setNewURL(new URL(ffMatch.group(1)));
                            this.httpFile.setPluginID("filefactory.com");

                    }  else

                    if (dpOK.equals("ok")) {
                            this.httpFile.setNewURL(new URL(dpMatch.group(1)));
                            this.httpFile.setPluginID("depositfiles.com");
                    }
            
                    this.httpFile.setState(DownloadState.QUEUED);

            //Find Deposit Files

            //Find Uploaded.to

            //Find Rapidshare

            //Find Easy-share   >> Captcha

            //Find Megaupload    >> Captcha
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("was not found")) {
            throw new URLNotAvailableAnymoreException("The page you requested was not found in our database.");
        }
    }

        private String checkDepositFiles(String content) throws Exception {
        if (!content.contains("depositfiles")) {
            logger.warning(getContentAsString());
            //throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
            return "not available";

        }

        if (content.contains("file does not exist")) {
            return "not available";
            //throw new URLNotAvailableAnymoreException(String.format("<b>Such file does not exist or it has been removed for infringement of copyrights.</b><br>"));
        }
        Matcher matcher = getMatcherAgainstContent("<b>([0-9.]+&nbsp;.B)</b>");
        if (matcher.find()) {
            logger.info("File size " + matcher.group(1));
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1).replaceAll("&nbsp;", "")));
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }
        matcher = getMatcherAgainstContent("class\\=\"info[^=]*\\=\"([^\"]*)\"");
        if (matcher.find()) {
            final String fn = matcher.group(1);
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
        } else return "not available";
            return "ok";
   }
    
        private String checkUploadedto(String content) throws Exception {

        if (!content.contains("uploaded.to")) {
            return "not available";
        }
        if (content.contains("File doesn")) {
            return "not available";
        }

        Matcher matcher = PlugUtils.matcher("([0-9.]+ .B)", content);
        if (matcher.find()) {
            final String fileSize = matcher.group(1);
            logger.info("File size " + fileSize);
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSize));

        } else return "not available";
//        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        return "ok";

    }
        private String checkFileFactory(String contentAsString) throws URLNotAvailableAnymoreException, YouHaveToWaitException, InvalidURLOrServiceProblemException {
        Matcher matcher = PlugUtils.matcher("Size: ([0-9-\\.]* .B)", contentAsString);
        if (!matcher.find()) {
            if (contentAsString.contains("file has been deleted") || contentAsString.contains("file is no longer available")) {
                return "not available";
            } else {
                if (contentAsString.contains("no free download slots")) {
                    throw new YouHaveToWaitException("Sorry, there are currently no free download slots available on this server.", 120);
                }
                return "not available";
            }
        }
        //String s = matcher.group(1);
        //httpFile.setFileSize(PlugUtils.getFileSizeFromString(s));
        //httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        return "ok";
    }
}
