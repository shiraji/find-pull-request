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
            if (!FindPullRequestConfig.getProtocol(project).equals(((Protocol)selectedItem).getText())) {
                return true;
            }
        }

        return FindPullRequestConfig.isDebugMode(project) != debugMode.isSelected()
                || FindPullRequestConfig.isJumpToFile(project) != jumpToFile.isSelected()
                || FindPullRequestConfig.isDisable(project) != disable.isSelected();
    }

    @Override
    public void apply() throws ConfigurationException {
        FindPullRequestConfig.setDisable(disable.isSelected(), project);
        FindPullRequestConfig.setDebugMode(debugMode.isSelected(), project);
        FindPullRequestConfig.setJumpToFile(jumpToFile.isSelected(), project);
        Object selectedItem = protocols.getSelectedItem();
        if (selectedItem instanceof Protocol) {
            FindPullRequestConfig.setProtocol(((Protocol)selectedItem).getText(), project);
        }
    }

    @Override
    public void reset() {

    }

    @Override
    public void disposeUIResources() {

    }
}
