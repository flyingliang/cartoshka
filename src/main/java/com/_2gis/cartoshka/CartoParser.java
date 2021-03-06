package com._2gis.cartoshka;

import com._2gis.cartoshka.function.Functions;
import com._2gis.cartoshka.scanner.Token;
import com._2gis.cartoshka.scanner.TokenType;
import com._2gis.cartoshka.tree.*;
import com._2gis.cartoshka.tree.expression.*;
import com._2gis.cartoshka.tree.expression.literal.Boolean;
import com._2gis.cartoshka.tree.expression.literal.*;

import java.io.Reader;
import java.util.*;

/**
 * CartoCSS parser for Java. Builds AST-trees for given styles.
 * All AST-trees parsed by one instance have a common symbol table.
 * This parser does not verify CartoCSS properties.
 */
public final class CartoParser extends com._2gis.cartoshka.scanner.Scanner {
    private static final int MaxArguments = 32;

    private final Map<String, Function> functions;

    private SymbolTable symbolTable;

    public CartoParser() {
        this.functions = new HashMap<>();
        this.symbolTable = new SymbolTable();
        for (Function function : Functions.BUILTIN_FUNCTIONS) {
            addOrReplaceFunction(function);
        }
    }

    /**
     * Parse an input data into an abstract syntax tree.
     *
     * @param name   Source name (e.g. file path, or URL). This parameter is used for debug.
     * @param reader Source reader
     * @return An instance of {@link Block}, which is root of an abstract syntax tree.
     * @throws CartoshkaException
     */
    public Block parse(String name, Reader reader) {
        initialize(name, reader);
        return parseBlock();
    }

    /**
     * Adds a custom function or replaces built-in.
     *
     * @param function An instance of {@link Function}.
     */
    public void addOrReplaceFunction(Function function) {
        functions.put(function.getName(), function);
    }

    private Block parseBlock() {
        List<Node> nodes = new ArrayList<>();
        Location location = peek().getLocation();
        while (peek().getType() != TokenType.EOS && peek().getType() != TokenType.RBRACE) {
            switch (peek().getType()) {
                case VARIABLE:
                    Rule variable = symbolTable.setVariable(parseRule());
                    nodes.add(variable);
                    break;

                case IDENTIFIER:
                    nodes.add(parseRule());
                    break;

                default:
                    symbolTable = symbolTable.createNested();
                    nodes.add(parseRuleSet());
                    symbolTable = symbolTable.getParent();
            }
        }

        return new Block(location, nodes, symbolTable);
    }

    private Rule parseRule() {
        Token token = expect(TokenType.VARIABLE, TokenType.IDENTIFIER);
        expect(TokenType.COLON);
        Value value = parseValue();
        expect(TokenType.SEMICOLON);
        return new Rule(token.getLocation(), token.getText(), value, token.getType() == TokenType.VARIABLE);
    }

    // A Value is a comma-delimited list of Expressions
    // In a Rule, a Value represents everything after the `:`,
    // and before the `;`.
    private Value parseValue() {
        Collection<Expression> expressions = new ArrayList<>();
        Location location = peek().getLocation();
        while (true) {
            Expression expression = parseExpression();
            expressions.add(expression);
            if (peek().getType() != TokenType.COMMA) {
                break;
            }

            expect(TokenType.COMMA); // consume comma
        }

        return new Value(location, expressions);
    }

    // Expressions either represent mathematical operations,
    // or white-space delimited Entities.  @var * 2
    private Expression parseExpression() {
        return parseBinaryExpression(1);
    }

    private Expression parseBinaryExpression(int precedence) {
        Expression result = parseUnaryExpression();
        for (int precedence1 = peek().getType().getPrecedence(); precedence1 >= precedence; precedence1--) {
            while (peek().getType().getPrecedence() == precedence1) {
                Token op = expect(TokenType.ADD, TokenType.SUB, TokenType.MUL, TokenType.DIV, TokenType.MOD);
                Expression right = parseBinaryExpression(precedence1 + 1);
                result = new BinaryOperation(op.getLocation(), op.getType(), result, right);
            }
        }

        return result;
    }

    private Expression parseUnaryExpression() {
        switch (peek().getType()) {
            case ADD:
                next();
                return parseUnaryExpression();

            case SUB:
                Token op = next();
                Expression expression = parseUnaryExpression();
                return new UnaryOperation(op.getLocation(), op.getType(), expression);

            default:
                return parsePrimaryExpression();
        }
    }

    private Expression parsePrimaryExpression() {
        switch (peek().getType()) {
            case NUMBER_LITERAL:
                Token number = next();
                return new Numeric(number.getLocation(), Double.valueOf(number.getText()), number.getText().indexOf('.') >= 0);

            case TRUE_LITERAL:
                return new Boolean(next().getLocation(), true);

            case FALSE_LITERAL:
                return new Boolean(next().getLocation(), false);

            case VARIABLE:
                Token variable = next();
                return new Variable(variable.getLocation(), symbolTable, variable.getText());

            case DIMENSION_LITERAL:
                return parseDimension();

            case HASH:
                return parseHexColor();

            case LBRACK:
                expect(TokenType.LBRACK);
                Token field = next();
                expect(TokenType.RBRACK);
                return new Field(field.getLocation(), field.getText());

            case LPAREN:
                expect(TokenType.LPAREN);
                Expression expression = parseExpression();
                expect(TokenType.RPAREN);
                return expression;

            case STRING_LITERAL:
            case URL:
                return parseString(peek().getType() == TokenType.URL);

            case MAP_KEYWORD:
            case ZOOM_KEYWORD:
            case IDENTIFIER:
                Token identifier = next();
                if (peek().getType() == TokenType.LPAREN) {
                    return parseFunctionCall();
                } else if (Colors.Strings.containsKey(identifier.getText())) {
                    int[] rgba = Colors.Strings.get(identifier.getText());
                    return Color.fromRGBA(identifier.getLocation(), rgba[0], rgba[1], rgba[2], rgba[3]);
                }

                return new Text(identifier.getLocation(), identifier.getText(), false, true);

            default:
                throw CartoshkaException.unexpectedToken(peek());
        }
    }

    private Expression parseString(boolean isURL) {
        Token token = next();
        ExpandableText text = new ExpandableText(token.getLocation(), symbolTable, token.getText(), isURL);
        if (text.isPlain()) {
            return text.getExpressions().get(0);
        }

        return text;
    }

    private Call parseFunctionCall() {
        Token token = current();
        Function function = functions.get(token.getText());
        if (function != null) {
            Collection<Expression> arguments = parseArgumentsExpression();
            if (function.getArgumentCount() != arguments.size()) {
                throw CartoshkaException.functionIncorrectArgumentCount(token.getLocation(), function.getArgumentCount(), arguments.size());
            }

            return new Call(token.getLocation(), function, arguments);
        }

        throw CartoshkaException.undefinedFunction(token.getLocation());
    }

    private Dimension parseDimension() {
        Token token = expect(TokenType.DIMENSION_LITERAL);
        String value = token.getText();
        for (int i = 2; i >= 1; i--) {
            String unit = value.substring(value.length() - i);
            if (DIMENSION_UNITS.contains(unit)) {
                String num = value.substring(0, value.length() - i);
                return new Dimension(token.getLocation(), Double.valueOf(num), unit, num.indexOf('.') >= 0);
            }
        }

        throw CartoshkaException.invalidDimensionUnit(token.getLocation());
    }

    private Color parseHexColor() {
        Token token = expect(TokenType.HASH);
        String text = token.getText();
        try {
            if (text.length() == 3) {
                int r = Integer.parseInt(text.substring(0, 1) + text.substring(0, 1), 16);
                int g = Integer.parseInt(text.substring(1, 2) + text.substring(1, 2), 16);
                int b = Integer.parseInt(text.substring(2, 3) + text.substring(2, 3), 16);
                return Color.fromRGBA(token.getLocation(), r, g, b, 1.0);
            } else if (text.length() == 6) {
                int r = Integer.parseInt(text.substring(0, 2), 16);
                int g = Integer.parseInt(text.substring(2, 4), 16);
                int b = Integer.parseInt(text.substring(4, 6), 16);
                return Color.fromRGBA(token.getLocation(), r, g, b, 1.0);
            }
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            // do nothing
        }

        throw CartoshkaException.invalidFormat(token.getLocation());
    }

    private Collection<Expression> parseArgumentsExpression() {
        Collection<Expression> args = new ArrayList<>();
        expect(TokenType.LPAREN);
        boolean done = peek().getType() == TokenType.RPAREN;
        while (!done) {
            args.add(parseExpression());
            if (args.size() > MaxArguments) {
                throw CartoshkaException.functionTooManyArguments(current().getLocation());
            }

            done = peek().getType() == TokenType.RPAREN;
            if (!done) {
                expect(TokenType.COMMA);
            }
        }

        expect(TokenType.RPAREN);
        return args;
    }

    private Node parseRuleSet() {
        // selectors block
        Location location = peek().getLocation();
        List<Selector> selectors = parseSelectors();
        expect(TokenType.LBRACE);
        Block block = parseBlock();
        expect(TokenType.RBRACE);
        return new Ruleset(location, selectors, block);
    }

    // Selectors are made out of one or more Elements, see above.
    private List<Selector> parseSelectors() {
        List<Selector> selectors = new ArrayList<>();
        while (true) {
            selectors.add(parseSelector());
            if (peek().getType() != TokenType.COMMA) {
                break;
            }

            expect(TokenType.COMMA); // consume comma
        }

        return selectors;
    }

    private Selector parseSelector() {
        // (element | zoom | filter)+ attachment?
        boolean done = false;
        Collection<Element> elements = new ArrayList<>();
        Collection<Zoom> zooms = new ArrayList<>();
        Collection<Filter> filters = new ArrayList<>();
        String attachment = null;
        Location location = peek().getLocation();
        int segments = 0;
        while (!done) {
            switch (peek().getType()) {
                case HASH:
                case PERIOD:
                case MUL:
                case MAP_KEYWORD:
                    elements.add(parseElement());
                    break;

                case LBRACK:
                    expect(TokenType.LBRACK);
                    if (peek().getType() == TokenType.ZOOM_KEYWORD) {
                        zooms.add(parseZoom());
                    } else {
                        filters.add(parseFilter());
                    }

                    break;

                case ATTACHMENT:
                    if (attachment != null) {
                        throw CartoshkaException.unexpectedToken(peek());
                    }

                    attachment = next().getText();
                    break;

                case LBRACE:
                case COMMA:
                    if (segments == 0) {
                        throw CartoshkaException.selectorWithoutSegments(peek().getLocation());
                    }

                    done = true;
                    break;

                default:
                    throw CartoshkaException.unexpectedToken(peek());
            }

            segments++;
        }

        return new Selector(location, elements, filters, zooms, attachment);
    }

    // Elements are the building blocks for Selectors. They consist of
    // an element name, such as a tag a class, or `*`.
    private Element parseElement() {
        Token token = expect(TokenType.HASH, TokenType.PERIOD, TokenType.MUL, TokenType.MAP_KEYWORD);
        switch (token.getType()) {
            case HASH:
                return new Element(token.getLocation(), token.getText(), Element.ElementType.ID);

            case PERIOD:
                return new Element(token.getLocation(), next().getText(), Element.ElementType.CLASS);

            case MUL:
                return new Element(token.getLocation(), "*", Element.ElementType.WILDCARD);
        }

        return new Element(token.getLocation(), token.getText(), Element.ElementType.MAP);
    }

    private Zoom parseZoom() {
        Token zoom = expect(TokenType.ZOOM_KEYWORD);
        Token op = expect(TokenType.EQ, TokenType.NE, TokenType.LT, TokenType.GT, TokenType.LTE, TokenType.GTE);
        Expression expression = parseExpression();
        expect(TokenType.RBRACK);
        return new Zoom(zoom.getLocation(), op.getType(), expression);
    }

    private Filter parseFilter() {
        Location location = peek().getLocation();
        Expression left = parsePrimaryExpression();
        Token op = expect(TokenType.EQ, TokenType.NE, TokenType.LT, TokenType.GT, TokenType.LTE, TokenType.GTE);
        Expression right = parseExpression();
        expect(TokenType.RBRACK);
        if (left.type() == NodeType.TEXT) {
            left = new Field(left.getLocation(), left.toString());
        }

        return new Filter(location, op.getType(), left, right);
    }

    private Token expect(TokenType... types) {
        Token token = next();
        for (TokenType type : types) {
            if (token.getType() == type) {
                return token;
            }
        }

        throw CartoshkaException.unexpectedToken(token);
    }
}