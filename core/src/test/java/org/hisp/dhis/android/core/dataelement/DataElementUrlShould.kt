package org.hisp.dhis.android.core.dataelement

import com.google.common.truth.Truth.assertThat
import org.hisp.dhis.android.core.common.CoreObjectShould
import org.hisp.dhis.android.network.dataelement.DataElementDTO
import org.junit.Test

// EyeSeeTea customization - URL Data Element Field
internal class DataElementUrlShould : CoreObjectShould<DataElementDTO>(
    "dataelement/data_element_with_url.json",
    DataElementDTO.serializer(),
) {

    @Test
    override fun map_from_json_string() {
        val dataElement = deserialize().toDomain()

        assertThat(dataElement.url()).isEqualTo("https://dhis2.org/reference/anc-visit")
    }
}
