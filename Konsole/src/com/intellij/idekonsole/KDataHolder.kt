package com.intellij.idekonsole

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

// FIXME: we can load separate instances of this class in different MegaLoader's -
// and initialize its fields via reflection before executing the payload
// but for now - just share same instances, expecting that no one will launch multiple commands simultaneously
object KDataHolder {
    var project: Project? = null
        set(p: Project?) {
            field = p
            if (p != null && scope == null) {
                scope = GlobalSearchScope.projectScope(p)
            }
        }
    var editor: KEditor? = null

    var scope: GlobalSearchScope? = null
}