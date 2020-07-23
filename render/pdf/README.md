# PDF output format

This module converts the intermediate representation into PDF files, using Apache PDFBox.
See the [project readme][file-readme] for more information on how to use it.

### Metadata JSON schema

Metadata can be added to output files with the `--metadata` option. The metadata JSON files are
deserialized to the [`PdfMetadata`][file-pdfmetadata] class.

Here's an example file:
```json
{
  "metadata": {
    "Title": "My PDF",
    "Author": "maltaisn",
    "Subject": "Examples",
    "Year": "2020"
  },
  "page_labels": ["I", "II", "III", "1", "2", "3", "4", "5"],
  "outline": [
    {
      "type": "fit_height",
      "title": "First page",
      "page": 0
    },
    {
      "type": "fit_width",
      "title": "My section",
      "page": 1,
      "y": 300,
      "children": [
        {
          "type": "fit_height",
          "title": "Section page 1",
          "page": 2,
          "x": 200
        },
        {
          "type": "fit_xyz",
          "title": "Some precise focus with zoom",
          "page": 3,
          "x": 150,
          "y": 150,
          "zoom": 0.5
        },
        {
          "type": "fit_rect",
          "title": "Some rectangle focus",
          "page": 4,
          "top": 100,
          "left": 100,
          "bottom": 200,
          "right": 200
        }
      ]
    }
  ],
  "outlineOpenLevel": 2
}
```
There's 4 main attributes, each is optional:
- `metadata`: map of metadata tags and values. See the [list of tags][pdf-standard-metadata] normally used.
- `page_labels`: array of custom page labels, in order.
- `outline`: list of outline (bookmark) items. There are 4 types of items:
    - `fit_width`: page will be adjusted to see full width. Optional `y` attribute to specify Y position to go to.
    - `fit_height`: page will be adjusted to see full height. Optional `x` attribute to specify X position to go to.
    - `fit_rect`: rectangle to focus on, specified with `top`, `left`, `bottom` and `right` attributes.
    - `fit_xyz`: position to focus on, specified with `x` and `y` attributes, as well as optional `zoom` 
    (percent zoom / 100).
- `outlineOpenLevel`: level to open the outline by default (in a compliant viewer). Default is 1.


[file-readme]: ../../README.md
[file-pdfmetadata]: src/main/kotlin/com/maltaisn/swfconvert/render/pdf/metadata/PdfMetadata.kt
[pdf-standard-metadata]: https://exiftool.org/TagNames/PDF.html
