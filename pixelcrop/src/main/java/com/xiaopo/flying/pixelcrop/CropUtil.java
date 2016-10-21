package com.xiaopo.flying.pixelcrop;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;

import java.util.Arrays;

import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

/**
 * The utility class in Crop
 * Created by snowbean on 16-10-15.
 */
class CropUtil {
    private static final String TAG = "CropUtil";

    private static float[] sCorners = new float[8];
    private static float[] sMinZooms = new float[100];
    private static Matrix sTempMatrix = new Matrix();

    //计算不同旋转角度下的最小缩放值
    static float calculateMinScale(CropWrapper cropWrapper, Border cropBorder, int degrees) {
        if (cropWrapper != null && cropBorder != null) {

            if (sMinZooms != null && sMinZooms.length > 45 + degrees && sMinZooms[45 + degrees] != 0) {
                return sMinZooms[45 + degrees];
            }

            float borderWidth = cropBorder.width();
            float borderHeight = cropBorder.height();

            float mDiagonal = (float) sqrt(pow(borderWidth, 2) + pow(borderHeight, 2));

            if (borderWidth > borderHeight) {
                double tempAlpha = atan(borderHeight / borderWidth);
                double tempScale = (float) (mDiagonal * sin(toRadians(abs(degrees)) + tempAlpha) / borderHeight);
                double hh = tempScale * borderHeight;
                double ww = tempScale * borderWidth;
                double temp = (hh * sin(toRadians(abs(degrees))) + ww * cos(toRadians(abs(degrees))));

                sMinZooms[45 + degrees] = (float) (temp / cropWrapper.getWidth());
                return sMinZooms[45 + degrees];
            } else {
                double tempAlpha = atan(borderWidth / borderHeight);
                double tempScale = (float) (mDiagonal * sin(toRadians(abs(degrees)) + tempAlpha) / borderWidth);
                double hh = tempScale * borderHeight;
                double ww = tempScale * borderWidth;
                double temp = (ww * sin(toRadians(abs(degrees))) + hh * cos(toRadians(abs(degrees))));

                sMinZooms[45 + degrees] = (float) (temp / cropWrapper.getHeight());
                return sMinZooms[45 + degrees];
            }

        }

        return 0;
    }

    //计算旋转缩放值
    static float calculateRotateScale(float borderWidth, float borderHeight, float degrees) {

        double mDiagonal = (float) sqrt(pow(borderWidth, 2) + pow(borderHeight, 2));

        if (borderWidth > borderHeight) {
            double tempAlpha = atan(borderHeight / borderWidth);
            return (float) (mDiagonal * sin(toRadians(abs(degrees)) + tempAlpha) / borderHeight);
        } else {
            double tempAlpha = atan(borderWidth / borderHeight);
            return (float) (mDiagonal * sin(toRadians(abs(degrees)) + tempAlpha) / borderWidth);
        }
    }


    //计算点A到BC之间的距离,进行四舍五入
    static long calculatePointToLine(PointF A, PointF B, PointF C) {
        if (B.y == C.y) {
            return round(Math.abs(A.y - B.y));
        }

        if (B.x == C.x) {
            return round(Math.abs(A.x - B.x));
        }

        float k = (B.y - C.y) / (B.x - C.x);
        float b = B.y - k * B.x;

        float c = A.y + 1 / k * A.x;

        double x = (c - b) * k / (Math.pow(k, 2) + 1);
        double y = k * x + b;

        return round(Math.sqrt(Math.pow(A.x - x, 2) + Math.pow(A.y - y, 2)));
    }

    //判断剪裁框是否在图片内
    static boolean judgeIsImageContainsBorder(CropWrapper cropWrapper, Border cropBorder, float rotateDegrees) {
        sTempMatrix.reset();
        sTempMatrix.setRotate(-rotateDegrees);
        float[] unrotatedWrapperCorner = new float[8];
        float[] unrotateBorderCorner = new float[8];
        sTempMatrix.mapPoints(unrotatedWrapperCorner, cropWrapper.getMappedBoundPoints());
        sTempMatrix.mapPoints(unrotateBorderCorner, cropBorder.cornerPoints);

        return CropUtil.trapToRect(unrotatedWrapperCorner).contains(CropUtil.trapToRect(unrotateBorderCorner));
    }


    static float[] calculateImageIndents(CropWrapper cropWrapper, Border cropBorder, float rotateDegrees) {
        if (cropBorder == null || cropWrapper == null)
            return new float[]{0, 0, 0, 0, 0, 0, 0, 0};

        sTempMatrix.reset();
        sTempMatrix.setRotate(-rotateDegrees);

        float[] currentImageCorners = cropWrapper.getMappedBoundPoints();
        float[] unrotatedImageCorners = Arrays.copyOf(currentImageCorners, currentImageCorners.length);
        float[] unrotatedCropBoundsCorners = getCornersFromRect(cropBorder.getRect());

        sTempMatrix.mapPoints(unrotatedImageCorners);
        sTempMatrix.mapPoints(unrotatedCropBoundsCorners);

        RectF unrotatedImageRect = CropUtil.trapToRect(unrotatedImageCorners);
        RectF unrotatedCropRect = CropUtil.trapToRect(unrotatedCropBoundsCorners);

        float deltaLeft = unrotatedImageRect.left - unrotatedCropRect.left;
        float deltaTop = unrotatedImageRect.top - unrotatedCropRect.top;
        float deltaRight = unrotatedImageRect.right - unrotatedCropRect.right;
        float deltaBottom = unrotatedImageRect.bottom - unrotatedCropRect.bottom;

        float indents[] = new float[4];

        indents[0] = (deltaLeft > 0) ? deltaLeft : 0;
        indents[1] = (deltaTop > 0) ? deltaTop : 0;
        indents[2] = (deltaRight < 0) ? deltaRight : 0;
        indents[3] = (deltaBottom < 0) ? deltaBottom : 0;

        sTempMatrix.reset();
        sTempMatrix.setRotate(rotateDegrees);
        sTempMatrix.mapPoints(indents);

        return indents;
    }


    //计算包含给出点的最小矩形
    private static RectF trapToRect(float[] array) {
        RectF r = new RectF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        for (int i = 1; i < array.length; i += 2) {
            float x = round(array[i - 1] * 10) / 10.f;
            float y = round(array[i] * 10) / 10.f;
            r.left = (x < r.left) ? x : r.left;
            r.top = (y < r.top) ? y : r.top;
            r.right = (x > r.right) ? x : r.right;
            r.bottom = (y > r.bottom) ? y : r.bottom;
        }
        r.sort();
        return r;
    }

    private static float[] getCornersFromRect(RectF r) {
        sCorners[0] = r.left;
        sCorners[1] = r.top;
        sCorners[2] = r.right;
        sCorners[3] = r.top;
        sCorners[4] = r.right;
        sCorners[5] = r.bottom;
        sCorners[6] = r.left;
        sCorners[7] = r.bottom;
        return sCorners;
    }

}
