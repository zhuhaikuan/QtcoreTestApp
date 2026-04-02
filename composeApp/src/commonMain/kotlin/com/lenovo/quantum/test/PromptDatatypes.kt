/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

// NOTE: synced with com.lenovo.quantum.core.datatypes.PromptDatatypes.kt

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonObject
enum class PromptInputTypes {
    TEXT,
    MARKDOWN,
    SHORT,
    ;

    override fun toString(): String {
        return when (this) {
            TEXT -> "text"
            MARKDOWN -> "markdown"
            SHORT -> "short"
        }
    }
}

enum class PromptOutputTypes {
    TEXT,
    TOOL,
    HANDLER,
    TIMINGS,
    ELICITATION,
    USER_DIRECT,  // Tool content with audience:["user"] - displayed directly to user
    TOPIC_SWITCH,  // Tool returned status=stopped with pending_user_question
    ;

    override fun toString(): String {
        return when (this) {
            TEXT -> "text"
            TOOL -> "tool"
            HANDLER -> "handler"
            TIMINGS -> "timings"
            ELICITATION -> "elicitation"
            USER_DIRECT -> "user_direct"
            TOPIC_SWITCH -> "topic_switch"
        }
    }
}

/**
 * Represents the input parameters for a prompt sent to a brain.
 *
 * @property handler The brain handler to be used.
 * @property query The user's input text.
 * @property location The user's location in readable format, e.g. `"Chicago, IL"`.
 * @property locale The user language/locale, e.g. `"en-US"` or `"pt-BR"`.
 * @property type The content type, such as `"text"` or `"markdown"`.
 * @property brainIntent Optional execution mode identifier:
 *                       - `null` (default): All tools available, save to history
 *                       - `"live"`: FKB-only tools (search_fkb, add_fkb, get_all_memories), save to history
 *                       - `"internal"`: All tools available, exclude from history (for system/tool execution queries)
 */
@Serializable
data class PromptInput(
    val handler: String,
    val query: String,
    val location: String? = null,
    val locale: String = "en-US",
    val type: String = "markdown",
    val brainIntent: String? = null,
)
/**
 * Represents the output returned by a brain after processing a prompt.
 *
 * @property type The output type; see [PromptOutputTypes].
 * @property response The full content of the response.
 * @property shortResponse A shorter version of the response for compact displays.
 * @property sources List of URLs used as sources by the brain.
 * @property searchUrls List of URLs generated during grounded search.
 * @property memoryCitations List of memory citations used by the brain.
 * @property usageInfo Billing and usage information for the operation.
 * @property metadata Additional metadata for specialized output types like elicitation.
 */
@Serializable
data class PromptOutput(
    val type: String,
    val response: String,
    val shortResponse: String = "",
    val sources: List<Citation> = listOf(),
    val searchUrls: List<Citation> = listOf(),
    val memoryCitations: List<MemoryCitation> = listOf(),
    val usageInfo: List<UsageInfo> = listOf(),
    val metadata: ElicitationMetadata? = null,
)

/**
 * Represents a source citation used by a brain response.
 *
 * @property title Title of the source content.
 * @property url URL of the source content.
 * @property snippet Brief excerpt of the source content.
 * @property source Source label included in the output, e.g. `"[3:0†source]"`.
 */
@Serializable
data class Citation(
    val title: String,
    val url: String,
    val snippet: String = "",
    val source: String = "",
)

/**
 * Represents a memory reference used in a brain response.
 *
 * @property operation The memory operation performed.
 * @property memoryId The unique identifier of the memory.
 * @property memoryContents The contents of the memory.
 */
@Serializable
data class MemoryCitation(
    val operation: String,
    val memoryId: String,
    val memoryContents: String,
)

/**
 * Provides usage and billing details for a specific feature.
 *
 * @property billingDateTime The billing timestamp, e.g. `"2025-10-11T03:20:03Z"`.
 * @property renewDateTime The renewal timestamp, e.g. `"2025-10-11T03:20:03Z"`.
 * @property consumption Current value of the consumption metric.
 * @property consumptionLimit Quota limit for the feature-specific metric.
 * @property exceeded True if the limit has been met or exceeded.
 */
@Serializable
data class UsageInfo(
    val billingDateTime: String,
    val renewDateTime: String,
    val consumption: Long,
    val consumptionLimit: Long,
    val exceeded: Boolean,
)

/**
 * Metadata included in elicitation notifications sent to UI.
 *
 * This contains the MCP server's requested schema and any special resources
 * like MCPUI definitions or URLs that need to be passed through to the UI.
 *
 * @property requestedSchema The JSON Schema from MCP server's elicitation/create request.
 *                           Example: `{"properties": {"value": {"type": "string"}}, "required": ["value"]}`
 * @property mcpUi Optional MCPUI resource definition for rich UI rendering (buttons, forms, etc.)
 * @property urls Map of URL/URI fields that may need special handling in the UI
 */
@Serializable
data class ElicitationMetadata(
    @SerialName("_requestedSchema")
    val requestedSchema: JsonObject? = null,
    @SerialName("MCPUI")
    val mcpUi: JsonObject? = null,
    val urls: Map<String, String>? = null
)

// ========== MCP Elicitation Data Types ==========

/**
 * Action types for routing requests in PromptFacade.
 * Updated to support MCP elicitation actions directly.
 */
enum class PromptAction {
    QUERY,
    ELICITATION_ACCEPT,    // User accepts elicitation and provides data
    ELICITATION_DECLINE,   // User explicitly declines to provide data
    ELICITATION_CANCEL;    // User cancels the elicitation operation

    override fun toString(): String = when (this) {
        QUERY -> "query"
        ELICITATION_ACCEPT -> "elicitation_accept"      // Keep wire format the same
        ELICITATION_DECLINE -> "elicitation_decline"
        ELICITATION_CANCEL -> "elicitation_cancel"
    }

    companion object {
        fun fromString(value: String): PromptAction? = when (value.lowercase()) {
            "query" -> QUERY
            "elicitation_accept" -> ELICITATION_ACCEPT
            "elicitation_decline" -> ELICITATION_DECLINE
            "elicitation_cancel" -> ELICITATION_CANCEL
            else -> null
        }
    }

    /**
     * Check if this action is an elicitation response type.
     */
    fun isElicitationResponse(): Boolean = this in listOf( ELICITATION_ACCEPT,
        ELICITATION_DECLINE,
        ELICITATION_CANCEL)
}

/**
 * Input structure for elicitation responses sent from UI to QtCore.
 *
 * ## UI Usage
 *
 * The UI should display 3 buttons for elicitation:
 * - **Accept** button: User provides input and accepts
 * - **Decline** button: User explicitly declines to provide input
 * - **Cancel** button: User cancels the operation
 *
 * **Accept (user clicks Accept button after entering text):**
 * ```json
 * {
 *   "action": "elicitation_accept",
 *   "content": "user_input_text"
 * }
 * ```
 *
 * **Decline (user clicks Decline button, optionally with reason):**
 * ```json
 * {
 *   "action": "elicitation_decline",
 *   "content": "optional reason for declining"
 * }
 * ```
 *
 * **Cancel (user clicks Cancel button, optionally with reason):**
 * ```json
 * {
 *   "action": "elicitation_cancel",
 *   "content": "optional cancellation reason"
 * }
 * ```
 *
 * **Example Flow:**
 *
 * UI receives notification:
 * ```json
 * {
 *   "type": "elicitation",
 *   "response": "Please provide your GitHub username",
 *   "metadata": {
 *     "_requestedSchema": {
 *       "properties": {"name": {"type": "string"}},
 *       "required": ["name"]
 *     }
 *   }
 * }
 * ```
 *
 * UI sends back (user accepted):
 * ```json
 * {
 *   "action": "elicitation_accept",
 *   "content": "octocat"
 * }
 * ```
 *
 * QtCore transforms this into schema-compliant response: `{"name": "octocat"}`
 * and sends it to the MCP server with CreateElicitationResult.Action.accept.
 *
 * @property action The elicitation action: "elicitation_accept", "elicitation_decline", or "elicitation_cancel"
 * @property content The user's input (required for accept, optional for decline/cancel)
 */
@Serializable
data class ElicitationResponseInput(
    val action: String,  // "elicitation_accept", "elicitation_decline", or "elicitation_cancel"
    val content: String? = null  // Required for accept, optional for decline/cancel
)