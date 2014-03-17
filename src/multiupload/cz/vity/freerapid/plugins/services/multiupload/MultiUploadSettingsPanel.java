package cz.vity.freerapid.plugins.services.multiupload;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author ntoskrnl
 */
public class MultiUploadSettingsPanel extends JPanel {
    private MultiUploadSettingsConfig config;

    private final JCheckBox checkDownloadService = new JCheckBox("Check for file errors on download server");
    private final JCheckBox checkQueueAllLinks = new JCheckBox("Queue all links");

    public MultiUploadSettingsPanel(MultiUploadServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        checkDownloadService.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkQueueAllLinks.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkDownloadService.setSelected(config.getCheckDownloadService());
        checkQueueAllLinks.setSelected(config.isQueueAllLinks());

        checkDownloadService.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setCheckDownloadService(checkDownloadService.isSelected());
            }
        });
        checkQueueAllLinks.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setQueueAllLinks(checkQueueAllLinks.isSelected());
            }
        });
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(checkDownloadService);
        add(checkQueueAllLinks);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}