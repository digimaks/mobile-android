// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.config

import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings.CredentialPolicy
import lv.zzdats.corelogic.model.DocumentIdentifier
import java.time.Duration

/**
 * Represents the configuration for document issuance.
 *
 * This class defines the rules for issuing documents, including a default rule and
 * specific rules for individual document types.
 *
 * @property defaultRule The default [DocumentIssuanceRule] to be applied when no specific rule
 *                       is defined for a document type.
 * @property documentSpecificRules A map where keys are [eu.europa.ec.corelogic.model.DocumentIdentifier]s and values are
 *                                  [DocumentIssuanceRule]s, defining specific rules for
 *                                  particular document types.
 * @property reissuanceRule defining rules for automated document re-issuance background operation.
 */
data class DocumentIssuanceConfig(
    val defaultRule: DocumentIssuanceRule,
    val documentSpecificRules: Map<DocumentIdentifier, DocumentIssuanceRule>,
    val reissuanceRule: ReIssuanceRule
) {

    /**
     * Retrieves the [DocumentIssuanceRule] for a given [DocumentIdentifier].
     *
     * If a specific rule is defined for the provided [documentIdentifier], that rule is returned.
     * Otherwise, the [defaultRule] is returned.
     *
     * @param documentIdentifier The identifier of the document for which to retrieve the rule.
     *                           If null, the [defaultRule] will be returned.
     * @return The [DocumentIssuanceRule] applicable to the given [documentIdentifier], or the
     *         [defaultRule] if no specific rule is found.
     */
    fun getRuleForDocument(documentIdentifier: DocumentIdentifier?): DocumentIssuanceRule =
        documentSpecificRules[documentIdentifier] ?: defaultRule
}

/**
 * Represents a rule for issuing a document.
 *
 * This class encapsulates the policy and the number of credentials associated with a document
 * issuance.
 *
 * @property policy The [CredentialPolicy] to be applied during document issuance.
 * @property numberOfCredentials The number of credentials to be issued for the document.
 */
data class DocumentIssuanceRule(
    val policy: CredentialPolicy,
    val numberOfCredentials: Int,
)

/**
 * Represents a rule for the re-issuance of a document
 *
 * @property minNumberOfCredentials Minimum number of instances remaining.
 * @property minExpirationHours Minimum number of hours remaining before expiration.
 * @property backgroundInterval Interval between background operations.
 */
data class ReIssuanceRule(
    val minNumberOfCredentials: Int,
    val minExpirationHours: Int,
    val backgroundInterval: Duration
)
