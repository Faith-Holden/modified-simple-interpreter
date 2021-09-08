package solution;
import resources.classes.TextIO;
import java.util.HashMap;

public class SimpleInterpreter {

    private static class ParseError extends Exception {
        ParseError(String message) {
            super(message);
        }
    } // end nested class ParseError

    private static HashMap<String, Object > symbolTable;


    public static void main(String[] args) {

        symbolTable = new HashMap<>();
        
        symbolTable.put("pi", Math.PI);
        symbolTable.put("e", Math.E);
        symbolTable.put("log", Functions.LOG);
        symbolTable.put("abs", Functions.ABS);
        symbolTable.put("cos", Functions.COS);
        symbolTable.put("sin", Functions.SIN);
        symbolTable.put("sqrt", Functions.SQRT);
        symbolTable.put("tan", Functions.TAN);


        System.out.println("\n\nEnter commands; press return to end.");
        System.out.println("Commands must have the form:\n");
        System.out.println("      print <expression>");
        System.out.println("  or");
        System.out.println("      let <variable> = <expression>");

        while (true) {
            TextIO.put("\n?  ");
            TextIO.skipBlanks();
            if ( TextIO.peek() == '\n' ) {
                break;
            }
            try {
                String command = TextIO.getWord();
                if (command.equalsIgnoreCase("print"))
                    doPrintCommand();
                else if (command.equalsIgnoreCase("let"))
                    doLetCommand();
                else
                    throw new ParseError("Command must begin with 'print' or 'let'.");
                TextIO.getln();
            }
            catch (ParseError e) {
                System.out.println("\n*** Error in input:    " + e.getMessage());
                System.out.println("*** Discarding input:  " + TextIO.getln());
            }
        }

        System.out.println("\n\nDone.");

    } // end main()


    private enum Functions { SIN, COS, TAN, ABS, SQRT, LOG }

    private static class StandardFunction {
        Functions functionCode;
        StandardFunction(Functions code) {
            functionCode = code;
        }
        double evaluate(double x) {
            switch(functionCode) {
                case SIN:
                    return Math.sin(x);
                case COS:
                    return Math.cos(x);
                case TAN:
                    return Math.tan(x);
                case ABS:
                    return Math.abs(x);
                case SQRT:
                    return Math.sqrt(x);
                default:
                    return Math.log(x);
            }
        }
    }

    private static void doLetCommand() throws ParseError {
        TextIO.skipBlanks();
        if ( ! Character.isLetter(TextIO.peek()) )
            throw new ParseError("Expected variable name after 'let'.");
        String varName = readWord();  // The name of the variable.
        TextIO.skipBlanks();
        if ( TextIO.peek() != '=' )
            throw new ParseError("Expected '=' operator for 'let' command.");
        TextIO.getChar();
        double val = expressionValue();  // The value of the variable.
        TextIO.skipBlanks();
        if ( TextIO.peek() != '\n' )
            throw new ParseError("Extra data after end of expression.");
        symbolTable.put( varName, val );  // Add to symbol table.
        System.out.println("ok");
    }

    private static void doPrintCommand() throws ParseError {
        double val = expressionValue();
        TextIO.skipBlanks();
        if ( TextIO.peek() != '\n' )
            throw new ParseError("Extra data after end of expression.");
        System.out.println("Value is " + val);
    }

    private static double expressionValue() throws ParseError {
        TextIO.skipBlanks();
        boolean negative;  // True if there is a leading minus sign.
        negative = false;
        if (TextIO.peek() == '-') {
            TextIO.getAnyChar();
            negative = true;
        }
        double val;  // Value of the expression.
        val = termValue();  // An expression must start with a term.
        if (negative)
            val = -val; // Apply the leading minus sign
        TextIO.skipBlanks();
        while ( TextIO.peek() == '+' || TextIO.peek() == '-' ) {
                // Read the next term and add it to or subtract it from
                // the value of previous terms in the expression.
            char op = TextIO.getAnyChar();
            double nextVal = termValue();
            if (op == '+')
                val += nextVal;
            else
                val -= nextVal;
            TextIO.skipBlanks();
        }
        return val;
    } // end expressionValue()


    private static double termValue() throws ParseError {
        TextIO.skipBlanks();
        double val;  // The value of the term.
        val = factorValue();  // A term must start with a factor.
        TextIO.skipBlanks();
        while ( TextIO.peek() == '*' || TextIO.peek() == '/' ) {
                // Read the next factor, and multiply or divide
                // the value-so-far by the value of this factor.
            char op = TextIO.getAnyChar();
            double nextVal = factorValue();
            if (op == '*')
                val *= nextVal;
            else
                val /= nextVal;
            TextIO.skipBlanks();
        }
        return val;
    } // end termValue()

    private static double factorValue() throws ParseError {
        TextIO.skipBlanks();
        double val;  // Value of the factor.
        val = primaryValue();  // A factor must start with a primary.
        TextIO.skipBlanks();
        while ( TextIO.peek() == '^' ) {
                // Read the next primary, and exponentiate
                // the value-so-far by the value of this primary.
            TextIO.getChar();
            double nextVal = primaryValue();
            val = Math.pow(val,nextVal);
            if (Double.isNaN(val))
                throw new ParseError("Illegal values for ^ operator.");
            TextIO.skipBlanks();
        }
        return val;
    } // end factorValue()

    private static double primaryValue() throws ParseError {
        TextIO.skipBlanks();
        char ch = TextIO.peek();
        if ( Character.isDigit(ch) ) {
                // The factor is a number.  Read it and
                // return its value.
            return TextIO.getDouble();
        }
        else if ( Character.isLetter(ch) ) {
                // The factor is a variable.  Read its name and
                // look up its value in the symbol table.  If the
                // variable is not in the symbol table, an error
                // occurs.  (Note that the values in the symbol
                // table are objects of type Double.)
            String name = readWord();

            Object obj = symbolTable.get(name);
            if (obj == null){
                throw new ParseError("Unknown variable \"" + name + "\"");
            }else if(obj instanceof Double){
                return (Double) obj;
            }
            else{
                if(obj instanceof  Functions){
                    Functions object = (Functions) obj;
                    StandardFunction function = new StandardFunction(object);
                    return function.evaluate(expressionValue());
                }
                throw new ParseError("Incompatible value");

            }
        }
        else if ( ch == '(' ) {
                // The factor is an expression in parentheses.
                // Return the value of the expression.
            TextIO.getAnyChar();  // Read the "("
            double val = expressionValue();
            TextIO.skipBlanks();
            if ( TextIO.peek() != ')' )
                throw new ParseError("Missing right parenthesis.");
            TextIO.getAnyChar();  // Read the ")"
            return val;
        }
        else if ( ch == '\n' )
            throw new ParseError("End-of-line encountered in the middle of an expression.");
        else if ( ch == ')' )
            throw new ParseError("Extra right parenthesis.");
        else if ( ch == '+' || ch == '-' || ch == '*' || ch == '/')
            throw new ParseError("Misplaced operator.");
        else
            throw new ParseError("Unexpected character \"" + ch + "\" encountered.");
    }  // end primaryValue()

    private static String readWord() {
        String word = "";  // The word.
        char ch = TextIO.peek();
        while (Character.isLetter(ch) || Character.isDigit(ch)) {
            word += TextIO.getChar(); // Add the character to the word.
            ch = TextIO.peek();
        }
        return word;
    }

} // end class SimpleInterpreter
