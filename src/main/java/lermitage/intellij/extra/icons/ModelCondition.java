// SPDX-License-Identifier: MIT

package lermitage.intellij.extra.icons;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.Tag;
import lermitage.intellij.extra.icons.enablers.IconEnabler;
import lermitage.intellij.extra.icons.enablers.IconEnablerProvider;
import lermitage.intellij.extra.icons.enablers.IconEnablerType;
import lermitage.intellij.extra.icons.utils.I18nUtils;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Tag
public class ModelCondition {

    @OptionTag
    private boolean start = false;
    @OptionTag
    private boolean eq = false;
    @OptionTag
    private boolean mayEnd = false;
    @OptionTag
    private boolean end = false;
    @OptionTag
    private boolean noDot = false;
    @OptionTag
    private boolean checkParent = false;
    @OptionTag
    private boolean hasRegex = false;
    @OptionTag
    private boolean enabled = true;
    @OptionTag
    private boolean checkFacets = false;
    @OptionTag
    private boolean hasIconEnabler = false;
    @OptionTag
    private boolean isInProjectRootFolder = false;

    @OptionTag
    private String[] names = new String[0];
    @OptionTag
    private Set<String> parentNames = Collections.emptySet();
    @OptionTag
    private String[] extensions = new String[0];
    @OptionTag
    private String regex;
    @OptionTag
    private String[] facets = new String[0];

    // transient fields are excluded from IconPack items

    private Pattern pattern; // transient because computed dynamically
    private IconEnablerType iconEnablerType; // transient because not exposed to user models

    public void setParents(String... parents) {
        this.checkParent = true;
        this.parentNames = toLowerCaseSet(parents);
    }

    public void setStart(String... base) {
        this.start = true;
        this.names = toLowerCaseArray(base);
    }

    public void setEq(String... base) {
        this.eq = true;
        this.names = toLowerCaseArray(base);
    }

    public void setMayEnd(String... extensions) {
        this.mayEnd = true;
        this.extensions = toLowerCaseArray(extensions);
    }

    public void setEnd(String... extensions) {
        this.end = true;
        this.extensions = toLowerCaseArray(extensions);
    }

    public void setNoDot() {
        this.noDot = true;
    }

    public void setRegex(String regex) {
        this.hasRegex = true;
        this.regex = regex;
        this.pattern = Pattern.compile(regex);
    }

    public void setFacets(String[] facets) {
        this.checkFacets = true;
        this.facets = facets;
    }

    public void setIconEnablerType(IconEnablerType iconEnablerType) {
        this.hasIconEnabler = true;
        this.iconEnablerType = iconEnablerType;
    }

    public void setIsInProjectRootFolder() {
        this.isInProjectRootFolder = true;
    }

    public boolean check(String parentName, String fileName, @Nullable String fullPath, Set<String> prjFacets, Project project) {
        if (!this.enabled) {
            return false;
        }

        if (this.isInProjectRootFolder) {
            if (fullPath == null || !fullPath.equalsIgnoreCase(project.getBasePath() + "/" + fileName)) {
                return false;
            }
        }

        if (this.hasIconEnabler && fullPath != null) {
            IconEnabler iconEnabler = IconEnablerProvider.getIconEnabler(project, this.iconEnablerType);
            if (iconEnabler != null) {
                boolean iconEnabledVerified = iconEnabler.verify(project, fullPath);
                if (!iconEnabledVerified) {
                    return false;
                } else if (iconEnabler.terminatesConditionEvaluation()) {
                    return true;
                }
            }
        }

        // facet is a pre-condition, should always be associated with other conditions
        if (this.checkFacets && this.facets != null) {
            boolean facetChecked = false;
            for (String modelFacet : this.facets) {
                if (prjFacets.contains(modelFacet)) {
                    facetChecked = true;
                    break;
                }
            }
            if (!facetChecked) {
                return false;
            }
        }

        if (this.checkParent) {
            if (!(this.start || this.eq || this.end || this.mayEnd)) {
                if (this.parentNames.contains(parentName)) {
                    return true; // To style all files in a subdirectory
                }
            } else {
                if (!this.parentNames.contains(parentName)) {
                    return false;
                }
            }
        }

        if (this.hasRegex && fullPath != null) {
            if (this.pattern == null) {
                this.pattern = Pattern.compile(this.regex);
            }
            if (this.pattern.matcher(fullPath).matches()) {
                return true;
            }
        }

        if (this.eq) {
            if (this.end) {
                for (String n : this.names) {
                    for (String e : this.extensions) {
                        if (fileName.equals(n + e)) {
                            return true;
                        }
                    }
                }
            } else if (this.mayEnd) {
                for (String n : this.names) {
                    if (fileName.equals(n)) {
                        return true;
                    }
                    for (String e : this.extensions) {
                        if (fileName.equals(n + e)) {
                            return true;
                        }
                    }
                }
            } else {
                for (String n : this.names) {
                    if (fileName.equals(n)) {
                        return true;
                    }
                }
            }
        }

        if (this.start) {
            if (this.end) {
                for (String n : this.names) {
                    for (String e : this.extensions) {
                        if (fileName.startsWith(n) && fileName.endsWith(e)) {
                            return true;
                        }
                    }
                }
            } else if (this.mayEnd) {
                for (String n : this.names) {
                    if (fileName.startsWith(n)) {
                        return true;
                    }
                    for (String e : this.extensions) {
                        if (fileName.startsWith(n) && fileName.endsWith(e)) {
                            return true;
                        }
                    }
                }
            } else if (this.noDot) {
                for (String n : this.names) {
                    if (fileName.startsWith(n) && !fileName.contains(".")) {
                        return true;
                    }
                }
            } else {
                for (String n : this.names) {
                    if (fileName.startsWith(n)) {
                        return true;
                    }
                }
            }
        }

        if (this.end & !this.eq & !this.start) {
            for (String e : this.extensions) {
                if (fileName.endsWith(e)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasStart() {
        return this.start;
    }

    public boolean hasEq() {
        return this.eq;
    }

    public boolean hasMayEnd() {
        return this.mayEnd;
    }

    public boolean hasEnd() {
        return this.end;
    }

    public boolean hasNoDot() {
        return this.noDot;
    }

    public boolean hasCheckParent() {
        return this.checkParent;
    }

    public boolean hasRegex() {
        return this.hasRegex;
    }

    public boolean hasFacets() {
        return this.checkFacets;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isValid() {
        return this.hasRegex || this.checkParent || this.start || this.eq || this.end || this.mayEnd;
    }

    public String[] getNames() {
        return this.names;
    }

    public String[] getExtensions() {
        return this.extensions;
    }

    public Set<String> getParents() {
        return this.parentNames;
    }

    public String getRegex() {
        return this.regex;
    }

    public String[] getFacets() {
        return this.facets;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String asReadableString(String delimiter) {
        ArrayList<String> parameters = new ArrayList<>();
        if (this.hasRegex) {
            parameters.add(MessageFormat.format(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.regex"), this.regex));
        }

        if (this.checkParent) {
            parameters.add(MessageFormat.format(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.check.parents"), String.join(delimiter, this.parentNames)));
        }

        if (this.start || this.eq) {
            String names = String.join(delimiter, this.names);
            if (this.start) {
                names = MessageFormat.format(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.name.starts.with"), names);
                if (this.noDot) {
                    names += I18nUtils.RESOURCE_BUNDLE.getString("model.condition.name.starts.with.and.no.dot");
                }
            } else {
                names = MessageFormat.format(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.name.equals"), names);
            }
            parameters.add(names);
        }

        if (this.mayEnd || this.end) {
            String extensions = String.join(delimiter, this.extensions);
            if (this.mayEnd) {
                extensions = MessageFormat.format(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.name.may.end.with"), extensions);
            } else {
                extensions = MessageFormat.format(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.name.ends.with"), extensions);
            }
            parameters.add(extensions);
        }

        if (this.checkFacets) {
            parameters.add(MessageFormat.format(I18nUtils.RESOURCE_BUNDLE.getString("model.condition.facets"), Arrays.toString(this.facets)));
        }

        return StringUtil.capitalize(String.join(", ", parameters));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelCondition that = (ModelCondition) o;
        return this.start == that.start &&
               this.eq == that.eq &&
               this.mayEnd == that.mayEnd &&
               this.end == that.end &&
               this.noDot == that.noDot &&
               this.checkParent == that.checkParent &&
               this.hasRegex == that.hasRegex &&
               this.enabled == that.enabled &&
               Arrays.equals(this.names, that.names) &&
               this.parentNames.equals(that.parentNames) &&
               Arrays.equals(this.extensions, that.extensions) &&
               Objects.equals(this.regex, that.regex) &&
               Arrays.equals(this.facets, that.facets);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(this.start, this.eq, this.mayEnd, this.end, this.noDot, this.checkParent, this.hasRegex, this.enabled, this.parentNames, this.regex);
        result = 31 * result + Arrays.hashCode(this.names);
        result = 31 * result + Arrays.hashCode(this.extensions);
        result = 31 * result + Arrays.hashCode(this.facets);
        return result;
    }

    private String[] toLowerCaseArray(String... s) {
        String[] res = new String[s.length];
        for (int i = 0; i < s.length; i++) {
            res[i] = s[i].toLowerCase();
        }
        return res;
    }

    private Set<String> toLowerCaseSet(String... s) {
        Set<String> res = new HashSet<>(s.length);
        for (String str : s) {
            res.add(str.toLowerCase());
        }
        return res;
    }
}
