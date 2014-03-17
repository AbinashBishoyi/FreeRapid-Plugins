package cz.vity.freerapid.plugins.services.uploadjockey;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

//import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;
//import javax.swing.JOptionPane;

/**
 * @author Alex
 */
class UploadJockeyRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(UploadJockeyRunner.class.getName());
    public String dpOK ="Invalid";
    public String upOK = "Invalid";
    public String rsOK = "Invalid";
    public String esOK = "Invalid";
    public String ffOK = "Invalid";
    public String muOK = "Invalid";

    public String dpURL ="Invalid";
    public String upURL = "Invalid";
    public String rsURL = "Invalid";
    public String esURL = "Invalid";
    public String ffURL = "Invalid";
    public String muURL = "Invalid";


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
//Deposit files
//Uploaded.to
//Rapidshare

//Easy-Share - Captcha
//File Factory - Capthca
//MegaUpload - Captcha

        final GetMethod mJockey = getGetMethod(fileURL);
        if (makeRequest(mJockey)) { //Load Jockey Page
            //Find File Factory // First for testing file removed error later make this last because there is >> Captcha

                    final String mContent = getContentAsString();


                    dpURL = checkServer(mContent,"DepositFiles");
                    upURL = checkServer(mContent,"Uploaded");
                    rsURL = checkServer(mContent,"Rapidshare");
                    esURL = checkServer(mContent,"Easy");
                    ffURL = checkServer(mContent,"FileFactory");
                    muURL = checkServer(mContent,"Megaupload");

                    

                    System.out.println(dpOK);
                    

                    if (dpOK.equals("ok")) {
                            this.httpFile.setNewURL(new URL(dpURL));
                    }  else

                    if (upOK.equals("ok")) {
                            this.httpFile.setNewURL(new URL(upURL));
                    } else

                    if (rsOK.equals("ok")) {
                            this.httpFile.setNewURL(new URL(rsURL));
                    }  else

                    if (esOK.equals("ok")) {
                            this.httpFile.setNewURL(new URL(esURL));
                    } else
                    if (ffOK.equals("ok")) {
                            this.httpFile.setNewURL(new URL(ffURL));
                    }  else

                    if (ffOK.equals("ok")) {
                            this.httpFile.setNewURL(new URL(muURL));
                    } else throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");



                    this.httpFile.setPluginID("");
                    this.httpFile.setState(DownloadState.QUEUED);
                    System.out.println(dpURL);
                    
                    

                    

                    

            //Find Deposit Files

            //Find Uploaded.to

            //Find Rapidshare

            //Find Easy-share   >> Captcha

            //Find Megaupload    >> Captcha
        }
    }

    //private void checkProblems() throws ErrorDuringDownloadingException {
    //    final String contentAsString = getContentAsString();
    //    if (contentAsString.contains("was not found")) {
    //        throw new URLNotAvailableAnymoreException("The page you requested was not found in our database.");
    //    }
    //}
        private String checkServer(String tContent, String tService) throws Exception {


             final Matcher dpMatch = PlugUtils.matcher("<a href=\"([^\"]+)\" target=\"_blank\" >" + tService, tContent);
                    if (dpMatch.find()) {
                        //System.out.println("Processing DP");
                        final GetMethod gDP = getGetMethod(dpMatch.group(1));
                            if (makeRedirectedRequest(gDP)) {
                                final Matcher dpMatch2 = getMatcherAgainstContent("<iframe src=\"([^\"]+)");
                                    if(dpMatch2.find()) {
                                        String cURL = dpMatch2.group(1);
                                        final GetMethod gDP2 = getGetMethod(cURL);
                                            if (makeRedirectedRequest(gDP2)) {
                                                String dpContent = getContentAsString();

                                                if (tService.equals("DepositFiles")) {
                                                    dpOK = checkDepositFiles(dpContent);
                                                } else
                                                if (tService.equals("Uploaded")) {
                                                    upOK = checkUploaded(dpContent);
                                                }
                                                if (tService.equals("Rapidshare")) {
                                                    rsOK = checkRapidshare(dpContent);
                                                }
                                                if (tService.equals("Easy")) {
                                                    esOK = checkEasy(dpContent);
                                                }

                                                if (tService.equals("FileFactory")) {
                                                    ffOK = checkFileFactory(dpContent);
                                                }
                                                if (tService.equals("Megaupload")) {
                                                    muOK = checkMegaupload(dpContent);
                                                }

                                                //dpURL = checkServer(mContent,"DepositFiles");
                                                //upURL = checkServer(mContent,"Uploaded");
                                                //rsURL = checkServer(mContent,"Rapidshare");
                                                //esURL = checkServer(mContent,"Easy");
                                                //ffURL = checkServer(mContent,"FileFactory");
                                                //muURL = checkServer(mContent,"Megaupload");

                                                return cURL;
                                            }
                                    }
                            }
                    }
            return "Invalid";
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

        matcher = getMatcherAgainstContent("class\\=\"info[^=]*\\=\"([^\"]*)\"");
        if (matcher.find()) {
  //          final String fn = matcher.group(1);
 //           logger.info("File name " + fn);
 //           httpFile.setFileName(fn);
            return "ok";
        } else return "not available";
            
   }

        private String checkUploaded(String content) throws Exception {

        if (!content.contains("uploaded.to")) {
            return "not available";
        }
        if (content.contains("File doesn")) {
            return "not available";
        }

       Matcher matcher = PlugUtils.matcher("([0-9.]+ .B)", content);
        if (matcher.find()) {
 //           final String fileSize = matcher.group(1);
//            logger.info("File size " + fileSize);
//            httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSize));
            return "ok";
        } else return "not available";
        //httpFile.setFileState(FileState.CHECKED_AND_EXISTING);


    }
        
        private String checkMegaupload(String content) throws Exception {

        if (content.contains("link you have clicked is not available")) {
            return "not available";

        }

        Matcher matcher = PlugUtils.matcher("Filename:(</font>)?</b> ([^<]*)", content);
        if (matcher.find()) {
            //final String fn = PlugUtils.unescapeHtml(matcher.group(2));
            //logger.info("File name " + fn);
            //httpFile.setFileName(fn);
            //httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
            return "ok";
        } else return "not available";

    }


        private String checkFileFactory(String content) throws URLNotAvailableAnymoreException, YouHaveToWaitException, InvalidURLOrServiceProblemException {
        Matcher matcher = PlugUtils.matcher("Size: ([0-9-\\.]* .B)", content);
        if (!matcher.find()) {
            if (content.contains("file has been deleted") || content.contains("file is no longer available")) {
                return "not available";
            } else {
                if (content.contains("no free download slots")) {
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

        private String checkEasy(String content) throws Exception {

        if (!content.contains("easy-share")) {
            return "not available";
        }

        if (content.contains("File not found")) {
            return "not available";

        }
        Matcher matcher = PlugUtils.matcher("Download ([^,]+), upload", content);
        if (matcher.find()) {
            //final String fn = new String(matcher.group(1).getBytes("windows-1252"), "UTF-8");
            //logger.info("File name " + fn);
            //httpFile.setFileName(fn);
            return "ok";
        } else logger.warning("File name was not found" + content);
        return "not available";
    }

        private String checkRapidshare(String content) throws URLNotAvailableAnymoreException {
        Matcher matcher;
        if (!content.contains("form id=\"ff\" action=")) {

            matcher = PlugUtils.matcher("class=\"klappbox\">((\\s|.)*?)</div>",content);

            if (matcher.find()) {
                final String error = matcher.group(1);
                if (error.contains("illegal content") || error.contains("file has been removed") || error.contains("has removed") || error.contains("file is neither allocated to") || error.contains("limit is reached"))
                    return "not available";
                if (error.contains("file could not be found"))
                    return "not available";


            if (content.contains("has removed file") || content.contains("file could not be found") || content.contains("illegal content") || content.contains("file has been removed") || content.contains("limit is reached"))
                return "not available";

        }
        //| 5277 KB</font>
        matcher = getMatcherAgainstContent("\\| (.*? .B)</font>");
        if (matcher.find()) {
            //Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
            return "ok";
        }

    }
       return "not available";
}
}

