// SPDX-License-Identifier: MIT

package lermitage.intellij.extra.icons;

import com.intellij.ide.FileIconProvider;
import com.intellij.ide.IconProvider;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ProjectViewNodeDecorator;
import com.intellij.openapi.diagnostic.ControlFlowException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.NewUI;
import lermitage.intellij.extra.icons.cfg.services.SettingsIDEService;
import lermitage.intellij.extra.icons.cfg.services.SettingsProjectService;
import lermitage.intellij.extra.icons.cfg.services.SettingsService;
import lermitage.intellij.extra.icons.lic.ExtraIconsLicenseStatus;
import lermitage.intellij.extra.icons.services.FacetsFinderService;
import lermitage.intellij.extra.icons.utils.I18nUtils;
import lermitage.intellij.extra.icons.utils.IconUtils;
import lermitage.intellij.extra.icons.utils.ProjectUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Edoardo Luppi
 * @author Jonathan Lermitage
 */
public abstract class BaseIconProvider
    extends IconProvider // the most important icon provider
    implements FileIconProvider, // may drop FileIconProvider as most of the work is done by the IconProvider
    ProjectViewNodeDecorator // fix for https://youtrack.jetbrains.com/issue/PY-44417
{

    private static final @NonNls Logger LOGGER = Logger.getInstance(BaseIconProvider.class);

    private final List<Model> models;

    private long nbGetIcon = 0;
    private long checks_done = 0;
    private long checks_saved = 0;
    private final UITypeIconsPreference uiTypeIconsPreference;

    private static final ResourceBundle i18n = I18nUtils.getResourceBundle();

    public BaseIconProvider() {
        super();
        final UIType uiType = NewUI.isEnabled() ? UIType.NEW_UI : UIType.OLD_UI;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Detected UI Type: " + uiType);
        }
        this.models = getAllModels().stream()
            .filter(model -> model.getModelType() == ModelType.FILE || model.getModelType() == ModelType.DIR)
            .filter(model -> model.getUiType() == null || model.getUiType() == uiType)
            .toList();
        uiTypeIconsPreference = SettingsIDEService.getInstance().getUiTypeIconsPreference();
    }

    /**
     * Get list of all models managed by this icon provider. Their 'enabled' field doesn't matter.
     * This list will be processed by constructor and models 'enabled' field updated according to running IDE configuration.
     * Useful if BaseIconProvider has multiple inheritors, per example if one of them depends on a 3rd-party plugin (did
     * that for Angular, before removing it in favor of an Icon Enabler). May refactor and remove this method later if
     * it's still not used.
     */
    protected abstract List<Model> getAllModels();

    private static Model extractAltModel(Model model, int altIconIdx) {
        String altDescription;
        String altId;
        if (altIconIdx < 1) {
            if (model.getAltIcons().length == 1) {
                altDescription = MessageFormat.format(i18n.getString("model.desc.alternative"), model.getDescription());
            } else {
                altDescription = MessageFormat.format(i18n.getString("model.desc.alternative.first"), model.getDescription());
            }
            altId = model.getId() + "_alt"; //NON-NLS
        } else {
            altDescription = MessageFormat.format(i18n.getString("model.desc.alternative.other"), model.getDescription(), altIconIdx + 1);
            altId = model.getId() + "_alt" + (altIconIdx + 1); //NON-NLS
        }
        return Model.createAltModel(model, altId, model.getIdeIcon(), model.getAltIcons()[altIconIdx], altDescription);
    }

    public static Stream<Model> modelList(Model model) {
        if (model.getAltIcons() == null || model.getAltIcons().length == 0) {
            return Stream.of(model);
        }
        List<Model> models = new ArrayList<>();
        models.add(model);
        for (int i = 0; i < model.getAltIcons().length; i++) {
            models.add(extractAltModel(model, i));
        }
        return models.stream();
    }

    private String parent(@NotNull File file) {
        return file.getParent() == null ? null : file.getParentFile().getName().toLowerCase();
    }

    private void logError(@NotNull Throwable e) {
        if (e instanceof ControlFlowException) {
            // Control-flow exceptions should never be logged.
            LOGGER.info("ControlFlowException errors can be safely ignored as the impact on user experience should be very limited", e);
            return;
        }
        if (e instanceof MissingResourceException) {
            // TODO investigate https://github.com/jonathanlermitage/intellij-extra-icons-plugin/issues/137
            //  For now, silent errors when trying to find the PSI object for a file: PsiManager.getInstance(project).findFile(file).
            //  It should not impact user experience.
            LOGGER.info("MissingResourceException errors can be safely ignored as the impact on user experience should be very limited", e);
            return;
        }
        // Workaround for https://github.com/jonathanlermitage/intellij-extra-icons-plugin/issues/39
        // Plugin may want to reload icon on closed or disposed project. Just ignore it
        if (e.getMessage() != null) {
            String errMsg = e.getMessage()
                .replaceAll("[\\s_]", "") //NON-NLS
                .toUpperCase();
            if (errMsg.contains("DISPOSEINPROGRESS") || errMsg.contains("PROJECTISALREADYDISPOSED")) {
                LOGGER.infoWithDebug("Tried to show an icon but the project is disposed or being disposed. " +
                    "We can safely ignore this error with no impact on user experience", e);
            } else {
                LOGGER.warn(e);
            }
        }
    }

    @Nullable
    @Override // overrides FileIconProvider
    public Icon getIcon(@NotNull VirtualFile file, int flags, @Nullable Project project) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("FileIconProvider->" + file.getPath());
        }
        try {
            if (!ProjectUtils.isProjectAlive(project)) {
                return null;
            }
            if (file instanceof LightVirtualFile) {
                // TODO need to reproduce and understand what happens in
                //  https://github.com/jonathanlermitage/intellij-extra-icons-plugin/issues/86
                //  (error: Light files should have PSI only in one project)
                return null;
            }
            return getIcon(new File(file.getPath()),
                file.isDirectory() ? FileType.FOLDER : FileType.FILE,
                project);
        } catch (Throwable e) {
            logError(e);
        } finally {
            logCacheHitStats();
        }
        return null;
    }

    @Nullable
    @Override // overrides IconProvider
    public final Icon getIcon(@NotNull final PsiElement psiElement, final int flags) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("IconProvider->" + psiElement);
        }
        try {
            PsiFileSystemItem currentPsiFileItem;
            if (psiElement instanceof PsiDirectory psiDirectoryElt) {
                currentPsiFileItem = psiDirectoryElt;
            } else {
                final Optional<PsiFile> optFile = Optional.ofNullable(psiElement.getContainingFile());
                currentPsiFileItem = optFile.orElse(null);
            }
            if (currentPsiFileItem == null) {
                return null;
            }
            String fullPath = getFullPath(currentPsiFileItem);
            if (fullPath == null) {
                return null;
            }
            File file = new File(fullPath);
            return getIcon(file,
                psiElement instanceof PsiDirectory ? FileType.FOLDER : FileType.FILE,
                psiElement.getProject());
        } catch (Throwable e) {
            logError(e);
        } finally {
            logCacheHitStats();
        }
        return null;
    }

    @Override // overrides ProjectViewNodeDecorator
    public void decorate(ProjectViewNode<?> node, PresentationData data) {
        try {
            VirtualFile virtualFile = node.getVirtualFile();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("ProjectViewNodeDecorator->" + (virtualFile == null ? null : virtualFile.getPath()));
            }
            if (virtualFile != null) {
                File file = new File(virtualFile.getPath());
                FileType fileType = file.isDirectory() ? FileType.FOLDER : FileType.FILE;
                Project project = node.getProject();
                Icon icon = getIcon(file, fileType, project);
                if (icon != null) {
                    node.setIcon(icon);
                    if (data != null) {
                        data.setIcon(icon);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn(e);
        }
    }

    @Nullable
    private Icon getIcon(@NotNull File file, @NotNull FileType fileType, @Nullable Project project) {
        if (!ExtraIconsLicenseStatus.isLicenseActivated()) {
            return null;
        }
        nbGetIcon++;
        try {
            if (!ProjectUtils.isProjectAlive(project)) {
                return null;
            }
            ModelType currentModelType;
            if (fileType == FileType.FOLDER) {
                if (isPatternIgnored(project, file)) {
                    return null;
                }
                currentModelType = ModelType.DIR;
            } else {
                if (isPatternIgnored(project, file)) {
                    return null;
                }
                currentModelType = ModelType.FILE;
            }
            String parentName = parent(file);
            String normalizedFileName = getSanitizeFilePath(file.getName());
            String normalizedFileAbsPath = getSanitizeFilePath(file.getAbsolutePath());
            assert project != null;
            Set<String> facets = FacetsFinderService.getInstance(project).getFacets();
            Double additionalUIScale = SettingsIDEService.getInstance().getAdditionalUIScale2();
            SettingsService settingsService = getBestSettingsService(project);
            Object parentModelIdWhoseCheckFailed = null;
            for (final Model model : getModelsIncludingUserModels(project)) {
                if (model.getModelType() == currentModelType && model.isEnabled() && !settingsService.getDisabledModelIds().contains(model.getId())) {
                    if (model.getParentId() != null && parentModelIdWhoseCheckFailed == model.getParentId()) {
                        // check already returned false for this model (parent or alt), don't need to check again
                        checks_saved++;
                        continue;
                    }
                    checks_done++;
                    if (model.check(parentName, normalizedFileName, normalizedFileAbsPath, facets, project)) {
                        return IconUtils.getIcon(model, additionalUIScale, uiTypeIconsPreference);
                    } else {
                        parentModelIdWhoseCheckFailed = model.getParentId() == null ? model.getId() : model.getParentId();
                    }
                }
            }
        } catch (Throwable e) {
            logError(e);
        } finally {
            logCacheHitStats();
        }
        return null;
    }

    private void logCacheHitStats() {
        if (LOGGER.isDebugEnabled() && nbGetIcon > 0 && checks_done > 0 && (nbGetIcon < 5 || nbGetIcon < 100 ? nbGetIcon % 20 == 0 : nbGetIcon % 100 == 0)) {
            // activate with Help > Diagnostic Tools > Debug Log Settings > #lermitage.intellij.extra.icons.BaseIconProvider
            LOGGER.debug("[" + Thread.currentThread().getId() + "] " +
                "getIcon: " + nbGetIcon + ", " +
                "checks_done: " + checks_done + ", " +
                "checks_saved: " + checks_saved + " (" + (checks_saved * 100 / checks_done) + "%)");
        }
    }

    @Nullable
    private String getFullPath(@NotNull PsiFileSystemItem file) {
        if (file.getVirtualFile() != null) {
            return file.getVirtualFile().getPath().toLowerCase();
        }
        return null;
    }

    private String getSanitizeFilePath(String path) {
        return path.toLowerCase().replaceAll("\\\\", "/");
    }

    /**
     * Depending on whether the checkbox in the settings is checked, this method appends
     * the user added models to the model list.
     */
    private List<Model> getModelsIncludingUserModels(@Nullable Project project) {
        Stream<Model> customModelsStream = null;

        if (project != null) {
            SettingsProjectService settingsProjectService = SettingsProjectService.getInstance(project);
            if (settingsProjectService.isOverrideIDESettings()) {
                if (settingsProjectService.isAddToIDEUserIcons()) {
                    customModelsStream = Stream.concat(SettingsIDEService.getInstance().getCustomModels().stream(),
                        settingsProjectService.getCustomModels().stream());
                } else {
                    customModelsStream = settingsProjectService.getCustomModels().stream();
                }
            }
        }
        if (customModelsStream == null) {
            customModelsStream = SettingsIDEService.getInstance().getCustomModels().stream();
        }

        return Stream.concat(customModelsStream, models.stream()).collect(Collectors.toList());//
    }

    /**
     * Indicates if given file/folder should be ignored.
     * @param project project.
     * @param file current file or folder.
     */
    private boolean isPatternIgnored(Project project, File file) {
        try {
            SettingsService service = getBestSettingsService(project);
            if (service.getIgnoredPatternObj() == null || service.getIgnoredPattern() == null || service.getIgnoredPattern().isEmpty()) {
                return false;
            }
            String projectBasePath;
            if (ProjectUtils.isProjectAlive(project)) {
                projectBasePath = project.getBasePath();
                if (projectBasePath == null) {
                    return false;
                }
            } else {
                return false;
            }
            String normalizedFileAbsPath = file.getAbsolutePath().toLowerCase().replaceAll("\\\\", "/");
            String normalizedProjectPath = projectBasePath.toLowerCase().replaceAll("\\\\", "/");
            if (!normalizedFileAbsPath.startsWith(normalizedProjectPath) || normalizedFileAbsPath.length() <= normalizedProjectPath.length()) {
                return false;
            }
            return service.getIgnoredPatternObj().matcher(normalizedFileAbsPath.substring(normalizedProjectPath.length() + 1)).matches();
        } catch (Exception e) {
            LOGGER.warn(file.getAbsolutePath() + ", " + project.getBasePath());
            logError(e);
            return false;
        }
    }

    /**
     * Returns the Project settings service if the project is not null and if the checkbox in the project settings was checked,
     * otherwise returns the IDE settings service.
     */
    @NotNull
    private SettingsService getBestSettingsService(@Nullable Project project) {
        if (project != null) {
            SettingsProjectService settingsProjectService = SettingsProjectService.getInstance(project);
            if (settingsProjectService.isOverrideIDESettings()) {
                return settingsProjectService;
            }
        }
        return SettingsIDEService.getInstance();
    }

    private enum FileType {
        FILE, FOLDER
    }
}
