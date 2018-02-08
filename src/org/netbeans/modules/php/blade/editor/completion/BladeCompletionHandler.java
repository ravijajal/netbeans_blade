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
package org.netbeans.modules.php.blade.editor.completion;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.csl.api.CodeCompletionContext;
import org.netbeans.modules.csl.api.CodeCompletionHandler2;
import org.netbeans.modules.csl.api.CodeCompletionResult;
import org.netbeans.modules.csl.api.CompletionProposal;
import org.netbeans.modules.csl.api.Documentation;
import org.netbeans.modules.csl.api.ElementHandle;
import org.netbeans.modules.csl.api.ParameterInfo;
import org.netbeans.modules.csl.spi.DefaultCompletionResult;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.php.blade.editor.completion.BladeCompletionProposal.CompletionRequest;
import org.netbeans.modules.php.blade.editor.completion.BladeDocumentationFactory.FilterDocumentationFactory;
import org.netbeans.modules.php.blade.editor.completion.BladeDocumentationFactory.FunctionDocumentationFactory;
import org.netbeans.modules.php.blade.editor.completion.BladeDocumentationFactory.OperatorDocumentationFactory;
import org.netbeans.modules.php.blade.editor.completion.BladeDocumentationFactory.TagDocumentationFactory;
import org.netbeans.modules.php.blade.editor.completion.BladeDocumentationFactory.TestDocumentationFactory;
import org.netbeans.modules.php.blade.editor.completion.BladeElement.Parameter;
import org.netbeans.modules.php.blade.editor.lexer.BladeBlockTokenId;
import org.netbeans.modules.php.blade.editor.lexer.BladeLexerUtils;
import org.netbeans.modules.php.blade.editor.lexer.BladeTopTokenId;
import org.netbeans.modules.php.blade.editor.lexer.BladeVariableTokenId;
import org.netbeans.modules.php.blade.editor.parsing.BladeParserResult;

public class BladeCompletionHandler implements CodeCompletionHandler2 {
    private static final Logger LOGGER = Logger.getLogger(BladeCompletionHandler.class.getName());
    private static URL documentationUrl = null;
    static {
        try {
            documentationUrl = new URL("http://twig.sensiolabs.org/documentation"); //NOI18N
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.FINE, null, ex);
        }
    }
    private static final Collection<Character> AUTOPOPUP_STOP_CHARS = new TreeSet<>(
            Arrays.asList('=', ';', '+', '-', '*', '/', '%', '(', ')', '[', ']', '{', '}', '?', ' ', '\t', '\n'));

    private static final Set<BladeElement> TAGS = new HashSet<>();
    static {
        BladeDocumentationFactory documentationFactory = TagDocumentationFactory.getInstance();
        TAGS.add(BladeElement.Factory.create("autoescape", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("endautoescape", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("block", documentationFactory, "block ${name}")); //NOI18N
        TAGS.add(BladeElement.Factory.create("endblock", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("do", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("embed", documentationFactory, "embed \"${template.blade}\"")); //NOI18N
        TAGS.add(BladeElement.Factory.create("endembed", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("extends", documentationFactory, "extends \"${template.blade}\"")); //NOI18N
        TAGS.add(BladeElement.Factory.create("filter", documentationFactory, "filter ${name}")); //NOI18N
        TAGS.add(BladeElement.Factory.create("endfilter", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("flush", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("for", documentationFactory, "for ${item} in ${array}")); //NOI18N
        TAGS.add(BladeElement.Factory.create("endfor", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("from", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("if", documentationFactory, "if ${true}")); //NOI18N
        TAGS.add(BladeElement.Factory.create("else", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("elseif", documentationFactory, "elseif ${true}")); //NOI18N
        TAGS.add(BladeElement.Factory.create("endif", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("import", documentationFactory, "import '${page.html}' as ${alias}")); //NOI18N
        TAGS.add(BladeElement.Factory.create("include", documentationFactory, "include '${page.html}'")); //NOI18N
        TAGS.add(BladeElement.Factory.create("macro", documentationFactory, "macro ${name}()")); //NOI18N
        TAGS.add(BladeElement.Factory.create("endmacro", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("raw", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("endraw", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("verbatim", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("endverbatim", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("sandbox", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("endsandbox", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("set", documentationFactory, "set ${variable}")); //NOI18N
        TAGS.add(BladeElement.Factory.create("endset", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("spaceless", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("endspaceless", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("use", documentationFactory, "use \"${page.html}\"")); //NOI18N
        TAGS.add(BladeElement.Factory.create("trans", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("endtrans", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("tesi", documentationFactory)); //NOI18N
        TAGS.add(BladeElement.Factory.create("endtesi", documentationFactory)); //NOI18N
    }

    private static final Set<BladeElement> FILTERS = new HashSet<>();
    static {
        BladeDocumentationFactory documentationFactory = FilterDocumentationFactory.getInstance();
        FILTERS.add(BladeElement.Factory.create("abs", documentationFactory)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("capitalize", documentationFactory)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("convert_encoding", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("'to'"), new Parameter("'from'")}))); //NOI18N
        FILTERS.add(BladeElement.Factory.create("date", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("'format'")}))); //NOI18N
        FILTERS.add(BladeElement.Factory.create("date_modify", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("'modifier'")}))); //NOI18N
        FILTERS.add(BladeElement.Factory.create("default", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("'value'")}))); //NOI18N
        FILTERS.add(BladeElement.Factory.create("escape", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("'html'")}))); //NOI18N
        FILTERS.add(BladeElement.Factory.create("format", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("var")}))); //NOI18N
        FILTERS.add(BladeElement.Factory.create("join", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("'separator'")}))); //NOI18N
        FILTERS.add(BladeElement.Factory.create("json_encode", documentationFactory, Collections.EMPTY_LIST)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("keys", documentationFactory)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("length", documentationFactory)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("lower", documentationFactory)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("merge", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("array")}))); //NOI18N
        FILTERS.add(BladeElement.Factory.create("nl2br", documentationFactory)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("number_format", documentationFactory)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("raw", documentationFactory)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("replace", documentationFactory, Collections.EMPTY_LIST)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("reverse", documentationFactory)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("slice", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("start"), new Parameter("length")}))); //NOI18N
        FILTERS.add(BladeElement.Factory.create("sort", documentationFactory)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("striptags", documentationFactory)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("title", documentationFactory)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("trim", documentationFactory)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("upper", documentationFactory)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("url_encode", documentationFactory, Collections.EMPTY_LIST)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("trans", documentationFactory)); //NOI18N
        FILTERS.add(BladeElement.Factory.create("truncate", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("int")}))); //NOI18N
        FILTERS.add(BladeElement.Factory.create(
                "wordwrap",
                documentationFactory,
                Arrays.asList(new Parameter[] {new Parameter("width"), new Parameter("'break'"), new Parameter("cut")}))); //NOI18N
    }

    private static final Set<BladeElement> FUNCTIONS = new HashSet<>();
    static {
        BladeDocumentationFactory documentationFactory = FunctionDocumentationFactory.getInstance();
        FUNCTIONS.add(BladeElement.Factory.create(
                "attribute",
                documentationFactory,
                Arrays.asList(new Parameter[] {new Parameter("object"), new Parameter("method"), new Parameter("arguments", Parameter.Need.OPTIONAL)}))); //NOI18N
        FUNCTIONS.add(BladeElement.Factory.create("block", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("'name'")}))); //NOI18N
        FUNCTIONS.add(BladeElement.Factory.create("constant", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("'name'")}))); //NOI18N
        FUNCTIONS.add(BladeElement.Factory.create("cycle", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("array"), new Parameter("i")}))); //NOI18N
        FUNCTIONS.add(BladeElement.Factory.create(
                "date",
                documentationFactory,
                Arrays.asList(new Parameter[] {new Parameter("'date'"), new Parameter("'timezone'", Parameter.Need.OPTIONAL)}))); //NOI18N
        FUNCTIONS.add(BladeElement.Factory.create("dump", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("variable", Parameter.Need.OPTIONAL)}))); //NOI18N
        FUNCTIONS.add(BladeElement.Factory.create("parent", documentationFactory, Collections.EMPTY_LIST)); //NOI18N
        FUNCTIONS.add(BladeElement.Factory.create("random", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("'value'")}))); //NOI18N
        FUNCTIONS.add(BladeElement.Factory.create(
                "range",
                documentationFactory,
                Arrays.asList(new Parameter[] {new Parameter("start"), new Parameter("end"), new Parameter("step", Parameter.Need.OPTIONAL)}))); //NOI18N
    }

    private static final Set<BladeElement> TESTS = new HashSet<>();
    static {
        BladeDocumentationFactory documentationFactory = TestDocumentationFactory.getInstance();
        TESTS.add(BladeElement.Factory.create("constant", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("'const'")}))); //NOI18N
        TESTS.add(BladeElement.Factory.create("defined", documentationFactory)); //NOI18N
        TESTS.add(BladeElement.Factory.create("divisibleby", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("number")}))); //NOI18N
        TESTS.add(BladeElement.Factory.create("empty", documentationFactory)); //NOI18N
        TESTS.add(BladeElement.Factory.create("even", documentationFactory)); //NOI18N
        TESTS.add(BladeElement.Factory.create("iterable", documentationFactory)); //NOI18N
        TESTS.add(BladeElement.Factory.create("null", documentationFactory)); //NOI18N
        TESTS.add(BladeElement.Factory.create("odd", documentationFactory)); //NOI18N
        TESTS.add(BladeElement.Factory.create("sameas", documentationFactory, Arrays.asList(new Parameter[] {new Parameter("variable")}))); //NOI18N
    }

    private static final Set<BladeElement> OPERATORS = new HashSet<>();
    static {
        BladeDocumentationFactory documentationFactory = OperatorDocumentationFactory.getInstance();
        OPERATORS.add(BladeElement.Factory.create("in", documentationFactory)); //NOI18N
        OPERATORS.add(BladeElement.Factory.create("as", documentationFactory)); //NOI18N
        OPERATORS.add(BladeElement.Factory.create("is", documentationFactory)); //NOI18N
        OPERATORS.add(BladeElement.Factory.create("and", documentationFactory)); //NOI18N
        OPERATORS.add(BladeElement.Factory.create("or", documentationFactory)); //NOI18N
        OPERATORS.add(BladeElement.Factory.create("not", documentationFactory)); //NOI18N
        OPERATORS.add(BladeElement.Factory.create("b-and", documentationFactory)); //NOI18N
        OPERATORS.add(BladeElement.Factory.create("b-or", documentationFactory)); //NOI18N
        OPERATORS.add(BladeElement.Factory.create("b-xor", documentationFactory)); //NOI18N
    }

    @Override
    public CodeCompletionResult complete(CodeCompletionContext codeCompletionContext) {
        final List<CompletionProposal> completionProposals = new ArrayList<>();
        ParserResult parserResult = codeCompletionContext.getParserResult();
        if (parserResult instanceof BladeParserResult) {
            BladeParserResult bladeParserResult = (BladeParserResult) parserResult;
            CompletionRequest request = new CompletionRequest();
            request.prefix = codeCompletionContext.getPrefix();
            int caretOffset = codeCompletionContext.getCaretOffset();
            String properPrefix = getPrefix(bladeParserResult, caretOffset, true);
            request.anchorOffset = caretOffset - (properPrefix == null ? 0 : properPrefix.length());
            request.parserResult = bladeParserResult;
            request.context = BladeCompletionContextFinder.find(request.parserResult, caretOffset);
            doCompletion(completionProposals, request);
        }
        return new DefaultCompletionResult(completionProposals, false);
    }

    private void doCompletion(final List<CompletionProposal> completionProposals, final CompletionRequest request) {
        switch (request.context) {
            case FILTER:
                completeFilters(completionProposals, request);
                break;
            case BLOCK:
                completeAll(completionProposals, request);
                break;
            case VARIABLE:
                completeFilters(completionProposals, request);
                completeFunctions(completionProposals, request);
                completeTests(completionProposals, request);
                completeOperators(completionProposals, request);
                break;
            case ALL:
                completeAll(completionProposals, request);
                break;
            case NONE:
                break;
            default:
                completeAll(completionProposals, request);
        }
    }

    private void completeAll(final List<CompletionProposal> completionProposals, final CompletionRequest request) {
        completeTags(completionProposals, request);
        completeFilters(completionProposals, request);
        completeFunctions(completionProposals, request);
        completeTests(completionProposals, request);
        completeOperators(completionProposals, request);
    }

    private void completeTags(final List<CompletionProposal> completionProposals, final CompletionRequest request) {
        for (BladeElement tag : TAGS) {
            if (startsWith(tag.getName(), request.prefix)) {
                completionProposals.add(new BladeCompletionProposal.TagCompletionProposal(tag, request));
            }
        }
    }

    private void completeFilters(final List<CompletionProposal> completionProposals, final CompletionRequest request) {
        for (BladeElement parameterizedItem : FILTERS) {
            if (startsWith(parameterizedItem.getName(), request.prefix)) {
                completionProposals.add(new BladeCompletionProposal.FilterCompletionProposal(parameterizedItem, request));
            }
        }
    }

    private void completeFunctions(final List<CompletionProposal> completionProposals, final CompletionRequest request) {
        for (BladeElement parameterizedItem : FUNCTIONS) {
            if (startsWith(parameterizedItem.getName(), request.prefix)) {
                completionProposals.add(new BladeCompletionProposal.FunctionCompletionProposal(parameterizedItem, request));
            }
        }
    }

    private void completeTests(final List<CompletionProposal> completionProposals, final CompletionRequest request) {
        for (BladeElement test : TESTS) {
            if (startsWith(test.getName(), request.prefix)) {
                completionProposals.add(new BladeCompletionProposal.TestCompletionProposal(test, request));
            }
        }
    }

    private void completeOperators(final List<CompletionProposal> completionProposals, final CompletionRequest request) {
        for (BladeElement operator : OPERATORS) {
            if (startsWith(operator.getName(), request.prefix)) {
                completionProposals.add(new BladeCompletionProposal.OperatorCompletionProposal(operator, request));
            }
        }
    }

    @Override
    public Documentation documentElement(ParserResult parserResult, ElementHandle elementHandle, Callable<Boolean> cancel) {
        Documentation result = null;
        if (elementHandle instanceof BladeElement) {
            result = Documentation.create(((BladeElement) elementHandle).getDocumentation().asText(), documentationUrl);
        }
        return result;
    }

    @Override
    public String document(ParserResult pr, ElementHandle eh) {
        return null;
    }

    @Override
    public ElementHandle resolveLink(String string, ElementHandle eh) {
        return null;
    }

    @Override
    public String getPrefix(ParserResult info, int offset, boolean upToOffset) {
        return PrefixResolver.create(info, offset, upToOffset).resolve();
    }

    @Override
    public QueryType getAutoQuery(JTextComponent component, String typedText) {
        QueryType result = QueryType.ALL_COMPLETION;
        if (typedText.length() == 0) {
            result = QueryType.NONE;
        } else {
            char lastChar = typedText.charAt(typedText.length() - 1);
            if (AUTOPOPUP_STOP_CHARS.contains(Character.valueOf(lastChar))) {
                result = QueryType.STOP;
            } else {
                Document document = component.getDocument();
                int offset = component.getCaretPosition();
                TokenSequence<? extends TokenId> ts = BladeLexerUtils.getBladeMarkupTokenSequence(document, offset);
                if (ts == null) {
                    result = QueryType.STOP;
                }
            }
        }
        return result;
    }

    @Override
    public String resolveTemplateVariable(String string, ParserResult pr, int i, String string1, Map map) {
        return null;
    }

    @Override
    public Set<String> getApplicableTemplates(Document dcmnt, int i, int i1) {
        return null;
    }

    @Override
    public ParameterInfo parameters(ParserResult pr, int i, CompletionProposal cp) {
        return ParameterInfo.NONE;
    }

    private static boolean startsWith(String theString, String prefix) {
        return prefix.length() == 0 ? true : theString.toLowerCase().startsWith(prefix.toLowerCase());
    }

    private static final class PrefixResolver {
        private final ParserResult info;
        private final int offset;
        private final boolean upToOffset;
        private String result = "";

        static PrefixResolver create(ParserResult info, int offset, boolean upToOffset) {
            return new PrefixResolver(info, offset, upToOffset);
        }

        private PrefixResolver(ParserResult info, int offset, boolean upToOffset) {
            this.info = info;
            this.offset = offset;
            this.upToOffset = upToOffset;
        }

        String resolve() {
            TokenHierarchy<?> th = info.getSnapshot().getTokenHierarchy();
            if (th != null) {
                processHierarchy(th);
            }
            return result;
        }

        private void processHierarchy(TokenHierarchy<?> th) {
            TokenSequence<BladeTopTokenId> tts = th.tokenSequence(BladeTopTokenId.language());
            if (tts != null) {
                processTopSequence(tts);
            }
        }

        private void processTopSequence(TokenSequence<BladeTopTokenId> tts) {
            tts.move(offset);
            if (tts.moveNext() || tts.movePrevious()) {
                TokenSequence<? extends TokenId> ts = tts.embedded(BladeBlockTokenId.language());
                if (ts == null) {
                    ts = tts.embedded(BladeVariableTokenId.language());
                }
                processSequence(ts);
            }
        }

        private void processSequence(TokenSequence<? extends TokenId> ts) {
            if (ts != null) {
                processValidSequence(ts);
            }
        }

        private void processValidSequence(TokenSequence<? extends TokenId> ts) {
            ts.move(offset);
            if (ts.moveNext() || ts.movePrevious()) {
                processToken(ts);
            }
        }

        private void processToken(TokenSequence<? extends TokenId> ts) {
            if (ts.offset() == offset) {
                ts.movePrevious();
            }
            Token<?> token = ts.token();
            if (token != null) {
                processSelectedToken(ts);
            }
        }

        private void processSelectedToken(TokenSequence<? extends TokenId> ts) {
            TokenId id = ts.token().id();
            if (isValidTokenId(id)) {
                createResult(ts);
            }
        }

        private void createResult(TokenSequence<? extends TokenId> ts) {
            if (upToOffset) {
                String text = ts.token().text().toString();
                result = text.substring(0, offset - ts.offset());
            }
        }

        private static boolean isValidTokenId(TokenId id) {
            return BladeBlockTokenId.T_BLADE_TAG.equals(id) || BladeBlockTokenId.T_BLADE_NAME.equals(id) || BladeVariableTokenId.T_BLADE_NAME.equals(id);
        }

    }

}
