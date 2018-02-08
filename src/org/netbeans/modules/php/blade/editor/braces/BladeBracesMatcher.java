/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.blade.editor.braces;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.text.BadLocationException;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.php.blade.editor.lexer.BladeBlockTokenId;
import org.netbeans.modules.php.blade.editor.lexer.BladeLexerUtils;
import org.netbeans.modules.php.blade.editor.lexer.BladeTopTokenId;
import org.netbeans.spi.editor.bracesmatching.BracesMatcher;
import org.netbeans.spi.editor.bracesmatching.BracesMatcherFactory;
import org.netbeans.spi.editor.bracesmatching.MatcherContext;

/**
 *
 * @author Ondrej Brejla <obrejla@netbeans.org>
 */
public final class BladeBracesMatcher implements BracesMatcher {
    private static final List<Matcher> MATCHERS = new ArrayList<>();
    static {
        MATCHERS.add(new StartEndBlockMatcher("autoescape")); //NOI18N
        MATCHERS.add(new StartEndBlockMatcher("block")); //NOI18N
        MATCHERS.add(new StartEndBlockMatcher("tesi")); //NOI18N
        MATCHERS.add(new StartEndBlockMatcher("embed")); //NOI18N
        MATCHERS.add(new StartEndBlockMatcher("filter")); //NOI18N
        MATCHERS.add(new StartEndBlockMatcher("for")); //NOI18N
        MATCHERS.add(new StartEndBlockMatcher("macro")); //NOI18N
        MATCHERS.add(new StartEndBlockMatcher("raw")); //NOI18N
        MATCHERS.add(new StartEndBlockMatcher("sandbox")); //NOI18N
        MATCHERS.add(new StartEndBlockMatcher("set")); //NOI18N
        MATCHERS.add(new StartEndBlockMatcher("spaceless")); //NOI18N
        MATCHERS.add(new StartEndBlockMatcher("verbatim")); //NOI18N
        MATCHERS.add(new IfMatcher());
        MATCHERS.add(new EndIfMatcher());
        MATCHERS.add(new ElseMatcher());
        MATCHERS.add(new ElseIfMatcher());
    }
    private final MatcherContext context;

    private BladeBracesMatcher(MatcherContext context) {
        this.context = context;
    }

    @Override
    public int[] findOrigin() throws InterruptedException, BadLocationException {
        int[] result = null;
        BaseDocument document = (BaseDocument) context.getDocument();
        document.readLock();
        try {
            result = findOriginUnderLock();
        } finally {
            document.readUnlock();
        }
        return result;
    }

    private int[] findOriginUnderLock() {
        int[] result = null;
        TokenSequence<? extends BladeBlockTokenId> ts = BladeLexerUtils.getBladeBlockTokenSequence(context.getDocument(), context.getSearchOffset());
        if (ts != null) {
            result = findOriginInSequence(ts);
        }
        return result;
    }

    private int[] findOriginInSequence(TokenSequence<? extends BladeBlockTokenId> ts) {
        int[] result = null;
        ts.move(context.getSearchOffset());
        if (ts.moveNext()) {
            Token<? extends BladeBlockTokenId> currentToken = ts.token();
            if (currentToken != null) {
                BladeBlockTokenId currentTokenId = currentToken.id();
                if (currentTokenId == BladeBlockTokenId.T_BLADE_TAG) {
                    result = new int[] {ts.offset(), ts.offset() + currentToken.length()};
                }
            }
        }
        return result;
    }

    @Override
    public int[] findMatches() throws InterruptedException, BadLocationException {
        int[] result = null;
        BaseDocument document = (BaseDocument) context.getDocument();
        document.readLock();
        try {
            result = findMatchesUnderLock();
        } finally {
            document.readUnlock();
        }
        return result;
    }

    private int[] findMatchesUnderLock() {
        int[] result = null;
        TokenSequence<? extends BladeTopTokenId> topTs = BladeLexerUtils.getBladeTokenSequence(context.getDocument(), context.getSearchOffset());
        if (topTs != null) {
            result = findMatchesInTopSequence(topTs);
        }
        return result;
    }

    private int[] findMatchesInTopSequence(TokenSequence<? extends BladeTopTokenId> topTs) {
        assert topTs != null;
        int[] result = null;
        topTs.move(context.getSearchOffset());
        topTs.moveNext();
        TokenSequence<BladeBlockTokenId> ts = topTs.embeddedJoined(BladeBlockTokenId.language());
        if (ts != null) {
            result = findMatchesInEmbeddedSequence(topTs, ts);
        }
        return result;
    }

    private int[] findMatchesInEmbeddedSequence(TokenSequence<? extends BladeTopTokenId> topTs, TokenSequence<BladeBlockTokenId> embeddedTs) {
        int[] result = null;
        embeddedTs.move(context.getSearchOffset());
        if (embeddedTs.moveNext()) {
            Token<? extends BladeBlockTokenId> currentToken = embeddedTs.token();
            if (currentToken != null) {
                result = processMatchers(currentToken, topTs);
            }
        }
        return result;
    }

    private int[] processMatchers(Token<? extends BladeBlockTokenId> currentToken, TokenSequence<? extends BladeTopTokenId> topTs) {
        int[] result = null;
        for (Matcher matcher : MATCHERS) {
            if (matcher.matches(currentToken)) {
                result = matcher.findMatches(currentToken, topTs);
                break;
            }
        }
        return result;
    }

    private static int[] createMatches(List<OffsetRange> offsetRanges) {
        int[] result = null;
        if (!offsetRanges.isEmpty()) {
            int resultSize = offsetRanges.size() * 2;
            result = new int[resultSize];
            for (int i = 0, j = 0; i < offsetRanges.size(); i++, j += 2) {
                result[j] = offsetRanges.get(i).getStart();
                result[j + 1] = offsetRanges.get(i).getEnd();
            }
        }
        return result;
    }

    private interface Matcher {

        boolean matches(Token<? extends BladeBlockTokenId> token);

        int[] findMatches(Token<? extends BladeBlockTokenId> token, TokenSequence<? extends BladeTopTokenId> topTs);

    }

    private abstract static class IfConditionMatcher implements Matcher {
        protected static final BladeLexerUtils.BladeTokenText IF_TOKEN = BladeLexerUtils.BladeTokenTextImpl.create(BladeBlockTokenId.T_BLADE_TAG, "if"); //NOI18N
        protected static final BladeLexerUtils.BladeTokenText ELSE_IF_TOKEN = BladeLexerUtils.BladeTokenTextImpl.create(BladeBlockTokenId.T_BLADE_TAG, "elseif"); //NOI18N
        protected static final BladeLexerUtils.BladeTokenText ELSE_TOKEN = BladeLexerUtils.BladeTokenTextImpl.create(BladeBlockTokenId.T_BLADE_TAG, "else"); //NOI18N
        protected static final BladeLexerUtils.BladeTokenText END_IF_TOKEN = BladeLexerUtils.BladeTokenTextImpl.create(BladeBlockTokenId.T_BLADE_TAG, "endif"); //NOI18N

        @Override
        public boolean matches(Token<? extends BladeBlockTokenId> token) {
            return matchingToken().matches(token);
        }

        protected abstract BladeLexerUtils.BladeTokenText matchingToken();

    }

    private static final class IfMatcher extends IfConditionMatcher {

        @Override
        protected BladeLexerUtils.BladeTokenText matchingToken() {
            return IF_TOKEN;
        }

        @Override
        public int[] findMatches(Token<? extends BladeBlockTokenId> token, TokenSequence<? extends BladeTopTokenId> topTs) {
            assert token != null;
            assert topTs != null;
            List<OffsetRange> offsetRanges = BladeLexerUtils.findForwardMatching(
                    topTs,
                    IF_TOKEN,
                    END_IF_TOKEN,
                    Arrays.asList(ELSE_IF_TOKEN, ELSE_TOKEN));
            return createMatches(offsetRanges);
        }

    }

    private static final class EndIfMatcher extends IfConditionMatcher {

        @Override
        protected BladeLexerUtils.BladeTokenText matchingToken() {
            return END_IF_TOKEN;
        }

        @Override
        public int[] findMatches(Token<? extends BladeBlockTokenId> token, TokenSequence<? extends BladeTopTokenId> topTs) {
            assert token != null;
            assert topTs != null;
            List<OffsetRange> offsetRanges = BladeLexerUtils.findBackwardMatching(
                    topTs,
                    END_IF_TOKEN,
                    IF_TOKEN,
                    Arrays.asList(ELSE_IF_TOKEN, ELSE_TOKEN));
            return createMatches(offsetRanges);
        }

    }

    private static final class ElseMatcher extends IfConditionMatcher {

        @Override
        protected BladeLexerUtils.BladeTokenText matchingToken() {
            return ELSE_TOKEN;
        }

        @Override
        public int[] findMatches(Token<? extends BladeBlockTokenId> token, TokenSequence<? extends BladeTopTokenId> topTs) {
            assert token != null;
            assert topTs != null;
            List<OffsetRange> offsetRanges = BladeLexerUtils.findBackwardMatching(
                    topTs,
                    END_IF_TOKEN,
                    IF_TOKEN,
                    Arrays.asList(ELSE_IF_TOKEN, ELSE_TOKEN));
            offsetRanges.addAll(BladeLexerUtils.findForwardMatching(topTs,
                    IF_TOKEN,
                    END_IF_TOKEN,
                    Arrays.asList(BladeLexerUtils.BladeTokenText.NONE)));
            return createMatches(offsetRanges);
        }

    }

    private static final class ElseIfMatcher extends IfConditionMatcher {

        @Override
        protected BladeLexerUtils.BladeTokenText matchingToken() {
            return ELSE_IF_TOKEN;
        }

        @Override
        public int[] findMatches(Token<? extends BladeBlockTokenId> token, TokenSequence<? extends BladeTopTokenId> topTs) {
            assert token != null;
            assert topTs != null;
            List<OffsetRange> offsetRanges = BladeLexerUtils.findBackwardMatching(
                    topTs,
                    END_IF_TOKEN,
                    IF_TOKEN,
                    Arrays.asList(ELSE_IF_TOKEN, ELSE_TOKEN));
            offsetRanges.addAll(BladeLexerUtils.findForwardMatching(
                    topTs,
                    IF_TOKEN,
                    END_IF_TOKEN,
                    Arrays.asList(ELSE_IF_TOKEN, ELSE_TOKEN)));
            return createMatches(offsetRanges);
        }

    }

    private static final class StartEndBlockMatcher implements Matcher {
        private static final String END = "end"; //NOI18N
        private final String blockName;

        public StartEndBlockMatcher(String blockName) {
            assert blockName != null;
            this.blockName = blockName;
        }

        @Override
        public boolean matches(Token<? extends BladeBlockTokenId> token) {
            assert token != null;
            return token.id() == BladeBlockTokenId.T_BLADE_TAG
                    && (blockName.equals(token.text().toString()) || (END + blockName).equals(token.text().toString()));
        }

        @Override
        public int[] findMatches(Token<? extends BladeBlockTokenId> token, TokenSequence<? extends BladeTopTokenId> topTs) {
            assert token != null;
            assert topTs != null;
            int[] result = null;
            String tagText = token.text().toString();
            if (tagText.equals(blockName)) {
                List<OffsetRange> offsetRanges = BladeLexerUtils.findForwardMatching(topTs,
                        BladeLexerUtils.BladeTokenTextImpl.create(BladeBlockTokenId.T_BLADE_TAG, blockName),
                        BladeLexerUtils.BladeTokenTextImpl.create(BladeBlockTokenId.T_BLADE_TAG, END + blockName));
                result = createMatches(offsetRanges);
            } else if (tagText.equals(END + blockName)) {
                List<OffsetRange> offsetRanges = BladeLexerUtils.findBackwardMatching(topTs,
                        BladeLexerUtils.BladeTokenTextImpl.create(BladeBlockTokenId.T_BLADE_TAG, END + blockName),
                        BladeLexerUtils.BladeTokenTextImpl.create(BladeBlockTokenId.T_BLADE_TAG, blockName));
                result = createMatches(offsetRanges);
            }
            return result;
        }

    }

    public static final class Factory implements BracesMatcherFactory {

        @Override
        public BracesMatcher createMatcher(MatcherContext context) {
            return new BladeBracesMatcher(context);
        }

    }

}
