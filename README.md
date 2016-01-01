wmf2png: WMF to PNG converter
=============================

# About

**wmf2png** converts Windows Meta File to PNG (Portable Network Graphics) format, based on WmfView 0.8 implemented in Java.

# Requirement

* Java Runtime Environment

Tested on Oracle JDK 1.8 on OSX 10.9.

# Install

Project directory contains pre-build binary, so just copy it to install in your system.

    $ git clone https://edy555.github.com/wmf2png
    $ sudo cp wmf2png/wmf2png /usr/local/bin
    $ sudo chmod +x /usr/local/bin/wmf2png

# Usage

    $ wmf2png foo.wmf

This will generate foo.png from foo.wmf.

# Build from source

If you will modify code, you need following. Please consult Makefile.

* Requires Java Development Kit.
* Just run make

# Credit

* Great original work is WmfView 0.8 (Albrecht Kleine) http://sax.sax.de/~adlibit/
(Original site is missing, but archived in follows https://web.archive.org/web/20060209035602/http://sax.sax.de/~adlibit/)
* Modified as standalone executable by @edy555 (31/Dec 2015)
