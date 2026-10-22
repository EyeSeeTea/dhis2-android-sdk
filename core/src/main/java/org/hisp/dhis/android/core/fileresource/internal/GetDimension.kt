package org.hisp.dhis.android.core.fileresource.internal

import org.hisp.dhis.android.core.arch.helpers.FileResizerHelper
import org.hisp.dhis.android.core.fileresource.internal.FileResourceUtil.computeScalingDimension

// EyeSeeTea customization - Original Size Image Download
internal fun getDimension(
    configuredDimension: FileResizerHelper.Dimension?,
    contentLength: Long?,
    maxContentLength: Long?,
    fileIsImage: Boolean,
): String {
    return if (fileIsImage && configuredDimension != null) {
        configuredDimension.name
    } else {
        computeScalingDimension(contentLength, maxContentLength, fileIsImage)
    }
}
