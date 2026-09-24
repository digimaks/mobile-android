// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.util.securearea

import java.util.UUID

object KeyAliasGenerator {
    /**
     * Generates a unique alias for a new key.
     */
    fun generate(): String = "hardware_key_${UUID.randomUUID()}"
}
