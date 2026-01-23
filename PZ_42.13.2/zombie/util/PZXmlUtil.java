// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller.Listener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugType;
import zombie.util.list.PZArrayUtil;

public final class PZXmlUtil {
    private static final ThreadLocal<DocumentBuilder> documentBuilders = ThreadLocal.withInitial(() -> {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            return dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException var1) {
            ExceptionLogger.logException(var1);
            throw new RuntimeException(var1);
        }
    });

    public static Element parseXml(String source) throws PZXmlParserException {
        String fileName = ZomboidFileSystem.instance.resolveFileOrGUID(source);

        Element root;
        try {
            root = parseXmlInternal(fileName);
        } catch (IOException | SAXException var6) {
            throw new PZXmlParserException("Exception thrown while parsing XML file \"" + fileName + "\"", var6);
        }

        root = includeAnotherFile(root, fileName);
        String extendsSource = root.getAttribute("x_extends");
        if (extendsSource != null && !extendsSource.trim().isEmpty()) {
            if (!ZomboidFileSystem.instance.isValidFilePathGuid(extendsSource)) {
                extendsSource = ZomboidFileSystem.instance.resolveRelativePath(fileName, extendsSource);
            }

            Element extendedRoot = parseXml(extendsSource);
            return resolve(root, extendedRoot);
        } else {
            return root;
        }
    }

    private static Element includeAnotherFile(Element root, String fileName) throws PZXmlParserException {
        String includeSource = root.getAttribute("x_include");
        if (includeSource != null && !includeSource.trim().isEmpty()) {
            if (!ZomboidFileSystem.instance.isValidFilePathGuid(includeSource)) {
                includeSource = ZomboidFileSystem.instance.resolveRelativePath(fileName, includeSource);
            }

            Element includeRoot = parseXml(includeSource);
            if (!includeRoot.getTagName().equals(root.getTagName())) {
                return root;
            } else {
                Document doc = createNewDocument();
                Node rootCopy = doc.importNode(root, true);
                Node insertBefore = rootCopy.getFirstChild();

                for (Node node = includeRoot.getFirstChild(); node != null; node = node.getNextSibling()) {
                    if (node instanceof Element) {
                        Element nodeCopy = (Element)doc.importNode(node, true);
                        rootCopy.insertBefore(nodeCopy, insertBefore);
                    }
                }

                rootCopy.normalize();
                return (Element)rootCopy;
            }
        } else {
            return root;
        }
    }

    private static Element resolve(Element in_root, Element in_parent) {
        Document doc = createNewDocument();
        Element result = resolve(in_root, in_parent, doc);
        doc.appendChild(result);
        if (DebugType.Xml.isEnabled()) {
            DebugType.Xml
                .debugln(
                    "PZXmlUtil.resolve> \r\n<Parent>\r\n"
                        + elementToPrettyStringSafe(in_parent)
                        + "\r\n</Parent>\r\n<Child>\r\n"
                        + elementToPrettyStringSafe(in_root)
                        + "\r\n</Child>\r\n<Resolved>\r\n"
                        + elementToPrettyStringSafe(result)
                        + "\r\n</Resolved>"
                );
        }

        return result;
    }

    private static Element resolve(Element in_root, Element in_parent, Document in_resultDoc) {
        if (isTextOnly(in_root)) {
            return (Element)in_resultDoc.importNode(in_root, true);
        } else {
            Element result = in_resultDoc.createElement(in_root.getTagName());
            ArrayList<Attr> resolvedAttributes = new ArrayList<>();
            NamedNodeMap parentAttributes = in_parent.getAttributes();

            for (int iattr = 0; iattr < parentAttributes.getLength(); iattr++) {
                Node attrNode = parentAttributes.item(iattr);
                if (!(attrNode instanceof Attr)) {
                    DebugType.Xml.trace("PZXmlUtil.resolve> Skipping parent.attrib: %s", attrNode);
                } else {
                    Attr attr = (Attr)in_resultDoc.importNode(attrNode, true);
                    resolvedAttributes.add(attr);
                }
            }

            NamedNodeMap childAttributes = in_root.getAttributes();

            for (int iattrx = 0; iattrx < childAttributes.getLength(); iattrx++) {
                Node attrNode = childAttributes.item(iattrx);
                if (!(attrNode instanceof Attr)) {
                    DebugType.Xml.trace("PZXmlUtil.resolve> Skipping attrib: %s", attrNode);
                } else {
                    Attr attr = (Attr)in_resultDoc.importNode(attrNode, true);
                    String attrName = attr.getName();
                    boolean isNewAttrib = true;

                    for (int i = 0; i < resolvedAttributes.size(); i++) {
                        Attr parentAttr = resolvedAttributes.get(i);
                        String parentName = parentAttr.getName();
                        if (parentName.equals(attrName)) {
                            resolvedAttributes.set(i, attr);
                            isNewAttrib = false;
                            break;
                        }
                    }

                    if (isNewAttrib) {
                        resolvedAttributes.add(attr);
                    }
                }
            }

            for (Attr attr : resolvedAttributes) {
                result.setAttributeNode(attr);
            }

            PZXmlUtil.TagTable parentTagTable = PZXmlUtil.TagTable.createTagTable(in_parent, in_resultDoc);
            PZXmlUtil.TagTable childTagTable = PZXmlUtil.TagTable.createTagTable(in_root, in_resultDoc);
            parentTagTable.resolveWith(childTagTable, in_resultDoc);

            for (PZXmlUtil.NamedTagEntry resolvedElement : parentTagTable.resolvedElements) {
                result.appendChild(resolvedElement.element);
            }

            return result;
        }
    }

    private static boolean isTextOnly(Element root) {
        boolean result = false;

        for (Node childNode = root.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            boolean isText = false;
            if (childNode instanceof Text) {
                String textContent = childNode.getTextContent();
                boolean isWhitespace = StringUtils.isNullOrWhitespace(textContent);
                isText = !isWhitespace;
            }

            if (!isText) {
                result = false;
                break;
            }

            result = true;
        }

        return result;
    }

    public static String elementToPrettyStringSafe(Element node) {
        try {
            return elementToPrettyString(node);
        } catch (TransformerException var2) {
            return null;
        }
    }

    public static String elementToPrettyString(Element node) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty("indent", "yes");
        transformer.setOutputProperty("omit-xml-declaration", "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(node);
        transformer.transform(source, result);
        return result.getWriter().toString();
    }

    public static Document createNewDocument() {
        DocumentBuilder dBuilder = documentBuilders.get();
        return dBuilder.newDocument();
    }

    private static Element parseXmlInternal(String fileName) throws SAXException, IOException {
        try {
            Element var6;
            try (
                FileInputStream fis = new FileInputStream(fileName);
                BufferedInputStream adrFile = new BufferedInputStream(fis);
            ) {
                DocumentBuilder dBuilder = documentBuilders.get();
                Document doc = dBuilder.parse(adrFile);
                adrFile.close();
                Element root = doc.getDocumentElement();
                root.normalize();
                var6 = root;
            }

            return var6;
        } catch (SAXException var11) {
            System.err.println("Exception parsing filename: " + fileName);
            throw var11;
        }
    }

    public static void forEachElement(Element root, Consumer<Element> consumer) {
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element childElement) {
                try {
                    consumer.accept(childElement);
                } catch (Exception var5) {
                    throw new PZForEeachElementXmlParseException("Exception thrown parsing xml.", childElement, var5);
                }
            }
        }
    }

    public static <T> T parse(Class<T> type, String source) throws PZXmlParserException {
        Element root = parseXml(source);
        return unmarshall(type, root);
    }

    public static <T> T unmarshall(Class<T> type, Element root) throws PZXmlParserException {
        try {
            Unmarshaller unmarshaller = PZXmlUtil.UnmarshallerAllocator.get(type);
            return (T)unmarshaller.unmarshal(root);
        } catch (JAXBException var4) {
            throw new PZXmlParserException("Exception thrown loading source: \"" + root.getLocalName() + "\". Loading for type \"" + type + "\"", var4);
        }
    }

    public static <T> void write(T data, File outFile) throws TransformerException, IOException, JAXBException {
        Document doc = createNewDocument();
        Marshaller marshaller = PZXmlUtil.MarshallerAllocator.get(data);
        marshaller.marshal(data, doc);
        write(doc, outFile);
    }

    public static void write(Document doc, File outFile) throws TransformerException, IOException {
        Element rootElement = doc.getDocumentElement();
        String content = elementToPrettyString(rootElement);
        FileOutputStream out = new FileOutputStream(outFile, false);
        PrintWriter writer = new PrintWriter(out);
        writer.write(content);
        writer.flush();
        out.flush();
        out.close();
    }

    public static <T> boolean tryWrite(T data, File outFile) {
        try {
            write(data, outFile);
            return true;
        } catch (IOException | JAXBException | TransformerException var3) {
            ExceptionLogger.logException(var3, "Exception thrown writing data: \"" + data + "\". Out file: \"" + outFile + "\"");
            return false;
        }
    }

    public static boolean tryWrite(Document doc, File outFile) {
        try {
            write(doc, outFile);
            return true;
        } catch (IOException | TransformerException var3) {
            ExceptionLogger.logException(var3, "Exception thrown writing document: \"" + doc + "\". Out file: \"" + outFile + "\"");
            return false;
        }
    }

    private static final class MarshallerAllocator {
        private static final ThreadLocal<PZXmlUtil.MarshallerAllocator> instance = ThreadLocal.withInitial(PZXmlUtil.MarshallerAllocator::new);
        private final Map<Class<?>, Marshaller> map = new HashMap<>();

        public static <T> Marshaller get(T type) throws JAXBException {
            return get(type.getClass());
        }

        public static <T> Marshaller get(Class<T> type) throws JAXBException {
            return instance.get().getOrCreate(type);
        }

        private <T> Marshaller getOrCreate(Class<T> type) throws JAXBException {
            Marshaller unmarshaller = this.map.get(type);
            if (unmarshaller == null) {
                JAXBContext context = JAXBContext.newInstance(type);
                unmarshaller = context.createMarshaller();
                unmarshaller.setListener(new Listener() {
                    {
                        Objects.requireNonNull(MarshallerAllocator.this);
                    }

                    @Override
                    public void beforeMarshal(Object source) {
                        super.beforeMarshal(source);
                    }
                });
                this.map.put(type, unmarshaller);
            }

            return unmarshaller;
        }
    }

    private static class NamedTagEntry {
        public String tag;
        public String name;
        public Element element;
        public int index;

        public String getUniqueKey() {
            return StringUtils.isNullOrWhitespace(this.name) ? "node_" + this.index : this.name;
        }
    }

    private static class NamedTags {
        public final HashMap<String, PZXmlUtil.NamedTagEntry> namedTags = new HashMap<>();
    }

    private static class TagTable {
        public final HashMap<String, PZXmlUtil.NamedTags> tags = new HashMap<>();
        public final ArrayList<PZXmlUtil.NamedTagEntry> resolvedElements = new ArrayList<>();

        public static PZXmlUtil.TagTable createTagTable(Element in_root, Document in_resultDoc) {
            PZXmlUtil.TagTable tagTable = new PZXmlUtil.TagTable();

            for (Node childNode = in_root.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
                if (!(childNode instanceof Element)) {
                    DebugType.Xml.trace("PZXmlUtil.resolve> Skipping node: %s", childNode);
                } else {
                    Element nodeElement = (Element)in_resultDoc.importNode(childNode, true);
                    tagTable.addEntry(nodeElement);
                }
            }

            return tagTable;
        }

        public PZXmlUtil.NamedTagEntry getEntry(PZXmlUtil.NamedTagEntry childElement) {
            PZXmlUtil.NamedTags namedTags = this.tags.get(childElement.tag);
            if (namedTags == null) {
                return null;
            } else {
                return StringUtils.isNullOrWhitespace(childElement.name)
                    ? PZArrayUtil.find(namedTags.namedTags.values(), value -> value.index == childElement.index)
                    : namedTags.namedTags.get(childElement.name);
            }
        }

        public void addEntry(Element in_nodeElement) {
            String nodeTag = in_nodeElement.getTagName();
            int tagIndex = this.getTagIndex(nodeTag);
            String nodeName = this.getNodeName(in_nodeElement);
            PZXmlUtil.NamedTagEntry tagEntry = new PZXmlUtil.NamedTagEntry();
            tagEntry.tag = nodeTag;
            tagEntry.name = nodeName;
            tagEntry.element = in_nodeElement;
            tagEntry.index = tagIndex;
            this.addEntry(tagEntry);
        }

        public void addEntry(PZXmlUtil.NamedTagEntry in_entry) {
            this.resolvedElements.add(in_entry);
            PZXmlUtil.NamedTags tagsEntry = this.getOrCreateTableEntry(in_entry.tag);
            tagsEntry.namedTags.put(in_entry.getUniqueKey(), in_entry);
        }

        private PZXmlUtil.NamedTags getOrCreateTableEntry(String in_tag) {
            PZXmlUtil.NamedTags tagsEntry = this.tags.get(in_tag);
            if (tagsEntry == null) {
                tagsEntry = new PZXmlUtil.NamedTags();
                this.tags.put(in_tag, tagsEntry);
            }

            return tagsEntry;
        }

        private String getNodeName(Element nodeElement) {
            return nodeElement.getAttribute("x_name");
        }

        private String getNodeNameFromTagIdx(String nodeTag) {
            int tagIndex = this.getTagIndex(nodeTag);
            return "nodeTag_" + tagIndex;
        }

        private int getTagIndex(String nodeTag) {
            PZXmlUtil.NamedTags namedTagsEntry = this.tags.get(nodeTag);
            int tagIndex = 0;
            if (namedTagsEntry != null) {
                tagIndex = namedTagsEntry.namedTags.size();
            }

            return tagIndex;
        }

        public void resolveWith(PZXmlUtil.TagTable in_childTable, Document in_resultDoc) {
            for (PZXmlUtil.NamedTagEntry childEntry : in_childTable.resolvedElements) {
                PZXmlUtil.NamedTagEntry parentEntry = this.getEntry(childEntry);
                if (parentEntry == null) {
                    this.addEntry(childEntry);
                } else {
                    parentEntry.element = PZXmlUtil.resolve(childEntry.element, parentEntry.element, in_resultDoc);
                }
            }
        }
    }

    private static final class UnmarshallerAllocator {
        private static final ThreadLocal<PZXmlUtil.UnmarshallerAllocator> instance = ThreadLocal.withInitial(PZXmlUtil.UnmarshallerAllocator::new);
        private final Map<Class<?>, Unmarshaller> map = new HashMap<>();

        public static <T> Unmarshaller get(Class<T> type) throws JAXBException {
            return instance.get().getOrCreate(type);
        }

        private <T> Unmarshaller getOrCreate(Class<T> type) throws JAXBException {
            Unmarshaller unmarshaller = this.map.get(type);
            if (unmarshaller != null) {
                return unmarshaller;
            } else {
                JAXBContext context = JAXBContext.newInstance(type);
                unmarshaller = context.createUnmarshaller();
                unmarshaller.setListener(new javax.xml.bind.Unmarshaller.Listener() {
                    {
                        Objects.requireNonNull(UnmarshallerAllocator.this);
                    }

                    @Override
                    public void beforeUnmarshal(Object target, Object parent) {
                        super.beforeUnmarshal(target, parent);
                    }
                });
                this.map.put(type, unmarshaller);
                return unmarshaller;
            }
        }
    }
}
