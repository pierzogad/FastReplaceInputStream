/**
 * Provides the classes necessary to create an InputStream wrapper that allows
 * substitution of one strings with another.
 *
 * It can be used for initial cleansing of input data based on dictionary e.g.:
 * <ul>
 *      <li>\n =&gt; &lt;br&gt;</li>
 *      <li>&amp; =&gt; &amp;amp;</li>
 * </ul>
 *
 * Main focus in this implementation was to minimize performance impact even for a big transformation dictionaries.<br>
 *
 * Additional feature is ability to replace longest match - i.e. given dictionary:
 * <ul>
 *      <li>abc =&gt; three</li>
 *      <li>abcd =&gt; four</li>
 * </ul>
 *
 * text: <br>
 *     "This is as simple as abc or even abcd" will be transformed to <br>
 *     "This is as simple as <b>three</b> or even <b>four</b>"
 *
 * usage example:
 * <pre>
 * ...
 * InputStream someInputStream = new FileInputStream(new File("filename"));
 *
 * FastReplaceInputStream wrappedInputStream = FastReplaceInputStream.builder()
 *   .withReplacement("\r\n", "\n")
 *   .withReplacement("Mr. ", "Sir ")
 *   .withReplacement("Mrs. ", "Madame ")
 *   .build(someInputStream);
 *
 * InputStreamReader reader  = new InputStreamReader(wrappedInputStream);
 * ...
 * </pre>
 *
 * It will allow reading the file forcing Unix style newlines and replacing in flight set strings.
 *
 *
 *
 */
package im.wilk.utils.io;
