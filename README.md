# Valid Sig

Valid Sig uses the [ZXing-C++][zxing_cpp] ("Zebra Crossing") barcode
scanning library.

Thank you to [Markus Fisch]: https://www.markusfisch.de for the codebase used to build this. 

## Supported Barcode Formats

### Read

ZXing can read many barcode formats:

* [PDF417][pdf417]

But We're really only interested in this one. 

### Generate

ZXing can generate many barcode formats:

* [PDF 417][pdf417]

We will see if this works for visual verification.. TBD


[zxing_cpp]: https://github.com/zxing-cpp/zxing-cpp
[kotlin]: http://kotlinlang.org/
[pdf417]: https://en.wikipedia.org/wiki/PDF417
[ValidSig]: https://github.com/markusfisch/ValidSig