# swf-convert

Command line application for converting SWF files to PDF or SVG. This program differs from existing alternatives by the
fact that it's explicitly meant to convert back files that were originally in PDF (or other vector formats) and have 
been converted to SWF using programs such as swf-tools' [pdf2swf][swf-tools] or Adobe InDesign. 

As such, only a subset of the SWF specification is supported, with no support for animations. See 
[limitations](#limitations) for more details. Only static files can be converted. For example, this program could be be 
used to convert a book composed of one SWF file per page. 

To reduce output file size, several features are available:

- Merging similar fonts across files.
- Downsampling images to limit density.
- Detecting and removing duplicate images.
- Using JPG format only, even for images with alpha channel.
- Rasterizing frames with complex paths (PDF only).
- Reusing identical paths (SVG only).
- Compressing output.

### Table of contents

* [Download](#download)
* [Usage](#usage)
    * [Main options](#main-options)
    * [Shared options](#shared-options)
    * [PDF options](#pdf-options)
    * [SVG options](#svg-options)
    * [IR options](#ir-options)
    * [Advanced options](#advanced-options)
* [Examples](#examples)
* [Limitations](#limitations)
  - [PDF limitations](#pdf-limitations)
  - [SVG limitations](#svg-limitations)
* [Building](#building)
* [Contributing](#contributing)
* [Changelog](#changelog)
* [Licenses](#licenses)
* [References](#references)

### Download

[![version][badge-version]][releases-latest]

The latest release can be found under the [Releases][releases-latest] section.
Java 8 or later is required to run this program. The same build should be able to run on all desktop platforms.

## Usage
The JAR file can be run using the following command:
```text
java -jar swf-convert.jar [main options] <output format> <input files> [output options]
```
It can also take a configuration file:
```text
java -jar swf-convert.jar @config.txt
```
Multiple input files or folders can be specified. A file *collection* is created for each file or folder specified.
Some arguments will require to have the same number of arguments as there are input collections.

It is recommended to use `-Xmx` to increase heap size when converting large input collections (>1000 files) since
the program uses a lot of memory, particularly for PDF frame rasterization. As much as 10 GB has been required in my 
case...

### Main options
For use in place of `[main options]` in the above command.

- **`-h`**, **`--help`**: Show help message for the program.
- **`-v`**, **`--version`**: Show version name.
<br><br>
- `--log <level>`: Set minimum log level to show in stdout (off: 0, fatal: 1, error: 2, warn: 3, info: 4, debug: 5, 
all: 6). Logs of all levels are also written to `~/swfconvert/logs`.
- `-s`, `--silent`: Don't display progress during conversion.

### Shared options
For use in place of `[output options]` in the above command. Shared by all output formats.

- **`-h`**, **`--help`**: Show help message for the output format.
<br><br>
- **`-o`**, **`--output <path> [paths]`**: Output files or directories. There must be as many as input file collections. By 
default, output is written to the same path as input. If specifying files, they must have the same extension as the 
desired output format.
- **`-t`**, **`--tempdir <path>`**: Temp directory used for intermediate files. Default temp directory is the same as 
input directory. Temp files are automatically removed after conversion, unless specified otherwise.
<br><br>
- **`-e`**, **`--ignore-empty`**: Ignore empty frames, not generating output for them.

##### Fonts

- **`-g`**, **`--dont-group-fonts`**: Used to disable font grouping (merging compatible fonts in a single font).
- `--keep-font-names`: Used to keep original font names instead of using generic names.

##### Images
- `--keep-duplicate-images`: Used to keep duplicate images with same binary data.
- `--image-format <format>`: Format to use for images, can be one of `default`, `jpg` or `png`. Default is `default`, 
in which case PNG format will be used for DefineBitsLossless tags and JPEG format will be used for DefineBitsJPEG tags. 
tags.
- `--jpeg-quality <quality>`: JPEG image quality between 0 and 100. Default is 75.

##### Downsampling images

- `--downsample-images`: Used to downsample images to limit output density.
- `--downsample-filter <name>`: Filter used to downsample images, can be one of `fast`, `bell`, `bicubic`, `bicubichf`, 
`box`, `bspline`, `hermite`, `lanczos3`, `mitchell` or `triangle`. Default is `lanczos3`.
- `--downsample-min-size <size>`: Minimum size in pixels that images can be downsampled to or from. Must be at least 3 
px, default is 10 px.
- `--max-dpi <dpi>`: If downsampling images, the maximum allowed image density. Default is 200 DPI.

### PDF options
For use in place of `[output options]` in the above command, with the `pdf` output format.
PDF output will produce one page per frame. The frames of all files in a collection are written to the same output file.

- `--no-compress`: Used to disable output PDF compression. 
<br><br>
- `--metadata <file> [files]`: Metadata JSON files used for each input file collection. Use underscore `_` to apply no metadata
 for a particular collection. There must be as many values as there are input collections. See 
 [this section][pdf-metadata-docs] for more information on JSON schema.
- `--dont-optimize-page-labels`: Used to disable page labels optimization (if set in metadata).

##### Rasterization options

- **`--rasterization-enabled`**: Used to enable rasterization of complex frames.
- `--rasterization-threshold <threshold>`: Minimum input file complexity required to perform rasterization, in
(somewhat) arbitrary units. Default is 100,000. Should be tuned manually to see which at which point rasterization 
produces smaller files.
- `--rasterization-dpi <dpi>`: Density in DPI to use to rasterize frames if rasterization is enabled. Default is 200 
DPI.
- `--rasterization-format`: Image format to use for rasterized frames, either `jpg` or `png`. Default is `jpg`.
- `--rasterization-jpeg-quality`: JPEG image quality for rasterization, between 0 and 100. Default is 75.

### SVG options
For use in place of `[output options]` in the above command, with the `svg` output format.
SVG will produce one file per input frame.

- **`-p`**, **`--pretty`**: Used to pretty print output SVG. This also disables a number of optimizations to increase 
readability.
- `--svgz`: Used to output in SVGZ format (gzip compression).
- `--no-prolog`: Used to omit the XML prolog.
<br><br>
- **`--precision`**: Precision of SVG path, position, and dimension values. Default is 1.
- `--transform-precision`: Precision of SVG transform values. Default is 2.
- `--percent-precision`: Precision of SVG percentage values. Default is 2.
<br><br>
- `--images-mode <mode>`: Controls how images are included in SVG, can one of `external` (as files) or `base64` 
(embedded as base64 encoded URLs). Default is `external`.
- `--fonts-mode <mode>`: Controls how fonts are included in SVG, can be one of `external`(as TTF files), `base64` 
(embedded as base64 encoded URLs) or `none` (no fonts, use paths).

When images and fonts are not embedded, the files are placed in the same directory as the output.

### IR options
For use in place of `[output options]` in the above command, with the `ir` output format.
IR will produce one JSON file per input frame.

When converting SWF to the chosen output format, the program first converts the SWF toa SVG-like intermediate 
representation in order to abstract the difficulties presented by the SWF format. For debugging purposes, it's possible 
to output this IR as JSON structures. Images and fonts are written as files.

- **`-p`**, **`--pretty`**: Used to pretty print output JSON.
- `--indent-size <size>`: Indent size used if pretty printing.
<br><br>
- `--y-direction`: Y axis direction, either `up` or `down`. Default is up.

### Advanced options
For use in place of `[output options]` in the above command.

- `-DkeepFonts`, `-DkeepImages`: used to keep temp image and font files.
- Parallelization options to control which steps are run in parallel. Useful when debugging.
    - `-DparallelSwfDecoding`: SWF file decoding.
    - `-DparallelSwfConversion`: conversion to intermediate representation.
    - `-DparallelImageCreation`: creation of image files.
    - `-DparallelFrameRendering`: rendering from IR to output format.
- Draw bounds to draw rectangles around SWF element bounds for debugging:
    - `-DdrawShapeBounds`: for DefineShape tags.
    - `-DdrawTextBounds`: for DefineText tags.
    - `-DdrawClipBounds`: for PlaceObject tags with clipping depth.
    - `-DdebugLineWidth=<width>`: bounds line width in twips, default is 20 twips.
    - `-DdebugLineColor=<color>`: bounds line color, default is green. (color is a `#rrggbb` or `#aarrggbb` hex color)
- Features disable for debugging:
    - `-DdisableClipping`: disable clipping.
    - `-DdisableBlending`: disable blending except alpha blend mode.
    - `-DdisableMasking`: disable alpha blend mode.
- `-DframePadding`: padding to add around frames in inches.
- Custom font scale values, thoroughly explained [here][file-fontscale]. Value is a list of 4 comma-separated values 
enclosed in brackets.
    - `-DfontScale2=[<sx>,<sy>,<usx>,<usy>]`: for DefineFont2 tags.
    - `-DfontScale3=[<sx>,<sy>,<usx>,<usy>]`: for DefineFont3 tags.
- `-DignoreGlyphOffsetsThreshold=<threshold>`: threshold under which custom glyph advances are ignore for DefineText 
tags, in glyph space units (1 em = 1024 glyph space units). Used to reduce output file size. Default is 32.

Keep in mind that these options are meant for advanced use or for debugging purposes. Otherwise common uses include:
- `-DignoreGlyphOffsetsThreshold=0`: keep all original glyph advances.
- `-DfontScale2=[0.05, 0.05, 20, -20]`: font scale used for converting files made with swf-tools.
- `-DkeepFonts` and `-DkeepImages`: extracting fonts or images.

## Examples

**1. PDF to SWF and back.**

Here an [arbitrary PDF][random-pdf] with 92 pages is converted to SWF files with swf-tools' pdf2swf:
```text
pdf2swf -o pages/%.swf -z input.pdf
```
The result is 92 SWF files named 1.swf to 92.swf in the `pages` directory. 
Now let's convert them back to a single PDF file using swf-convert:
```text
java -jar swfconvert.jar pdf pages/ -o report.pdf 
    --image-format jpg --ignore-empty 
    -DfontScale2=[0.05,0.05,20,-20]
```
Additionally we'll ignore empty frames, use only JPG images, and we'll use the special font scale option needed 
for swf-tools. In a few seconds, the `report.pdf` file is created. 40 fonts were created out of the 719 contained in all 
input files, and 333 duplicate images were removed out of 344 images!

**2. Self-contained SVG**

A SWF file with a single frame is converted to a SVG:

```text
java -jar swfconvert.jar --log 4
  svg input.swf -o output.svg
  --downsample-images --max-dpi 30 --image-format jpg
  --images-mode base64 --fonts-mode none
  --transform-precision 2 --no-prolog
```
Images are embedded and paths are used instead of fonts. Precision for the `transform` attribute is also increased to
avoid rounding issues with `fonts-mode none`. To avoid making the SVG too big, images are also downsampled.

## Limitations

swf-convert can only convert *static* files, with no support for animations or actions. Most other 
limitations arise from the fact that I had no test data to test some features with, so I opted for a lazy
implementation. Here's a detailed list of current limitations.

- **Tags:**
    - Supported tags: ShowFrame, DefineShape, PlaceObject, RemoveObject, DefineBitsLossless, DefineBitsJPEG2,
DefineShape2, PlaceObject2, RemoveObject2, DefineShape3, DefineText2, DefineBitsJPEG3, DefineBitsLossless2, 
DefineSprite, DefineFont2, PlaceObject3, DefineFont3, DefineShape4. 
    - All other tags are unsupported.
- **Shapes:**
    - Radial gradient fill.
    - Tiled or smoothed bitmap fill. (only bitmap fill type 0x41 is supported)
    - Non-solid line fill style.
    - Different start and end line caps.
    - Reflect & repeat gradient spread mode.
    - Linear RGB mode gradient interpolation
- **Fonts & text:**
    - Font kernings.
    - Text spans using both DefineFont2 and DefineFont3 fonts within the same DefineText object.
    - DefineFont/DefineFont4 as mentioned previously.
- **Display list:**
    - PlaceObject tags with PlaceFlagMove set to 1.
    - Placing a character at a depth where another already resides.
    - Interlaced clip paths.
    - All filters except identity color matrix.
    - Non-image mask (alpha blend mode).
    
Nearly all of these limitations will result in an exception being thrown, and the conversion will fail.
If you ever have an use case needing support for one of the above, please open an issue and provide the required
test data, I'll do my best to implement it. Again, this tool was implemented lazily as to cover my own use case and
nothing more. More test data will surely allow to fill the holes.

When I say test data, I mean SWF files that can be converted and the result is visually compared with the original.
I unfortunately didn't spend the time implementing automated testing that would do that.

### PDF limitations

- Unsupported blend modes: add, difference, erase, invert, null, subtract.

### SVG limitations

- Unsupported blend modes: add, erase, invert, null, subtract.
- Blending will not work if blending against a background that was clipped. This is due to SVG creating isolated
groups after clipping, meaning the background is reset to transparent black.
- SVG produced is often incompatible with a lot of viewers, only Chrome seems to have full support.
- Please note that SVG is a bit of a second-class citizen in this project since many tools already exist to do the same 
job. But it should still work fine for most purposes.

## Building
The project is built with Gradle, which can be run with:
```text
./gradlew <tasks> [options]
```
Useful tasks are:
- `clean`: clean build results
- `build`: build project
- `detekt`: run detekt analysis on project
- `app:dist`: output fat jar to `app/build/libs`
- `app:run`: run program, using properties from `dev.properties` file:
    - `app-test-working-dir`: working dir to use
    - `app-test-args`: options to use

## Contributing

All contributions are welcome. Please read [contribution guidelines][file-contributing].

## Changelog

View the [`CHANGELOG.md`][file-changelog] file for detailed release notes.

## Licenses
This program is licensed under LGPL v3, see the [license file][file-license] for more details. It uses modified code
for the following libraries, which can be found in the `libsrc` directory:

- [DoubleType][lib-doubletype]: for creating TTF font files - GPL v2
- [transform-swf][lib-transform-swf]: for decoding SWF files - BSD 3-clause

Other libraries are also used:

- [Apache PDFBox][lib-pdfbox]: for creating PDF files - Apache 2.0
- [Dagger][lib-dagger]: dependency injection - Apache 2.0
- [JCommander][lib-jcommander]: CLI - Apache 2.0
- [Java image scaling][lib-image-scaling]: for downsampling images - BSD 3-clause
- [log4j2][lib-log4j2]: logging - Apache 2.0

### References

- [SWF reference][reference-swf]
- [PDF reference][reference-pdf]
- [SVG reference][reference-svg]


[badge-version]: https://img.shields.io/github/v/release/maltaisn/swf-convert?label=version
[file-changelog]: CHANGELOG.md
[file-contributing]: CONTRIBUTING.md
[file-fontscale]: core/src/main/kotlin/com/maltaisn/swfconvert/core/text/FontScale.kt
[file-license]: LICENSE.txt
[lib-dagger]: https://github.com/google/dagger
[lib-doubletype]: https://sourceforge.net/projects/doubletype
[lib-image-scaling]: https://github.com/mortennobel/java-image-scaling
[lib-jcommander]: https://github.com/cbeust/jcommander
[lib-log4j2]: https://logging.apache.org/log4j/2.x
[lib-pdfbox]: https://pdfbox.apache.org/
[lib-transform-swf]: https://github.com/StuartMacKay/transform-swf
[pdf-metadata-docs]: render/pdf/README.md#metadata-json-schema
[random-pdf]: https://apps.who.int/iris/bitstream/handle/10665/332070/9789240005105-eng.pdf
[reference-pdf]: https://www.adobe.com/content/dam/acom/en/devnet/pdf/pdfs/PDF32000_2008.pdf
[reference-svg]: https://www.w3.org/TR/SVG/
[reference-swf]: https://www.adobe.com/content/dam/acom/en/devnet/pdf/swf-file-format-spec.pdf
[releases-latest]: https://github.com/maltaisn/swf-convert/releases/latest
[stack-overflow-path-bug]: https://stackoverflow.com/questions/60572635
[swf-tools]: http://www.swftools.org/
