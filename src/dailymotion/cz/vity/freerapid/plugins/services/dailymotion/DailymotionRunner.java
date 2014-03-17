package cz.vity.freerapid.plugins.services.dailymotion;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.HttpUtils;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class which contains main code
 * @author Ramsestom,JPEXS
 */
class DailymotionRunner extends AbstractRunner 
{
    private final static Logger logger = Logger.getLogger(DailymotionRunner.class.getName());
    private static final String SERVICE_WEB = "http://www.dailymotion.com";
    static Pattern PAT_DM = Pattern.compile(".*addVariable\\(\"video\", \"(.*)\".*");
    private String videoName="";

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkName(getContentAsString());//ok let's extract file name and size from the page
        } else
            throw new PluginImplementationException();
    }

    private void checkName(String content) throws ErrorDuringDownloadingException 
    {
    	 final Matcher matcher = getMatcherAgainstContent("<h1[^>]*>(.+?)</h1>");

         if (matcher.find()) {
             videoName=matcher.group(1).trim();
             setFileExtension(".flv");
         } else {
             logger.warning("File name was not found");
             throw new PluginImplementationException();
         }

         httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void setFileExtension(String ext){
        httpFile.setFileName(HttpUtils.replaceInvalidCharsForFileSystem(PlugUtils.unescapeHtml(videoName+ext), "_"));
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkName(contentAsString);//extract file name and size from the page
            
            
            if (contentAsString.indexOf("addVariable(\"video") != -1) 
            {
            	String tpcontent = contentAsString.substring(contentAsString.indexOf("addVariable(\"video"));
    			Matcher m = PAT_DM.matcher(tpcontent);
    			if (m.find()) 
    			{
    				String str = urlDecode(m.group(1));
    				String[] t = split(str, "||");
    				String video = "";
    				int quality = 6;
    				for (int i = 0; i < t.length; i++) {
    					int tq = getDaylimotionQuality(t[i]);
    					if (tq < quality) {
    						video = t[i];
    						quality = tq;
    					}
    				}
                                setFileExtension(getExtensionByQuality(quality));
    				video = video.substring(0, video.indexOf("@@"));
    					
    				final String finalURL = video;
    				
    				GetMethod getMethod = getGetMethod(finalURL);
    				
    	            //here is the download link extraction
    	            if (!tryDownloadAndSaveFile(getMethod)) 
    	            {
    	                checkProblems();//if downloading failed
    	                logger.warning(getContentAsString());//log the info
    	                throw new PluginImplementationException();//some unknown problem
    	            }
    			}
    			else {
                    throw new PluginImplementationException("Download parameters were not found");
                }
    		}
            else {
                throw new PluginImplementationException();
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("The page you tried to reach was not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }


    private static String getExtensionByQuality(int quality){
        if(quality==2){
            return ".mp4";
        }else{
            return ".flv";
        }
    }

    private static int getDaylimotionQuality(String url) 
    {
		if (url.endsWith("vp6-hd")) {
			return 0;
		} else if (url.endsWith("vp6-hq")) {
			return 1;
		} else if (url.endsWith("h264")) {
			return 2;
		} else if (url.endsWith("vp6")) {
			return 3;
		} else if (url.endsWith("spark")) {
			return 4;
		} else if (url.endsWith("spak-mini")) {
			return 5;
		} else {
			return 6;
		}
	}
    
    
    private static String[] split(String line, String sep) 
    {
		if (line == null || line.equals(""))
			return null;

		int len = line.length();
		int count = freq(line, sep) + 1;

		String[] tab = new String[count];
		int index = 0;
		for (int i = 0; i < count; i++) {
			int endindex = line.indexOf(sep, index);
			endindex = endindex < 0 ? len : endindex;
			tab[i] = line.substring(index, endindex);
			index = endindex + sep.length();
		}
		return tab;
	}
    
    public static int freq(String line, String sep) 
    {
		int len = line.length();
		int count = 0;
		for (int i = 0; i < len; i++) {
			if (line.startsWith(sep, i) == true) {
				count++;
			}
		}
		return count;
	}
    
    public static String urlDecode(String url) {
		try {
			return url == null ? "" : URLDecoder.decode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UTF-8 shoud be supported", e);
		}
	}
}
