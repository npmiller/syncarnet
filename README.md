NoteSync
===========
TODO-list application with peer-to-peer synchronisation (without central server).

To compile the code into an ``apk``, place yourself in the application directory and use the ant tool:
```bash
ant debug
```
You can then find the ``apk`` in the bin folder.

To run the test suite, first you need to compile the application, then place yourself in the tests folder and simply type:
```bash
make                # build the tests
make tests          # run the tests
```

This application is built using the androïd SDK API 18 and supports only androïd >= 4.

===
Copyright (C) 2013/2014 Nicolas Miller, Florian Paindorge

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
