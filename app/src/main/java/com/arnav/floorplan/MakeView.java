package com.arnav.floorplan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static android.media.CamcorderProfile.get;
import static com.arnav.floorplan.Constants.coordinates;

/**
 * Created by Arnav on 20/02/2017.
 */

public class MakeView extends View {

    Paint mPaint;
    Path mPath;
    Region mRegion;
    Canvas mCanvas;
    List<DatasetStoreDetails> listDatasetStoreDetails = new ArrayList<>();

    // Zoom
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1f;
    private float mScaleRegionFactor = 2;

    // Pan
    private int _xDelta;
    private int _yDelta;
    private boolean mPanning = false;

    private float differenceX = 1f;
    private float differenceY = 1f;

    public MakeView(Context context) {
        super(context);
        mPaint = new Paint();
        String coords = coordinates;
        coords = coords.replaceAll("\\\\", "");
        coords = coords.replaceAll("&", "&amp;");
        coords = coords.replaceAll("href=\"", "");
        loadMapFromServer(coords);
    }

    public void loadMapFromServer(String response) {
        Document document;
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(response));

            document = documentBuilder.parse(inputSource);
            NodeList nodes = document.getElementsByTagName("area");

            for (int i = 0; i < nodes.getLength(); i++) {
                String shape = "";
                String coords = "";
                String alt = "";
                String id = "";

                Element element = (Element) nodes.item(i);

                if (element != null) {
                    shape = element.getAttribute("shape");
                    coords = element.getAttribute("coords");
                    alt = element.getAttribute("alt");
                    id = element.getAttribute("id");
                }
                if (shape != null && coords != null && alt != null && id != null) {
                    String[] coordinates = coords.split(",");
                    List<Float> listFloat = new ArrayList<>();
                    for (int j = 0; j < coordinates.length; j++) {
                        listFloat.add(Float.valueOf(coordinates[j]));
                    }
                    listDatasetStoreDetails.add(new DatasetStoreDetails(shape, listFloat, null, alt, id, null));
                }
            }

        } catch (SAXException | IOException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPaint.setAntiAlias(true);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setTextSize(32);

        for (int i = 0; i < listDatasetStoreDetails.size(); i++) {
            DatasetStoreDetails datasetStoreDetails = listDatasetStoreDetails.get(i);

            // Check for Rectangle Shape
            if (datasetStoreDetails.getShape().toLowerCase().equals("rect") && datasetStoreDetails.getCoordinates().size() == 4) {
                float x1 = (datasetStoreDetails.getCoordinates().get(0) * mScaleRegionFactor);
                float y1 = (datasetStoreDetails.getCoordinates().get(1) * mScaleRegionFactor);
                float x2 = (datasetStoreDetails.getCoordinates().get(2) * mScaleRegionFactor);
                float y2 = (datasetStoreDetails.getCoordinates().get(3) * mScaleRegionFactor);

                drawRectBorder(canvas, x1, y1, x2, y2);

                saveRegion();

                datasetStoreDetails.setPath(mPath);
                datasetStoreDetails.setRegion(mRegion);
                listDatasetStoreDetails.set(i, datasetStoreDetails);

                drawRectFill(canvas, x1, y1, x2, y2);

                // Type Text on Block
                typeTextOnRectStore(canvas, datasetStoreDetails.getName(), x1, y1, x2, y2);
            }
            // Check for Polygon Shape
            else if (datasetStoreDetails.getShape().toLowerCase().equals("poly")) {
                List<Float> listCoordinates = datasetStoreDetails.getCoordinates();
                drawPolyBorder(canvas, listCoordinates);

                saveRegion();

                datasetStoreDetails.setPath(mPath);
                datasetStoreDetails.setRegion(mRegion);
                listDatasetStoreDetails.set(i, datasetStoreDetails);

                drawPolyFill(canvas, listCoordinates);

                typeTextOnPolyStore(canvas, datasetStoreDetails.getName(), listCoordinates);
            }
        }
    }

    private void drawRectBorder(Canvas canvas, float x1, float y1, float x2, float y2) {
        mPath = new Path();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(getResources().getColor(R.color.colorBlack));
        mPaint.setStrokeWidth(8);

        mPath.moveTo(x1, y1);
        // Top Left to Top Right
        mPath.lineTo(x2, y1);
        // Top Right to Bottom Right
        mPath.lineTo(x2, y2);
        // Bottom Right to Bottom Left
        mPath.lineTo(x1, y2);
        // Bottom Left to Top Left
        mPath.lineTo(x1, y1);
        // Draw on Canvas
        canvas.drawPath(mPath, mPaint);
        mPath.close();

    }

    private void saveRegion() {
        RectF rectF = new RectF();
        mPath.computeBounds(rectF, true);
        mRegion = new Region();
        mRegion.setPath(mPath, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));
    }

    private void drawRectFill(Canvas canvas, float x1, float y1, float x2, float y2) {
        mPath = new Path();
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(getResources().getColor(R.color.colorStore));
        mPaint.setStrokeWidth(3);

        mPath.moveTo(x1, y1);
        // Top Left to Top Right
        mPath.lineTo(x2, y1);
        // Top Right to Bottom Right
        mPath.lineTo(x2, y2);
        // Bottom Right to Bottom Left
        mPath.lineTo(x1, y2);
        // Bottom Left to Top Left
        mPath.lineTo(x1, y1);
        // Draw on Canvas
        canvas.drawPath(mPath, mPaint);
        mPath.close();
    }

    private void typeTextOnRectStore(Canvas canvas, String storeName, float x1, float y1, float x2, float y2) {
        storeName = storeName.replaceAll(" ", "\n");
        mPaint.setColor(getResources().getColor(R.color.colorWhite));
        mPaint.setStrokeWidth(1);
        int storeNameSize = storeName.length();
        double midPointX = (x1 + x2) / 2;
        double midPointY = (y1 + y2) / 2;

        canvas.drawText(storeName, (float) midPointX, (float) midPointY + 4, mPaint);
    }


    private void drawPolyBorder(Canvas canvas, List<Float> listCoordinates) {
        mPath = new Path();
        mPaint.setStrokeWidth(8);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(getResources().getColor(R.color.colorBlack));

        int listCoordinateSize = listCoordinates.size();

        mPath.moveTo((listCoordinates.get(0) * mScaleRegionFactor), (listCoordinates.get(1) * mScaleRegionFactor));
        for (int j = 2; j < listCoordinateSize; j += 2) {
            float lineToX = (listCoordinates.get(j) * mScaleRegionFactor);
            float lineToY = (listCoordinates.get(j + 1) * mScaleRegionFactor);
            mPath.lineTo(lineToX, lineToY);

            // Connecting Last Point and First Point

            if (j == listCoordinateSize - 2) {
                float endX = (listCoordinates.get(listCoordinateSize - 2) * mScaleRegionFactor);
                float endY = (listCoordinates.get(listCoordinateSize - 1) * mScaleRegionFactor);
                float startX = (listCoordinates.get(0) * mScaleRegionFactor);
                float startY = (listCoordinates.get(1) * mScaleRegionFactor);
                mPath.lineTo(startX, startY);
                canvas.drawPath(mPath, mPaint);
                mPath.close();
            }
        }
    }

    private void drawPolyFill(Canvas canvas, List<Float> listCoordinates) {
        mPath = new Path();
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(getResources().getColor(R.color.colorStore));
        mPaint.setStrokeWidth(1);

        int listCoordinateSize = listCoordinates.size();

        mPath.moveTo((listCoordinates.get(0) * mScaleRegionFactor), (listCoordinates.get(1) * mScaleRegionFactor));
        for (int j = 2; j < listCoordinateSize; j += 2) {
            float lineToX = (listCoordinates.get(j) * mScaleRegionFactor);
            float lineToY = (listCoordinates.get(j + 1) * mScaleRegionFactor);
            mPath.lineTo(lineToX, lineToY);

            // Connecting Last Point and First Point

            if (j == listCoordinateSize - 2) {
                float endX = (listCoordinates.get(listCoordinateSize - 2) * mScaleRegionFactor);
                float endY = (listCoordinates.get(listCoordinateSize - 1) * mScaleRegionFactor);
                float startX = (listCoordinates.get(0) * mScaleRegionFactor);
                float startY = (listCoordinates.get(1) * mScaleRegionFactor);
                mPath.lineTo(startX, startY);
                canvas.drawPath(mPath, mPaint);
                mPath.close();
            }
        }
    }

    private void typeTextOnPolyStore(Canvas canvas, String name, List<Float> listCoordinates) {
        name = name.replaceAll(" ", "\n");

        int textSize = 32;
        mPaint.setTextSize(textSize);
        mPaint.setColor(getResources().getColor(R.color.colorWhite));
        mPaint.setStrokeWidth(1);
        float textWidth = mPaint.measureText(name);
        double maxLength = 0;
        double minLength = Integer.MAX_VALUE;
        float maxX1 = 0;
        float maxY1 = 0;
        float maxX2 = 0;
        float maxY2 = 0;
        for (int i = 0; i < listCoordinates.size(); i += 2) {
            float x1 = (listCoordinates.get(i) * mScaleRegionFactor);
            float y1 = (listCoordinates.get(i + 1) * mScaleRegionFactor);
            for (int j = 0; j < listCoordinates.size(); j += 2) {
                float x2 = (listCoordinates.get(j) * mScaleRegionFactor);
                float y2 = (listCoordinates.get(j + 1) * mScaleRegionFactor);
                double lengthBetweenPoints = Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2));
                if (lengthBetweenPoints > maxLength) {
                    maxLength = lengthBetweenPoints;
                    maxX1 = x1;
                    maxY1 = y1;
                    maxX2 = x2;
                    maxY2 = y2;
                }
                if (lengthBetweenPoints < minLength && lengthBetweenPoints != 0) {
                    minLength = lengthBetweenPoints;
                }
            }
        }

        while (textWidth >= maxLength + 10) {
            textSize -= 1;
            textWidth = mPaint.measureText(name);
            mPaint.setTextSize(textSize);
        }

        if (maxX1 != 0 && maxY1 != 0 && maxX2 != 0 && maxY2 != 0) {
            float midPointX = (maxX2 + maxX1) / 2;
            float midPointY = (maxY2 + maxY1) / 2;
            canvas.drawText(name, midPointX, midPointY, mPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        Point point = new Point();
        point.x = (int) event.getX();
        point.y = (int) event.getY();

        final int action = event.getAction();

        final int X = (int) event.getRawX();
        final int Y = (int) event.getRawY();
        switch (event.getActionMasked() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                Log.d("EVENTS!!!", "ACTION DOWN");
                RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) this.getLayoutParams();
                _xDelta = X - lParams.leftMargin;
                _yDelta = Y - lParams.topMargin;
                inRegion(point.x, point.y);
                break;
            case MotionEvent.ACTION_UP:
                Log.d("EVENTS!!!", "ACTION UP");
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.d("EVENTS!!!", "ACTION POINTER DOWN");
                break;
            case MotionEvent.ACTION_POINTER_UP:
                Log.d("EVENTS!!!", "ACTION POINTER UP");
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d("EVENTS!!!", "ACTION MOVE");
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.getLayoutParams();
                layoutParams.leftMargin = X - _xDelta;
                layoutParams.topMargin = Y - _yDelta;
                this.setLayoutParams(layoutParams);
                break;
        }
        invalidate();

        return true;
    }

    private void inRegion(int touchX, int touchY) {
        for (int i = 0; i < listDatasetStoreDetails.size(); i++) {
            if (listDatasetStoreDetails.get(i).getRegion().contains(touchX, touchY)) {
                Toast.makeText(getContext(), listDatasetStoreDetails.get(i).getName(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
