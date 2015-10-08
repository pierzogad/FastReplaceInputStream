package im.wilk.utils;

import com.sun.xml.internal.fastinfoset.stax.events.CharactersEvent;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class FastReplaceInputStream extends FilterInputStream {

    private static final int MAX_SEARCHED_BYTES = 1024;
    private final Map<byte[], byte[]> replacements;

    private static class OneMap {
        short [] map;
        byte [] replacement;
        int parent;
        byte value;
        OneMap(int parent, byte value) {
            this.parent = parent;
            this.value = value;
        }
    };

    private final List<OneMap> maps = new ArrayList<>();
    private byte [] current;
    private int offset;
    byte [] bytes = new byte [MAX_SEARCHED_BYTES];

    /**
     * Creates a <code>FilterInputStream</code>
     * by assigning the  argument <code>in</code>
     * to the field <code>this.in</code> so as
     * to remember it for later use.
     *
     * @param in the underlying input stream, or <code>null</code> if
     *           this instance is to be created without an underlying stream.
     */
    public FastReplaceInputStream(PushbackInputStream in, Map<byte [], byte []> replace) {
        super(in);
        this.replacements = replace;
        replacements.forEach((k, v) -> populateMaps(k, v));
    }

    private void populateMaps(byte [] searched, byte [] replacement) {
        if (searched.length > MAX_SEARCHED_BYTES) {
            throw new IllegalArgumentException(
                    "Maximum size of single searched text is " + MAX_SEARCHED_BYTES + " bytes.");
        }
        int mapNo = 0;
        int nextMapNo = 0;
        if (maps.size() == 0) {
            maps.add(new OneMap(-1, (byte)0));
        }
        OneMap currentMap;
        for (int i = 0 ; i < searched.length ; i++) {
            int idx = searched[i] & 0xff;
            currentMap = maps.get(mapNo);
            if (currentMap.map == null) {
                currentMap.map = new short [256];
            }
            short nr = currentMap.map[idx];
            nextMapNo = (nr & 0x7FFF);
            if (nextMapNo == 0) {
                nextMapNo = maps.size();
                maps.add(new OneMap(mapNo, (byte) idx));
                currentMap.map[idx] = (short) nextMapNo;
            }
            mapNo = nextMapNo;
        }
        maps.get(nextMapNo).replacement = replacement;
    }

    @Override
    public int read() throws IOException {

        if (current != null && offset < current.length) {
            return current[offset++];
        }
        current = null;
        OneMap currentMap = maps.get(0);

        int ret = in.read();

        if (ret < 0 || currentMap.map[ret] == 0) {
            return ret;
        }

        int stored = 0;

        currentMap = maps.get(currentMap.map[ret]);
        while (true) {
            int idx = in.read();

            if (idx > 0) {
                bytes[stored++] = (byte) idx;
                if (currentMap.map != null && currentMap.map[idx] > 0) {
                    currentMap = maps.get(currentMap.map[idx]);
                    if (currentMap.replacement != null) {
                        current = currentMap.replacement;
                        stored = 0;
                    }
                    continue;
                }
            }

            if (current != null) {
                if (current.length == 0) {
                    current = null;
                    if (stored > 0) {
                        ret = bytes[0];
                        System.arraycopy(bytes, 1, bytes, 0, --stored);
                    } else {
                        ret = -1;
                    }
                    continue;
                }
                offset = 1;
                ret = current[0];
            }

            if (stored > 0) {
                ((PushbackInputStream) in).unread(bytes, 0, stored);
            }
            return ret;
        }
    }

    @Override
    public int read(byte [] cbuf, int off, int len) throws IOException {

        int added = 0;
        while(added < len) {
            int b = read();
            if (b < 0) {
                return (added > 0) ? added : -1;
            }
            cbuf[off++] = (byte)b;
            ++added;
        }
        return added;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PushbackInputStream pushbackInputStream;
        private Map<String, String> replace = new HashMap<>();
        private Charset charset = Charset.forName("UTF8");

        public Builder withInputStream(InputStream in) {
            pushbackInputStream = new PushbackInputStream(in, MAX_SEARCHED_BYTES);
            return this;
        }

        public Builder withReplaceMap(Map<String, String> replaceMap) {
            replace = replaceMap;
            return this;
        }

        public Builder withReplacement(String toReplace, String substitute) {
            replace.put(toReplace, substitute);
            return this;
        }
        
        public Builder withCharset(Charset charset) {
            this.charset = charset;
            return this;
        }
        public FastReplaceInputStream build() {
            Map<byte [], byte []> asBytes = new HashMap<>();
            replace.forEach((k, v) -> asBytes.put(k.getBytes(charset), v.getBytes(charset)));
            return new FastReplaceInputStream(pushbackInputStream, asBytes);
        }
    }
}
