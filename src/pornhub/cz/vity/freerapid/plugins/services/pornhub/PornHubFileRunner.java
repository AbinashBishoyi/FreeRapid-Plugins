package cz.vity.freerapid.plugins.services.pornhub;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class PornHubFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(PornHubFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws Exception {
        PlugUtils.checkName(httpFile, content, "<title>", " - Pornhub.com</title>");
        httpFile.setFileName(httpFile.getFileName() + ".mp4");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private final String[] qualityList = {"quality_720p", "quality_480p", "quality_240p", "quality_180p", "video_url"};

    private String findBestQuality(final String content) throws Exception {
        for (String quality : qualityList) {
            final Matcher match = PlugUtils.matcher("\"" + quality + "\":\"(.+?)\",", content);
            if (match.find())
                return match.group(1);
        }
        throw new PluginImplementationException("Video quality not found");
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String content = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(content);//extract file name and size from the page

            final String encURL = URLDecoder.decode(findBestQuality(content), "UTF-8");
            final String name = PlugUtils.getStringBetween(content, "\"video_title\":\"", "\"").replace('+', ' ');
            final HttpMethod httpMethod = getGetMethod(decodeAesUrl(encURL, name));
            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("Page not found")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let to know user in FRD
        }
    }


    // borrowed from http://yourjavascript.com/5818134233/aes.js
    private final static String aes_js = "var Aes={cipher:function(b,f){for(var a=f.length/4-1,d=[[],[],[],[]],c=0;16>c;c++)d[c%4][Math.floor(c/4)]=b[c];d=Aes.addRoundKey(d,f,0,4);for(c=1;c<a;c++)d=Aes.subBytes(d,4),d=Aes.shiftRows(d,4),d=Aes.mixColumns(d,4),d=Aes.addRoundKey(d,f,c,4);d=Aes.subBytes(d,4);d=Aes.shiftRows(d,4);d=Aes.addRoundKey(d,f,a,4);a=Array(16);for(c=0;16>c;c++)a[c]=d[c%4][Math.floor(c/4)];return a},keyExpansion:function(b){for(var f=b.length/4,a=f+6,d=Array(4*(a+1)),c=Array(4),e=0;e<f;e++)d[e]=[b[4*e],b[4*e+1],b[4*e+2],b[4*e+3]];for(e=f;e<4*(a+1);e++){d[e]=Array(4);for(b=0;4>b;b++)c[b]=d[e-1][b];if(0==e%f)for(c=Aes.subWord(Aes.rotWord(c)),b=0;4>b;b++)c[b]^=Aes.rCon[e/f][b];else 6<f&&4==e%f&&(c=Aes.subWord(c));for(b=0;4>b;b++)d[e][b]=d[e-f][b]^c[b]}return d},subBytes:function(b,f){for(var a=0;4>a;a++)for(var d=0;d<f;d++)b[a][d]=Aes.sBox[b[a][d]];return b},shiftRows:function(b,f){for(var a=Array(4),d=1;4>d;d++){for(var c=0;4>c;c++)a[c]=b[d][(c+d)%f];for(c=0;4>c;c++)b[d][c]=a[c]}return b},mixColumns:function(b,f){for(var a=0;4>a;a++){for(var d=Array(4),c=Array(4),e=0;4>e;e++)d[e]=b[e][a],c[e]=b[e][a]&128?b[e][a]<<1^283:b[e][a]<<1;b[0][a]=c[0]^d[1]^c[1]^d[2]^d[3];b[1][a]=d[0]^c[1]^d[2]^c[2]^d[3];b[2][a]=d[0]^d[1]^c[2]^d[3]^c[3];b[3][a]=d[0]^c[0]^d[1]^d[2]^c[3]}return b},addRoundKey:function(b,f,a,d){for(var c=0;4>c;c++)for(var e=0;e<d;e++)b[c][e]^=f[4*a+e][c];return b},subWord:function(b){for(var f=0;4>f;f++)b[f]=Aes.sBox[b[f]];return b},rotWord:function(b){for(var f=b[0],a=0;3>a;a++)b[a]=b[a+1];b[3]=f;return b},sBox:[99,124,119,123,242,107,111,197,48,1,103,43,254,215,171,118,202,130,201,125,250,89,71,240,173,212,162,175,156,164,114,192,183,253,147,38,54,63,247,204,52,165,229,241,113,216,49,21,4,199,35,195,24,150,5,154,7,18,128,226,235,39,178,117,9,131,44,26,27,110,90,160,82,59,214,179,41,227,47,132,83,209,0,237,32,252,177,91,106,203,190,57,74,76,88,207,208,239,170,251,67,77,51,133,69,249,2,127,80,60,159,168,81,163,64,143,146,157,56,245,188,182,218,33,16,255,243,210,205,12,19,236,95,151,68,23,196,167,126,61,100,93,25,115,96,129,79,220,34,42,144,136,70,238,184,20,222,94,11,219,224,50,58,10,73,6,36,92,194,211,172,98,145,149,228,121,231,200,55,109,141,213,78,169,108,86,244,234,101,122,174,8,186,120,37,46,28,166,180,198,232,221,116,31,75,189,139,138,112,62,181,102,72,3,246,14,97,53,87,185,134,193,29,158,225,248,152,17,105,217,142,148,155,30,135,233,206,85,40,223,140,161,137,13,191,230,66,104,65,153,45,15,176,84,187,22],rCon:[[0,0,0,0],[1,0,0,0],[2,0,0,0],[4,0,0,0],[8,0,0,0],[16,0,0,0],[32,0,0,0],[64,0,0,0],[128,0,0,0],[27,0,0,0],[54,0,0,0]],Ctr:{}};Aes.Ctr.encrypt=function(b,f,a){if(128!=a&&192!=a&&256!=a)return\"\";b=Utf8.encode(b);f=Utf8.encode(f);var d=a/8,c=Array(d);for(a=0;a<d;a++)c[a]=isNaN(f.charCodeAt(a))?0:f.charCodeAt(a);c=Aes.cipher(c,Aes.keyExpansion(c));c=c.concat(c.slice(0,d-16));f=Array(16);a=(new Date).getTime();var d=a%1E3,e=Math.floor(a/1E3),l=Math.floor(65535*Math.random());for(a=0;2>a;a++)f[a]=d>>>8*a&255;for(a=0;2>a;a++)f[a+2]=l>>>8*a&255;for(a=0;4>a;a++)f[a+4]=e>>>8*a&255;d=\"\";for(a=0;8>a;a++)d+=String.fromCharCode(f[a]);for(var c=Aes.keyExpansion(c),e=Math.ceil(b.length/16),l=Array(e),k=0;k<e;k++){for(a=0;4>a;a++)f[15-a]=k>>>8*a&255;for(a=0;4>a;a++)f[15-a-4]=k/4294967296>>>8*a;var g=Aes.cipher(f,c),m=k<e-1?16:(b.length-1)%16+1,h=Array(m);for(a=0;a<m;a++)h[a]=g[a]^b.charCodeAt(16*k+a),h[a]=String.fromCharCode(h[a]);l[k]=h.join(\"\")}b=d+l.join(\"\");return b=Base64.encode(b)};Aes.Ctr.decrypt=function(b,f,a){if(128!=a&&192!=a&&256!=a)return\"\";b=Base64.decode(b);f=Utf8.encode(f);var d=a/8,c=Array(d);for(a=0;a<d;a++)c[a]=isNaN(f.charCodeAt(a))?0:f.charCodeAt(a);c=Aes.cipher(c,Aes.keyExpansion(c));c=c.concat(c.slice(0,d-16));f=Array(8);ctrTxt=b.slice(0,8);for(a=0;8>a;a++)f[a]=ctrTxt.charCodeAt(a);d=Aes.keyExpansion(c);c=Math.ceil((b.length-8)/16);a=Array(c);for(var e=0;e<c;e++)a[e]=b.slice(8+16*e,16*e+24);b=a;for(var l=Array(b.length),e=0;e<c;e++){for(a=0;4>a;a++)f[15-a]=e>>>8*a&255;for(a=0;4>a;a++)f[15-a-4]=(e+1)/4294967296-1>>>8*a&255;var k=Aes.cipher(f,d),g=Array(b[e].length);for(a=0;a<b[e].length;a++)g[a]=k[a]^b[e].charCodeAt(a),g[a]=String.fromCharCode(g[a]);l[e]=g.join(\"\")}b=l.join(\"\");return b=Utf8.decode(b)};var Base64={code:\"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=\",encode:function(b,f){var a,d,c,e,l=[],k=\"\",g,m,h=Base64.code;m=(\"undefined\"==typeof f?0:f)?b.encodeUTF8():b;g=m.length%3;if(0<g)for(;3>g++;)k+=\"=\",m+=\"\\x00\";for(g=0;g<m.length;g+=3)a=m.charCodeAt(g),d=m.charCodeAt(g+1),c=m.charCodeAt(g+2),e=a<<16|d<<8|c,a=e>>18&63,d=e>>12&63,c=e>>6&63,e&=63,l[g/3]=h.charAt(a)+h.charAt(d)+h.charAt(c)+h.charAt(e);l=l.join(\"\");return l=l.slice(0,l.length-k.length)+k},decode:function(b,f){f=\"undefined\"==typeof f?!1:f;var a,d,c,e,l,k=[],g,m=Base64.code;g=f?b.decodeUTF8():b;for(var h=0;h<g.length;h+=4)a=m.indexOf(g.charAt(h)),d=m.indexOf(g.charAt(h+1)),e=m.indexOf(g.charAt(h+2)),l=m.indexOf(g.charAt(h+3)),c=a<<18|d<<12|e<<6|l,a=c>>>16&255,d=c>>>8&255,c&=255,k[h/4]=String.fromCharCode(a,d,c),64==l&&(k[h/4]=String.fromCharCode(a,d)),64==e&&(k[h/4]=String.fromCharCode(a));e=k.join(\"\");return f?e.decodeUTF8():e}},Utf8={encode:function(b){b=b.replace(/[\\u0080-\\u07ff]/g,function(b){b=b.charCodeAt(0);return String.fromCharCode(192|b>>6,128|b&63)});return b=b.replace(/[\\u0800-\\uffff]/g,function(b){b=b.charCodeAt(0);return String.fromCharCode(224|b>>12,128|b>>6&63,128|b&63)})},decode:function(b){b=b.replace(/[\\u00e0-\\u00ef][\\u0080-\\u00bf][\\u0080-\\u00bf]/g,function(b){b=(b.charCodeAt(0)&15)<<12|(b.charCodeAt(1)&63)<<6|b.charCodeAt(2)&63;return String.fromCharCode(b)});return b=b.replace(/[\\u00c0-\\u00df][\\u0080-\\u00bf]/g,function(b){b=(b.charCodeAt(0)&31)<<6|b.charCodeAt(1)&63;return String.fromCharCode(b)})}};";

    private String decodeAesUrl(final String encoded, final String name) throws Exception {
        try {
            final String function = "OUTPUT=Aes.Ctr.decrypt(\"" + encoded + "\", \"" + name + "\", 256);";
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            return engine.eval(aes_js + function).toString();
        } catch (Exception e) {
            throw new PluginImplementationException("JS evaluation error " + e.getLocalizedMessage());
        }
    }

}