package com.gomdev.gallery;

import android.content.Context;
import android.util.TypedValue;

import com.gomdev.gles.GLESShader;
import com.gomdev.gles.GLESVertexInfo;

/**
 * Created by gomdev on 14. 12. 27..
 */
public class GalleryUtils {
    static int getActionBarHeight(Context context) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(
                android.R.attr.actionBarSize, tv, true)) {
            int actionBarHeight = TypedValue.complexToDimensionPixelSize(
                    tv.data, context.getResources().getDisplayMetrics());

            return actionBarHeight;

        }

        return 0;
    }

    static GLESVertexInfo createImageVertexInfo(GLESShader shader, float width, float height) {
        float right = width * 0.5f;
        float left = -right;
        float top = height * 0.5f;
        float bottom = -top;
        float z = 0.0f;

        float[] vertex = {
                left, bottom, z,
                right, bottom, z,
                left, top, z,
                right, top, z
        };

        GLESVertexInfo vertexInfo = new GLESVertexInfo();

        vertexInfo.setBuffer(shader.getPositionAttribIndex(), vertex, 3);

        float[] texCoord = {
                0f, 1f,
                1f, 1f,
                0f, 0f,
                1f, 0f
        };

        vertexInfo.setBuffer(shader.getTexCoordAttribIndex(), texCoord, 2);

        vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
        vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);

        return vertexInfo;
    }

    static GLESVertexInfo createColorVertexInfo(GLESShader shader,
                                                float x, float y,
                                                float width, float height,
                                                float r, float g, float b, float a) {
        float left = x;
        float right = x + width;
        float top = y;
        float bottom = y - height;
        float z = 0.0f;

        float[] position = {
                left, bottom, z,
                right, bottom, z,
                left, top, z,
                right, top, z
        };

        GLESVertexInfo vertexInfo = new GLESVertexInfo();

        vertexInfo.setBuffer(shader.getPositionAttribIndex(), position, 3);

        float[] color = {
                r, g, b, a,
                r, g, b, a,
                r, g, b, a,
                r, g, b, a
        };

        vertexInfo.setBuffer(shader.getColorAttribIndex(), color, 4);

        vertexInfo.setRenderType(GLESVertexInfo.RenderType.DRAW_ARRAYS);
        vertexInfo.setPrimitiveMode(GLESVertexInfo.PrimitiveMode.TRIANGLE_STRIP);

        return vertexInfo;
    }
}
