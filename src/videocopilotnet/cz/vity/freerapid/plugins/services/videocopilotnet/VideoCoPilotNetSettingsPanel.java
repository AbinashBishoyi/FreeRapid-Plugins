package cz.vity.freerapid.plugins.services.videocopilotnet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author tong2shot
 */
class VideoCoPilotNetSettingsPanel extends JPanel {
    private VideoCoPilotNetSettingsConfig config;

    public VideoCoPilotNetSettingsPanel(VideoCoPilotNetServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final JLabel qualityLabel = new JLabel("Preferred video quality:");
        final JComboBox qualityList = new JComboBox(VideoQuality.getItems());
        final JCheckBox projectCheck = new JCheckBox("Download project");

        qualityLabel.setLabelFor(qualityList);
        qualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setAlignmentX(Component.LEFT_ALIGNMENT);

        qualityList.setSelectedItem(config.getVideoQuality());
        projectCheck.setSelected(config.isDownloadProject());

        qualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setVideoQuality((VideoQuality) qualityList.getSelectedItem());
            }
        });
        projectCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setDownloadProject(projectCheck.isSelected());
            }
        });


        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(qualityLabel);
        add(qualityList);
        add(projectCheck);
        add(Box.createRigidArea(new Dimension(0, 15)));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }
}
