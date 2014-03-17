package cz.vity.freerapid.plugins.services.xfilesharingcommon;

/**
 * @author tong2shot
 */
public interface CustomRun {
    //checkPrerequisites() and set language cookie are already handled by run()
    public void customRun() throws Exception;
}
