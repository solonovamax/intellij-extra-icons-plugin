// SPDX-License-Identifier: MIT

package lermitage.intellij.extra.icons.activity;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lermitage.intellij.extra.icons.enablers.IconEnablerProvider;
import lermitage.intellij.extra.icons.enablers.IconEnablerType;
import lermitage.intellij.extra.icons.enablers.services.GitSubmoduleFolderEnablerService;
import lermitage.intellij.extra.icons.messaging.RefreshIconsNotifierService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// TODO migrate to Listener https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html#defining-project-level-listeners
public class VFSChangesListenersProjectActivity implements ProjectActivity {

    private static final Logger LOGGER = Logger.getInstance(VFSChangesListenersProjectActivity.class);

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
                refreshGitSubmodules(events, project);
            }
        });
        return null;
    }

    // TODO big refactoring: update all enablers with modified files, this way there is no need to re-init manually from the UI, which would eliminate the problem of enablers (IDE filename index queries) from EDT
    private void refreshGitSubmodules(@NotNull List<? extends VFileEvent> events, @NotNull Project project) {
        try {
            DumbService.getInstance(project).runReadActionInSmartMode(() -> {
                final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
                boolean gitmodulesUpdated = events.stream()
                    .anyMatch(vFileEvent -> vFileEvent.getFile() != null
                        && vFileEvent.isFromSave()
                        && fileIndex.isInProject(vFileEvent.getFile())
                        && vFileEvent.getPath().endsWith(GitSubmoduleFolderEnablerService.GIT_MODULES_FILENAME)
                    );
                if (gitmodulesUpdated) {
                    IconEnablerProvider.getIconEnabler(project, IconEnablerType.IS_GIT_SUBMODULE_FOLDER).ifPresent(iconEnabler ->
                        iconEnabler.init(project)
                    );
                    RefreshIconsNotifierService.getInstance().triggerProjectIconsRefresh(project);
                }
            });
        } catch (Exception e) {
            LOGGER.warn(e);
        }
    }
}
