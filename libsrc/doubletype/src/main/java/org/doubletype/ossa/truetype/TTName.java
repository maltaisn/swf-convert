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

public class TTName {

    public static final int k_unicode = 0;

    public static final int k_unicodeDefaultEncode = 0;

    public static final int k_macintosh = 1;

    public static final int k_macRomanEncode = 0;

    public static final int k_macEnglishLang = 0;

    public static final int k_microsoft = 3;

    public static final int k_winSymbolEncode = 0;

    public static final int k_winUnicodeEncode = 1;

    public static final int k_winShiftJisEncode = 2;

    public static final int k_winEnglishLang = 0x409;

    private int m_platformId;

    private int m_encodingId;

    private int m_languageId;

    private int m_nameId;

    private byte m_bytes[];

    public TTName(int a_platformId,
            int a_encodingId,
            int a_languageId,
            int a_nameId,
            byte a_bytes[]) {
        m_platformId = a_platformId;
        m_encodingId = a_encodingId;
        m_languageId = a_languageId;
        m_nameId = a_nameId;
        m_bytes = a_bytes;
    }

    public int getPlatformId() {
        return m_platformId;
    }

    public int getEncodingId() {
        return m_encodingId;
    }

    public int getLanguageId() {
        return m_languageId;
    }

    public int getNameId() {
        return m_nameId;
    }

    //	BUGFIX 958996
    public byte[] getBytes() {
        return m_bytes;
    }

    public int getStringLength() {
        return m_bytes.length;
    }
}
