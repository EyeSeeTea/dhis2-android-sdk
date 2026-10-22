package org.hisp.dhis.android.core.fileresource.internal

import com.google.common.truth.Truth.assertThat
import org.hisp.dhis.android.core.arch.helpers.FileResizerHelper.Dimension
import org.hisp.dhis.android.core.fileresource.internal.FileResourceUtil.computeScalingDimension
import org.junit.Test

// EyeSeeTea customization - Original Size Image Download
class GetDimensionShould {

    @Test
    fun return_stock_selection_for_images_when_no_dimension_is_configured() {
        assertThat(getDimension(null, LARGE_IMAGE_LENGTH, MAX_CONTENT_LENGTH, fileIsImage = true))
            .isEqualTo(computeScalingDimension(LARGE_IMAGE_LENGTH, MAX_CONTENT_LENGTH, true))
        assertThat(getDimension(null, LARGE_IMAGE_LENGTH, SMALL_MAX_CONTENT_LENGTH, fileIsImage = true))
            .isEqualTo(computeScalingDimension(LARGE_IMAGE_LENGTH, SMALL_MAX_CONTENT_LENGTH, true))
    }

    @Test
    fun return_configured_dimension_for_images() {
        assertThat(getDimension(Dimension.SMALL, LARGE_IMAGE_LENGTH, MAX_CONTENT_LENGTH, fileIsImage = true))
            .isEqualTo("SMALL")
        assertThat(getDimension(Dimension.MEDIUM, LARGE_IMAGE_LENGTH, MAX_CONTENT_LENGTH, fileIsImage = true))
            .isEqualTo("MEDIUM")
    }

    @Test
    fun return_configured_dimension_above_the_stock_medium_cap() {
        assertThat(computeScalingDimension(LARGE_IMAGE_LENGTH, MAX_CONTENT_LENGTH, true)).isEqualTo("MEDIUM")

        assertThat(getDimension(Dimension.LARGE, LARGE_IMAGE_LENGTH, MAX_CONTENT_LENGTH, fileIsImage = true))
            .isEqualTo("LARGE")
        assertThat(getDimension(Dimension.ORIGINAL, LARGE_IMAGE_LENGTH, MAX_CONTENT_LENGTH, fileIsImage = true))
            .isEqualTo("ORIGINAL")
    }

    @Test
    fun return_configured_dimension_for_images_the_stock_selection_does_not_support() {
        assertThat(computeScalingDimension(LARGE_IMAGE_LENGTH, SMALL_MAX_CONTENT_LENGTH, true))
            .isEqualTo("NOT_SUPPORTED")

        assertThat(getDimension(Dimension.ORIGINAL, LARGE_IMAGE_LENGTH, SMALL_MAX_CONTENT_LENGTH, fileIsImage = true))
            .isEqualTo("ORIGINAL")
    }

    @Test
    fun ignore_configured_dimension_for_non_images() {
        Dimension.entries.forEach { dimension ->
            assertThat(getDimension(dimension, LARGE_IMAGE_LENGTH, MAX_CONTENT_LENGTH, fileIsImage = false))
                .isEqualTo(computeScalingDimension(LARGE_IMAGE_LENGTH, MAX_CONTENT_LENGTH, false))
            assertThat(getDimension(dimension, LARGE_IMAGE_LENGTH, SMALL_MAX_CONTENT_LENGTH, fileIsImage = false))
                .isEqualTo(computeScalingDimension(LARGE_IMAGE_LENGTH, SMALL_MAX_CONTENT_LENGTH, false))
        }
    }

    private companion object {
        const val LARGE_IMAGE_LENGTH = 3_000_000L
        const val MAX_CONTENT_LENGTH = 6_000_000L
        const val SMALL_MAX_CONTENT_LENGTH = 300_000L
    }
}
