Parse, generate and combine Source Map files.

# Examples

Parse Source Map

``` Java
// Parsing source map data.
SourceMap map = new SourceMapImpl(
    "{\n" +
    "  \"version\":3,\n" +
    "  \"sources\":[\"/script.js\"],\n" +
    "  \"names\":[],\n" +
    "  \"mappings\":\"AAAA,KAAU;;AAIV,KAKA;\"\n" +
    "}"
);

// Get single mapping.
System.out.println("Single mapping: " + map.getMapping(0, 5));

// Iterate over each mapping.
System.out.println("All mappings:");
map.eachMapping(new SourceMap.EachMappingCallback() { public void apply(Mapping mapping) {
    System.out.println(mapping);
}});
```

Generate Source Map

``` Java
SourceMap map = new SourceMapImpl();

map.addMapping(0, 0, 0, 0, "/script.js");
map.addMapping(0, 5, 0, 10, "/script.js");
map.addMapping(2, 0, 4, 0, "/script.js");
map.addMapping(2, 5, 9, 0, "/script.js");

System.out.println("Generated Source Map: " + map.generate());
```

Add offset for Source Map, needed when some postprocessing applied to generated file. Like wrapping in `try/catch`
block and adding one line before and after.

``` Java
SourceMap map = new SourceMapImpl();
map.addMapping(0, 0, 0, 0, "/script.js");

SourceMap mapWithOffset = Util.offset(map, 1);

System.out.println("Source Map after applying offset: " + mapWithOffset.generateForHumans());
```

Rebase source map, needed when multiple transformation applied to the source. In example
below two transformation applied CoffeeScript -> JavaScript -> Minified JavaScript.

``` Java
SourceMap coffeeToJs = new SourceMapImpl();
coffeeToJs.addMapping(1, 0, 0, 0, "/script.coffee");

SourceMap jsToMinJs = new SourceMapImpl();
jsToMinJs.addMapping(2, 0, 1, 0, "/script.js");

SourceMap minJsToCoffee = Util.rebase(jsToMinJs, coffeeToJs);
System.out.println("SourceMap for CoffeeScript -> JS -> MinJS: " + minJsToCoffee.generateForHumans());
```

Join multiple Source Map in one file, needed when files joined in batch. In example below
two files `a.js` and `b.js` joined in `batch.js`.
 
``` Java
// Source Map for a.js
String a = "var a = 0;";
SourceMap mapA = new SourceMapImpl();
mapA.addMapping(0, 0, 0, 0, "/a.js");

// Source Map for b.js
String b = "var b = 0;";
SourceMap mapB = new SourceMapImpl();
mapB.addMapping(0, 0, 0, 0, "/b.js");

// Source Map for a.js and b.js joined into batch.js.
SourceMapJoiner joiner = Util.joiner();
joiner.addSourceMap(mapA, Util.countLines(a), 0);
joiner.addSourceMap(mapB, Util.countLines(b), 0);
SourceMap batchMap = joiner.join();

System.out.println("Source Map for batch.js: " + batchMap.generateForHumans());
```

Note: `offset`, `join` and `rebase` could be combined in arbitrary order, so it is possible to create Source Map
for minified batch of CoffeeScript files etc.

# Credits

Some code based on the code from Google Closure Compiler.

# License

Released under the Apache License, Version 2.0