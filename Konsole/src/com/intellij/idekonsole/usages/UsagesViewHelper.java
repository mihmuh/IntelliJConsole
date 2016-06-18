package com.intellij.idekonsole.usages;

import com.intellij.openapi.project.Project;
import com.intellij.usages.UsageViewManager;
import com.intellij.usages.UsageViewPresentation;
import com.intellij.usages.impl.UsageViewImpl;
import com.intellij.usages.impl.UsageViewManagerImpl;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class UsagesViewHelper {
    public static void addContent(@NotNull Project project, @NotNull UsageViewImpl usageView, @NotNull UsageViewPresentation presentation) {
        UsageViewManagerImpl usageViewManager = (UsageViewManagerImpl) UsageViewManager.getInstance(project);
        try {
            Method method = null;
            for (Method method1 : UsageViewManagerImpl.class.getDeclaredMethods()) {
                if (method1.getName().equals("addContent")) {
                    method = method1;
                }
            }
            if (method == null) {
                throw new NoSuchMethodException();
            }
            method.setAccessible(true);
            method.invoke(usageViewManager, usageView, presentation);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
