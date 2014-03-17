package cz.vity.freerapid.plugins.services.navratdoreality;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author iki
 */
class NavratDoRealityFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(NavratDoRealityFileRunner.class.getName());

    private String AGE_CHECK = "Je mi více než 18";
    private String BASE_URL = "http://navratdoreality.cz/";
    private String filePath = ""; // external/kosik/video/auto.flv
    private List<URI> uriList = new ArrayList<URI>();
    private List<String> regexList = new ArrayList<String>();
    private String REGEXP_PAGE = "http://(www\\.)?navratdoreality\\.cz/(index\\.php)?\\?p=view.id=[0-9]+";
    private String REGEXP_RAW_FILE = "http://(www\\.)?navratdoreality\\.cz/content/.+";
    private List<NavratDoRealityFilePattern> regexPatterns = new ArrayList<NavratDoRealityFilePattern>();

    public NavratDoRealityFileRunner(){
        this.regexList.add(0, this.REGEXP_RAW_FILE);
        this.regexList.add(1, this.REGEXP_PAGE);

        //regular video
        this.regexPatterns.add(new NavratDoRealityFilePattern(  "so\\.addVariable\\(\"file\",\"",
                                                                ".*?",
                                                                "\"\\);"));
        //zip file
        this.regexPatterns.add(new NavratDoRealityFilePattern(  "class=\"download\"><a href=\"",
                                                                ".*?",
                                                                "\" title"));
        //youtube link
        this.regexPatterns.add(new NavratDoRealityFilePattern(  "<iframe width=\"705\" height=\"528\" src=\"",
                                                                ".*?",
                                                                "\" frameborder=\""));

        //standalone pictures without zip file
        this.regexPatterns.add(new NavratDoRealityFilePattern(  "<img src=\"",
                                                                "content/.*?",
                                                                "\">"));

    }
    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();

        int type = this.getFileType(super.fileURL);
        switch (type){
            case 0:
                this.filePath = super.fileURL;
                this.httpFile.setFileName(this.getFileName(super.fileURL));

                break;
            case 1:
                final GetMethod method = getGetMethod(super.fileURL); //create GET request
                if (makeRedirectedRequest(method)) { //we make the main request
                    String contentAsString = super.getContentAsString();//check for response
                    this.ageCheck(contentAsString);
                    contentAsString = this.getContentAsString();
                    this.fillUriListAndFileName(contentAsString);
                    this.httpFile.setFileName(this.getFileName(this.filePath));
                }
                break;
            default:
                throw new ServiceConnectionProblemException("Cant match the URI: " + super.fileURL);
        }
        this.httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + super.fileURL);

        int type = this.getFileType(super.fileURL);
        switch (type){
            case 0:
                this.filePath = super.fileURL;
                this.httpFile.setFileName(this.getFileName(super.fileURL));
                break;
            case 1:
                final GetMethod method = getGetMethod(super.fileURL); //create GET request
                if (makeRedirectedRequest(method)) { //we make the main request
                    String contentAsString = super.getContentAsString();//check for response
                    this.ageCheck(contentAsString);
                    contentAsString = this.getContentAsString();

                    this.fillUriListAndFileName(contentAsString);
                    this.addUrisToQueue(this.uriList);
                }

                break;
            default:
                throw new ServiceConnectionProblemException("Cant match the URI.");
        }

        if(this.getFileType(this.filePath) < 0){
            throw new URLNotAvailableAnymoreException("No file there - maybe link with external video");
        };

        this.httpFile.setFileName(this.getFileName(this.filePath));
        final HttpMethod httpMethod = getMethodBuilder().setReferer(super.fileURL).setAction(PlugUtils.replaceEntities(this.filePath)).toGetMethod();
        if (!super.tryDownloadAndSaveFile(httpMethod)) {
            checkProblems();//if downloading failed
            throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
        }
    }

    private void fillUriListAndFileName(String contentAsString) throws Exception{
        //TODO matcher se musí postarat o více souborů
        //TODO a přidat do jména souboru i první, pokud existuje

        for(NavratDoRealityFilePattern regexp : this.regexPatterns){
            Matcher matcher = PlugUtils.matcher(regexp.getFullRegexp(), contentAsString);

            while(matcher.find()){
                try{
                    String center = matcher.group();
                    center = center.replaceFirst(regexp.getBefore(), "").replaceFirst(regexp.getAfter(), "");
                    //center = URLEncoder.encode(center, "UTF-8");
                    center = center.replace(" ", "%20");

                    if(center.startsWith("http://")){ //if its link like http://youtube.com/watch?v=JDLFKJSDF
                        center = this.correctExternalUri(center);
                        this.uriList.add(new URI(center));
                    }else{
                        this.uriList.add(new URI(this.BASE_URL + center));
                    }

                }catch(Exception e){
                    logger.warning(e.getMessage());
                    logger.warning("Cant create URI from content");
                }
            }
        }

        if(this.uriList.size() > 0){
            this.filePath = this.uriList.get(0).toString();

            if(this.getFileType(this.filePath) >= 0){ //because of youtube video only (for example)
                this.uriList.remove(0); //can handle one file
            };
        }else{
            throw new PluginImplementationException("No files found on the page.");
        }
    }

    /**
     * some external uri changes
     * @param center
     * @return
     */
    private String correctExternalUri(String center) {
        if(center.matches("http://(www\\.)?youtube.+/embed/.+")){
            return center.replaceFirst("embed/", "watch?v="); //youtube.com/embed/LJKSDFLJ => youtube.com/watch?v=LSDJFJLJSD
        }

        if(center.matches("http://(www\\.)?xhamster\\.com/xembed\\.php.+")){
            return center.replaceFirst("xembed\\.php\\?video=", "movies/") + "/cokoli.html"; //http://xhamster.com/xembed.php?video=315441 => http://xhamster.com/movies/315441/cokoli.html
        }

        return center;
    }

    /**
     * other file in the page
     * @param uriList
     */
    private void addUrisToQueue(List<URI> uriList) {
        if(uriList.size() > 0){
            this.getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(super.httpFile, uriList);
        }
    }

    /**
     * @param link link to the page or raw file
     * @return int regexp number from list
     */
    private int getFileType(String link){
        for(int i = 0; i < this.regexList.size(); i++){
            if(PlugUtils.matcher(this.regexList.get(i), link).matches()){
                return i;
            }
        }

        return -1;
    }

    /**
     * @param link
     * @return String filename - from http://co.com/path/path/filename.ext returns filename.ext
     */
    private String getFileName(String link){
        return link.replaceFirst(".*/", "");
    }

    /**
     * entering form
     * @param content
     * @throws Exception
     */
    private void ageCheck(String content) throws Exception {
        if (content.contains(this.AGE_CHECK)) {
            String confirmUrl = super.fileURL;//super.fileURL + "?do=askAgeForm-submit";
            PostMethod confirmMethod = (PostMethod) getMethodBuilder()
                    .setAction(confirmUrl)
                    .setEncodePathAndQuery(true)
                    .setAndEncodeParameter("ACTION", "check_adult")
                    .setAndEncodeParameter("check", "18plus")
                    .toPostMethod();
            makeRedirectedRequest(confirmMethod);

            if (getContentAsString().contains(this.AGE_CHECK)) {
                throw new PluginImplementationException("Cannot confirm age");
            }
        }
    }

    /**
     * not so much working
     * @throws ErrorDuringDownloadingException
     */
    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")) {//TODO
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }

}