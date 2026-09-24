// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.extension

import lv.zzdats.corelogic.model.DomainClaim

/**
 * Recursively removes empty groups from a list of [DomainClaim].
 *
 * This function traverses the list of [DomainClaim] and filters out any [DomainClaim.Group]
 * that, after recursively filtering its items, becomes empty.
 *
 * @receiver The list of [DomainClaim] to filter.
 * @return A new list of [DomainClaim] with empty groups removed.
 *         Groups are considered empty if, after recursively filtering their items,
 *         they contain no items. Non-group claims are always kept.
 */
fun List<DomainClaim>.removeEmptyGroups(): List<DomainClaim> {
    return this.mapNotNull { claim ->
        when (claim) {
            is DomainClaim.Group -> {
                val filteredItems =
                    claim.items.removeEmptyGroups() // Recursively filter child groups
                if (filteredItems.isNotEmpty()) {
                    claim.copy(items = filteredItems) // Keep group if it has valid items
                } else {
                    null // Remove empty groups
                }
            }

            is DomainClaim.Primitive -> claim // Keep non-group claims (Primitive)
        }
    }
}

/**
 * Recursively sorts a list of [DomainClaim] based on the provided [selector].
 *
 * This function sorts the list of [DomainClaim] by applying the [selector] to each element.
 * For [DomainClaim.Group] elements, it recursively sorts the `items` within the group
 * before sorting the list at the current level. [DomainClaim.Primitive] elements are left unchanged.
 *
 * @param selector A function that extracts a [Comparable] value from a [DomainClaim] for sorting purposes.
 * @return A new list of [DomainClaim] sorted recursively according to the [selector].
 */
fun <T : Comparable<T>> List<DomainClaim>.sortRecursivelyBy(
    selector: (DomainClaim) -> T
): List<DomainClaim> {
    return this.map { claim ->
        when (claim) {
            is DomainClaim.Group -> claim.copy(
                items = claim.items.sortRecursivelyBy(selector) // Recursively sort children
            )

            is DomainClaim.Primitive -> claim // Primitives stay unchanged
        }
    }.sortedBy(selector) // Apply sorting at the current level
}