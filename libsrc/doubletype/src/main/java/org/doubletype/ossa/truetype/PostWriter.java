/*
 * Copyright (C) 2020 Nicolas Maltais
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 */
package org.doubletype.ossa.truetype;

import java.io.IOException;

/**
 * @author e.e
 */
public class PostWriter extends FontFormatWriter {

    boolean m_isMonospaced = false;

    public PostWriter() {
        super();
    }

    public void write() throws IOException {
        // table version number
        writeFixed32(1.0);
        writeFixed32(10.0); // italic angle
        writeFWord(0); // underline pos
        writeFWord(60); // underline thickness

        if (m_isMonospaced) {
            writeUInt32(1); // fixed pitch
        } else {
            writeUInt32(0);
        }

        // vm memory stuff
        writeUInt32(0);
        writeUInt32(0);
        writeUInt32(0);
        writeUInt32(0);

        pad();
    }

    protected String getTag() {
        return "post";
    }
}
