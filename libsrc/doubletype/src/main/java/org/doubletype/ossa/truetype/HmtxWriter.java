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
 * HtmxWriter depends on GlyfWriter.
 *
 * @author e.e
 */
public class HmtxWriter extends FontFormatWriter {

    HheaWriter m_hhea;

    GlyfWriter m_glyf;

    public HmtxWriter(GlyfWriter a_glyf, HheaWriter a_hhea) {
        super();

        m_hhea = a_hhea;
        m_glyf = a_glyf;
    }

    public void write() throws IOException {
        int i;

        TTGlyph glyphZero = m_glyf.getGlyph(0);
        int maxWidth = glyphZero.getAdvanceWidth();
        int minRightSideBearing = glyphZero.getRightSideBearing();
        for (i = 0; i < m_glyf.numOfGlyph(); i++) {
            TTGlyph glyph = m_glyf.getGlyph(i);

            if (glyph.getAdvanceWidth() > maxWidth) {
                maxWidth = glyph.getAdvanceWidth();
            }

            if (glyph.getRightSideBearing() < minRightSideBearing) {
                minRightSideBearing = glyph.getRightSideBearing();
            }

            writeUFWord(glyph.getAdvanceWidth());
            writeFWord(glyph.getLeftSideBearing());
        }

        writeFWord(0);

        m_hhea.setMaxAdvanceWidth(maxWidth);
        m_hhea.setMinRightSideBearing(minRightSideBearing);

        pad();
    }

    protected String getTag() {
        return "hmtx";
    }
}
