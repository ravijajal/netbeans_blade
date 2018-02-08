/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
/*
 * Contributor(s): Sebastian Hörl
 */
package org.netbeans.modules.php.blade.editor.lexer;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.html.lexer.HTMLTokenId;
import org.netbeans.api.lexer.InputAttributes;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.LanguagePath;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.modules.php.blade.editor.gsf.BladeLanguage;
import org.netbeans.spi.lexer.LanguageEmbedding;
import org.netbeans.spi.lexer.LanguageHierarchy;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;

public enum BladeTopTokenId implements TokenId {
    T_BLADE_OTHER("blade_error"), //NOI18N
    T_BLADE_COMMENT("blade_comment"), //NOI18N
    T_BLADE_BLOCK_START("blade_block_delimiter"), //NOI18N
    T_BLADE_BLOCK("blade_block"), //NOI18N
    T_BLADE_BLOCK_END("blade_block_delimiter"), //NOI18N
    T_BLADE_VAR_START("blade_var_delimiter"), //NOI18N
    T_BLADE_VAR("blade_var"), //NOI18N
    T_BLADE_VAR_END("blade_var_delimiter"), //NOI18N
    T_HTML("blade_html"), //NOI18N
    T_BLADE_SECTION_START("blade_section"), //NOI18N
    T_BLADE_SECTION_END("blade_section"), //NOI18N
    T_BLADE_SECTION_PREFIX("blade_prefix"); //NOI18N

    private final String primaryCategory;

    BladeTopTokenId(String primaryCategory) {
        this.primaryCategory = primaryCategory;
    }

    @Override
    public String primaryCategory() {
        return primaryCategory;
    }
    private static final Language<BladeTopTokenId> LANGUAGE =
            new LanguageHierarchy<BladeTopTokenId>() {
                @Override
                protected Collection<BladeTopTokenId> createTokenIds() {
                    return EnumSet.allOf(BladeTopTokenId.class);
                }

                @Override
                protected Map<String, Collection<BladeTopTokenId>> createTokenCategories() {
                    Map<String, Collection<BladeTopTokenId>> cats = new HashMap<>();
                    return cats;
                }

                @Override
                protected Lexer<BladeTopTokenId> createLexer(LexerRestartInfo<BladeTopTokenId> info) {
                    return new BladeTopLexer(info);
                }

                @Override
                protected String mimeType() {
                    return BladeLanguage.BLADE_MIME_TYPE;
                }

                @Override
                protected LanguageEmbedding<?> embedding(Token<BladeTopTokenId> token,
                        LanguagePath languagePath, InputAttributes inputAttributes) {

                    BladeTopTokenId id = token.id();
                    if (id == T_HTML) {
                        return LanguageEmbedding.create(HTMLTokenId.language(), 0, 0, true);
                    } else if (id == T_BLADE_BLOCK) {
                        return LanguageEmbedding.create(BladeBlockTokenId.language(), 0, 0);
                    } else if (id == T_BLADE_VAR) {
                        return LanguageEmbedding.create(BladeVariableTokenId.language(), 0, 0);
                    }

                    return null;

                }
            }.language();

    public static Language<BladeTopTokenId> language() {
        return LANGUAGE;
    }
}
