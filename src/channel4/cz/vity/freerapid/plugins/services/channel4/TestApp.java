package cz.vity.freerapid.plugins.services.channel4;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author ntoskrnl
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //log everything
            //InputStream is = new BufferedInputStream(new FileInputStream("E:\\Stuff\\logtest.properties"));
            //LogManager.getLogManager().readConfiguration(is);
            //we set file URL
            httpFile.setNewURL(new URL("http://www.channel4.com/programmes/a-short-history-of-everything-else/4od#3366695"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 9050, Proxy.Type.SOCKS); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final Channel4ServiceImpl service = new Channel4ServiceImpl(); //instance of service - of our plugin
            //runcheck makes the validation
            testRun(service, httpFile, connectionSettings);//download file with service and its Runner
            //all output goes to the console
        } catch (Exception e) {//catch possible exception
            e.printStackTrace(); //writes error output - stack trace to console
        }
        this.exit();//exit application
    }

    /**
     * Main start method for running this application
     * Called from IDE
     *
     * @param args arguments for application
     */
    public static void main(String[] args) {
        Application.launch(TestApp.class, args);//starts the application - calls startup() internally

        /*
        <uriData>
            <streamUri>rtmpe://ll.securestream.channel4.com/a4174/e1/mp4:xcuassets/CH4_08_02_16_48970006001004_002.mp4</streamUri>
            <token>Iv5ISb56UZWzsVoctIf/spWnH0ba6VvBKpdoDXcjAFhKEBHXR0+MZQ==</token>
            <cdn>ll</cdn>
            <ip>[snip]</ip>
            <e>1285248951</e>
        </uriData>

        <uriData>
            <streamUri>rtmpe://ak.securestream.channel4.com/4oD/mp4:assets/CH4_08_02_16_48970006001004_002.mp4</streamUri>
            <token>w+ZiRLbFa1LB36Ea2GoL+8irPzh8MnX901jjnU7I5o10qGZ6d09ohRoV08C185BPY3SaVsg/XhXQvCaeN7dDXJ0xGtu0kkW4xWqMoLEwTYOhN4d4C09atzBdNjUXn2Wpr0Lp/8ZmXiAsgII6ZzKCNJVfCg+GTmCPwhNkADC4d26N3m/Zh0z61g==</token>
            <fingerprint>v002</fingerprint>
            <slist>assets/CH4_08_02_16_48970006001004_002.mp4</slist>
            <cdn>ak</cdn>
        </uriData>
         */

        /*
        //some notes... they probably don't make much sense, as
        //the encrypted names and numbers change every time a new SWF is released.
        //in fact a new version was released while I was testing.

        //initialize _-3v from XML
        //set some local variables:
        //slot 5 (enced) = _-3v.k
        System.out.println(decrypt(939, 185));//slot 6 (nm) = blowfish-ecb
        System.out.println(decrypt(940, 186));//slot 7 (c2)
        System.out.println(decrypt(938, 188));//slot 8 (c2_)
        System.out.println(decrypt(937, 187));//slot 9 (c1)
        System.out.println(decrypt(938, 188));//slot 10 (c3)
        System.out.println(decrypt(948, 178));//slot 11 (c4)
        //slot 12 (enced) = 3v.k
        //slot 13 (cd) = ByteArray(slot 7)
        //slot 14 (ef) = decodeBASE64(slot 12)
        //slot 15 (p) = new PKCS5
        //slot 16 (mode) = _-DE/_-AR._-Dy(slot 6, slot 13, slot 15)
        //slot15.setBlockSize(slot16.getBlockSize())
        //decrypt(slot 14)
        //slot 5 (enced) = slot14.toString
        //if ak {
        //System.out.println(decrypt(511, 107));//auth=  + slot 5
        //System.out.println(decrypt(512, 108));//&aifp=  + _-3v.l
        //System.out.println(decrypt(504, 100));//&slist=  + _-3v.m
        //} else {
        //e= + _-3v.e + &ip= + _-3v.ip + &h= + slot 5
        //}
        */

        /*try {
            final byte[] cd = "STINGMIMI".getBytes("UTF-8");
            final byte[] ef = Base64.decodeBase64("E+XJmVYIgUdLvfZ6FWtrD4wNLvA4aozhQCebP2VMIIOJky0MXIr7hdXJZ/to8ayy3OKBkBLx3PhqqkO7uoRFx8FFOIinKx7G9yf5rW1p1uNpBPfd2YNFzNCK1JsgUXrw5uKoAgiVYpsV+VJBx6a2Tjo8HDl9pQcemEGJdVbQCljQhhaXRxLEeQ==".getBytes("UTF-8"));
            //final byte[] ef = Base64.decodeBase64("BYUDqo4uEJKX5XesbpnqUIdnaUFUCxwL6Qzm8+S9gNtKEBHXR0+MZQ==".getBytes("UTF-8"));
            final byte[] decrypted = new CryptographySupport().setEngine(Engine.Blowfish).setKey(cd).decrypt(ef);
            System.out.println("da_cCasaGbTaiaWaData1bQbka_bhaKaMcU-bmM5k4-eS-lxU-mfptsqsDtwtRnDmJktnBrSqqsZqKsAr6lfpcr6sStbtwnul9kInrrSqvs8qbscszmWp5sJstt9tfn7mf");
            //System.out.println("15df87e843fdd470506be65ce9f50277");
            System.out.println(new String(decrypted, "UTF-8").trim().replace('+', '-').replace('/', '_').replace("=", ""));
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        /*try {
            final PrintStream out = new PrintStream("C:\\Users\\Administrator\\Desktop\\out.txt");
            for (int i = -1000; i <= 1000; i++) {
                for (int j = -1000; j <= 1000; j++) {
                    try {
                        out.println(i + ", " + j + ": " + decrypt(i, j));
                    } catch (Exception e) {
                        //ignore
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    private final static int[][] _7C = {{-1037072301, -379534167, -284865758, -950407827, 1956817250, -283733296}, {-2055593871, 1301056896, 1447144283, 1228666526, 1661078744, 1486293077, 169702676, 957432064}, {385759517, 1801127595, -1499937227, -709151612}, {-941059683, -1507855739, 644456575, 1693947911, 1833482332, 445249941, 699925890, 2013216460, 610460717, 1453263257, -671058728, 1134964482}, {-598124475, 1946827291, 991629386, -121950213, -1781359971, -1342124576, 1471257429, -1312693483, 1535215004, 394557203}, {-2143741121, -46780472, -508817188, -108652108, 1272679770, -842853672, -1746075306, 1171087868, -1217692208, -1992235463, 1502745834, -1112254346}, {-194475579, -327143782, 1940392216, -1507593704, -165134024, -2015996029, -1158216526, 935129358, -348192713, 752972768}, {-1471091127, -1854492628, -1330881458, -807435319, 950018374, 608345957, 474573753, 96322592, -210599493, -1455417368}, {136201403, 328000996, -1756103462, -1044507558, 1822511611, -1676580064}, {1970299441, 186723111, 45292, -1913924795, 2030969730, -77155990}, {714870289, 1298968197, -1680573027, 860888636, 1037137674, -1936048464, 1669386378, 85111296, 31386672, -256199683}, {-449105247, -752849441, 236822295, -97149308, -1604814195, 135819619, 404345929, -84903459}, {-1146965033, 1896874913, 1861981646, 2119669732, 1710984089, 684184050, -788321179, 101251317, 275279367, -1670740511}};
    private final static int[][] _6J = {{1908906649, -1346194107, -738565873, -570474545}, {-504853063, 970651231, 79896729, -638571765}, {879119379, -298930171, 1784989640, -1925288492}, {-78859410, 1892170286, 1947483996, 574125247}, {441537286, 716722342, 836718448, 1914634349}, {1142980503, 1464910047, -52973200, -400305691}, {808258827, -909542026, -602678994, 991619889}, {-2123897216, 1242986106, -1796938995, -36531343}, {1463147428, -2081641398, 718987543, -1613469519}, {675059087, -686527706, 1724690286, 1355277411}, {-2088176865, 1901944054, 1364107828, 62082534}, {1419240080, -1868013257, -1618444294, 398076527}, {-25291181, 138639270, -1702127924, -103028795}};

    private static String decrypt(int param1, int param2) {
        StringBuilder sb = new StringBuilder();
        int _loc4_;
        int _loc5_;
        int _loc6_;
        int _loc7_;
        long _loc8_;
        long _loc9_;
        _loc4_ = 0;
        _loc5_ = _7C[param1 - 5 ^ 931].length;
        while (_loc4_ < _loc5_) {
            _loc6_ = _7C[param1 - 5 ^ 931][_loc4_];
            _loc4_ = _loc4_ + 1;
            _loc7_ = _7C[param1 - 5 ^ 931][_loc4_];
            _loc8_ = 2654435769L;
            _loc9_ = 84941944608L;
            while (_loc9_ != 0) {
                _loc7_ = (int) (_loc7_ - ((_loc6_ << 4 ^ _loc6_ >>> 5) + _loc6_ ^ _loc9_ + _6J[param2 + 3 ^ 185][(int) (_loc9_ >>> 11 & 3)]));
                _loc9_ = _loc9_ - _loc8_;
                _loc6_ = (int) (_loc6_ - ((_loc7_ << 4 ^ _loc7_ >>> 5) + _loc7_ ^ _loc9_ + _6J[param2 + 3 ^ 185][(int) (_loc9_ & 3)]));
            }
            sb = sb.append((char) _loc6_).append((char) _loc7_);
            _loc4_ = _loc4_ + 1;
        }
        String s = sb.toString();
        if (s.charAt(s.length() - 1) == 0) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

}