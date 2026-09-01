package org.hisp.dhis.android.core.retention.internal

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.category.CategoryCombo
import org.hisp.dhis.android.core.common.ObjectWithUid
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.android.core.dataelement.DataElement
import org.hisp.dhis.android.core.dataelement.internal.DataElementStore
import org.hisp.dhis.android.core.fileresource.FileResource
import org.hisp.dhis.android.core.fileresource.internal.FileResourceStore
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttribute
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityAttributeStore
import org.hisp.dhis.android.core.utils.integration.mock.TestDatabaseAdapterFactory
import org.hisp.dhis.android.core.utils.runner.D2JunitRunner
import org.hisp.dhis.android.persistence.category.CategoryComboStoreImpl
import org.hisp.dhis.android.persistence.dataelement.DataElementStoreImpl
import org.hisp.dhis.android.persistence.fileresource.FileResourceStoreImpl
import org.hisp.dhis.android.persistence.trackedentity.TrackedEntityAttributeStoreImpl
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(D2JunitRunner::class)
class ValueFileResourcePurgerIntegrationShould {

    private val databaseAdapter = TestDatabaseAdapterFactory.get()
    private val dataElementStore: DataElementStore = DataElementStoreImpl(databaseAdapter)
    private val trackedEntityAttributeStore: TrackedEntityAttributeStore =
        TrackedEntityAttributeStoreImpl(databaseAdapter)
    private val fileResourceStore: FileResourceStore = FileResourceStoreImpl(databaseAdapter)
    private val categoryComboStore = CategoryComboStoreImpl(databaseAdapter)
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val purger = ValueFileResourcePurger(dataElementStore, trackedEntityAttributeStore, fileResourceStore)

    @Before
    fun setUp() {
        runBlocking {
            dataElementStore.delete()
            trackedEntityAttributeStore.delete()
            fileResourceStore.delete()
            categoryComboStore.delete()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            dataElementStore.delete()
            trackedEntityAttributeStore.delete()
            fileResourceStore.delete()
            categoryComboStore.delete()
        }
    }

    @Test
    fun purge_the_file_resource_referenced_by_a_file_type_data_element_value() = runTest {
        val (fileResource, physicalFile) = givenAFileResourceWithPhysicalFile("referencedFile")
        fileResourceStore.insert(fileResource)

        val fileDataElement = givenADataElement("fileDataElement", ValueType.IMAGE)
        dataElementStore.insert(fileDataElement)

        purger.purgeIfDataElementReferencesFile("fileDataElement", "referencedFile")

        assertThat(fileResourceStore.selectUids()).isEmpty()
        assertThat(physicalFile.exists()).isFalse()
    }

    @Test
    fun keep_the_file_resource_when_the_data_element_is_not_a_file_type() = runTest {
        val fileResource = givenAFileResource("referencedFile", path = null)
        fileResourceStore.insert(fileResource)

        val textDataElement = givenADataElement("textDataElement", ValueType.TEXT)
        dataElementStore.insert(textDataElement)

        purger.purgeIfDataElementReferencesFile("textDataElement", "referencedFile")

        assertThat(fileResourceStore.selectUids()).containsExactly("referencedFile")
    }

    @Test
    fun purge_the_file_resource_referenced_by_a_file_type_tracked_entity_attribute_value() = runTest {
        val fileResource = givenAFileResource("referencedFile", path = null)
        fileResourceStore.insert(fileResource)

        val fileAttribute = givenATrackedEntityAttribute("fileAttribute", ValueType.FILE_RESOURCE)
        trackedEntityAttributeStore.insert(fileAttribute)

        purger.purgeIfAttributeReferencesFile("fileAttribute", "referencedFile")

        assertThat(fileResourceStore.selectUids()).isEmpty()
    }

    @Test
    fun keep_the_file_resource_when_the_tracked_entity_attribute_is_not_a_file_type() = runTest {
        val fileResource = givenAFileResource("referencedFile", path = null)
        fileResourceStore.insert(fileResource)

        val textAttribute = givenATrackedEntityAttribute("textAttribute", ValueType.TEXT)
        trackedEntityAttributeStore.insert(textAttribute)

        purger.purgeIfAttributeReferencesFile("textAttribute", "referencedFile")

        assertThat(fileResourceStore.selectUids()).containsExactly("referencedFile")
    }

    @Test
    fun do_nothing_when_the_value_names_no_existing_file_resource() = runTest {
        val fileDataElement = givenADataElement("fileDataElement", ValueType.IMAGE)
        dataElementStore.insert(fileDataElement)

        purger.purgeIfDataElementReferencesFile("fileDataElement", "doesNotExist")
    }

    @Test
    fun do_nothing_when_the_value_is_null() = runTest {
        val fileDataElement = givenADataElement("fileDataElement", ValueType.IMAGE)
        dataElementStore.insert(fileDataElement)

        purger.purgeIfDataElementReferencesFile("fileDataElement", null)
    }

    private suspend fun givenADataElement(uid: String, valueType: ValueType): DataElement {
        val categoryCombo = CategoryCombo.builder().uid("$uid-categoryCombo").build()
        categoryComboStore.insert(categoryCombo)

        return DataElement.builder()
            .uid(uid)
            .valueType(valueType)
            .categoryCombo(ObjectWithUid.fromIdentifiable(categoryCombo))
            .domainType("AGGREGATE")
            .build()
    }

    private fun givenATrackedEntityAttribute(uid: String, valueType: ValueType): TrackedEntityAttribute {
        return TrackedEntityAttribute.builder()
            .uid(uid)
            .valueType(valueType)
            .build()
    }

    private fun givenAFileResource(uid: String, path: String?): FileResource {
        val builder = FileResource.builder()
            .uid(uid)
            .syncState(State.SYNCED)
        path?.let { builder.path(it) }
        return builder.build()
    }

    private fun givenAFileResourceWithPhysicalFile(uid: String): Pair<FileResource, File> {
        val physicalFile = File.createTempFile(uid, ".txt", context.cacheDir).apply { writeText("content") }
        return givenAFileResource(uid, physicalFile.path) to physicalFile
    }
}
