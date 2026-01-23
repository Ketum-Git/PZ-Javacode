// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting;

import java.util.ArrayList;
import zombie.util.StringUtils;

public final class ScriptParser {
    public static final String DEFAULT_INDENTATION = "    ";
    private static final StringBuilder stringBuilder = new StringBuilder();

    public static int readBlock(String s, int start, ScriptParser.Block block) {
        int i;
        for (i = start; i < s.length(); i++) {
            if (s.charAt(i) == '{') {
                ScriptParser.Block child = new ScriptParser.Block();
                block.children.add(child);
                block.elements.add(child);
                String header = s.substring(start, i).trim();
                String[] ss = header.split("\\s+");
                child.type = ss[0];
                child.id = ss.length > 1 ? ss[1] : null;
                if (ScriptBucket.getCurrentScriptObject() != null) {
                    child.uid = "UID:" + ScriptBucket.getCurrentScriptObject() + "@" + child.type + "@" + i;
                }

                i = readBlock(s, i + 1, child);
                start = i;
            } else {
                if (s.charAt(i) == '}') {
                    return i + 1;
                }

                if (s.charAt(i) == ',') {
                    ScriptParser.Value value = new ScriptParser.Value();
                    value.string = s.substring(start, i);
                    block.values.add(value);
                    block.elements.add(value);
                    start = i + 1;
                }
            }
        }

        return i;
    }

    public static ScriptParser.Block parse(String s) {
        ScriptParser.Block block = new ScriptParser.Block();
        readBlock(s, 0, block);
        return block;
    }

    public static String stripComments(String totalFile) {
        stringBuilder.setLength(0);
        stringBuilder.append(totalFile);
        int end = stringBuilder.lastIndexOf("*/");

        while (end != -1) {
            int start = stringBuilder.lastIndexOf("/*", end - 1);
            if (start == -1) {
                break;
            }

            int innerCommentEnd = stringBuilder.lastIndexOf("*/", end - 1);

            while (innerCommentEnd > start) {
                int innerCommentStart = start;
                start = stringBuilder.lastIndexOf("/*", start - 2);
                if (start == -1) {
                    break;
                }

                innerCommentEnd = stringBuilder.lastIndexOf("*/", innerCommentStart - 2);
            }

            if (start == -1) {
                break;
            }

            stringBuilder.replace(start, end + 2, "");
            end = stringBuilder.lastIndexOf("*/", start);
        }

        totalFile = stringBuilder.toString();
        stringBuilder.setLength(0);
        stringBuilder.trimToSize();
        return totalFile;
    }

    public static ArrayList<String> parseTokens(String totalFile) {
        ArrayList<String> Tokens = new ArrayList<>();

        while (true) {
            int depth = 0;
            int nextindexOfOpen = 0;
            int nextindexOfClosed = 0;
            if (totalFile.indexOf("}", nextindexOfOpen + 1) == -1) {
                if (!totalFile.trim().isEmpty()) {
                    Tokens.add(totalFile.trim());
                }

                return Tokens;
            }

            do {
                nextindexOfOpen = totalFile.indexOf("{", nextindexOfOpen + 1);
                nextindexOfClosed = totalFile.indexOf("}", nextindexOfClosed + 1);
                if ((nextindexOfClosed >= nextindexOfOpen || nextindexOfClosed == -1) && nextindexOfOpen != -1) {
                    nextindexOfClosed = nextindexOfOpen;
                    depth++;
                } else {
                    nextindexOfOpen = nextindexOfClosed;
                    depth--;
                }
            } while (depth > 0);

            Tokens.add(totalFile.substring(0, nextindexOfOpen + 1).trim());
            totalFile = totalFile.substring(nextindexOfOpen + 1);
        }
    }

    public static class Block implements ScriptParser.BlockElement {
        public String type;
        public String id;
        public final ArrayList<ScriptParser.BlockElement> elements = new ArrayList<>();
        public final ArrayList<ScriptParser.Value> values = new ArrayList<>();
        public final ArrayList<ScriptParser.Block> children = new ArrayList<>();
        private String uid;
        public String comment;

        public String getUid() {
            return this.uid;
        }

        @Override
        public ScriptParser.Block asBlock() {
            return this;
        }

        @Override
        public ScriptParser.Value asValue() {
            return null;
        }

        public boolean isEmpty() {
            return this.elements.isEmpty();
        }

        @Override
        public void prettyPrint(int indent, StringBuilder sb, String eol) {
            this.prettyPrint(indent, sb, eol, "    ");
        }

        @Override
        public void prettyPrint(int indent, StringBuilder sb, String eol, String indentation) {
            sb.append(indentation.repeat(indent));
            if (!StringUtils.isNullOrWhitespace(this.comment)) {
                sb.append(this.comment);
                sb.append(eol);
                sb.append(indentation.repeat(indent));
            }

            sb.append(this.type);
            if (this.id != null) {
                sb.append(" ");
                sb.append(this.id);
            }

            sb.append(eol);
            sb.append(indentation.repeat(indent));
            sb.append('{');
            sb.append(eol);
            this.prettyPrintElements(indent + 1, sb, eol, indentation);
            sb.append(indentation.repeat(indent));
            sb.append('}');
            sb.append(eol);
        }

        public void prettyPrintElements(int indent, StringBuilder sb, String eol) {
            this.prettyPrintElements(indent, sb, eol, "    ");
        }

        public void prettyPrintElements(int indent, StringBuilder sb, String eol, String indentation) {
            ScriptParser.BlockElement prev = null;

            for (ScriptParser.BlockElement element : this.elements) {
                if (element.asBlock() != null && prev != null) {
                    sb.append(eol);
                }

                if (element.asValue() != null && prev instanceof ScriptParser.Block) {
                    sb.append(eol);
                }

                element.prettyPrint(indent, sb, eol, indentation);
                prev = element;
            }
        }

        public ScriptParser.Block addBlock(String type, String id) {
            ScriptParser.Block block = new ScriptParser.Block();
            block.type = type;
            block.id = id;
            this.elements.add(block);
            this.children.add(block);
            return block;
        }

        public ScriptParser.Block getBlock(String type, String id) {
            for (ScriptParser.Block block : this.children) {
                if (block.type.equals(type) && (block.id != null && block.id.equals(id) || block.id == null && id == null)) {
                    return block;
                }
            }

            return null;
        }

        public ScriptParser.Value getValue(String key) {
            for (ScriptParser.Value value1 : this.values) {
                int p = value1.string.indexOf(61);
                if (p > 0 && value1.getKey().trim().equals(key)) {
                    return value1;
                }
            }

            return null;
        }

        public void setValue(String key, String value) {
            ScriptParser.Value value1 = this.getValue(key);
            if (value1 == null) {
                this.addValue(key, value);
            } else {
                value1.string = key + " = " + value;
            }
        }

        public ScriptParser.Value addValue(String key, String value) {
            ScriptParser.Value value1 = new ScriptParser.Value();
            value1.string = key + " = " + value;
            this.elements.add(value1);
            this.values.add(value1);
            return value1;
        }

        public void moveValueAfter(String keyMove, String keyAfter) {
            ScriptParser.Value valueMove = this.getValue(keyMove);
            ScriptParser.Value valueAfter = this.getValue(keyAfter);
            if (valueMove != null && valueAfter != null) {
                this.elements.remove(valueMove);
                this.values.remove(valueMove);
                this.elements.add(this.elements.indexOf(valueAfter) + 1, valueMove);
                this.values.add(this.values.indexOf(valueAfter) + 1, valueMove);
            }
        }
    }

    public interface BlockElement {
        ScriptParser.Block asBlock();

        ScriptParser.Value asValue();

        void prettyPrint(int indent, StringBuilder sb, String eol);

        void prettyPrint(int var1, StringBuilder var2, String var3, String var4);
    }

    public static class Value implements ScriptParser.BlockElement {
        public String string;

        @Override
        public ScriptParser.Block asBlock() {
            return null;
        }

        @Override
        public ScriptParser.Value asValue() {
            return this;
        }

        @Override
        public void prettyPrint(int indent, StringBuilder sb, String eol) {
            this.prettyPrint(indent, sb, eol, "    ");
        }

        @Override
        public void prettyPrint(int indent, StringBuilder sb, String eol, String indentation) {
            sb.append(indentation.repeat(indent));
            sb.append(this.string.trim());
            sb.append(',');
            sb.append(eol);
        }

        public String getKey() {
            int p = this.string.indexOf(61);
            return p == -1 ? this.string : this.string.substring(0, p);
        }

        public String getValue() {
            int p = this.string.indexOf(61);
            return p == -1 ? "" : this.string.substring(p + 1);
        }
    }
}
