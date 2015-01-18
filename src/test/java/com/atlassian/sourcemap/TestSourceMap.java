package com.atlassian.sourcemap;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestSourceMap {
    @Test
    public void shouldGenerateSourceMap() {
        SourceMap map = new SourceMapImpl();
        map.addMapping(0, 0, 0, 0, "/a.js");
        map.addMapping(0, 5, 0, 10, "/a.js");
        map.addMapping(2, 0, 4, 0, "/a.js");
        map.addMapping(2, 5, 9, 0, "/a.js");
        assertThat(map.generate(), containsString("AAAA,KAAU;;AAIV,KAKA;"));
    }

    @Test
    public void shouldParseSourceMap() {
        SourceMap map = new SourceMapImpl();
        map.addMapping(0, 0, 0, 0, "/a.js");
        map.addMapping(0, 5, 0, 10, "/a.js");
        map.addMapping(2, 0, 4, 0, "/a.js");
        map.addMapping(2, 5, 9, 0, "/a.js");
        String mapAsString = map.generate();

        final SourceMap check = new SourceMapImpl();
        map = new SourceMapImpl(mapAsString);
        map.eachMapping(new SourceMap.EachMappingCallback()
        {
            public void apply(Mapping mapping)
            {
                check.addMapping(mapping);
            }
        });

        assertThat(check.generate(), equalTo(mapAsString));
    }

    @Test
    public void shouldGenerate1to1SourceMap() {
        assertThat(Util.create1to1SourceMap("var a = 1;\nvar b = 2", "/script.js").generate(), containsString("AAAA;AACA;"));
    }

    @Test
    public void shouldOffsetSourceMap() {
        SourceMap map = new SourceMapImpl();
        map.addMapping(0, 0, 0, 0, "/a.js");
        map.addMapping(1, 0, 1, 0, "/a.js");
        map.addMapping(2, 0, 4, 0, "/a.js");
        String originalSourceMap = map.generate();

        SourceMap mapWithOffset = Util.offset(map, 3);
        mapWithOffset = Util.offset(mapWithOffset, 1);

        SourceMap expected = new SourceMapImpl();
        expected.addMapping(4, 0, 0, 0, "/a.js");
        expected.addMapping(5, 0, 1, 0, "/a.js");
        expected.addMapping(6, 0, 4, 0, "/a.js");

        assertThat(mapWithOffset.generate(), equalTo(expected.generate()));
        assertThat(map.generate(), equalTo(originalSourceMap));
    }

    @Test
    public void shouldJoinMaps() {
        String src1 = "var a = 1;\nvar b = 2";
        String src2 = "var a = 1;\nvar b = 2";
        SourceMap map1 = Util.create1to1SourceMap(src1, "/script1.js");
        SourceMap map2 = Util.create1to1SourceMap(src2, "/script2.js");
        String src1WithOffsets = "\n\n" + src1 + "\n\n";
        String src2WithOffsets = "\n\n" + src2 + "\n\n";

        SourceMapJoiner joiner = Util.joiner();
        joiner.addSourceMap(map1, Util.countLines(src1WithOffsets), 2);
        joiner.addSourceMap(map2, Util.countLines(src2WithOffsets), 2);

        Generator expected = new Generator();
        expected.addMapping(2, 0, 0, 0, "/script1.js");
        expected.addMapping(3, 0, 1, 0, "/script1.js");
        expected.addMapping(8, 0, 0, 0, "/script2.js");
        expected.addMapping(9, 0, 1, 0, "/script2.js");
        assertThat(joiner.join().generate(), equalTo(expected.generate()));
    }

    @Test
    public void shouldRebaseMaps() {
        SourceMap base = new SourceMapImpl();
        // Switching first and second lines, leaving third line intact (a, b, c -> b, a, c)
        base.addMapping(0, 0, 1, 0, "/a.js");
        base.addMapping(1, 0, 0, 0, "/a.js");
        base.addMapping(2, 0, 2, 0, "/a.js");

        SourceMap map = new SourceMapImpl();
        // Switching second and third lines (b, a, c -> b, c, a).
        map.addMapping(0, 0, 0, 0, "/a.js");
        map.addMapping(1, 0, 2, 0, "/a.js");
        map.addMapping(2, 0, 1, 0, "/a.js");

        // Final transformations should be (a, b, c -> b, c, a)
        SourceMap expected = new SourceMapImpl();
        expected.addMapping(0, 0, 1, 0, "/a.js");
        expected.addMapping(1, 0, 2, 0, "/a.js");
        expected.addMapping(2, 0, 0, 0, "/a.js");

        SourceMap rebased = Util.rebase(map, base);
        assertThat(rebased.generate(), equalTo(expected.generate()));

        // Also make sure rebase works for map in read state.
        SourceMap rebased2 = Util.rebase(new SourceMapImpl(map.generate()), base);
        assertThat(rebased2.generate(), equalTo(expected.generate()));
    }

    @Test
    public void shouldRebaseIfSomeMappingsAreMissing() {
        SourceMap base = new SourceMapImpl();
        // Shifting source, there will be no first line in the mapping (a, b -> none, a, b)
        base.addMapping(1, 0, 0, 0, "/a.js");
        base.addMapping(2, 0, 1, 0, "/a.js");

        SourceMap map = new SourceMapImpl();
        // Shifting one more time (none, a, b -> none, none, a, b).
        map.addMapping(1, 0, 0, 0, "/a.js");
        map.addMapping(2, 0, 1, 0, "/a.js");
        map.addMapping(3, 0, 2, 0, "/a.js");

        // Final transformations should be (a, b -> none, none, a, b)
        SourceMap expected = new SourceMapImpl();
        expected.addMapping(2, 0, 0, 0, "/a.js");
        expected.addMapping(3, 0, 1, 0, "/a.js");

        SourceMap rebased = Util.rebase(map, base);
        assertThat(rebased.generate(), equalTo(expected.generate()));
    }

    @Test
    public void shouldGenerateAfterRead() {
        SourceMap sourceMap = new SourceMapImpl();
        sourceMap.addMapping(1, 0, 0, 0, "/a.js");
        String sourceMapContent = sourceMap.generate();
        sourceMap = new SourceMapImpl(sourceMapContent);
        assertThat(sourceMapContent, equalTo(sourceMap.generate()));
    }

    @Test
    public void shouldGenerateEmptyMap() {
        assertThat((new SourceMapImpl()).generate(), equalTo(
            "{\n" +
            "  \"version\":3,\n" +
            "  \"sources\":[],\n" +
            "  \"names\":[],\n" +
            "  \"mappings\":\";\"\n" +
            "}"
        ));
    }
}