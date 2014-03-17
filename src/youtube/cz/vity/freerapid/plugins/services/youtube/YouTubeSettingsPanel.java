package cz.vity.freerapid.plugins.services.youtube;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Kajda, JPEXS
 */
public class YouTubeSettingsPanel extends JPanel implements ActionListener {
    private YouTubeSettingsConfig config;

    public YouTubeSettingsPanel(YouTubeServiceImpl service) throws Exception {
        super(new BorderLayout());
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final String[] qualityStrings = {"0 (lowest)","1","2","3","maximum available"};
        final JLabel qualityLabel = new JLabel("Preffered quality level:");
        final JComboBox qualityList = new JComboBox(qualityStrings);
        qualityList.setSelectedIndex(config.getQualitySetting());
        qualityList.addActionListener(this);
        add(qualityLabel, BorderLayout.PAGE_START);
        add(qualityList, BorderLayout.PAGE_END);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

    public void actionPerformed(ActionEvent e) {
        final JComboBox cb = (JComboBox) e.getSource();
        config.setQualitySetting(cb.getSelectedIndex());
    }
}