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
public final class EContourPoint implements EPoint {

    private boolean isOn;

    private double x;

    private double y;

    private EControlPoint controlPoint1;

    private EControlPoint controlPoint2;

    public EContourPoint() {
        super();
    }

    public EContourPoint(double a_x, double a_y, boolean a_isOn) {
        x = a_x;
        y = a_y;

        setOn(a_isOn);
    }

    public static final int k_defaultPixelSize = 16;

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public EControlPoint getControlPoint1() {
        return controlPoint1;
    }

    public EControlPoint getControlPoint2() {
        return controlPoint2;
    }

    public void setControlPoint1(EControlPoint a_point) {
        controlPoint1 = a_point;
    }

    public void setControlPoint2(EControlPoint a_point) {
        controlPoint2 = a_point;
    }

    public boolean hasControlPoint1() {
        return getControlPoint1() != null;
    }

    public boolean hasControlPoint2() {
        return getControlPoint2() != null;
    }

    public boolean isOn() {
        return isOn;
    }

    public void setOn(boolean a_isOnCurve) {
        isOn = a_isOnCurve;
    }
}
