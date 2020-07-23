# v0.0.1
- **Initial release**
- PDF, SVG and IR (intermediate representation, as JSON) output formats.
- SWF specification unsupported features:
    - Tags:
        - Supported tags: ShowFrame, DefineShape, PlaceObject, RemoveObject, DefineBitsLossless, DefineBitsJPEG2,
    DefineShape2, PlaceObject2, RemoveObject2, DefineShape3, DefineText2, DefineBitsJPEG3, DefineBitsLossless2, 
    DefineSprite, DefineFont2, PlaceObject3, DefineFont3, DefineShape4. 
        - All other tags are unsupported.
    - Shapes:
        - Radial gradient fill.
        - Tiled or smoothed bitmap fill. (only bitmap fill type 0x41 is supported)
        - Non-solid line fill style.
        - Different start and end line caps.
        - Reflect & repeat gradient spread mode.
        - Linear RGB mode gradient interpolation
    - Fonts & text:
        - Font kernings.
        - Text spans using both DefineFont2 and DefineFont3 fonts within the same DefineText object.
    - Display list:
        - PlaceObject tags with PlaceFlagMove set to 1.
        - Placing a character at a depth where another already resides.
        - Interlaced clip paths.
        - All filters except identity color matrix.
        - Non-image mask (alpha blend mode).
        - Several blend modes have no PDF or SVG equivalent.
- A few known issues, with certain paths notably.
