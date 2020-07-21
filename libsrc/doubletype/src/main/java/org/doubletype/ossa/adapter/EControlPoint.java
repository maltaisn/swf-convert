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

/**
 * @author e.e
 */
public class EControlPoint implements EPoint {

    private EContourPoint contourPoint;

    public EControlPoint(double x, double y) {
        contourPoint = new EContourPoint(x, y, false);
    }

    public double getX() {
        return contourPoint.getX();
    }

    public double getY() {
        return contourPoint.getY();
    }

    public EContourPoint getContourPoint() {
        return contourPoint;
    }
}
