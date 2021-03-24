package uk.ac.ncl.openlab.ongoingness.collections

import android.graphics.drawable.BitmapDrawable
import java.io.File

/**
 * Content representation containing the file and the file type to be rendered.
 *
 * @param file path to the file to be rendered
 * @param type type of the file
 * @param bitmapDrawable content of the file
 * @author Luis Carvalho
 */
class ContentPiece (val file: File, val type: ContentType, val bitmapDrawable: BitmapDrawable)
