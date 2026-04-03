/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.lua.project;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.FileContentUtil;
import com.tang.intellij.lua.LuaBundle;
import com.tang.intellij.lua.lang.LuaLanguageLevel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;
import java.util.SortedMap;

/**
 * Created by tangzx on 2017/6/12.
 */
public class LuaSettingsPanel implements SearchableConfigurable {
    private final LuaSettings settings;
    private JPanel myPanel;
    private JTextField constructorNames;
    private JCheckBox strictDoc;
    private JCheckBox smartCloseEnd;
    private JCheckBox showWordsInFile;
    private JCheckBox enforceTypeSafety;
    private JCheckBox nilStrict;
    private JCheckBox recognizeGlobalNameAsCheckBox;
    private LuaAdditionalSourcesRootPanel additionalRoots;
    private JCheckBox enableGenericCheckBox;
    private JCheckBox captureOutputDebugString;
    private JCheckBox captureStd;
    private JComboBox<String> defaultCharset;
    private JComboBox<LuaLanguageLevel> languageLevel;
    private JTextField requireFunctionNames;
    private JComboBox<String> requirePathSeparator;
    private JTextField tooLargerFileThreshold;
    private JTextField strictGlobalNames;
    private JTextField strictGlobalNamesFilePath;
    private JButton openStrictGlobalNamesFileButton;

    public LuaSettingsPanel() {
        this.settings = LuaSettings.Companion.getInstance();
        constructorNames.setText(settings.getConstructorNamesString());
        tooLargerFileThreshold.setDocument(new IntegerDocument());
        tooLargerFileThreshold.setText(String.valueOf(settings.getTooLargerFileThreshold()));
        strictDoc.setSelected(settings.isStrictDoc());
        smartCloseEnd.setSelected(settings.isSmartCloseEnd());
        showWordsInFile.setSelected(settings.isShowWordsInFile());
        enforceTypeSafety.setSelected(settings.isEnforceTypeSafety());
        nilStrict.setSelected(settings.isNilStrict());
        recognizeGlobalNameAsCheckBox.setSelected(settings.isRecognizeGlobalNameAsType());
        additionalRoots.setRoots(settings.getAdditionalSourcesRoot());
        enableGenericCheckBox.setSelected(settings.getEnableGeneric());
        requireFunctionNames.setText(settings.getRequireLikeFunctionNamesString());
        requirePathSeparator.setModel(new DefaultComboBoxModel<>(new String[]{LuaSettings.REQUIRE_PATH_SEPARATOR_SLASH, LuaSettings.REQUIRE_PATH_SEPARATOR_DOT}));
        requirePathSeparator.setSelectedItem(settings.getRequirePathSeparator());
        strictGlobalNames.setText(settings.getStrictGlobalNamesString());
        strictGlobalNamesFilePath.setEditable(false);
        updateStrictGlobalNamesPath(getSingleOpenProject());
        openStrictGlobalNamesFileButton.addActionListener(event -> {
            Project project = chooseProjectForStrictGlobalNames();
            if (project != null && StrictGlobalNamesManager.openFile(project)) {
                updateStrictGlobalNamesPath(project);
            }
        });

        captureStd.setSelected(settings.getAttachDebugCaptureStd());
        captureOutputDebugString.setSelected(settings.getAttachDebugCaptureOutput());

        SortedMap<String, Charset> charsetSortedMap = Charset.availableCharsets();
        ComboBoxModel<String> outputCharsetModel = new DefaultComboBoxModel<>(ArrayUtil.toStringArray(charsetSortedMap.keySet()));
        defaultCharset.setModel(outputCharsetModel);
        defaultCharset.setSelectedItem(settings.getAttachDebugDefaultCharsetName());

        //language level
        ComboBoxModel<LuaLanguageLevel> lanLevelModel = new DefaultComboBoxModel<>(LuaLanguageLevel.values());
        languageLevel.setModel(lanLevelModel);
        lanLevelModel.setSelectedItem(settings.getLanguageLevel());
    }

    @NotNull
    @Override
    public String getId() {
        return "Lua";
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Lua";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.add(myPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(contentWrapper,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    @Override
    public boolean isModified() {
        return !StringUtil.equals(settings.getConstructorNamesString(), constructorNames.getText()) ||
                !StringUtil.equals(settings.getRequireLikeFunctionNamesString(), requireFunctionNames.getText()) ||
                !StringUtil.equals(settings.getRequirePathSeparator(), (String) requirePathSeparator.getSelectedItem()) ||
                settings.getTooLargerFileThreshold() != getTooLargerFileThreshold() ||
                settings.isSmartCloseEnd() != smartCloseEnd.isSelected() ||
                settings.isShowWordsInFile() != showWordsInFile.isSelected() ||
                settings.isEnforceTypeSafety() != enforceTypeSafety.isSelected() ||
                settings.isNilStrict() != nilStrict.isSelected() ||
                settings.isRecognizeGlobalNameAsType() != recognizeGlobalNameAsCheckBox.isSelected() ||
                settings.getEnableGeneric() != enableGenericCheckBox.isSelected() ||
                settings.getAttachDebugCaptureOutput() != captureOutputDebugString.isSelected() ||
                settings.getAttachDebugCaptureStd() != captureStd.isSelected() ||
                settings.getAttachDebugDefaultCharsetName() != defaultCharset.getSelectedItem() ||
                settings.getLanguageLevel() != languageLevel.getSelectedItem() ||
                !Arrays.equals(settings.getAdditionalSourcesRoot(), additionalRoots.getRoots(), String::compareTo) ||
                !StringUtil.equals(settings.getStrictGlobalNamesString(), strictGlobalNames.getText());
    }

    @Override
    public void apply() {
        settings.setConstructorNamesString(constructorNames.getText());
        constructorNames.setText(settings.getConstructorNamesString());
        settings.setRequireLikeFunctionNamesString(requireFunctionNames.getText());
        requireFunctionNames.setText(settings.getRequireLikeFunctionNamesString());
        settings.setRequirePathSeparator((String) Objects.requireNonNull(requirePathSeparator.getSelectedItem()));
        settings.setTooLargerFileThreshold(getTooLargerFileThreshold());
        settings.setSmartCloseEnd(smartCloseEnd.isSelected());
        settings.setShowWordsInFile(showWordsInFile.isSelected());
        settings.setEnforceTypeSafety(enforceTypeSafety.isSelected());
        settings.setNilStrict(nilStrict.isSelected());
        settings.setRecognizeGlobalNameAsType(recognizeGlobalNameAsCheckBox.isSelected());
        settings.setAdditionalSourcesRoot(additionalRoots.getRoots());
        settings.setEnableGeneric(enableGenericCheckBox.isSelected());
        settings.setAttachDebugCaptureOutput(captureOutputDebugString.isSelected());
        settings.setAttachDebugCaptureStd(captureStd.isSelected());
        settings.setAttachDebugDefaultCharsetName((String) Objects.requireNonNull(defaultCharset.getSelectedItem()));
        settings.setStrictGlobalNamesString(strictGlobalNames.getText());
        strictGlobalNames.setText(settings.getStrictGlobalNamesString());
        LuaLanguageLevel selectedLevel = (LuaLanguageLevel) Objects.requireNonNull(languageLevel.getSelectedItem());
        if (selectedLevel != settings.getLanguageLevel()) {
            settings.setLanguageLevel(selectedLevel);
            StdLibraryProvider.Companion.reload();

            FileContentUtil.reparseOpenedFiles();
        } else {
            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                DaemonCodeAnalyzer.getInstance(project).restart();
            }
        }
    }

    private void updateStrictGlobalNamesPath(@Nullable Project project) {
        strictGlobalNamesFilePath.setText(StrictGlobalNamesManager.getDisplayPath(project));
        strictGlobalNamesFilePath.setCaretPosition(0);
    }

    @Nullable
    private Project getSingleOpenProject() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        return projects.length == 1 ? projects[0] : null;
    }

    @Nullable
    private Project chooseProjectForStrictGlobalNames() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects.length == 0) {
            Messages.showInfoMessage(LuaBundle.message("ui.settings.strict_global_names_no_project"), getDisplayName());
            return null;
        }
        if (projects.length == 1) {
            return projects[0];
        }

        String[] projectNames = new String[projects.length];
        for (int i = 0; i < projects.length; i++) {
            projectNames[i] = projects[i].getName();
        }

        int index = Messages.showDialog(
                myPanel,
                LuaBundle.message("ui.settings.strict_global_names_choose_project"),
                LuaBundle.message("ui.settings.strict_global_names_choose_title"),
                projectNames,
                0,
                null);
        return index >= 0 ? projects[index] : null;
    }

    private int getTooLargerFileThreshold() {
        int value;
        try {
            value = Integer.parseInt(tooLargerFileThreshold.getText());
        } catch (NumberFormatException e) {
            value = settings.getTooLargerFileThreshold();
        }
        return value;
    }

    static class IntegerDocument extends PlainDocument {
        public void insertString(int offset, String s, AttributeSet attributeSet) throws BadLocationException {
            try {
                Integer.parseInt(s);
            } catch (Exception ex) {
                return;
            }
            super.insertString(offset, s, attributeSet);
        }
    }
}
