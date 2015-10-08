package im.wilk.utils;


import org.hamcrest.CoreMatchers;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;

/**
 * Created by andrzej on 07/10/2015.
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