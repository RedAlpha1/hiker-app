package com.ridgeline.data

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Client-generated UUID keys (DECISIONS.md, "No backend, no accounts in
 * v1") -- generated here rather than left to the database, so an id is
 * stable before a row is ever persisted (e.g. a [Track] exists in memory
 * during recording before its first write).
 */
@OptIn(ExperimentalUuidApi::class)
fun newId(): String = Uuid.random().toString()
