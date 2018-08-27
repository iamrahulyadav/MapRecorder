package com.application.ningyitong.maprecorder;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;

public class DouglasPeuckerAlgorithm {
    public static ArrayList<GeoPoint> reduceWithTolerance(ArrayList<GeoPoint> routePolyline, double tolerance)
    {
        int pointsNumber = routePolyline.size();
        // if a shape has 2 or less points it cannot be reduced
        if (tolerance <= 0 || routePolyline.size() < 3) {
            return routePolyline;
        }

        boolean[] validPoints = new boolean[pointsNumber]; //vertex indexes to keep will be marked as "true"
        for (int i=1; i<pointsNumber-1; i++)
            validPoints[i] = false;
        // automatically add the first and last point to the returned shape
        validPoints[0] = validPoints[pointsNumber - 1] = true;

        // the first and last points in the original shape are
        // used as the entry point to the algorithm.
        douglasPeuckerReduction(routePolyline, validPoints, tolerance, 0,  pointsNumber - 1);

        // all done, return the reduced shape
        ArrayList<GeoPoint> newRoutePolyline = new ArrayList<>(pointsNumber); // the new shape to return
        for (int i=0; i<pointsNumber; i++) {
            if (validPoints[i])
                newRoutePolyline.add(routePolyline.get(i));
        }
        return newRoutePolyline;
    }

    /**
     * Reduce the points in shape between the specified first and last index.
     * Mark the points to keep in marked[]
     * @param shape
     *            The original shape
     * @param marked
     *            The points to keep (marked as true)
     * @param tolerance
     *            The tolerance to determine if a point is kept
     * @param firstIdx
     *            The index in original shape's point of the starting point for
     *            this line segment
     * @param lastIdx
     *            The index in original shape's point of the ending point for
     *            this line segment
     */
    private static void douglasPeuckerReduction(ArrayList<GeoPoint> shape, boolean[] marked, double tolerance, int firstIdx, int lastIdx)
    {
        if (lastIdx <= firstIdx + 1) {
            // overlapping indexes, just return
            return;
        }

        // loop over the points between the first and last points
        // and find the point that is the farthest away

        double maxDistance = 0.0;
        int indexFarthest = 0;

        GeoPoint firstPoint = shape.get(firstIdx);
        GeoPoint lastPoint = shape.get(lastIdx);

        for (int idx = firstIdx + 1; idx < lastIdx; idx++) {
            GeoPoint point = shape.get(idx);

            double distance = orthogonalDistance(point, firstPoint, lastPoint);

            // keep the point with the greatest distance
            if (distance > maxDistance) {
                maxDistance = distance;
                indexFarthest = idx;
            }
        }

        if (maxDistance > tolerance) {
            //The farthest point is outside the tolerance: it is marked and the algorithm continues.
            marked[indexFarthest] = true;

            // reduce the shape between the starting point to newly found point
            douglasPeuckerReduction(shape, marked, tolerance, firstIdx, indexFarthest);

            // reduce the shape between the newly found point and the finishing point
            douglasPeuckerReduction(shape, marked, tolerance, indexFarthest, lastIdx);
        }
        //else: the farthest point is within the tolerance, the whole segment is discarded.
    }

    /**
     * Calculate the orthogonal distance from the line joining the lineStart and
     * lineEnd points to point
     * @param point
     *            The point the distance is being calculated for
     * @param lineStart
     *            The point that starts the line
     * @param lineEnd
     *            The point that ends the line
     * @return The distance in points coordinate system
     */
    private static double orthogonalDistance(GeoPoint point, GeoPoint lineStart, GeoPoint lineEnd)
    {
        double area = Math.abs(
                (
                        1.0 * lineStart.getLatitude() * lineEnd.getLongitude()
                                + 1.0 * lineEnd.getLatitude() * point.getLongitude()
                                + 1.0 * point.getLatitude() * lineStart.getLongitude()
                                - 1.0 * lineEnd.getLatitude() * lineStart.getLongitude()
                                - 1.0 * point.getLatitude() * lineEnd.getLongitude()
                                - 1.0 * lineStart.getLatitude() * point.getLongitude()
                ) / 2.0
        );

        double bottom = Math.hypot(
                lineStart.getLatitude() - lineEnd.getLatitude(),
                lineStart.getLongitude() - lineEnd.getLongitude()
        );

        return (area / bottom * 2.0);
    }

}
