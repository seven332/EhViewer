/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.graphics;

import java.util.LinkedList;
import java.util.List;

public class JPath {

    private List<PathAction> mActionList = new LinkedList<>();

    /** startX, startY, length**/
    private float[] mTemp = new float[2];

    {
        mTemp[0] = 0f;
        mTemp[1] = 0f;
    }

    public void close() {
        // Empty
    }

    /**
     * Set the beginning of the next contour to the point (x,y).
     *
     * @param x The x-coordinate of the start of a new contour
     * @param y The y-coordinate of the start of a new contour
     */
    public void moveTo(float x, float y) {
        mActionList.add(new MoveAction(x, y));
    }

    /**
     * Set the beginning of the next contour relative to the last point on the
     * previous contour. If there is no previous contour, this is treated the
     * same as moveTo().
     *
     * @param dx The amount to add to the x-coordinate of the end of the
     *           previous contour, to specify the start of a new contour
     * @param dy The amount to add to the y-coordinate of the end of the
     *           previous contour, to specify the start of a new contour
     */
    public void rMoveTo(float dx, float dy) {
        mActionList.add(new RMoveAction(dx, dy));
    }

    /**
     * Add a line from the last point to the specified point (x,y).
     * If no moveTo() call has been made for this contour, the first point is
     * automatically set to (0,0).
     *
     * @param x The x-coordinate of the end of a line
     * @param y The y-coordinate of the end of a line
     */
    public void lineTo(float x, float y) {
        mActionList.add(new LineToAction(x, y));
    }

    /**
     * Same as lineTo, but the coordinates are considered relative to the last
     * point on this contour. If there is no previous point, then a moveTo(0,0)
     * is inserted automatically.
     *
     * @param dx The amount to add to the x-coordinate of the previous point on
     *           this contour, to specify a line
     * @param dy The amount to add to the y-coordinate of the previous point on
     *           this contour, to specify a line
     */
    public void rLineTo(float dx, float dy) {
        mActionList.add(new RLineToAction(dx, dy));
    }

    /**
     * Add a quadratic bezier from the last point, approaching control point
     * (x1,y1), and ending at (x2,y2). If no moveTo() call has been made for
     * this contour, the first point is automatically set to (0,0).
     *
     * @param x1 The x-coordinate of the control point on a quadratic curve
     * @param y1 The y-coordinate of the control point on a quadratic curve
     * @param x2 The x-coordinate of the end point on a quadratic curve
     * @param y2 The y-coordinate of the end point on a quadratic curve
     */
    public void quadTo(float x1, float y1, float x2, float y2) {
        mActionList.add(new QuadToAction(x1, y1, x2, y2));
    }

    /**
     * Same as quadTo, but the coordinates are considered relative to the last
     * point on this contour. If there is no previous point, then a moveTo(0,0)
     * is inserted automatically.
     *
     * @param dx1 The amount to add to the x-coordinate of the last point on
     *            this contour, for the control point of a quadratic curve
     * @param dy1 The amount to add to the y-coordinate of the last point on
     *            this contour, for the control point of a quadratic curve
     * @param dx2 The amount to add to the x-coordinate of the last point on
     *            this contour, for the end point of a quadratic curve
     * @param dy2 The amount to add to the y-coordinate of the last point on
     *            this contour, for the end point of a quadratic curve
     */
    public void rQuadTo(float dx1, float dy1, float dx2, float dy2) {
        mActionList.add(new RQuadToAction(dx1, dy1, dx2, dy2));
    }

    /**
     * Add a cubic bezier from the last point, approaching control points
     * (x1,y1) and (x2,y2), and ending at (x3,y3). If no moveTo() call has been
     * made for this contour, the first point is automatically set to (0,0).
     *
     * @param x1 The x-coordinate of the 1st control point on a cubic curve
     * @param y1 The y-coordinate of the 1st control point on a cubic curve
     * @param x2 The x-coordinate of the 2nd control point on a cubic curve
     * @param y2 The y-coordinate of the 2nd control point on a cubic curve
     * @param x3 The x-coordinate of the end point on a cubic curve
     * @param y3 The y-coordinate of the end point on a cubic curve
     */
    public void cubicTo(float x1, float y1, float x2, float y2,
            float x3, float y3) {
        mActionList.add(new CubicToAction(x1, y1, x2, y2, x3, y3));
    }

    /**
     * Same as cubicTo, but the coordinates are considered relative to the
     * current point on this contour. If there is no previous point, then a
     * moveTo(0,0) is inserted automatically.
     */
    public void rCubicTo(float x1, float y1, float x2, float y2,
            float x3, float y3) {
        mActionList.add(new RCubicToAction(x1, y1, x2, y2, x3, y3));
    }

    /**
     * Approximate the <code>Path</code> with a series of line segments.
     * This returns float[] with the array containing point components.
     * There are three components for each point, in order:
     * <ul>
     *     <li>Fraction along the length of the path that the point resides</li>
     *     <li>The x coordinate of the point</li>
     *     <li>The y coordinate of the point</li>
     * </ul>
     * <p>Two points may share the same fraction along its length when there is
     * a move action within the Path.</p>
     *
     * @param acceptableError The acceptable error for a line on the
     *                        Path. Typically this would be 0.5 so that
     *                        the error is less than half a pixel.
     * @return An array of components for points approximating the Path.
     */
    public float[] approximate(float acceptableError) {
        PointMap pm = new PointMap();
        for (PathAction pa : mActionList) {
            pa.approximate(pm, mTemp, acceptableError);
        }
        return pm.generate();
    }

    /**
     * Returns true if the path is empty (contains no lines or curves)
     *
     * @return true if the path is empty (contains no lines or curves)
     */
    public boolean isEmpty() {
        return mActionList.isEmpty();
    }

    /**
     * Clear any lines and curves from the path, making it empty.
     * This does NOT change the fill-type setting.
     */
    public void reset() {
        mActionList.clear();
    }

    private static final class PointMap {

        private int size = 0;

        private Link firstLink;
        private Link lastLink;

        float totalLength = 0f;

        private static final class Link {
            /** It is only for bezier **/
            float t;
            float x;
            float y;
            /** It should be Length first **/
            float f = -1;

            private Link previous, next;

            Link(float t, float x, float y, float f, Link p, Link n) {
                this.t = t;
                this.x = x;
                this.y = y;
                this.f = f;
                this.previous = p;
                this.next = n;
            }
        }

        public static final class Iterator {

            final PointMap parent;
            Link link;

            private Iterator(PointMap p, Link l) {
                parent = p;
                link = l;
            }

            boolean hasNext() {
                return link.next != null;
            }

            Iterator nextIterator() {
                if (hasNext()) {
                    return new Iterator(parent, link.next);
                } else {
                    return null;
                }
            }

            // Move iterator to the new data
            void addBefore(float t, float x, float y, float f) {
                Link p = link.previous;
                Link l = new Link(t, x, y, f, p, link);
                link.previous = l;
                p.next = l;
                link = l;
                parent.size++;
            }
        }

        public void add(float t, float x, float y, float f) {
            Link link = new Link(t, x, y, f, null, null);
            if (lastLink != null) {
                link.previous = lastLink;
                lastLink.next = link;
            }
            lastLink = link;
            if (firstLink == null) {
                firstLink = link;
            }
            size++;
        }

        // Get a iterator at first data or null
        public Iterator iterator() {
            if (firstLink != null) {
                return new Iterator(this, firstLink);
            } else {
                return null;
            }
        }

        public Iterator lastIterator() {
            if (lastLink != null) {
                return new Iterator(this, lastLink);
            } else {
                return null;
            }
        }

        public float[] generate() {
            int index = 0;
            float[] result = new float[size * 3];
            for (Link link = firstLink; link != null; link = link.next) {
                result[index++] = link.f / totalLength;
                result[index++] = link.x;
                result[index++] = link.y;
            }
            return result;
        }
    }


    private abstract static class PathAction {
        public abstract void approximate(PointMap pointMap, float[] startPoint, float acceptableError);
    }

    private static class MoveAction extends PathAction {

        private float mX;
        private float mY;

        public MoveAction(float x, float y) {
            mX = x;
            mY = y;
        }

        @Override
        public void approximate(PointMap pointMap, float[] startPoint, float acceptableError) {
            float x = mX;
            float y = mY;
            pointMap.add(0f, x, y, pointMap.totalLength);
            startPoint[0] = x;
            startPoint[1] = y;
        }
    }

    private static class RMoveAction extends PathAction {

        private float mDx;
        private float mDy;

        public RMoveAction(float dx, float dy) {
            mDx = dx;
            mDy = dy;
        }

        @Override
        public void approximate(PointMap pointMap, float[] startPoint, float acceptableError) {
            float x = startPoint[0] + mDx;
            float y = startPoint[1] + mDy;
            pointMap.add(0f, x, y, pointMap.totalLength);
            startPoint[0] = x;
            startPoint[1] = y;
        }
    }

    private static class LineToAction extends PathAction {

        private float mX;
        private float mY;

        public LineToAction(float x, float y) {
            mX = x;
            mY = y;
        }

        @Override
        public void approximate(PointMap pointMap, float[] startPoint, float acceptableError) {
            if (pointMap.size == 0) {
                // No start point in map, just add it
                pointMap.add(0f, startPoint[0], startPoint[1], 0f);
            }

            float x = mX;
            float y = mY;
            float length = dist(startPoint[0], startPoint[1], x, y) +
                    pointMap.totalLength;
            pointMap.add(0f, x, y, length);
            pointMap.totalLength = length;
            startPoint[0] = x;
            startPoint[1] = y;
        }
    }

    private static class RLineToAction extends PathAction {

        private float mDx;
        private float mDy;

        public RLineToAction(float dx, float dy) {
            mDx = dx;
            mDy = dy;
        }

        @Override
        public void approximate(PointMap pointMap, float[] startPoint, float acceptableError) {
            if (pointMap.size == 0) {
                // No start point in map, just add it
                pointMap.add(0f, startPoint[0], startPoint[1], 0f);
            }

            float x = startPoint[0] + mDx;
            float y = startPoint[1] + mDy;
            float length = (float) Math.hypot(mDx, mDy) +
                    pointMap.totalLength;
            pointMap.add(0f, x, y, length);
            pointMap.totalLength = length;
            startPoint[0] = x;
            startPoint[1] = y;
        }
    }

    private static class QuadToAction extends PathAction {

        private float mX1;
        private float mY1;
        private float mX2;
        private float mY2;

        public QuadToAction(float x1, float y1, float x2, float y2) {
            mX1 = x1;
            mY1 = y1;
            mX2 = x2;
            mY2 = y2;
        }

        @Override
        public void approximate(PointMap pointMap, float[] startPoint, float acceptableError) {
            float[] control = new float[]{
                    startPoint[0], startPoint[1],
                    mX1, mY1,
                    mX2, mY2
            };
            bezierApproximate(pointMap, control, acceptableError, false);
            startPoint[0] = control[0];
            startPoint[1] = control[1];
        }
    }

    private static class RQuadToAction extends PathAction {

        private float mDx1;
        private float mDy1;
        private float mDx2;
        private float mDy2;

        public RQuadToAction(float dx1, float dy1, float dx2, float dy2) {
            mDx1 = dx1;
            mDy1 = dy1;
            mDx2 = dx2;
            mDy2 = dy2;
        }

        @Override
        public void approximate(PointMap pointMap, float[] startPoint, float acceptableError) {
            float startX = startPoint[0];
            float startY = startPoint[1];
            float[] control = new float[]{
                    startX, startY,
                    startX + mDx1, startY + mDy1,
                    startX + mDx2, startY + mDy2
            };
            bezierApproximate(pointMap, control, acceptableError, false);
            startPoint[0] = control[0];
            startPoint[1] = control[1];
        }
    }

    private static class CubicToAction extends PathAction {

        private float mX1;
        private float mY1;
        private float mX2;
        private float mY2;
        private float mX3;
        private float mY3;

        public CubicToAction(float x1, float y1, float x2, float y2, float x3, float y3) {
            mX1 = x1;
            mY1 = y1;
            mX2 = x2;
            mY2 = y2;
            mX3 = x3;
            mY3 = y3;
        }

        @Override
        public void approximate(PointMap pointMap, float[] startPoint, float acceptableError) {
            float[] control = new float[]{
                    startPoint[0], startPoint[1],
                    mX1, mY1,
                    mX2, mY2,
                    mX3, mY3
            };
            bezierApproximate(pointMap, control, acceptableError, true);
            startPoint[0] = control[0];
            startPoint[1] = control[1];
        }
    }

    private static class RCubicToAction extends PathAction {

        private float mDx1;
        private float mDy1;
        private float mDx2;
        private float mDy2;
        private float mDx3;
        private float mDy3;

        public RCubicToAction(float dx1, float dy1, float dx2, float dy2, float dx3, float dy3) {
            mDx1 = dx1;
            mDy1 = dy1;
            mDx2 = dx2;
            mDy2 = dy2;
            mDx3 = dx3;
            mDy3 = dy3;
        }

        @Override
        public void approximate(PointMap pointMap, float[] startPoint, float acceptableError) {
            float startX = startPoint[0];
            float startY = startPoint[1];
            float[] control = new float[]{
                    startX, startY,
                    startX + mDx1, startY + mDy1,
                    startX + mDx2, startY + mDy2,
                    startX + mDx3, startY + mDy3,
            };
            bezierApproximate(pointMap, control, acceptableError, true);
            startPoint[0] = control[0];
            startPoint[1] = control[1];
        }
    }


    // Divides Bezier curves until linear interpolation is very close to accurate, using
    // errorSquared as a metric. Cubic Bezier curves can have an inflection point that improperly
    // short-circuit subdivision. If you imagine an S shape, the top and bottom points being the
    // starting and end points, linear interpolation would mark the center where the curve places
    // the point. It is clearly not the case that we can linearly interpolate at that point.
    // doubleCheckDivision forces a second examination between subdivisions to ensure that linear
    // interpolation works.
    private static void bezierApproximate(PointMap pointMap, float[] control,
            float acceptableError, boolean isCubic) {
        float errorSquared = acceptableError * acceptableError;

        if (pointMap.size == 0) {
            // No start point in map, just add it
            pointMap.add(0f, control[0], control[1], 0f);
        }

        PointMap.Iterator iter = pointMap.lastIterator();
        // Make sure start t is 0
        iter.link.t = 0f;

        // Add end point
        if (isCubic) {
            pointMap.add(1f, control[6], control[7], 0);
        } else {
            pointMap.add(1f, control[4], control[5], 0);
        }

        // Get next iterator
        PointMap.Iterator next = iter.nextIterator();

        boolean doubleCheckDivision = isCubic;
        while (next != null) {
            boolean needsSubdivision;

            do {
                PointMap.Link iterLink = iter.link;
                PointMap.Link nextLink = next.link;

                float midT = (iterLink.t + nextLink.t) / 2;
                float midX = (iterLink.x + nextLink.x) / 2;
                float midY = (iterLink.y + nextLink.y) / 2;

                float midPointX = bezierX(midT, control, isCubic);
                float midPointY = bezierY(midT, control, isCubic);

                needsSubdivision = isOutOfErrorSquared(midX, midY, midPointX, midPointY, errorSquared);

                if (!needsSubdivision && doubleCheckDivision) {
                    float quarterT = (iterLink.t + midT) / 2;
                    float quarterX = (iterLink.x + midPointX) / 2;
                    float quarterY = (iterLink.y + midPointY) / 2;

                    float quarterPointX = bezierX(quarterT, control, isCubic);
                    float quarterPointY = bezierY(quarterT, control, isCubic);

                    needsSubdivision = isOutOfErrorSquared(quarterX, quarterY, quarterPointX, quarterPointY, errorSquared);
                    if (needsSubdivision) {
                        // Found an inflection point. No need to double-check.
                        doubleCheckDivision = false;
                    }
                }
                if (needsSubdivision) {
                    // Add length soon
                    next.addBefore(midT, midPointX, midPointY, 0);
                } else {
                    float length = dist(iterLink.x, iterLink.y, nextLink.x, nextLink.y);
                    iterLink.f = pointMap.totalLength;
                    pointMap.totalLength += length;
                }
            } while (needsSubdivision);

            iter = next;
            next = next.nextIterator();
        }

        PointMap.Link iterLink = iter.link;
        iterLink.f = pointMap.totalLength;
        control[0] = iterLink.x;
        control[1] = iterLink.y;
    }

    private static boolean isOutOfErrorSquared(float midX, float midY,
            float midPointX, float midPointY, float errorSquared) {
        float xError = midPointX - midX;
        float yError = midPointY - midY;
        float midErrorSquared = (xError * xError) + (yError * yError);
        return midErrorSquared > errorSquared;
    }

    private static float bezierX(float t, float[] control, boolean isCubic) {
        if (isCubic) {
            return cubicCoordinateCalculation(t, control[0], control[2], control[4], control[6]);
        } else {
            return quadraticCoordinateCalculation(t, control[0], control[2], control[4]);
        }
    }

    private static float bezierY(float t, float[] control, boolean isCubic) {
        if (isCubic) {
            return cubicCoordinateCalculation(t, control[1], control[3], control[5], control[7]);
        } else {
            return quadraticCoordinateCalculation(t, control[1], control[3], control[5]);
        }
    }

    private static float quadraticCoordinateCalculation(float t, float p0, float p1, float p2) {
        float oneMinusT = 1 - t;
        return oneMinusT * ((oneMinusT * p0) + (t * p1)) + t * ((oneMinusT * p1) + (t * p2));
    }

    private static float cubicCoordinateCalculation(float t, float p0, float p1, float p2, float p3) {
        float oneMinusT = 1 - t;
        float oneMinusTSquared = oneMinusT * oneMinusT;
        float oneMinusTCubed = oneMinusTSquared * oneMinusT;
        float tSquared = t * t;
        float tCubed = tSquared * t;
        return (oneMinusTCubed * p0) + (3 * oneMinusTSquared * t * p1)
                + (3 * oneMinusT * tSquared * p2) + (tCubed * p3);
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        final float x = (x2 - x1);
        final float y = (y2 - y1);
        return (float) Math.hypot(x, y);
    }
}
