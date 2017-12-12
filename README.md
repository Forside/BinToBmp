BinToBmp
===========

BinToBmp allows you to convert any file into a beautiful bitmap. For this it takes every byte of the file and fills it into a generated bitmap, where every byte indicates an index of a color table (8-bit bitmap).

Usage is simple:

```shell
java -jar BinToBmp.jar <file>
```

This will generate a bitmap to `<file>.bmp`

Upcoming features:

- Memory improvements
- Sequential write to output file instead of generating the whole bitmap in memory before writing
- Ability to sort all pixels by color and put them grouped into the bitmap
- ~~Progress bar~~
- ~~Name output file corresponding to input file~~
- Ability to reverse convert bitmap