Read `context/sterling-knowledge.md` fully, paying close attention to the Custom API Pattern in Section B and the `api_list.xml` format in Section D.

Arguments: $ARGUMENTS
Expected format: `<ClassName>`
Example: `/new-api PriceOverride`

If arguments are missing, stop and respond:
"Usage: /new-api <ClassName>  (e.g. /new-api PriceOverride)"

---

Create ONE file: `src/main/java/com/mycompany/sterling/api/<ClassName>API.java`

Follow these rules exactly — do not deviate:

1. **Package**: `com.mycompany.sterling.api`

2. **Class declaration**: `public class <ClassName>API` with NO `extends` clause.
   Add this comment on the line above the class: `// In real Sterling: extends YIFCustomApi`

3. **Entry method**:
   ```java
   // In real Sterling: @Override
   public Document invoke(/* YFSEnvironment env, */ Document inputDoc) {
       // In real Sterling: throws YFSException
   ```

4. **Imports**: Only these — no others:
   ```
   import com.mycompany.sterling.util.SterlingAPIHelper;
   import org.w3c.dom.Document;
   import org.w3c.dom.Element;
   import javax.xml.parsers.DocumentBuilderFactory;
   import java.util.logging.Level;
   import java.util.logging.Logger;
   ```

5. **Logger**: `private static final Logger LOG = Logger.getLogger(<ClassName>API.class.getName());`
   Add comment: `// In real Sterling: YFCLogCategory LOG = YFCLogCategory.instance(<ClassName>API.class);`

6. **Null guard**: If inputDoc is null, log WARNING with `MYCO_<MODULE>_001` and return null.

7. **Top-level try/catch**: Wrap the entire method body (after null guard) in `try { ... } catch (Exception e) { LOG.log(Level.SEVERE, "MYCO_<MODULE>_002 - ...", e); return null; }`

8. **XML comment pattern**: Inside the try block, add these comments showing the real Sterling pattern:
   ```java
   // In real Sterling:
   // YFCDocument inDoc = YFCDocument.getDocumentFor(inputDoc);
   // YFCElement root = inDoc.getDocumentElement();
   // String val = root.getAttribute("SomeAttr"); // returns "" not null — check with "".equals(val)
   ```

9. **Method body**: A single line after the comments: `// TODO: implement <ClassName> logic here`

10. **Return value**: After the TODO, build and return a minimal success document:
    ```java
    // In real Sterling: YFCDocument outDoc = YFCDocument.createDocument("Output");
    // outDoc.getDocumentElement().setAttribute("Status", "SUCCESS");
    // return outDoc.getDocument();
    Document outDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    outDoc.appendChild(outDoc.createElement("Output")).getAttributes()
          .setNamedItem(outDoc.createAttribute("Status"));
    outDoc.getDocumentElement().setAttribute("Status", "SUCCESS");
    return outDoc;
    ```

11. **Error code MODULE**: Derive from <ClassName> using the module tags in Section E of the knowledge file. If no exact match, use `ORDER`.

12. **Class-level Javadoc**: Include the `api_list.xml` registration snippet in a `<pre>` block using the format from Section D.

---

After writing the file, print the `api_list.xml` registration snippet as plain text for easy copying.
