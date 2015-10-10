package im.wilk.utils.io;


import org.hamcrest.CoreMatchers;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Created on 07/10/2015.
 *
 */
public class FastReplaceInputStreamTest {

    @Test
    public void test() throws IOException {

        String s = "This is foobar";
        ByteArrayInputStream is = new ByteArrayInputStream(s.getBytes());
        FastReplaceInputStream st = FastReplaceInputStream.builder()
                .withInputStream(is)
                .withReplacement("foo", "FOO")
                .withReplacement("bar", "BAR")
                .build();
        BufferedReader r = new BufferedReader(new InputStreamReader(st));
        String res = r.readLine();
        Assert.assertThat(res, Is.is(CoreMatchers.equalTo("This is FOOBAR")));
    }

    @Test
    public void testRollback() throws IOException {

        String s = "1: abcd 2:abce";
        ByteArrayInputStream is = new ByteArrayInputStream(s.getBytes());
        FastReplaceInputStream st = FastReplaceInputStream.builder()
                .withInputStream(is)
                .withReplacement("abcd", "XX")
                .build();
        BufferedReader r = new BufferedReader(new InputStreamReader(st));
        String res = r.readLine();
        Assert.assertThat(res, Is.is(CoreMatchers.equalTo("1: XX 2:abce")));
    }

    @Test
    public void testOverlapingSearches() throws IOException {

        String s = "text is: abcd bcd.";
        ByteArrayInputStream is = new ByteArrayInputStream(s.getBytes());
        FastReplaceInputStream st = FastReplaceInputStream.builder()
                .withInputStream(is)
                .withReplacement("abcd", "long")
                .withReplacement("bcd", "shorter")
                .build();
        BufferedReader r = new BufferedReader(new InputStreamReader(st));
        String res = r.readLine();
        Assert.assertThat(res, Is.is(CoreMatchers.equalTo("text is: long shorter.")));
    }

    @Test
    public void testRemoveLastCharacter() throws IOException {

        String s = "Text\n with tabs\n and some\n control characters\n";
        ByteArrayInputStream is = new ByteArrayInputStream(s.getBytes());
        FastReplaceInputStream st = FastReplaceInputStream.builder()
                .withInputStream(is)
                .withReplacement("\n", "")
                .build();
        BufferedReader r = new BufferedReader(new InputStreamReader(st));
        String res = r.readLine();
        Assert.assertThat(res, Is.is(CoreMatchers.equalTo("Text with tabs and some control characters")));
    }

    @Test
    public void testControlCharacters() throws IOException {

        String s = "Text\twith tabs\r\nand some\ncontrol characters";
        ByteArrayInputStream is = new ByteArrayInputStream(s.getBytes());
        FastReplaceInputStream st = FastReplaceInputStream.builder()
                .withInputStream(is)
                .withReplacement("\t", " ")
                .withReplacement("\r", "")
                .withReplacement("\n", "<br/>")
                .build();
        BufferedReader r = new BufferedReader(new InputStreamReader(st));
        String res = r.readLine();
        Assert.assertThat(res, Is.is(CoreMatchers.equalTo("Text with tabs<br/>and some<br/>control characters")));
    }

    @Test
    public void testOtherCharsets() throws IOException {

        byte [] latinTextAsBytes = {0x41, 0x62, 0x63, 0x20, (byte)0xA9};             // "Abc " + (C) symbol in Latin-1
        byte [] utf8textAsBytes  = {0x41, 0x62, 0x63, 0x20, (byte)0xC2, (byte)0xA9}; // "Abc " + (C) symbol in UTF8

        ByteArrayInputStream is = new ByteArrayInputStream(latinTextAsBytes);
        FastReplaceInputStream st = FastReplaceInputStream.builder()
                .withInputStream(is)
                .withReplacement("©", "(C) - latin1")
                .withCharset(Charset.forName("CP1252"))
                .build();
        BufferedReader r = new BufferedReader(new InputStreamReader(st, Charset.forName("CP1252")));
        String res = r.readLine();
        Assert.assertThat(res, Is.is(CoreMatchers.equalTo("Abc (C) - latin1")));

        is = new ByteArrayInputStream(utf8textAsBytes);
        st = FastReplaceInputStream.builder()
                .withInputStream(is)
                // .withCharset(Charset.forName("UTF8")) UTF8 is default encoding
                .withReplacement("©", "(C) - utf8")
                .build();
        r = new BufferedReader(new InputStreamReader(st, Charset.forName("UTF8")));
        res = r.readLine();
        Assert.assertThat(res, Is.is(CoreMatchers.equalTo("Abc (C) - utf8")));

    }

    @Test
    public void testLongestMatchWins() throws IOException {

        String s = "text is: abcd abc ab.";
        ByteArrayInputStream is = new ByteArrayInputStream(s.getBytes());
        FastReplaceInputStream st = FastReplaceInputStream.builder()
                .withInputStream(is)
                .withReplacement("ab", ">2<")
                .withReplacement("abc", ">3<")
                .withReplacement("abcd", ">4<")
                .build();
        BufferedReader r = new BufferedReader(new InputStreamReader(st));
        String res = r.readLine();
        Assert.assertThat(res, Is.is(CoreMatchers.equalTo("text is: >4< >3< >2<.")));
    }

    @Test
    public void testRemovingStrings() throws IOException {

        String s = "text is: abcd abc ab.";
        ByteArrayInputStream is = new ByteArrayInputStream(s.getBytes());
        FastReplaceInputStream st = FastReplaceInputStream.builder()
                .withInputStream(is)
                .withReplacement("ab", "")
                .build();
        BufferedReader r = new BufferedReader(new InputStreamReader(st));
        String res = r.readLine();
        Assert.assertThat(res, Is.is(CoreMatchers.equalTo("text is: cd c .")));
    }

    @Test
    public void testRemovingStringsHard() throws IOException {

        String s = "text is: abcd abc ab.";
        ByteArrayInputStream is = new ByteArrayInputStream(s.getBytes());
        FastReplaceInputStream st = FastReplaceInputStream.builder()
                .withInputStream(is)
                .withReplacement("ab", "")
                .withReplacement("abcd", "aa")
                .build();
        BufferedReader r = new BufferedReader(new InputStreamReader(st));
        String res = r.readLine();
        Assert.assertThat(res, Is.is(CoreMatchers.equalTo("text is: aa c .")));
    }

    @Test
    public void testEndOfStream() throws IOException {

        String s = "text is: abc";
        ByteArrayInputStream is = new ByteArrayInputStream(s.getBytes());
        FastReplaceInputStream st = FastReplaceInputStream.builder()
                .withInputStream(is)
                .withReplacement("ab", ">2<")
                .withReplacement("abc", ">3<")
                .withReplacement("abcd", ">4<")
                .build();
        BufferedReader r = new BufferedReader(new InputStreamReader(st));
        String res = r.readLine();
        Assert.assertThat(res, Is.is(CoreMatchers.equalTo("text is: >3<")));
    }
}