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
package org.doubletype.ossa.adapter;

import java.util.ArrayList;

/**
 * @author e.e
 */
public class EContour {

    public static final String k_quadratic = "quadratic";

    public static final String k_cubic = "cubic";

    private String type;

    private ArrayList<EContourPoint> m_contourPoints = new ArrayList<>();

    public EContour() {
        setType(k_quadratic);
    }

    public boolean isCubic() {
        return type.equals(k_cubic);
    }

    public void setType(String a_type) {
        type = a_type;
    }

    public String getType() {
        return type;
    }

    public void addContourPoint(EContourPoint a_point) {
        m_contourPoints.add(a_point);
    }

    public ArrayList<EContourPoint> getContourPoints() {
        return m_contourPoints;
    }

    public EContour toQuadratic() {
        if (!isCubic()) {
            return this;
        }

        ArrayList<QuadraticSegment> quadraticSegments = new ArrayList<>();
        for (CubicSegment segment : CubicSegment.toSegments(this)) {
            quadraticSegments.addAll(segment.toQuadraticSegments());
        }

        return QuadraticSegment.toContour(quadraticSegments);
    }
}
