/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;

/**
 * This is a test authority to very how the platform deals with authority
 * containing a slash
 */
public class AuthorityWithProblematicID implements ChoiceAuthority {
    private String pluginInstanceName;

    public static final String SPECIAL_AUTHORITY = "TheSlash/ID";

    @Override
    public Choices getMatches(String query, int start, int limit, String locale) {
        // for our purpose all query matches our authority
        Choice v[] = new Choice[] {
            new Choice(SPECIAL_AUTHORITY, "The value", "The display")
            };
        return new Choices(v, 0, v.length, Choices.CF_AMBIGUOUS, false, 0);
    }

    @Override
    public Choices getBestMatch(String text, String locale) {
        // for our purpose all query matches our authority
        Choice v[] = new Choice[] {
            new Choice(SPECIAL_AUTHORITY, "The value", "The display")
            };
        return new Choices(v, 0, v.length, Choices.CF_AMBIGUOUS, false, 0);
    }

    @Override
    public String getLabel(String key, String locale) {
        if (StringUtils.equals(key, SPECIAL_AUTHORITY)) {
            return "The display";
        }
        return null;
    }

    @Override
    public boolean isScrollable() {
        return true;
    }

    @Override
    public String getPluginInstanceName() {
        return pluginInstanceName;
    }

    @Override
    public void setPluginInstanceName(String name) {
        this.pluginInstanceName = name;
    }
}
