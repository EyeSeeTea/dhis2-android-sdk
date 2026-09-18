package org.hisp.dhis.android.network.fileresource

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.arch.api.HttpServiceClient
import org.hisp.dhis.android.core.arch.api.testutils.HttpServiceClientFactory
import org.hisp.dhis.android.core.arch.helpers.FileResizerHelper.DimensionSize
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.android.core.fileresource.internal.MissingTrackerAttributeValue
import org.hisp.dhis.android.core.mockwebserver.Dhis2MockServer
import org.hisp.dhis.android.core.systeminfo.DHISVersion
import org.hisp.dhis.android.core.systeminfo.DHISVersionManager
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FileResourceNetworkHandlerImplShould {

    private val versionManager: DHISVersionManager = mock()
    private lateinit var handler: FileResourceNetworkHandlerImpl

    @Before
    fun setUp() {
        handler = FileResourceNetworkHandlerImpl(httpServiceClient, versionManager)
    }

    @Test
    fun use_tracker_image_endpoint_for_events_after_41() = runTest {
        givenServerAfter41()
        mockWebServer.enqueueMockResponse()

        handler.getImageFromEventValue(eventValue, "MEDIUM")

        assertThat(mockWebServer.takeRequest().path).isEqualTo(
            "/api/tracker/events/eventUid/dataValues/dataElementUid/image?dimension=MEDIUM",
        )
    }

    @Test
    fun omit_dimension_for_original_event_image_after_41() = runTest {
        givenServerAfter41()
        mockWebServer.enqueueMockResponse()

        handler.getImageFromEventValue(eventValue, DimensionSize.ORIGIANL_NAME)

        assertThat(mockWebServer.takeRequest().path).isEqualTo(
            "/api/tracker/events/eventUid/dataValues/dataElementUid/image",
        )
    }

    @Test
    fun use_legacy_image_endpoint_for_events_until_41() = runTest {
        givenServerUntil41()
        mockWebServer.enqueueMockResponse()

        handler.getImageFromEventValue(eventValue, "MEDIUM")

        assertThat(mockWebServer.takeRequest().path).isEqualTo(
            "/api/events/files?eventUid=eventUid&dataElementUid=dataElementUid&dimension=MEDIUM",
        )
    }

    @Test
    fun use_tracker_file_endpoint_for_events_after_41() = runTest {
        givenServerAfter41()
        mockWebServer.enqueueMockResponse()

        handler.getFileFromEventValue(eventValue)

        assertThat(mockWebServer.takeRequest().path).isEqualTo(
            "/api/tracker/events/eventUid/dataValues/dataElementUid/file",
        )
    }

    @Test
    fun use_legacy_file_endpoint_for_events_until_41() = runTest {
        givenServerUntil41()
        mockWebServer.enqueueMockResponse()

        handler.getFileFromEventValue(eventValue)

        assertThat(mockWebServer.takeRequest().path).isEqualTo(
            "/api/events/files?eventUid=eventUid&dataElementUid=dataElementUid",
        )
    }

    @Test
    fun use_tracker_image_endpoint_for_attributes_after_41() = runTest {
        givenServerAfter41()
        mockWebServer.enqueueMockResponse()

        handler.getImageFromTrackedEntityAttribute(attributeValue, "MEDIUM")

        assertThat(mockWebServer.takeRequest().path).isEqualTo(
            "/api/tracker/trackedEntities/trackedEntityUid/attributes/attributeUid/image?dimension=MEDIUM",
        )
    }

    @Test
    fun use_legacy_image_endpoint_for_attributes_until_41() = runTest {
        givenServerUntil41()
        mockWebServer.enqueueMockResponse()

        handler.getImageFromTrackedEntityAttribute(attributeValue, "MEDIUM")

        assertThat(mockWebServer.takeRequest().path).isEqualTo(
            "/api/trackedEntityInstances/trackedEntityUid/attributeUid/image?dimension=MEDIUM",
        )
    }

    @Test
    fun use_tracker_file_endpoint_for_attributes_after_41() = runTest {
        givenServerAfter41()
        mockWebServer.enqueueMockResponse()

        handler.getFileFromTrackedEntityAttribute(attributeValue)

        assertThat(mockWebServer.takeRequest().path).isEqualTo(
            "/api/tracker/trackedEntities/trackedEntityUid/attributes/attributeUid/file",
        )
    }

    @Test
    fun use_legacy_file_endpoint_for_attributes_until_41() = runTest {
        givenServerUntil41()
        mockWebServer.enqueueMockResponse()

        handler.getFileFromTrackedEntityAttribute(attributeValue)

        assertThat(mockWebServer.takeRequest().path).isEqualTo(
            "/api/trackedEntityInstances/trackedEntityUid/attributeUid/file",
        )
    }

    private fun givenServerAfter41() {
        whenever(versionManager.isGreaterThan(DHISVersion.V2_41)).thenReturn(true)
    }

    private fun givenServerUntil41() {
        whenever(versionManager.isGreaterThan(DHISVersion.V2_41)).thenReturn(false)
    }

    private val eventValue = TrackedEntityDataValue.builder()
        .dataElement("dataElementUid")
        .event("eventUid")
        .value("fileResourceUid")
        .build()

    private val attributeValue = MissingTrackerAttributeValue(
        TrackedEntityAttributeValue.builder()
            .trackedEntityAttribute("attributeUid")
            .trackedEntityInstance("trackedEntityUid")
            .value("fileResourceUid")
            .build(),
        ValueType.IMAGE,
    )

    companion object {
        private lateinit var mockWebServer: Dhis2MockServer
        private lateinit var httpServiceClient: HttpServiceClient

        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            mockWebServer = Dhis2MockServer(0)
            httpServiceClient = HttpServiceClientFactory.fromDHIS2MockServer(mockWebServer)
        }

        @AfterClass
        @JvmStatic
        fun tearDownClass() {
            mockWebServer.shutdown()
        }
    }
}
