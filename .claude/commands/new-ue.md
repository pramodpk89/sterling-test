Read `context/sterling-knowledge.md` fully. Then read `src/main/java/com/mycompany/sterling/ue/BeforeCreateOrderValidationUE.java` as the exact pattern to follow.

Arguments: $ARGUMENTS
Expected format: `<ClassName> <ApiName>`
Example: `/new-ue OrderHold createOrder`

If arguments are missing or the format cannot be parsed, stop and respond:
"Usage: /new-ue <ClassName> <ApiName>  (e.g. /new-ue OrderHold createOrder)"

---

Create ONE file: `src/main/java/com/mycompany/sterling/ue/Before<ClassName>UE.java`

Follow these rules exactly — do not deviate:

1. **Package**: `com.mycompany.sterling.ue`

2. **Class declaration**: `public class Before<ClassName>UE` with NO `implements` clause.
   Add this comment on the line above the class: `// In real Sterling: implements YFS<ApiNameInPascalCase>UE`

3. **Entry method**: Derive the method name from <ApiName> — e.g. `createOrder` → `beforeCreateOrder`, `changeOrder` → `beforeChangeOrder`.
   Signature: `public void <methodName>(/* YFSEnvironment env, */ Document inputDoc)`
   Add comment: `// In real Sterling: throws YFSException`

4. **Imports**: Only these — no others:
   ```
   import com.mycompany.sterling.util.SterlingAPIHelper;
   import org.w3c.dom.Document;
   import org.w3c.dom.Element;
   import java.util.logging.Level;
   import java.util.logging.Logger;
   ```

5. **Logger**: `private static final Logger LOG = Logger.getLogger(Before<ClassName>UE.class.getName());`
   Add comment: `// In real Sterling: YFCLogCategory LOG = YFCLogCategory.instance(Before<ClassName>UE.class);`

6. **Null guard**: First line of method — if inputDoc is null, log WARNING with error code `MYCO_<MODULE>_001` and return.

7. **Top-level try/catch**: Wrap the entire method body (after null guard) in `try { ... } catch (Exception e) { LOG.log(Level.SEVERE, "MYCO_<MODULE>_002 - ...", e); }` — swallow, never rethrow.

8. **Method body inside try**: A single line: `// TODO: implement <ClassName> logic here`

9. **XML comment pattern**: Inside the try block, before the TODO, add these comments showing the real Sterling pattern:
   ```java
   // In real Sterling:
   // YFCDocument inDoc = YFCDocument.getDocumentFor(inputDoc);
   // YFCElement orderElem = inDoc.getDocumentElement();
   // String val = orderElem.getAttribute("SomeAttr"); // returns "" not null — check with "".equals(val)
   ```

10. **Error code MODULE**: Derive from <ClassName> using the module tags in Section E of the knowledge file. If no exact match, use `ORDER`.

11. **Fail strategy**: Always fail-open for UEs — the catch block must swallow the exception.

12. **Class-level Javadoc**: Include the `api_list.xml` registration snippet in a `<pre>` block, using the format from Section D of the knowledge file.

---

After writing the file, print the `api_list.xml` registration snippet as plain text so the developer can copy it directly.
