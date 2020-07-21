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
import java.util.ArrayList;

/**
 * @author e.e
 */
public class LocaWriter extends FontFormatWriter {

    public ArrayList<Integer> m_offsets = new ArrayList<>();

    public LocaWriter() {
        super();
    }

    public void write() throws IOException {
		// assume glyf table is already written,
        // and offsets are stored in m_offsets

        int i;
        for (i = 0; i < m_offsets.size(); i++) {
            writeUInt32(m_offsets.get(i));
        }

        pad();
    }

    protected String getTag() {
        return "loca";
    }
}
