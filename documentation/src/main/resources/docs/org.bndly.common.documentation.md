# Bundle Documentation
## Introduction
Bndly applications offer the possibility to provide their documentation by themselves. This eases getting into bndly application development, because the documentation does not need to be looked up in a Wiki or PDF.

The basic idea is, that each OSGI bundle within a Bndly application can provide the human readable documentation. This allows to couple code and documentation in terms of integrity.

## How can I add documentation to my bundles?
In order to add documentation to an OSGI bundle, the bundle requires a special header in the bundle manifest. The header `Bndly-Documentation` contains a list of documentation files within the bundle. The files are separated by `;` symbols.

So if your bundle contains two documentation files, the header may look like this:

```
Bndly-Documentation: docs/readme.md;docs/howtos.md
```

The documentation files should be implemented as Markdown. This allows to provide a consistent styling of the documentation. In order to get into Markdown, we recommend to walk through this [tutorial](http://commonmark.org/help/tutorial/).

## How can I add images to my bundle documentation?
Markdown requires images to be loaded via URLs. This is possible with the bundle documentation as well. The problem may be, that a bundle developer is not able to provide a globally available server with the image resources. Therefore the images, that are required to the documentation, can be packaged right into the bundles as well. In order to access those images, the relative paths to the Markdown files within the bundle should be used.

Here is an example:
1. The bundle contains a Markdown file at `docs/readme.md`
2. The bundle contains two images: `docs/image.jpg` and `docs/images/foo.jpg`

In order to add `docs/image.jpg` to the documentation `docs/readme.md`, the following Markdown code should be used:
```
![](image.jpg)
```
The other image should be added by using the following Markdown code:
```
![](images/foo.jpg)
```

![](company_logo.gif)