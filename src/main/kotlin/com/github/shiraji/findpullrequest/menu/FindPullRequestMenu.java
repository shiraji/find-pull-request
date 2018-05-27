package com.github.shiraji.findpullrequest.menu;

import com.github.shiraji.findpullrequest.model.FindPullRequestConfig;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class FindPullRequestMenu implements Configurable {
    private ButtonGroup disableRadioButton;
    private JCheckBox disable;
    private JCheckBox debugMode;
    private JCheckBox jumpToFile;
    private JComboBox<Protocol> protocols;
    private JPanel root;
    private PropertiesComponent config;

    private enum Protocol {
        https("https://"), http("http://");
        private String text;
        Protocol(String text) {
            this.text = text;
        }

        @NotNull public String getText() {
            return text;
        }

        @Nullable public static Protocol findProtocolBy(String name) {
            switch (name) {
                case "https://": return https;
                case "http://": return http;
                default: return null;
            }
        }
    }

    FindPullRequestMenu(Project project) {
        super();

        config = PropertiesComponent.getInstance(project);
        DefaultComboBoxModel<Protocol> protocolDefaultComboBoxModel = new DefaultComboBoxModel<>(Protocol.values());
        protocols.setModel(protocolDefaultComboBoxModel);
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Find Pull Request Plugin";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return root;
    }

    @Override
    public boolean isModified() {
        Object selectedItem = protocols.getSelectedItem();
        if (selectedItem instanceof Protocol) {
            if (!FindPullRequestConfig.getProtocol(config).equals(((Protocol)selectedItem).getText())) {
                return true;
            }
        }

        return FindPullRequestConfig.isDebugMode(config) != debugMode.isSelected()
                || FindPullRequestConfig.isJumpToFile(config) != jumpToFile.isSelected()
                || FindPullRequestConfig.isDisable(config) != disable.isSelected();
    }

    @Override
    public void apply() throws ConfigurationException {
        FindPullRequestConfig.setDisable(config, disable.isSelected());
        FindPullRequestConfig.setDebugMode(config, debugMode.isSelected());
        FindPullRequestConfig.setJumpToFile(config, jumpToFile.isSelected());
        Object selectedItem = protocols.getSelectedItem();
        if (selectedItem instanceof Protocol) {
            FindPullRequestConfig.setProtocol(config, ((Protocol)selectedItem).getText());
        }
    }

    @Override
    public void reset() {
        String protocol = FindPullRequestConfig.getProtocol(config);
        Protocol protocolBy = Protocol.findProtocolBy(protocol);
        int selectedIndex = 0;
        if (protocolBy != null) {
            selectedIndex = protocolBy.ordinal();
        }
        protocols.setSelectedIndex(selectedIndex);
        disable.setSelected(FindPullRequestConfig.isDisable(config));
        debugMode.setSelected(FindPullRequestConfig.isDebugMode(config));
        jumpToFile.setSelected(FindPullRequestConfig.isJumpToFile(config));
    }

    @Override
    public void disposeUIResources() {

    }
}
