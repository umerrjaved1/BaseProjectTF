package com.professor.baseproject.remoteconfig

/**
 * The complete set of Firebase Remote Config parameters this app reads.
 *
 * Three keys, no more. Anything that is not an ad unit id, an ad/flow rule, or a native ad
 * colour is a compile-time constant in [com.professor.baseproject.constants.AppConfigDefaults]
 * instead - a remote key that is never flipped is a key that silently drifts from the code
 * that reads it.
 *
 * Matching JSON templates live in `remote_config_default/`; paste them straight into the
 * Firebase console.
 */
object RemoteConfigKeys {

    /** Per-placement AdMob unit ids. See `remote_config_default/ad_ids.json`. */
    const val AD_IDS = "ad_ids"

    /**
     * Ad frequency, splash variants, and the startup-flow skips.
     * See `remote_config_default/ad_rules.json`.
     */
    const val AD_RULES = "ad_rules"

    /** Native ad palette. See `remote_config_default/native_config.json`. */
    const val NATIVE_CONFIG = "native_config"
}
